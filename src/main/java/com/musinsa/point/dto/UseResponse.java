package com.musinsa.point.dto;


import java.time.LocalDateTime;
import java.util.List;

/**
 * 포인트 사용 응답
 */
public record UseResponse(
    String usePointKey,
    String userId,
    String orderNumber,
    Long usedAmount,
    Long remainingBalance,
    List<UsedFromDetail> usedFrom,
    LocalDateTime usedAt
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String usePointKey;
        private String userId;
        private String orderNumber;
        private Long usedAmount;
        private Long remainingBalance;
        private List<UsedFromDetail> usedFrom;
        private LocalDateTime usedAt;

        public Builder usePointKey(String usePointKey) {
            this.usePointKey = usePointKey;
            return this;
        }

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder orderNumber(String orderNumber) {
            this.orderNumber = orderNumber;
            return this;
        }

        public Builder usedAmount(Long usedAmount) {
            this.usedAmount = usedAmount;
            return this;
        }

        public Builder remainingBalance(Long remainingBalance) {
            this.remainingBalance = remainingBalance;
            return this;
        }

        public Builder usedFrom(List<UsedFromDetail> usedFrom) {
            this.usedFrom = usedFrom;
            return this;
        }

        public Builder usedAt(LocalDateTime usedAt) {
            this.usedAt = usedAt;
            return this;
        }

        public UseResponse build() {
            return new UseResponse(usePointKey, userId, orderNumber, usedAmount, remainingBalance, usedFrom, usedAt);
        }
    }
}
