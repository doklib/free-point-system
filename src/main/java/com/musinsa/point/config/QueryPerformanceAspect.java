package com.musinsa.point.config;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * 쿼리 성능 로깅을 위한 AOP Aspect
 * Repository 메서드 실행 시간을 측정하고 로깅합니다.
 */
@Slf4j
@Aspect
@Component
public class QueryPerformanceAspect {

    private static final long SLOW_QUERY_THRESHOLD_MS = 100;

    @Around("execution(* com.musinsa.point.repository..*(..))")
    public Object logQueryPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        String requestId = org.slf4j.MDC.get("requestId");
        String methodName = joinPoint.getSignature().toShortString();
        
        long startTime = System.currentTimeMillis();
        
        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;
            
            if (executionTime > SLOW_QUERY_THRESHOLD_MS) {
                log.warn("[{}] 느린 쿼리 감지 - 메서드: {}, 실행 시간: {}ms", 
                    requestId, methodName, executionTime);
            } else {
                log.debug("[{}] 쿼리 실행 완료 - 메서드: {}, 실행 시간: {}ms", 
                    requestId, methodName, executionTime);
            }
            
            return result;
        } catch (Throwable ex) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("[{}] 쿼리 실행 실패 - 메서드: {}, 실행 시간: {}ms", 
                requestId, methodName, executionTime, ex);
            throw ex;
        }
    }
}
