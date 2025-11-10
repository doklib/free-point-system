package com.musinsa.point.service;

import com.musinsa.point.domain.IdempotencyRecord;
import com.musinsa.point.domain.PointTransaction;
import com.musinsa.point.domain.TransactionType;
import com.musinsa.point.domain.UserPointSummary;
import com.musinsa.point.dto.EarnRequest;
import com.musinsa.point.dto.EarnResponse;
import com.musinsa.point.exception.PointBusinessException;
import com.musinsa.point.repository.PointTransactionRepository;
import com.musinsa.point.repository.UserPointSummaryRepository;
import com.musinsa.point.util.PointKeyGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 포인트 비즈니스 로직을 처리하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PointService {

    private final PointTransactionRepository pointTransactionRepository;
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
     * 금액 유효성 검증
     */
    private void validateAmount(Long amount) {
        if (amount == null || amount < 1 || amount > 100000) {
            throw PointBusinessException.invalidAmount(
                amount != null ? amount : 0,
                1L,
                100000L
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
}
