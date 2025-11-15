package com.musinsa.point.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class EarnRequest {
    
    @NotBlank(message = "사용자 ID는 필수입니다")
    private String userId;
    
    @NotNull(message = "적립 금액은 필수입니다")
    @Min(value = 1, message = "적립 금액은 1포인트 이상이어야 합니다")
    // 최대값은 ConfigService에서 동적으로 검증 (MAX_EARN_PER_TRANSACTION)
    private Long amount;
    
    @NotNull(message = "수기 지급 여부는 필수입니다")
    private Boolean isManualGrant;
    
    // 만료일 범위는 ConfigService에서 동적으로 검증 (MIN_EXPIRATION_DAYS, MAX_EXPIRATION_DAYS)
    private Integer expirationDays;
    
    private String description;

    public EarnRequest() {
    }

    public EarnRequest(String userId, Long amount, Boolean isManualGrant, Integer expirationDays, String description) {
        this.userId = userId;
        this.amount = amount;
        this.isManualGrant = isManualGrant;
        this.expirationDays = expirationDays;
        this.description = description;
    }

    public String getUserId() {
        return userId;
    }

    public Long getAmount() {
        return amount;
    }

    public Boolean getIsManualGrant() {
        return isManualGrant;
    }

    public Integer getExpirationDays() {
        return expirationDays;
    }

    public String getDescription() {
        return description;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String userId;
        private Long amount;
        private Boolean isManualGrant;
        private Integer expirationDays;
        private String description;

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder amount(Long amount) {
            this.amount = amount;
            return this;
        }

        public Builder isManualGrant(Boolean isManualGrant) {
            this.isManualGrant = isManualGrant;
            return this;
        }

        public Builder expirationDays(Integer expirationDays) {
            this.expirationDays = expirationDays;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public EarnRequest build() {
            return new EarnRequest(userId, amount, isManualGrant, expirationDays, description);
        }
    }
}
