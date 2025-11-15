package com.musinsa.point.dto;

import java.time.LocalDateTime;

/**
 * 포인트 적립 응답
 */
public record EarnResponse(
    String pointKey,
    String userId,
    Long amount,
    Long availableBalance,
    Long totalBalance,
    LocalDateTime expirationDate,
    Boolean isManualGrant,
    LocalDateTime createdAt
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String pointKey;
        private String userId;
        private Long amount;
        private Long availableBalance;
        private Long totalBalance;
        private LocalDateTime expirationDate;
        private Boolean isManualGrant;
        private LocalDateTime createdAt;

        public Builder pointKey(String pointKey) {
            this.pointKey = pointKey;
            return this;
        }

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder amount(Long amount) {
            this.amount = amount;
            return this;
        }

        public Builder availableBalance(Long availableBalance) {
            this.availableBalance = availableBalance;
            return this;
        }

        public Builder totalBalance(Long totalBalance) {
            this.totalBalance = totalBalance;
            return this;
        }

        public Builder expirationDate(LocalDateTime expirationDate) {
            this.expirationDate = expirationDate;
            return this;
        }

        public Builder isManualGrant(Boolean isManualGrant) {
            this.isManualGrant = isManualGrant;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public EarnResponse build() {
            return new EarnResponse(pointKey, userId, amount, availableBalance, totalBalance, expirationDate, isManualGrant, createdAt);
        }
    }
}
