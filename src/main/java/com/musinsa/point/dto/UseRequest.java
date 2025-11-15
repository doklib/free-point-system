package com.musinsa.point.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class UseRequest {
    
    @NotBlank(message = "사용자 ID는 필수입니다")
    private String userId;
    
    @NotBlank(message = "주문 번호는 필수입니다")
    private String orderNumber;
    
    @NotNull(message = "사용 금액은 필수입니다")
    @Min(value = 1, message = "사용 금액은 1포인트 이상이어야 합니다")
    private Long amount;

    public UseRequest() {
    }

    public UseRequest(String userId, String orderNumber, Long amount) {
        this.userId = userId;
        this.orderNumber = orderNumber;
        this.amount = amount;
    }

    public String getUserId() {
        return userId;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public Long getAmount() {
        return amount;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String userId;
        private String orderNumber;
        private Long amount;

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder orderNumber(String orderNumber) {
            this.orderNumber = orderNumber;
            return this;
        }

        public Builder amount(Long amount) {
            this.amount = amount;
            return this;
        }

        public UseRequest build() {
            return new UseRequest(userId, orderNumber, amount);
        }
    }
}
