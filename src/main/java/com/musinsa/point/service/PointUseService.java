package com.musinsa.point.service;

import com.musinsa.point.domain.IdempotencyRecord;
import com.musinsa.point.domain.PointAccount;
import com.musinsa.point.domain.PointTransaction;
import com.musinsa.point.domain.TransactionType;
import com.musinsa.point.domain.UserPointSummary;
import com.musinsa.point.dto.CancelUseRequest;
import com.musinsa.point.dto.CancelUseResponse;
import com.musinsa.point.dto.NewlyEarnedPointDetail;
import com.musinsa.point.dto.RestoredPointDetail;
import com.musinsa.point.dto.UseRequest;
import com.musinsa.point.dto.UseResponse;
import com.musinsa.point.dto.UsedFromDetail;
import com.musinsa.point.exception.PointBusinessException;
import com.musinsa.point.repository.PointAccountRepository;
import com.musinsa.point.repository.PointTransactionRepository;
import com.musinsa.point.repository.UserPointSummaryRepository;
import com.musinsa.point.util.PointKeyGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.OptimisticLockException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 포인트 사용 서비스
 * 포인트 사용 및 사용 취소를 담당
 */
@Service
public class PointUseService {

    private static final Logger log = LoggerFactory.getLogger(PointUseService.class);

    private final PointTransactionRepository pointTransactionRepository;
    private final PointAccountRepository pointAccountRepository;
    private final UserPointSummaryRepository userPointSummaryRepository;
    private final IdempotencyService idempotencyService;
    private final ConfigService configService;
    private final ObjectMapper objectMapper;

