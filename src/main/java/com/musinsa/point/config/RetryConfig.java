package com.musinsa.point.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Spring Retry 설정
 * OptimisticLockException 발생 시 자동 재시도를 위한 설정
 */
@Configuration
@EnableRetry
public class RetryConfig {

    private static final Logger log = LoggerFactory.getLogger(RetryConfig.class);

    @Bean
    public RetryListener retryListener() {
        return new RetryListener() {
            @Override
            public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
                String requestId = org.slf4j.MDC.get("requestId");
                log.warn("[{}] 재시도 발생 - 시도 횟수: {}, 예외: {}", 
                    requestId, 
                    context.getRetryCount(), 
                    throwable.getClass().getSimpleName());
            }
        };
    }
}
