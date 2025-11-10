package com.musinsa.point.util;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 포인트 트랜잭션의 고유 키를 생성하는 유틸리티 클래스
 * 형식: PT{timestamp}{counter}
 * 예: PT1699612345678001
 */
public class PointKeyGenerator {
    
    private static final AtomicLong counter = new AtomicLong(0);
    
    /**
     * 고유한 포인트 키를 생성합니다.
     * 
     * @return PT{timestamp}{counter} 형식의 포인트 키
     */
    public static String generate() {
        long timestamp = System.currentTimeMillis();
        long count = counter.incrementAndGet();
        return String.format("PT%d%03d", timestamp, count % 1000);
    }
    
    /**
     * 테스트 목적으로 카운터를 리셋합니다.
     * 프로덕션 코드에서는 사용하지 않습니다.
     */
    public static void resetCounter() {
        counter.set(0);
    }
}
