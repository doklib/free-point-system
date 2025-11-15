package com.musinsa.point.dto;


/**
 * 페이징 정보
 */
public record PageInfo(
    Integer number,
    Integer size,
    Long totalElements,
    Integer totalPages
) {
    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private Integer number, size, totalPages;
        private Long totalElements;
        public Builder number(Integer v) { this.number = v; return this; }
        public Builder size(Integer v) { this.size = v; return this; }
        public Builder totalElements(Long v) { this.totalElements = v; return this; }
        public Builder totalPages(Integer v) { this.totalPages = v; return this; }
        public PageInfo build() { return new PageInfo(number, size, totalElements, totalPages); }
    }
}
