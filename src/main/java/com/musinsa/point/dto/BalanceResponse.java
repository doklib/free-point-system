package com.musinsa.point.dto;


import java.util.List;

/**
 * 포인트 잔액 조회 응답
 */
public record BalanceResponse(
    String userId,
    Long totalBalance,
    List<AvailablePointDetail> availablePoints
) {
    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private String userId;
        private Long totalBalance;
        private List<AvailablePointDetail> availablePoints;
        public Builder userId(String v) { this.userId = v; return this; }
        public Builder totalBalance(Long v) { this.totalBalance = v; return this; }
        public Builder availablePoints(List<AvailablePointDetail> v) { this.availablePoints = v; return this; }
        public BalanceResponse build() { return new BalanceResponse(userId, totalBalance, availablePoints); }
    }
}
