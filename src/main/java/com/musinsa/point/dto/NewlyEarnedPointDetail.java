package com.musinsa.point.dto;


import java.time.LocalDateTime;

/**
 * 포인트 사용 취소 시 만료된 포인트를 신규 적립한 정보
 */
public record NewlyEarnedPointDetail(
    String pointKey,
    Long amount,
    LocalDateTime expirationDate
) {
    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private String pointKey;
        private Long amount;
        private LocalDateTime expirationDate;
        public Builder pointKey(String v) { this.pointKey = v; return this; }
        public Builder amount(Long v) { this.amount = v; return this; }
        public Builder expirationDate(LocalDateTime v) { this.expirationDate = v; return this; }
        public NewlyEarnedPointDetail build() { return new NewlyEarnedPointDetail(pointKey, amount, expirationDate); }
    }
}
