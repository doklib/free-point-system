package com.musinsa.point.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * X-Request-ID 헤더를 처리하고 MDC에 저장하는 필터
 * 요청 추적성을 위해 모든 요청에 고유한 Request ID를 할당합니다.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {
    
    private static final String REQUEST_ID_HEADER = "X-Request-ID";
    private static final String MDC_REQUEST_ID_KEY = "requestId";
    
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        
        // 헤더에 Request ID가 없으면 새로 생성
        if (requestId == null || requestId.trim().isEmpty()) {
            requestId = UUID.randomUUID().toString();
        }
        
        // MDC에 Request ID 저장 (로깅에 사용)
        MDC.put(MDC_REQUEST_ID_KEY, requestId);
        
        // 응답 헤더에 Request ID 추가
        response.setHeader(REQUEST_ID_HEADER, requestId);
        
        try {
            filterChain.doFilter(request, response);
        } finally {
            // 요청 처리 완료 후 MDC 정리
            MDC.remove(MDC_REQUEST_ID_KEY);
        }
    }
}
