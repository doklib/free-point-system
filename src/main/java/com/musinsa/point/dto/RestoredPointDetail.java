package com.musinsa.point.dto;


/**
 * 포인트 사용 취소 시 복구된 포인트 정보
 */
public record RestoredPointDetail(
    String earnPointKey,
    Long restoredAmount,
    Boolean isExpired
) {
    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private String earnPointKey;
        private Long restoredAmount;
        private Boolean isExpired;
        public Builder earnPointKey(String v) { this.earnPointKey = v; return this; }
        public Builder restoredAmount(Long v) { this.restoredAmount = v; return this; }
        public Builder isExpired(Boolean v) { this.isExpired = v; return this; }
        public RestoredPointDetail build() { return new RestoredPointDetail(earnPointKey, restoredAmount, isExpired); }
    }
}
