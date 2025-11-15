package com.musinsa.point.dto;


import java.time.LocalDateTime;
import java.util.List;

/**
 * 포인트 사용 취소 응답
 */
public record CancelUseResponse(
    String cancelUsePointKey,
    String originalUsePointKey,
    Long canceledAmount,
    Long totalBalance,
    List<RestoredPointDetail> restoredPoints,
    List<NewlyEarnedPointDetail> newlyEarnedPoints,
    LocalDateTime canceledAt
) {
    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private String cancelUsePointKey, originalUsePointKey;
        private Long canceledAmount, totalBalance;
        private List<RestoredPointDetail> restoredPoints;
        private List<NewlyEarnedPointDetail> newlyEarnedPoints;
        private LocalDateTime canceledAt;
        public Builder cancelUsePointKey(String v) { this.cancelUsePointKey = v; return this; }
        public Builder originalUsePointKey(String v) { this.originalUsePointKey = v; return this; }
        public Builder canceledAmount(Long v) { this.canceledAmount = v; return this; }
        public Builder totalBalance(Long v) { this.totalBalance = v; return this; }
        public Builder restoredPoints(List<RestoredPointDetail> v) { this.restoredPoints = v; return this; }
        public Builder newlyEarnedPoints(List<NewlyEarnedPointDetail> v) { this.newlyEarnedPoints = v; return this; }
        public Builder canceledAt(LocalDateTime v) { this.canceledAt = v; return this; }
        public CancelUseResponse build() { return new CancelUseResponse(cancelUsePointKey, originalUsePointKey, canceledAmount, totalBalance, restoredPoints, newlyEarnedPoints, canceledAt); }
    }
}
