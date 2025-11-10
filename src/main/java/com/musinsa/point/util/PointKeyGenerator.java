package com.musinsa.point.util;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 포인트 트랜잭션의 고유 키를 생성하는 유틸리티 클래스
 * 형식: 알파벳 순서 (A, B, C, ..., Z, AA, AB, ...)
 * 예: A, B, C, ..., Z, AA, AB, AC
 */
public class PointKeyGenerator {
    
    private static final AtomicLong counter = new AtomicLong(0);
    
    /**
     * 고유한 포인트 키를 생성합니다.
     * 알파벳 순서로 생성됩니다 (A, B, C, ..., Z, AA, AB, ...)
     * 
     * @return 알파벳 형식의 포인트 키
     */
    public static String generate() {
        long count = counter.incrementAndGet();
        return toAlphabetic(count);
    }
    
    /**
     * 숫자를 알파벳 문자열로 변환합니다.
     * 1 -> A, 2 -> B, ..., 26 -> Z, 27 -> AA, 28 -> AB, ...
     * 
     * @param number 변환할 숫자 (1부터 시작)
     * @return 알파벳 문자열
     */
    private static String toAlphabetic(long number) {
        StringBuilder result = new StringBuilder();
        
        while (number > 0) {
            number--; // 0-based로 변환
            result.insert(0, (char) ('A' + (number % 26)));
            number /= 26;
        }
        
        return result.toString();
    }
    
    /**
     * 테스트 목적으로 카운터를 리셋합니다.
     * 프로덕션 코드에서는 사용하지 않습니다.
     */
    public static void resetCounter() {
        counter.set(0);
    }
}
