package com.musinsa.point.exception;

import com.musinsa.point.dto.ErrorResponse;
import jakarta.persistence.OptimisticLockException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 전역 예외 처리 핸들러
 * 모든 예외를 일관된 형식으로 변환하여 클라이언트에 반환
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * PointBusinessException 처리
     */
    @ExceptionHandler(PointBusinessException.class)
    public ResponseEntity<ErrorResponse> handlePointBusinessException(
        PointBusinessException ex,
        HttpServletRequest request
    ) {
        String requestId = request.getHeader("X-Request-ID");
        
        log.warn("[{}] 비즈니스 예외 발생 - errorCode: {}, message: {}", 
            requestId, ex.getErrorCode(), ex.getMessage());
        
        ErrorResponse response = ErrorResponse.builder()
            .requestId(requestId)
            .errorCode(ex.getErrorCode())
            .message(ex.getMessage())
            .details(ex.getDetails())
            .timestamp(LocalDateTime.now())
            .build();
        
        return ResponseEntity
            .status(ex.getHttpStatus())
            .header("X-Request-ID", requestId)
            .body(response);
    }
    
    /**
     * OptimisticLockException 처리 (동시성 충돌)
     */
    @ExceptionHandler(OptimisticLockException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLockException(
        OptimisticLockException ex,
        HttpServletRequest request
    ) {
        String requestId = request.getHeader("X-Request-ID");
        
        log.warn("[{}] 동시성 충돌 발생 - {}", requestId, ex.getMessage());
        
        Map<String, Object> details = new HashMap<>();
        details.put("retryable", true);
        
        ErrorResponse response = ErrorResponse.builder()
            .requestId(requestId)
            .errorCode("CONCURRENCY_CONFLICT")
            .message("동시성 충돌이 발생했습니다. 잠시 후 다시 시도해주세요.")
            .details(details)
            .timestamp(LocalDateTime.now())
            .build();
        
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .header("X-Request-ID", requestId)
            .body(response);
    }
    
    /**
     * Validation 예외 처리
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
        MethodArgumentNotValidException ex,
        HttpServletRequest request
    ) {
        String requestId = request.getHeader("X-Request-ID");
        
        Map<String, Object> details = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            details.put(error.getField(), error.getDefaultMessage());
        }
        
        log.warn("[{}] 유효성 검증 실패 - {}", requestId, details);
        
        ErrorResponse response = ErrorResponse.builder()
            .requestId(requestId)
            .errorCode("VALIDATION_ERROR")
            .message("요청 데이터 유효성 검증에 실패했습니다.")
            .details(details)
            .timestamp(LocalDateTime.now())
            .build();
        
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .header("X-Request-ID", requestId)
            .body(response);
    }
    
    /**
     * 일반 예외 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(
        Exception ex,
        HttpServletRequest request
    ) {
        String requestId = request.getHeader("X-Request-ID");
        
        log.error("[{}] 예상치 못한 예외 발생", requestId, ex);
        
        Map<String, Object> details = new HashMap<>();
        details.put("exceptionType", ex.getClass().getSimpleName());
        
        ErrorResponse response = ErrorResponse.builder()
            .requestId(requestId)
            .errorCode("INTERNAL_SERVER_ERROR")
            .message("서버 내부 오류가 발생했습니다. 관리자에게 문의해주세요.")
            .details(details)
            .timestamp(LocalDateTime.now())
            .build();
        
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .header("X-Request-ID", requestId)
            .body(response);
    }
}
