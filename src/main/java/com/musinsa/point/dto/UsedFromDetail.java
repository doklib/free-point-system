package com.musinsa.point.dto;


/**
 * 포인트 사용 시 어떤 적립에서 얼마를 차감했는지 추적
 */
public record UsedFromDetail(
    String earnPointKey,
    Long usedAmount
) {
    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private String earnPointKey;
        private Long usedAmount;
        public Builder earnPointKey(String v) { this.earnPointKey = v; return this; }
        public Builder usedAmount(Long v) { this.usedAmount = v; return this; }
        public UsedFromDetail build() { return new UsedFromDetail(earnPointKey, usedAmount); }
    }
}
