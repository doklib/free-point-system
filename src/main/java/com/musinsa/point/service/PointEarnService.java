package com.musinsa.point.service;

import com.musinsa.point.domain.IdempotencyRecord;
import com.musinsa.point.domain.PointTransaction;
import com.musinsa.point.domain.TransactionType;
import com.musinsa.point.domain.UserPointSummary;
import com.musinsa.point.dto.CancelEarnRequest;
import com.musinsa.point.dto.CancelEarnResponse;
import com.musinsa.point.dto.EarnRequest;
import com.musinsa.point.dto.EarnResponse;
import com.musinsa.point.exception.PointBusinessException;
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

/**
 * 포인트 적립 서비스
 * 포인트 적립 및 적립 취소를 담당
 */
@Service
public class PointEarnService {

    private static final Logger log = LoggerFactory.getLogger(PointEarnService.class);

    private final PointTransactionRepository pointTransactionRepository;
    private final UserPointSummaryRepository userPointSummaryRepository;
    private final IdempotencyService idempotencyService;
    private final ConfigService configService;
    private final ObjectMapper objectMapper;

    public PointEarnService(PointTransactionRepository pointTransactionRepository,
                           UserPointSummaryRepository userPointSummaryRepository,
                           IdempotencyService idempotencyService,
                           ConfigService configService,
                           ObjectMapper objectMapper) {
        this.pointTransactionRepository = pointTransactionRepository;
        this.userPointSummaryRepository = userPointSummaryRepository;
        this.idempotencyService = idempotencyService;
        this.configService = configService;
        this.objectMapper = objectMapper;
    }

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

            // 2. 금액 유효성 검증 (최소값)
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
            EarnResponse response = new EarnResponse(
                pointKey,
                request.getUserId(),
                request.getAmount(),
                request.getAmount(),
                newTotalBalance,
                expirationDate,
                request.getIsManualGrant(),
                transaction.getCreatedAt()
            );

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
     * 금액 유효성 검증 (최소값만 검증)
     */
    private void validateAmount(Long amount) {
        // null 체크
        if (amount == null) {
            throw PointBusinessException.invalidAmount(0L, 1L, null);
        }
        
        // 최소값 체크
        if (amount < 1) {
            throw PointBusinessException.invalidAmount(amount, 1L, null);
        }
    }

    /**
     * 만료일 유효성 검증 및 기본값 반환
     */
    private Integer validateAndGetExpirationDays(Integer expirationDays) {
        // Early return: 만료일이 지정되지 않은 경우 기본값 사용
        if (expirationDays == null) {
            return configService.getDefaultExpirationDays();
        }
        
        // 만료일 범위 검증
        Integer minDays = configService.getMinExpirationDays();
        Integer maxDays = configService.getMaxExpirationDays();
        
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
