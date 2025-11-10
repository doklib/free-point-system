package com.musinsa.point.service;

import com.musinsa.point.domain.IdempotencyRecord;
import com.musinsa.point.domain.PointLedger;
import com.musinsa.point.domain.PointTransaction;
import com.musinsa.point.domain.TransactionType;
import com.musinsa.point.domain.UserPointSummary;
import com.musinsa.point.dto.CancelEarnRequest;
import com.musinsa.point.dto.CancelEarnResponse;
import com.musinsa.point.dto.CancelUseRequest;
import com.musinsa.point.dto.AvailablePointDetail;
import com.musinsa.point.dto.BalanceResponse;
import com.musinsa.point.dto.CancelUseResponse;
import com.musinsa.point.dto.EarnRequest;
import com.musinsa.point.dto.EarnResponse;
import com.musinsa.point.dto.HistoryResponse;
import com.musinsa.point.dto.NewlyEarnedPointDetail;
import com.musinsa.point.dto.PageInfo;
import com.musinsa.point.dto.RestoredPointDetail;
import com.musinsa.point.dto.TransactionDetail;
import com.musinsa.point.dto.UseRequest;
import com.musinsa.point.dto.UseResponse;
import com.musinsa.point.dto.UsedFromDetail;
import com.musinsa.point.exception.PointBusinessException;
import com.musinsa.point.repository.PointLedgerRepository;
import com.musinsa.point.repository.PointTransactionRepository;
import com.musinsa.point.repository.UserPointSummaryRepository;
import com.musinsa.point.util.PointKeyGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.OptimisticLockException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 포인트 비즈니스 로직을 처리하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PointService {

    private final PointTransactionRepository pointTransactionRepository;
    private final PointLedgerRepository pointLedgerRepository;
    private final UserPointSummaryRepository userPointSummaryRepository;
    private final IdempotencyService idempotencyService;
    private final ConfigService configService;
    private final ObjectMapper objectMapper;

    /**
     * 포인트 적립
     *
     * @param request 적립 요청
     * @param idempotencyKey 멱등성 키
     * @return 적립 응답
     */
    @Transactional
    @Retryable(
        retryFor = OptimisticLockException.class,
        maxAttempts = 3,
        backoff = @Backoff(delay = 100, multiplier = 2)
    )
    public EarnResponse earnPoints(EarnRequest request, String idempotencyKey) {
        String requestId = MDC.get("requestId");
        
        log.info("[{}] 포인트 적립 시작 - userId: {}, amount: {}, idempotencyKey: {}",
            requestId, request.getUserId(), request.getAmount(), idempotencyKey);

        try {
            // 1. 멱등성 검증 
            IdempotencyRecord existingRecord = idempotencyService.checkExisting(idempotencyKey);
            if (existingRecord != null) {
                log.info("[{}] 멱등성 레코드 발견 - 기존 응답 반환", requestId);
                return deserializeResponse(existingRecord.getResponseBody(), EarnResponse.class);
            }

            // 2. 금액 유효성 검증 
            validateAmount(request.getAmount());

            // 3. 1회 최대 적립 한도 검증 
            Long maxEarnPerTransaction = configService.getMaxEarnPerTransaction();
            if (request.getAmount() > maxEarnPerTransaction) {
                throw PointBusinessException.exceedMaxEarnLimit(request.getAmount(), maxEarnPerTransaction);
            }

            // 4. 만료일 유효성 검증 
            Integer expirationDays = validateAndGetExpirationDays(request.getExpirationDays());

            // 5. UserPointSummary 조회 또는 생성 
            UserPointSummary summary = userPointSummaryRepository.findByUserId(request.getUserId())
                .orElseGet(() -> createNewUserPointSummary(request.getUserId()));

            // 6. 개인별 최대 보유 한도 검증 
            Long maxBalancePerUser = configService.getMaxBalancePerUser();
            long newTotalBalance = summary.getTotalBalance() + request.getAmount();
            if (newTotalBalance > maxBalancePerUser) {
                throw PointBusinessException.exceedUserMaxBalance(
                    summary.getTotalBalance(), 
                    maxBalancePerUser, 
                    request.getAmount()
                );
            }

            // 7. PointTransaction 생성 
            String pointKey = PointKeyGenerator.generate();
            LocalDateTime expirationDate = LocalDateTime.now().plusDays(expirationDays);
            
            PointTransaction transaction = new PointTransaction();
            transaction.setPointKey(pointKey);
            transaction.setUserId(request.getUserId());
            transaction.setTransactionType(TransactionType.EARN);
            transaction.setAmount(request.getAmount());
            transaction.setAvailableBalance(request.getAmount());
            transaction.setIsManualGrant(request.getIsManualGrant());
            transaction.setExpirationDate(expirationDate);
            transaction.setDescription(request.getDescription());
            
            pointTransactionRepository.save(transaction);

            // 8. UserPointSummary 업데이트
            summary.setTotalBalance(newTotalBalance);
            userPointSummaryRepository.save(summary);

            // 9. 응답 생성
            EarnResponse response = EarnResponse.builder()
                .pointKey(pointKey)
                .userId(request.getUserId())
                .amount(request.getAmount())
                .availableBalance(request.getAmount())
                .totalBalance(newTotalBalance)
                .expirationDate(expirationDate)
                .isManualGrant(request.getIsManualGrant())
                .createdAt(transaction.getCreatedAt())
                .build();

            // 10. 멱등성 레코드 저장
            String responseBody = serializeResponse(response);
            idempotencyService.saveResponse(idempotencyKey, responseBody, 200);

            log.info("[{}] 포인트 적립 완료 - pointKey: {}, totalBalance: {}",
                requestId, pointKey, newTotalBalance);

            return response;

        } catch (PointBusinessException ex) {
            log.warn("[{}] 포인트 적립 실패 - errorCode: {}, message: {}",
                requestId, ex.getErrorCode(), ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("[{}] 포인트 적립 중 예상치 못한 오류 발생", requestId, ex);
            throw new RuntimeException("포인트 적립 처리 중 오류가 발생했습니다", ex);
        }
    }

    /**
     * 금액 유효성 검증 (최소값만 검증, 최대값은 ConfigService에서 조회한 한도로 검증)
     */
    private void validateAmount(Long amount) {
        if (amount == null || amount < 1) {
            throw PointBusinessException.invalidAmount(
                amount != null ? amount : 0,
                1L,
                null
            );
        }
    }

    /**
     * 만료일 유효성 검증 및 기본값 반환
     */
    private Integer validateAndGetExpirationDays(Integer expirationDays) {
        Integer minDays = configService.getMinExpirationDays();
        Integer maxDays = configService.getMaxExpirationDays();
        
        // 만료일이 지정되지 않은 경우 기본값 사용 
        if (expirationDays == null) {
            return configService.getDefaultExpirationDays();
        }
        
        // 만료일 범위 검증 
        if (expirationDays < minDays || expirationDays > maxDays) {
            throw PointBusinessException.invalidExpirationDays(expirationDays, minDays, maxDays);
        }
        
        return expirationDays;
    }

    /**
     * 새로운 UserPointSummary 생성
     */
    private UserPointSummary createNewUserPointSummary(String userId) {
        UserPointSummary summary = new UserPointSummary();
        summary.setUserId(userId);
        summary.setTotalBalance(0L);
        return summary;
    }

    /**
     * 응답 객체를 JSON 문자열로 직렬화
     */
    private String serializeResponse(Object response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            log.error("응답 직렬화 실패", e);
            throw new RuntimeException("응답 직렬화 중 오류가 발생했습니다", e);
        }
    }

    /**
     * JSON 문자열을 응답 객체로 역직렬화
     */
    private <T> T deserializeResponse(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("응답 역직렬화 실패", e);
            throw new RuntimeException("응답 역직렬화 중 오류가 발생했습니다", e);
        }
    }

    /**
     * 포인트 적립 취소
     *
     * @param request 적립 취소 요청
     * @param idempotencyKey 멱등성 키
     * @return 적립 취소 응답
     */
    @Transactional
    @Retryable(
        retryFor = OptimisticLockException.class,
        maxAttempts = 3,
        backoff = @Backoff(delay = 100, multiplier = 2)
    )
    public CancelEarnResponse cancelEarn(CancelEarnRequest request, String idempotencyKey) {
        String requestId = MDC.get("requestId");
        
        log.info("[{}] 포인트 적립 취소 시작 - pointKey: {}, idempotencyKey: {}",
            requestId, request.getPointKey(), idempotencyKey);

        try {
            // 1. 멱등성 검증 
            IdempotencyRecord existingRecord = idempotencyService.checkExisting(idempotencyKey);
            if (existingRecord != null) {
                log.info("[{}] 멱등성 레코드 발견 - 기존 응답 반환", requestId);
                return deserializeResponse(existingRecord.getResponseBody(), CancelEarnResponse.class);
            }

            // 2. PointTransaction 조회 
            PointTransaction originalTransaction = pointTransactionRepository.findByPointKey(request.getPointKey())
                .orElseThrow(() -> {
                    log.warn("[{}] 포인트 키를 찾을 수 없음 - pointKey: {}", requestId, request.getPointKey());
                    return PointBusinessException.pointKeyNotFound(request.getPointKey());
                });

            // 3. 사용 여부 검증 
            long usedAmount = originalTransaction.getAmount() - originalTransaction.getAvailableBalance();
            if (usedAmount > 0) {
                log.warn("[{}] 사용된 포인트 취소 시도 - pointKey: {}, usedAmount: {}", 
                    requestId, request.getPointKey(), usedAmount);
                throw PointBusinessException.cannotCancelUsedPoint(
                    request.getPointKey(),
                    usedAmount,
                    originalTransaction.getAmount()
                );
            }

            // 4. PointTransaction 생성 (CANCEL_EARN 타입)
            String cancelPointKey = PointKeyGenerator.generate();
            
            PointTransaction cancelTransaction = new PointTransaction();
            cancelTransaction.setPointKey(cancelPointKey);
            cancelTransaction.setUserId(originalTransaction.getUserId());
            cancelTransaction.setTransactionType(TransactionType.CANCEL_EARN);
            cancelTransaction.setAmount(originalTransaction.getAmount());
            cancelTransaction.setAvailableBalance(0L);
            cancelTransaction.setIsManualGrant(false);
            cancelTransaction.setReferencePointKey(request.getPointKey());
            cancelTransaction.setDescription(request.getReason());
            
            pointTransactionRepository.save(cancelTransaction);

            // 5. 원본 PointTransaction의 availableBalance를 0으로 업데이트 
            originalTransaction.setAvailableBalance(0L);
            pointTransactionRepository.save(originalTransaction);

            // 6. UserPointSummary 업데이트 (잔액 감소)
            UserPointSummary summary = userPointSummaryRepository.findByUserId(originalTransaction.getUserId())
                .orElseThrow(() -> new RuntimeException("사용자 포인트 요약을 찾을 수 없습니다"));
            
            long newTotalBalance = summary.getTotalBalance() - originalTransaction.getAmount();
            summary.setTotalBalance(newTotalBalance);
            userPointSummaryRepository.save(summary);

            // 7. 응답 생성
            CancelEarnResponse response = CancelEarnResponse.builder()
                .cancelPointKey(cancelPointKey)
                .originalPointKey(request.getPointKey())
                .canceledAmount(originalTransaction.getAmount())
                .totalBalance(newTotalBalance)
                .canceledAt(cancelTransaction.getCreatedAt())
                .build();

            // 8. 멱등성 레코드 저장 
            String responseBody = serializeResponse(response);
            idempotencyService.saveResponse(idempotencyKey, responseBody, 200);

            log.info("[{}] 포인트 적립 취소 완료 - cancelPointKey: {}, canceledAmount: {}, totalBalance: {}",
                requestId, cancelPointKey, originalTransaction.getAmount(), newTotalBalance);

            return response;

        } catch (PointBusinessException ex) {
            log.warn("[{}] 포인트 적립 취소 실패 - errorCode: {}, message: {}",
                requestId, ex.getErrorCode(), ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("[{}] 포인트 적립 취소 중 예상치 못한 오류 발생", requestId, ex);
            throw new RuntimeException("포인트 적립 취소 처리 중 오류가 발생했습니다", ex);
        }
    }

    /**
     * 포인트 사용
     *
     * @param request 사용 요청
     * @param idempotencyKey 멱등성 키
     * @return 사용 응답
     */
    @Transactional
    @Retryable(
        retryFor = OptimisticLockException.class,
        maxAttempts = 3,
        backoff = @Backoff(delay = 100, multiplier = 2)
    )
    public UseResponse usePoints(UseRequest request, String idempotencyKey) {
        String requestId = MDC.get("requestId");
        
        log.info("[{}] 포인트 사용 시작 - userId: {}, orderNumber: {}, amount: {}, idempotencyKey: {}",
            requestId, request.getUserId(), request.getOrderNumber(), request.getAmount(), idempotencyKey);

        try {
            // 1. 멱등성 검증
            IdempotencyRecord existingRecord = idempotencyService.checkExisting(idempotencyKey);
            if (existingRecord != null) {
                log.info("[{}] 멱등성 레코드 발견 - 기존 응답 반환", requestId);
                return deserializeResponse(existingRecord.getResponseBody(), UseResponse.class);
            }

            // 2. UserPointSummary 조회 (낙관적 잠금)
            UserPointSummary summary = userPointSummaryRepository.findByUserId(request.getUserId())
                .orElseThrow(() -> {
                    log.warn("[{}] 사용자 포인트 요약을 찾을 수 없음 - userId: {}", requestId, request.getUserId());
                    return PointBusinessException.insufficientBalance(0L, request.getAmount());
                });

            // 3. 잔액 검증
            if (summary.getTotalBalance() < request.getAmount()) {
                log.warn("[{}] 사용 가능 포인트 부족 - availableBalance: {}, requestedAmount: {}",
                    requestId, summary.getTotalBalance(), request.getAmount());
                throw PointBusinessException.insufficientBalance(summary.getTotalBalance(), request.getAmount());
            }

            // 4. 사용 가능한 포인트 조회 (수기 지급 우선, 만료일 순)
            LocalDateTime now = LocalDateTime.now();
            List<PointTransaction> availablePoints = pointTransactionRepository.findAvailablePointsForUse(
                request.getUserId(), 
                now
            );

            if (availablePoints.isEmpty()) {
                log.warn("[{}] 사용 가능한 포인트가 없음 - userId: {}", requestId, request.getUserId());
                throw PointBusinessException.insufficientBalance(0L, request.getAmount());
            }

            // 5. 포인트 차감 로직 구현 (여러 적립에서 순차적으로 차감)
            long remainingAmount = request.getAmount();
            List<UsedFromDetail> usedFromDetails = new ArrayList<>();
            
            for (PointTransaction earnTransaction : availablePoints) {
                if (remainingAmount <= 0) {
                    break;
                }

                long availableBalance = earnTransaction.getAvailableBalance();
                long amountToUse = Math.min(remainingAmount, availableBalance);

                // 적립 트랜잭션의 availableBalance 업데이트
                earnTransaction.setAvailableBalance(availableBalance - amountToUse);
                pointTransactionRepository.save(earnTransaction);

                // UsedFromDetail 추가
                usedFromDetails.add(UsedFromDetail.builder()
                    .earnPointKey(earnTransaction.getPointKey())
                    .usedAmount(amountToUse)
                    .build());

                remainingAmount -= amountToUse;

                log.debug("[{}] 포인트 차감 - earnPointKey: {}, usedAmount: {}, remainingAvailable: {}",
                    requestId, earnTransaction.getPointKey(), amountToUse, earnTransaction.getAvailableBalance());
            }

            // 차감 후에도 남은 금액이 있다면 오류 (이론적으로는 발생하지 않아야 함)
            if (remainingAmount > 0) {
                log.error("[{}] 포인트 차감 실패 - 남은 금액: {}", requestId, remainingAmount);
                throw PointBusinessException.insufficientBalance(
                    summary.getTotalBalance() - remainingAmount, 
                    request.getAmount()
                );
            }

            // 6. PointTransaction 생성 (USE 타입, orderNumber 설정)
            String usePointKey = PointKeyGenerator.generate();
            
            PointTransaction useTransaction = new PointTransaction();
            useTransaction.setPointKey(usePointKey);
            useTransaction.setUserId(request.getUserId());
            useTransaction.setTransactionType(TransactionType.USE);
            useTransaction.setAmount(request.getAmount());
            useTransaction.setAvailableBalance(0L);
            useTransaction.setIsManualGrant(false);
            useTransaction.setOrderNumber(request.getOrderNumber());
            useTransaction.setDescription("주문 " + request.getOrderNumber() + "에서 포인트 사용");
            
            pointTransactionRepository.save(useTransaction);

            // 7. PointLedger 생성 (각 적립별 사용 금액 기록)
            for (UsedFromDetail detail : usedFromDetails) {
                PointLedger ledger = new PointLedger(
                    usePointKey,
                    detail.getEarnPointKey(),
                    detail.getUsedAmount(),
                    0L
                );
                
                pointLedgerRepository.save(ledger);

                log.debug("[{}] PointLedger 생성 - usePointKey: {}, earnPointKey: {}, usedAmount: {}",
                    requestId, usePointKey, detail.getEarnPointKey(), detail.getUsedAmount());
            }

            // 8. UserPointSummary 업데이트 (잔액 감소)
            long newTotalBalance = summary.getTotalBalance() - request.getAmount();
            summary.setTotalBalance(newTotalBalance);
            userPointSummaryRepository.save(summary);

            // 9. 응답 생성
            UseResponse response = UseResponse.builder()
                .usePointKey(usePointKey)
                .userId(request.getUserId())
                .orderNumber(request.getOrderNumber())
                .usedAmount(request.getAmount())
                .remainingBalance(newTotalBalance)
                .usedFrom(usedFromDetails)
                .usedAt(useTransaction.getCreatedAt())
                .build();

            // 10. 멱등성 레코드 저장
            String responseBody = serializeResponse(response);
            idempotencyService.saveResponse(idempotencyKey, responseBody, 200);

            log.info("[{}] 포인트 사용 완료 - usePointKey: {}, usedAmount: {}, remainingBalance: {}, usedFromCount: {}",
                requestId, usePointKey, request.getAmount(), newTotalBalance, usedFromDetails.size());

            return response;

        } catch (PointBusinessException ex) {
            log.warn("[{}] 포인트 사용 실패 - errorCode: {}, message: {}",
                requestId, ex.getErrorCode(), ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("[{}] 포인트 사용 중 예상치 못한 오류 발생", requestId, ex);
            throw new RuntimeException("포인트 사용 처리 중 오류가 발생했습니다", ex);
        }
    }

    /**
     * 포인트 사용 취소
     *
     * @param request 사용 취소 요청
     * @param idempotencyKey 멱등성 키
     * @return 사용 취소 응답
     */
    @Transactional
    @Retryable(
        retryFor = OptimisticLockException.class,
        maxAttempts = 3,
        backoff = @Backoff(delay = 100, multiplier = 2)
    )
    public CancelUseResponse cancelUse(CancelUseRequest request, String idempotencyKey) {
        String requestId = MDC.get("requestId");
        
        log.info("[{}] 포인트 사용 취소 시작 - usePointKey: {}, amount: {}, idempotencyKey: {}",
            requestId, request.getUsePointKey(), request.getAmount(), idempotencyKey);

        try {
            // 1. 멱등성 검증
            IdempotencyRecord existingRecord = idempotencyService.checkExisting(idempotencyKey);
            if (existingRecord != null) {
                log.info("[{}] 멱등성 레코드 발견 - 기존 응답 반환", requestId);
                return deserializeResponse(existingRecord.getResponseBody(), CancelUseResponse.class);
            }

            // 2. 사용 PointTransaction 조회 (usePointKey로)
            PointTransaction useTransaction = pointTransactionRepository.findByPointKey(request.getUsePointKey())
                .orElseThrow(() -> {
                    log.warn("[{}] 사용 포인트 키를 찾을 수 없음 - usePointKey: {}", requestId, request.getUsePointKey());
                    return PointBusinessException.pointKeyNotFound(request.getUsePointKey());
                });

            // 3. 사용 트랜잭션 타입 검증
            if (useTransaction.getTransactionType() != TransactionType.USE) {
                log.warn("[{}] 사용 트랜잭션이 아님 - pointKey: {}, type: {}", 
                    requestId, request.getUsePointKey(), useTransaction.getTransactionType());
                throw PointBusinessException.pointKeyNotFound(request.getUsePointKey());
            }

            // 4. PointLedger 조회 (usePointKey로)
            List<PointLedger> ledgers = pointLedgerRepository.findByUsePointKey(request.getUsePointKey());
            
            if (ledgers.isEmpty()) {
                log.error("[{}] PointLedger를 찾을 수 없음 - usePointKey: {}", requestId, request.getUsePointKey());
                throw new RuntimeException("포인트 사용 내역을 찾을 수 없습니다");
            }

            // 5. 취소 가능 금액 검증 (이미 취소된 금액 고려)
            long totalCanceledAmount = ledgers.stream()
                .mapToLong(PointLedger::getCanceledAmount)
                .sum();
            
            long originalUseAmount = useTransaction.getAmount();
            long availableToCancelAmount = originalUseAmount - totalCanceledAmount;
            
            if (request.getAmount() > availableToCancelAmount) {
                log.warn("[{}] 취소 가능 금액 초과 - originalAmount: {}, alreadyCanceled: {}, requested: {}",
                    requestId, originalUseAmount, totalCanceledAmount, request.getAmount());
                throw PointBusinessException.exceedOriginalUseAmount(
                    originalUseAmount,
                    request.getAmount(),
                    totalCanceledAmount
                );
            }

            // 6. 각 PointLedger를 처리: 만료된 포인트를 먼저 처리한 후, 만료되지 않은 포인트를 역순으로 처리
            LocalDateTime now = LocalDateTime.now();
            long remainingCancelAmount = request.getAmount();
            List<RestoredPointDetail> restoredPoints = new ArrayList<>();
            List<NewlyEarnedPointDetail> newlyEarnedPoints = new ArrayList<>();
            
            // 6-1. 먼저 만료된 포인트들을 처리 (전체 금액 복구)
            for (int i = 0; i < ledgers.size() && remainingCancelAmount > 0; i++) {
                PointLedger ledger = ledgers.get(i);
                
                // 이 ledger에서 취소 가능한 금액 계산
                long usedInThisLedger = ledger.getUsedAmount();
                long alreadyCanceledInThisLedger = ledger.getCanceledAmount();
                long availableToCancel = usedInThisLedger - alreadyCanceledInThisLedger;
                
                if (availableToCancel <= 0) {
                    continue;
                }
                
                // 원본 적립 PointTransaction 조회
                PointTransaction earnTransaction = pointTransactionRepository.findByPointKey(ledger.getEarnPointKey())
                    .orElseThrow(() -> {
                        log.error("[{}] 원본 적립 트랜잭션을 찾을 수 없음 - earnPointKey: {}", 
                            requestId, ledger.getEarnPointKey());
                        return new RuntimeException("원본 적립 트랜잭션을 찾을 수 없습니다");
                    });

                // 만료일 확인
                boolean isExpired = earnTransaction.getExpirationDate() != null 
                    && earnTransaction.getExpirationDate().isBefore(now);
                
                // 만료된 포인트만 처리
                if (isExpired) {
                    long amountToCancelFromThisLedger = Math.min(remainingCancelAmount, availableToCancel);
                    
                    // 만료된 경우: 원본 적립 금액 전체를 신규 PointTransaction으로 생성
                    long originalEarnAmount = earnTransaction.getAmount();
                    
                    String newPointKey = PointKeyGenerator.generate();
                    Integer defaultExpirationDays = configService.getDefaultExpirationDays();
                    LocalDateTime newExpirationDate = now.plusDays(defaultExpirationDays);
                    
                    PointTransaction newEarnTransaction = new PointTransaction();
                    newEarnTransaction.setPointKey(newPointKey);
                    newEarnTransaction.setUserId(useTransaction.getUserId());
                    newEarnTransaction.setTransactionType(TransactionType.EARN);
                    newEarnTransaction.setAmount(originalEarnAmount);
                    newEarnTransaction.setAvailableBalance(originalEarnAmount);
                    newEarnTransaction.setIsManualGrant(false);
                    newEarnTransaction.setExpirationDate(newExpirationDate);
                    newEarnTransaction.setDescription(
                        String.format("사용 취소로 인한 신규 적립 (원본: %s, 만료됨)", ledger.getEarnPointKey())
                    );
                    
                    pointTransactionRepository.save(newEarnTransaction);
                    
                    newlyEarnedPoints.add(NewlyEarnedPointDetail.builder()
                        .pointKey(newPointKey)
                        .amount(originalEarnAmount)
                        .expirationDate(newExpirationDate)
                        .build());
                    
                    // PointLedger의 canceledAmount 증가
                    ledger.setCanceledAmount(alreadyCanceledInThisLedger + amountToCancelFromThisLedger);
                    pointLedgerRepository.save(ledger);
                    
                    remainingCancelAmount -= amountToCancelFromThisLedger;
                    
                    log.info("[{}] 만료된 포인트 신규 적립 - originalEarnKey: {}, newPointKey: {}, originalAmount: {}, cancelingAmount: {}",
                        requestId, ledger.getEarnPointKey(), newPointKey, originalEarnAmount, amountToCancelFromThisLedger);
                }
            }
            
            // 6-2. 만료되지 않은 포인트들을 역순으로 처리 (부분 복구)
            for (int i = ledgers.size() - 1; i >= 0 && remainingCancelAmount > 0; i--) {
                PointLedger ledger = ledgers.get(i);
                
                // 이 ledger에서 취소 가능한 금액 계산
                long usedInThisLedger = ledger.getUsedAmount();
                long alreadyCanceledInThisLedger = ledger.getCanceledAmount();
                long availableToCancel = usedInThisLedger - alreadyCanceledInThisLedger;
                
                if (availableToCancel <= 0) {
                    continue;
                }
                
                // 원본 적립 PointTransaction 조회
                PointTransaction earnTransaction = pointTransactionRepository.findByPointKey(ledger.getEarnPointKey())
                    .orElseThrow(() -> {
                        log.error("[{}] 원본 적립 트랜잭션을 찾을 수 없음 - earnPointKey: {}", 
                            requestId, ledger.getEarnPointKey());
                        return new RuntimeException("원본 적립 트랜잭션을 찾을 수 없습니다");
                    });

                // 만료일 확인
                boolean isExpired = earnTransaction.getExpirationDate() != null 
                    && earnTransaction.getExpirationDate().isBefore(now);
                
                // 만료된 포인트는 이미 처리했으므로 건너뜀
                if (isExpired) {
                    continue;
                }
                
                long amountToCancelFromThisLedger = Math.min(remainingCancelAmount, availableToCancel);
                
                // 만료되지 않은 경우: availableBalance 증가
                long newAvailableBalance = earnTransaction.getAvailableBalance() + amountToCancelFromThisLedger;
                earnTransaction.setAvailableBalance(newAvailableBalance);
                pointTransactionRepository.save(earnTransaction);
                
                // PointLedger의 canceledAmount 증가
                ledger.setCanceledAmount(alreadyCanceledInThisLedger + amountToCancelFromThisLedger);
                pointLedgerRepository.save(ledger);
                
                // restoredPoints에 추가
                restoredPoints.add(RestoredPointDetail.builder()
                    .earnPointKey(ledger.getEarnPointKey())
                    .restoredAmount(amountToCancelFromThisLedger)
                    .isExpired(false)
                    .build());
                
                remainingCancelAmount -= amountToCancelFromThisLedger;
                
                log.info("[{}] 포인트 복구 - earnPointKey: {}, restoredAmount: {}, newAvailableBalance: {}",
                    requestId, ledger.getEarnPointKey(), amountToCancelFromThisLedger, newAvailableBalance);
            }

            // 이론적으로는 발생하지 않아야 하지만, 안전장치
            if (remainingCancelAmount > 0) {
                log.error("[{}] 취소 처리 실패 - 남은 금액: {}", requestId, remainingCancelAmount);
                throw new RuntimeException("포인트 사용 취소 처리 중 오류가 발생했습니다");
            }

            // 12. PointTransaction 생성 (CANCEL_USE 타입, referencePointKey 설정)
            String cancelUsePointKey = PointKeyGenerator.generate();
            
            PointTransaction cancelUseTransaction = new PointTransaction();
            cancelUseTransaction.setPointKey(cancelUsePointKey);
            cancelUseTransaction.setUserId(useTransaction.getUserId());
            cancelUseTransaction.setTransactionType(TransactionType.CANCEL_USE);
            cancelUseTransaction.setAmount(request.getAmount());
            cancelUseTransaction.setAvailableBalance(0L);
            cancelUseTransaction.setIsManualGrant(false);
            cancelUseTransaction.setReferencePointKey(request.getUsePointKey());
            cancelUseTransaction.setDescription(request.getReason());
            
            pointTransactionRepository.save(cancelUseTransaction);

            // 13. UserPointSummary 업데이트 (잔액 증가)
            UserPointSummary summary = userPointSummaryRepository.findByUserId(useTransaction.getUserId())
                .orElseThrow(() -> {
                    log.error("[{}] 사용자 포인트 요약을 찾을 수 없음 - userId: {}", 
                        requestId, useTransaction.getUserId());
                    return new RuntimeException("사용자 포인트 요약을 찾을 수 없습니다");
                });
            
            long newTotalBalance = summary.getTotalBalance() + request.getAmount();
            summary.setTotalBalance(newTotalBalance);
            userPointSummaryRepository.save(summary);

            // 14. 응답 생성
            CancelUseResponse response = CancelUseResponse.builder()
                .cancelUsePointKey(cancelUsePointKey)
                .originalUsePointKey(request.getUsePointKey())
                .canceledAmount(request.getAmount())
                .totalBalance(newTotalBalance)
                .restoredPoints(restoredPoints)
                .newlyEarnedPoints(newlyEarnedPoints)
                .canceledAt(cancelUseTransaction.getCreatedAt())
                .build();

            // 15. 멱등성 레코드 저장
            String responseBody = serializeResponse(response);
            idempotencyService.saveResponse(idempotencyKey, responseBody, 200);

            log.info("[{}] 포인트 사용 취소 완료 - cancelUsePointKey: {}, canceledAmount: {}, totalBalance: {}, restoredCount: {}, newlyEarnedCount: {}",
                requestId, cancelUsePointKey, request.getAmount(), newTotalBalance, 
                restoredPoints.size(), newlyEarnedPoints.size());

            return response;

        } catch (PointBusinessException ex) {
            log.warn("[{}] 포인트 사용 취소 실패 - errorCode: {}, message: {}",
                requestId, ex.getErrorCode(), ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("[{}] 포인트 사용 취소 중 예상치 못한 오류 발생", requestId, ex);
            throw new RuntimeException("포인트 사용 취소 처리 중 오류가 발생했습니다", ex);
        }
    }

    /**
     * 포인트 잔액 조회
     *
     * @param userId 사용자 ID
     * @return 잔액 응답
     */
    @Transactional(readOnly = true)
    public BalanceResponse getBalance(String userId) {
        String requestId = MDC.get("requestId");
        
        log.info("[{}] 포인트 잔액 조회 시작 - userId: {}", requestId, userId);

        try {
            // 1. UserPointSummary 조회
            UserPointSummary summary = userPointSummaryRepository.findByUserId(userId)
                .orElse(createNewUserPointSummary(userId));

            // 2. 사용 가능한 PointTransaction 목록 조회 (만료되지 않고 availableBalance > 0)
            LocalDateTime now = LocalDateTime.now();
            List<PointTransaction> availableTransactions = pointTransactionRepository.findAvailablePointsForUse(
                userId, 
                now
            );

            // 3. AvailablePointDetail 목록 생성
            List<AvailablePointDetail> availablePoints = availableTransactions.stream()
                .map(transaction -> AvailablePointDetail.builder()
                    .pointKey(transaction.getPointKey())
                    .amount(transaction.getAmount())
                    .availableBalance(transaction.getAvailableBalance())
                    .isManualGrant(transaction.getIsManualGrant())
                    .expirationDate(transaction.getExpirationDate())
                    .build())
                .collect(Collectors.toList());

            // 4. BalanceResponse 생성
            BalanceResponse response = BalanceResponse.builder()
                .userId(userId)
                .totalBalance(summary.getTotalBalance())
                .availablePoints(availablePoints)
                .build();

            log.info("[{}] 포인트 잔액 조회 완료 - userId: {}, totalBalance: {}, availablePointsCount: {}",
                requestId, userId, summary.getTotalBalance(), availablePoints.size());

            return response;

        } catch (Exception ex) {
            log.error("[{}] 포인트 잔액 조회 중 예상치 못한 오류 발생", requestId, ex);
            throw new RuntimeException("포인트 잔액 조회 중 오류가 발생했습니다", ex);
        }
    }

    /**
     * 포인트 이력 조회
     *
     * @param userId 사용자 ID
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @return 이력 응답
     */
    @Transactional(readOnly = true)
    public HistoryResponse getHistory(String userId, int page, int size) {
        String requestId = MDC.get("requestId");
        
        log.info("[{}] 포인트 이력 조회 시작 - userId: {}, page: {}, size: {}", 
            requestId, userId, page, size);

        try {
            // 1. PointTransaction 목록 조회 (userId로, 페이징, 최신순 정렬)
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<PointTransaction> transactionPage = pointTransactionRepository.findByUserId(userId, pageable);

            // 2. TransactionDetail 목록 생성
            List<TransactionDetail> transactions = transactionPage.getContent().stream()
                .map(transaction -> TransactionDetail.builder()
                    .pointKey(transaction.getPointKey())
                    .type(transaction.getTransactionType())
                    .amount(transaction.getAmount())
                    .balance(transaction.getAvailableBalance())
                    .orderNumber(transaction.getOrderNumber())
                    .description(transaction.getDescription())
                    .createdAt(transaction.getCreatedAt())
                    .build())
                .collect(Collectors.toList());

            // 3. PageInfo 생성
            PageInfo pageInfo = PageInfo.builder()
                .number(transactionPage.getNumber())
                .size(transactionPage.getSize())
                .totalElements(transactionPage.getTotalElements())
                .totalPages(transactionPage.getTotalPages())
                .build();

            // 4. HistoryResponse 생성
            HistoryResponse response = HistoryResponse.builder()
                .userId(userId)
                .transactions(transactions)
                .page(pageInfo)
                .build();

            log.info("[{}] 포인트 이력 조회 완료 - userId: {}, transactionCount: {}, totalElements: {}",
                requestId, userId, transactions.size(), transactionPage.getTotalElements());

            return response;

        } catch (Exception ex) {
            log.error("[{}] 포인트 이력 조회 중 예상치 못한 오류 발생", requestId, ex);
            throw new RuntimeException("포인트 이력 조회 중 오류가 발생했습니다", ex);
        }
    }
}
