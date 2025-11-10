package com.musinsa.point.exception;

import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;

/**
 * 포인트 시스템의 비즈니스 로직 예외를 처리하는 클래스
 * 명확한 오류 코드와 컨텍스트 정보를 제공하여 문제 파악을 용이하게 함
 */
public class PointBusinessException extends RuntimeException {
    
    private final String errorCode;
    private final HttpStatus httpStatus;
    private final Map<String, Object> details;
    
    public PointBusinessException(String errorCode, String message, HttpStatus httpStatus, Map<String, Object> details) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.details = details != null ? details : new HashMap<>();
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
    
    public Map<String, Object> getDetails() {
        return details;
    }
    
    // 정적 팩토리 메서드들
    
    /**
     * 1회 최대 적립 한도 초과 예외
     */
    public static PointBusinessException exceedMaxEarnLimit(long requestedAmount, long maxLimit) {
        return new PointBusinessException(
            "EXCEED_MAX_EARN_LIMIT",
            String.format("1회 최대 적립 한도(%d)를 초과했습니다. 요청 금액: %d", maxLimit, requestedAmount),
            HttpStatus.BAD_REQUEST,
            Map.of(
                "requestedAmount", requestedAmount,
                "maxLimit", maxLimit
            )
        );
    }
    
    /**
     * 개인별 최대 보유 한도 초과 예외
     */
    public static PointBusinessException exceedUserMaxBalance(long currentBalance, long maxBalance, long requestedAmount) {
        return new PointBusinessException(
            "EXCEED_USER_MAX_BALANCE",
            String.format("개인별 최대 보유 한도(%d)를 초과했습니다. 현재 잔액: %d, 요청 금액: %d", 
                maxBalance, currentBalance, requestedAmount),
            HttpStatus.BAD_REQUEST,
            Map.of(
                "currentBalance", currentBalance,
                "maxBalance", maxBalance,
                "requestedAmount", requestedAmount
            )
        );
    }
    
    /**
     * 사용 가능 포인트 부족 예외
     */
    public static PointBusinessException insufficientBalance(long availableBalance, long requestedAmount) {
        return new PointBusinessException(
            "INSUFFICIENT_POINT_BALANCE",
            String.format("사용 가능한 포인트가 부족합니다. 현재 잔액: %d, 요청 금액: %d",
                availableBalance, requestedAmount),
            HttpStatus.BAD_REQUEST,
            Map.of(
                "availableBalance", availableBalance,
                "requestedAmount", requestedAmount
            )
        );
    }
    
    /**
     * 포인트 키를 찾을 수 없음 예외
     */
    public static PointBusinessException pointKeyNotFound(String pointKey) {
        return new PointBusinessException(
            "POINT_KEY_NOT_FOUND",
            String.format("포인트 키를 찾을 수 없습니다: %s", pointKey),
            HttpStatus.NOT_FOUND,
            Map.of("pointKey", pointKey)
        );
    }
    
    /**
     * 사용된 포인트는 취소 불가 예외
     * Requirements: 2.2
     */
    public static PointBusinessException cannotCancelUsedPoint(String pointKey, long usedAmount, long totalAmount) {
        return new PointBusinessException(
            "CANNOT_CANCEL_USED_POINT",
            String.format("사용된 포인트는 취소할 수 없습니다. 포인트 키: %s, 사용된 금액: %d, 전체 금액: %d",
                pointKey, usedAmount, totalAmount),
            HttpStatus.BAD_REQUEST,
            Map.of(
                "pointKey", pointKey,
                "usedAmount", usedAmount,
                "totalAmount", totalAmount
            )
        );
    }
    
    /**
     * 원래 사용 금액 초과 예외
     */
    public static PointBusinessException exceedOriginalUseAmount(long originalAmount, long requestedCancelAmount, long alreadyCanceledAmount) {
        return new PointBusinessException(
            "EXCEED_ORIGINAL_USE_AMOUNT",
            String.format("원래 사용 금액을 초과했습니다. 원래 금액: %d, 요청 취소 금액: %d, 이미 취소된 금액: %d",
                originalAmount, requestedCancelAmount, alreadyCanceledAmount),
            HttpStatus.BAD_REQUEST,
            Map.of(
                "originalAmount", originalAmount,
                "requestedCancelAmount", requestedCancelAmount,
                "alreadyCanceledAmount", alreadyCanceledAmount
            )
        );
    }
    
    /**
     * 유효하지 않은 만료일 예외
     */
    public static PointBusinessException invalidExpirationDays(int requestedDays, int minDays, int maxDays) {
        return new PointBusinessException(
            "INVALID_EXPIRATION_DAYS",
            String.format("유효하지 않은 만료일입니다. 요청: %d일, 허용 범위: %d~%d일",
                requestedDays, minDays, maxDays),
            HttpStatus.BAD_REQUEST,
            Map.of(
                "requestedDays", requestedDays,
                "minDays", minDays,
                "maxDays", maxDays
            )
        );
    }
    
    /**
     * 유효하지 않은 금액 예외
     */
    public static PointBusinessException invalidAmount(long amount, Long minAmount, Long maxAmount) {
        String message;
        Map<String, Object> details = new HashMap<>();
        details.put("amount", amount);
        
        if (minAmount != null && maxAmount != null) {
            message = String.format("유효하지 않은 금액입니다. 요청: %d, 허용 범위: %d~%d",
                amount, minAmount, maxAmount);
            details.put("minAmount", minAmount);
            details.put("maxAmount", maxAmount);
        } else if (minAmount != null) {
            message = String.format("유효하지 않은 금액입니다. 요청: %d, 최소 금액: %d",
                amount, minAmount);
            details.put("minAmount", minAmount);
        } else if (maxAmount != null) {
            message = String.format("유효하지 않은 금액입니다. 요청: %d, 최대 금액: %d",
                amount, maxAmount);
            details.put("maxAmount", maxAmount);
        } else {
            message = String.format("유효하지 않은 금액입니다. 요청: %d", amount);
        }
        
        return new PointBusinessException(
            "INVALID_AMOUNT",
            message,
            HttpStatus.BAD_REQUEST,
            details
        );
    }
    
    /**
     * 중복된 멱등성 키 예외
     */
    public static PointBusinessException duplicateIdempotencyKey(String idempotencyKey, Object existingResult) {
        return new PointBusinessException(
            "DUPLICATE_IDEMPOTENCY_KEY",
            String.format("중복된 멱등성 키입니다: %s", idempotencyKey),
            HttpStatus.CONFLICT,
            Map.of(
                "idempotencyKey", idempotencyKey,
                "existingResult", existingResult
            )
        );
    }
}
