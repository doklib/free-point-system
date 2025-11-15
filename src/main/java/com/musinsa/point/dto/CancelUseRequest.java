package com.musinsa.point.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CancelUseRequest {
    
    @NotBlank(message = "주문 번호는 필수입니다")
    private String orderNumber;
    
    @NotNull(message = "취소 금액은 필수입니다")
    @Min(value = 1, message = "취소 금액은 1포인트 이상이어야 합니다")
    private Long amount;
    
    private String reason;

    public CancelUseRequest() {
    }

    public CancelUseRequest(String orderNumber, Long amount, String reason) {
        this.orderNumber = orderNumber;
        this.amount = amount;
        this.reason = reason;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public Long getAmount() {
        return amount;
    }

    public String getReason() {
        return reason;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String orderNumber;
        private Long amount;
        private String reason;

        public Builder orderNumber(String orderNumber) {
            this.orderNumber = orderNumber;
            return this;
        }

        public Builder amount(Long amount) {
            this.amount = amount;
            return this;
        }

        public Builder reason(String reason) {
            this.reason = reason;
            return this;
        }

        public CancelUseRequest build() {
            return new CancelUseRequest(orderNumber, amount, reason);
        }
    }
}
