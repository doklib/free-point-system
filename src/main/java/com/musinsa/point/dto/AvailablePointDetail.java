package com.musinsa.point.dto;


import java.time.LocalDateTime;


public record AvailablePointDetail(
    String pointKey,
    Long amount,
    Long availableBalance,
    Boolean isManualGrant,
    LocalDateTime expirationDate
) {
    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private String pointKey;
        private Long amount, availableBalance;
        private Boolean isManualGrant;
        private LocalDateTime expirationDate;
        public Builder pointKey(String v) { this.pointKey = v; return this; }
        public Builder amount(Long v) { this.amount = v; return this; }
        public Builder availableBalance(Long v) { this.availableBalance = v; return this; }
        public Builder isManualGrant(Boolean v) { this.isManualGrant = v; return this; }
        public Builder expirationDate(LocalDateTime v) { this.expirationDate = v; return this; }
        public AvailablePointDetail build() { return new AvailablePointDetail(pointKey, amount, availableBalance, isManualGrant, expirationDate); }
    }
}
