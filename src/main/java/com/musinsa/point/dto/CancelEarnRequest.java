package com.musinsa.point.dto;

import jakarta.validation.constraints.NotBlank;

public class CancelEarnRequest {
    
    @NotBlank(message = "포인트 키는 필수입니다")
    private String pointKey;
    
    private String reason;

    public CancelEarnRequest() {
    }

    public CancelEarnRequest(String pointKey, String reason) {
        this.pointKey = pointKey;
        this.reason = reason;
    }

    public String getPointKey() {
        return pointKey;
    }

    public String getReason() {
        return reason;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String pointKey;
        private String reason;

        public Builder pointKey(String pointKey) {
            this.pointKey = pointKey;
            return this;
        }

        public Builder reason(String reason) {
            this.reason = reason;
            return this;
        }

        public CancelEarnRequest build() {
            return new CancelEarnRequest(pointKey, reason);
        }
    }
}
