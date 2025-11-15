package com.musinsa.point.dto;


import java.time.LocalDateTime;

/**
 * 포인트 적립 취소 응답
 */
public record CancelEarnResponse(
    String cancelPointKey,
    String originalPointKey,
    Long canceledAmount,
    Long totalBalance,
    LocalDateTime canceledAt
) {
    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private String cancelPointKey, originalPointKey;
        private Long canceledAmount, totalBalance;
        private LocalDateTime canceledAt;
        public Builder cancelPointKey(String v) { this.cancelPointKey = v; return this; }
        public Builder originalPointKey(String v) { this.originalPointKey = v; return this; }
        public Builder canceledAmount(Long v) { this.canceledAmount = v; return this; }
        public Builder totalBalance(Long v) { this.totalBalance = v; return this; }
        public Builder canceledAt(LocalDateTime v) { this.canceledAt = v; return this; }
        public CancelEarnResponse build() { return new CancelEarnResponse(cancelPointKey, originalPointKey, canceledAmount, totalBalance, canceledAt); }
    }
}
