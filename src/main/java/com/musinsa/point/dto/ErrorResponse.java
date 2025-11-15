package com.musinsa.point.dto;


import java.time.LocalDateTime;
import java.util.Map;

/**
 * 에러 응답
 */
public record ErrorResponse(
    String requestId,
    String errorCode,
    String message,
    Map<String, Object> details,
    LocalDateTime timestamp
) {
    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private String requestId, errorCode, message;
        private Map<String, Object> details;
        private LocalDateTime timestamp;
        public Builder requestId(String v) { this.requestId = v; return this; }
        public Builder errorCode(String v) { this.errorCode = v; return this; }
        public Builder message(String v) { this.message = v; return this; }
        public Builder details(Map<String, Object> v) { this.details = v; return this; }
        public Builder timestamp(LocalDateTime v) { this.timestamp = v; return this; }
        public ErrorResponse build() { return new ErrorResponse(requestId, errorCode, message, details, timestamp); }
    }
}
