package com.musinsa.point.dto;

import com.musinsa.point.domain.TransactionType;

import java.time.LocalDateTime;

/**
 * 포인트 거래 이력 상세 정보
 */
public record TransactionDetail(
    String pointKey,
    TransactionType type,
    Long amount,
    Long balance,
    String orderNumber,
    String description,
    LocalDateTime createdAt
) {
    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private String pointKey, orderNumber, description;
        private TransactionType type;
        private Long amount, balance;
        private LocalDateTime createdAt;
        public Builder pointKey(String v) { this.pointKey = v; return this; }
        public Builder type(TransactionType v) { this.type = v; return this; }
        public Builder amount(Long v) { this.amount = v; return this; }
        public Builder balance(Long v) { this.balance = v; return this; }
        public Builder orderNumber(String v) { this.orderNumber = v; return this; }
        public Builder description(String v) { this.description = v; return this; }
        public Builder createdAt(LocalDateTime v) { this.createdAt = v; return this; }
        public TransactionDetail build() { return new TransactionDetail(pointKey, type, amount, balance, orderNumber, description, createdAt); }
    }
}