    public PointUseService(PointTransactionRepository pointTransactionRepository,
                          PointAccountRepository pointAccountRepository,
                          UserPointSummaryRepository userPointSummaryRepository,
                          IdempotencyService idempotencyService,
                          ConfigService configService,
                          ObjectMapper objectMapper) {
        this.pointTransactionRepository = pointTransactionRepository;
        this.pointAccountRepository = pointAccountRepository;
        this.userPointSummaryRepository = userPointSummaryRepository;
        this.idempotencyService = idempotencyService;
        this.configService = configService;
        this.objectMapper = objectMapper;
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

            // 7. PointAccount 생성 (각 적립별 사용 금액 기록)
            for (UsedFromDetail detail : usedFromDetails) {
                PointAccount account = new PointAccount(
                    usePointKey,
                    detail.earnPointKey(),
                    detail.usedAmount(),
                    0L
                );
                
                pointAccountRepository.save(account);

                log.debug("[{}] PointAccount 생성 - usePointKey: {}, earnPointKey: {}, usedAmount: {}",
                    requestId, usePointKey, detail.earnPointKey(), detail.usedAmount());
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
        
        log.info("[{}] 포인트 사용 취소 시작 - orderNumber: {}, amount: {}, idempotencyKey: {}",
            requestId, request.getOrderNumber(), request.getAmount(), idempotencyKey);

        try {
            // 1. 멱등성 검증
            IdempotencyRecord existingRecord = idempotencyService.checkExisting(idempotencyKey);
            if (existingRecord != null) {
                log.info("[{}] 멱등성 레코드 발견 - 기존 응답 반환", requestId);
                return deserializeResponse(existingRecord.getResponseBody(), CancelUseResponse.class);
            }

            // 2. 사용 PointTransaction 조회 (orderNumber로)
            PointTransaction useTransaction = pointTransactionRepository.findByOrderNumber(request.getOrderNumber())
                .orElseThrow(() -> {
                    log.warn("[{}] 주문 번호를 찾을 수 없음 - orderNumber: {}", requestId, request.getOrderNumber());
                    return PointBusinessException.orderNumberNotFound(request.getOrderNumber());
                });

            // 3. 사용 트랜잭션 타입 검증
            if (useTransaction.getTransactionType() != TransactionType.USE) {
                log.warn("[{}] 사용 트랜잭션이 아님 - orderNumber: {}, type: {}", 
                    requestId, request.getOrderNumber(), useTransaction.getTransactionType());
                throw PointBusinessException.orderNumberNotFound(request.getOrderNumber());
            }

            // 3-1. UserPointSummary 락 획득 (동시성 제어)
            UserPointSummary summary = userPointSummaryRepository.findByUserIdWithLock(useTransaction.getUserId())
                .orElseThrow(() -> {
                    log.error("[{}] UserPointSummary를 찾을 수 없음 - userId: {}", requestId, useTransaction.getUserId());
                    return new RuntimeException("사용자 포인트 정보를 찾을 수 없습니다");
                });

            // 4. PointAccount 조회 (usePointKey로)
            List<PointAccount> accounts = pointAccountRepository.findByUsePointKey(useTransaction.getPointKey());
            
            if (accounts.isEmpty()) {
                log.error("[{}] PointAccount를 찾을 수 없음 - orderNumber: {}, usePointKey: {}", 
                    requestId, request.getOrderNumber(), useTransaction.getPointKey());
                throw new RuntimeException("포인트 사용 내역을 찾을 수 없습니다");
            }

            // 5. 취소 가능 금액 검증 (이미 취소된 금액 고려)
            long totalCanceledAmount = accounts.stream()
                .mapToLong(PointAccount::getCanceledAmount)
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

            // 6. CANCEL_USE 트랜잭션 먼저 생성 (pointKey 순서 보장)
            String cancelUsePointKey = PointKeyGenerator.generate();
            
            PointTransaction cancelUseTransaction = new PointTransaction();
            cancelUseTransaction.setPointKey(cancelUsePointKey);
            cancelUseTransaction.setUserId(useTransaction.getUserId());
            cancelUseTransaction.setTransactionType(TransactionType.CANCEL_USE);
            cancelUseTransaction.setAmount(request.getAmount());
            cancelUseTransaction.setAvailableBalance(0L);
            cancelUseTransaction.setIsManualGrant(false);
            cancelUseTransaction.setReferencePointKey(useTransaction.getPointKey());
            cancelUseTransaction.setDescription(request.getReason());
            
            pointTransactionRepository.save(cancelUseTransaction);

            // 7. 각 PointAccount를 처리: 만료된 포인트를 먼저 처리한 후, 만료되지 않은 포인트를 역순으로 처리
            LocalDateTime now = LocalDateTime.now();
            CancelResult cancelResult = processCancelAccounts(
                accounts, 
                request.getAmount(), 
                now, 
                useTransaction.getUserId(), 
                requestId
            );

            // 이론적으로는 발생하지 않아야 하지만, 안전장치
            if (cancelResult.remainingAmount() > 0) {
                log.error("[{}] 취소 처리 실패 - 남은 금액: {}", requestId, cancelResult.remainingAmount());
                throw new RuntimeException("포인트 사용 취소 처리 중 오류가 발생했습니다");
            }

            // 8. UserPointSummary 업데이트 (잔액 증가) - 이미 락으로 조회한 summary 사용
            long newTotalBalance = summary.getTotalBalance() + request.getAmount();
            summary.setTotalBalance(newTotalBalance);
            userPointSummaryRepository.save(summary);

            // 9. 응답 생성
            CancelUseResponse response = CancelUseResponse.builder()
                .cancelUsePointKey(cancelUsePointKey)
                .originalUsePointKey(useTransaction.getPointKey())
                .canceledAmount(request.getAmount())
                .totalBalance(newTotalBalance)
                .restoredPoints(cancelResult.restoredPoints())
                .newlyEarnedPoints(cancelResult.newlyEarnedPoints())
                .canceledAt(cancelUseTransaction.getCreatedAt())
                .build();

            // 10. 멱등성 레코드 저장
            String responseBody = serializeResponse(response);
            idempotencyService.saveResponse(idempotencyKey, responseBody, 200);

            log.info("[{}] 포인트 사용 취소 완료 - cancelUsePointKey: {}, canceledAmount: {}, totalBalance: {}, restoredCount: {}, newlyEarnedCount: {}",
                requestId, cancelUsePointKey, request.getAmount(), newTotalBalance, 
                cancelResult.restoredPoints().size(), cancelResult.newlyEarnedPoints().size());

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
     * 취소 처리 결과를 담는 내부 record
     */
    private record CancelResult(
        List<RestoredPointDetail> restoredPoints,
        List<NewlyEarnedPointDetail> newlyEarnedPoints,
        long remainingAmount
    ) {}

    /**
     * 포인트 사용 취소 시 계정별 처리
     * 만료된 포인트는 신규 적립, 만료되지 않은 포인트는 복구
     */
    private CancelResult processCancelAccounts(
        List<PointAccount> accounts,
        long cancelAmount,
        LocalDateTime now,
        String userId,
        String requestId
    ) {
        List<RestoredPointDetail> restoredPoints = new ArrayList<>();
        List<NewlyEarnedPointDetail> newlyEarnedPoints = new ArrayList<>();
        long remainingAmount = cancelAmount;

        // 1. 만료된 포인트 처리 (신규 적립)
        remainingAmount = processExpiredPoints(
            accounts, remainingAmount, now, userId, requestId, newlyEarnedPoints
        );

        // 2. 만료되지 않은 포인트 처리 (복구)
        remainingAmount = processActivePoints(
            accounts, remainingAmount, now, requestId, restoredPoints
        );

        return new CancelResult(restoredPoints, newlyEarnedPoints, remainingAmount);
    }

    /**
     * 만료된 포인트 처리 - 신규 적립으로 생성
     */
    private long processExpiredPoints(
        List<PointAccount> accounts,
        long remainingAmount,
        LocalDateTime now,
        String userId,
        String requestId,
        List<NewlyEarnedPointDetail> newlyEarnedPoints
    ) {
        for (PointAccount account : accounts) {
            if (remainingAmount <= 0) {
                break;
            }

            long availableToCancel = calculateAvailableToCancel(account);
            if (availableToCancel <= 0) {
                continue;
            }

            PointTransaction earnTransaction = findEarnTransaction(account, requestId);
            if (!isExpired(earnTransaction, now)) {
                continue;
            }

            // 만료된 포인트 처리
            long cancelAmount = Math.min(remainingAmount, availableToCancel);
            createNewEarnForExpired(account, cancelAmount, now, userId, requestId, newlyEarnedPoints);
            updateAccountCanceledAmount(account, cancelAmount);
            remainingAmount -= cancelAmount;
        }

        return remainingAmount;
    }

    /**
     * 만료되지 않은 포인트 처리 - 원본 적립에 복구
     */
    private long processActivePoints(
        List<PointAccount> accounts,
        long remainingAmount,
        LocalDateTime now,
        String requestId,
        List<RestoredPointDetail> restoredPoints
    ) {
        // 역순으로 처리 (LIFO)
        for (int i = accounts.size() - 1; i >= 0 && remainingAmount > 0; i--) {
            PointAccount account = accounts.get(i);

            long availableToCancel = calculateAvailableToCancel(account);
            if (availableToCancel <= 0) {
                continue;
            }

            PointTransaction earnTransaction = findEarnTransaction(account, requestId);
            if (isExpired(earnTransaction, now)) {
                continue;
            }

            // 만료되지 않은 포인트 복구
            long cancelAmount = Math.min(remainingAmount, availableToCancel);
            restoreToOriginalEarn(earnTransaction, account, cancelAmount, requestId, restoredPoints);
            updateAccountCanceledAmount(account, cancelAmount);
            remainingAmount -= cancelAmount;
        }

        return remainingAmount;
    }

    /**
     * 계정에서 취소 가능한 금액 계산
     */
    private long calculateAvailableToCancel(PointAccount account) {
        return account.getUsedAmount() - account.getCanceledAmount();
    }

    /**
     * 원본 적립 트랜잭션 조회
     */
    private PointTransaction findEarnTransaction(PointAccount account, String requestId) {
        return pointTransactionRepository.findByPointKey(account.getEarnPointKey())
            .orElseThrow(() -> {
                log.error("[{}] 원본 적립 트랜잭션을 찾을 수 없음 - earnPointKey: {}", 
                    requestId, account.getEarnPointKey());
                return new RuntimeException("원본 적립 트랜잭션을 찾을 수 없습니다");
            });
    }

    /**
     * 포인트 만료 여부 확인
     */
    private boolean isExpired(PointTransaction transaction, LocalDateTime now) {
        return transaction.getExpirationDate() != null 
            && transaction.getExpirationDate().isBefore(now);
    }

    /**
     * 만료된 포인트를 신규 적립으로 생성
     */
    private void createNewEarnForExpired(
        PointAccount account,
        long amount,
        LocalDateTime now,
        String userId,
        String requestId,
        List<NewlyEarnedPointDetail> newlyEarnedPoints
    ) {
        String newPointKey = PointKeyGenerator.generate();
        Integer defaultExpirationDays = configService.getDefaultExpirationDays();
        LocalDateTime newExpirationDate = now.plusDays(defaultExpirationDays);

        PointTransaction newEarnTransaction = new PointTransaction();
        newEarnTransaction.setPointKey(newPointKey);
        newEarnTransaction.setUserId(userId);
        newEarnTransaction.setTransactionType(TransactionType.EARN);
        newEarnTransaction.setAmount(amount);
        newEarnTransaction.setAvailableBalance(amount);
        newEarnTransaction.setIsManualGrant(false);
        newEarnTransaction.setExpirationDate(newExpirationDate);
        newEarnTransaction.setDescription(
            String.format("사용 취소로 인한 신규 적립 (원본: %s, 만료됨)", account.getEarnPointKey())
        );

        pointTransactionRepository.save(newEarnTransaction);

        newlyEarnedPoints.add(NewlyEarnedPointDetail.builder()
            .pointKey(newPointKey)
            .amount(amount)
            .expirationDate(newExpirationDate)
            .build());

        log.info("[{}] 만료된 포인트 신규 적립 - originalEarnKey: {}, newPointKey: {}, amount: {}",
            requestId, account.getEarnPointKey(), newPointKey, amount);
    }

    /**
     * 만료되지 않은 포인트를 원본 적립에 복구
     */
    private void restoreToOriginalEarn(
        PointTransaction earnTransaction,
        PointAccount account,
        long amount,
        String requestId,
        List<RestoredPointDetail> restoredPoints
    ) {
        long newAvailableBalance = earnTransaction.getAvailableBalance() + amount;
        earnTransaction.setAvailableBalance(newAvailableBalance);
        pointTransactionRepository.save(earnTransaction);

        restoredPoints.add(RestoredPointDetail.builder()
            .earnPointKey(account.getEarnPointKey())
            .restoredAmount(amount)
            .isExpired(false)
            .build());

        log.info("[{}] 포인트 복구 - earnPointKey: {}, restoredAmount: {}, newAvailableBalance: {}",
            requestId, account.getEarnPointKey(), amount, newAvailableBalance);
    }

    /**
     * 계정의 취소 금액 업데이트
     */
    private void updateAccountCanceledAmount(PointAccount account, long additionalCanceledAmount) {
        long newCanceledAmount = account.getCanceledAmount() + additionalCanceledAmount;
        account.setCanceledAmount(newCanceledAmount);
        pointAccountRepository.save(account);
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
}
