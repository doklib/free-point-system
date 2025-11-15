package com.musinsa.point.dto;


import java.util.List;

/**
 * 포인트 거래 내역 조회 응답
 */
public record HistoryResponse(
    String userId,
    List<TransactionDetail> transactions,
    PageInfo page
) {
    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private String userId;
        private List<TransactionDetail> transactions;
        private PageInfo page;
        public Builder userId(String v) { this.userId = v; return this; }
        public Builder transactions(List<TransactionDetail> v) { this.transactions = v; return this; }
        public Builder page(PageInfo v) { this.page = v; return this; }
        public HistoryResponse build() { return new HistoryResponse(userId, transactions, page); }
    }
}
