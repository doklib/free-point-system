package com.musinsa.point.domain;

/**
 * 포인트 트랜잭션 타입
 */
public enum TransactionType {
    /**
     * 포인트 적립
     */
    EARN,
    
    /**
     * 포인트 적립 취소
     */
    CANCEL_EARN,
    
    /**
     * 포인트 사용
     */
    USE,
    
    /**
     * 포인트 사용 취소
     */
    CANCEL_USE
}
