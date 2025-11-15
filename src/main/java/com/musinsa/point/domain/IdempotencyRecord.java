package com.musinsa.point.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * 멱등성 레코드 엔티티
 * 멱등성 키를 관리하여 중복 요청 방지
 */
@Entity
@Table(name = "idempotency_records", indexes = {
    @Index(name = "idx_idempotency_key", columnList = "idempotency_key", unique = true),
    @Index(name = "idx_created_at", columnList = "created_at")
})
public class IdempotencyRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
    private String idempotencyKey;
    
    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;
    
    @Column(name = "http_status", nullable = false)
    private Integer httpStatus;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    
    protected IdempotencyRecord() {
    }
    
    public IdempotencyRecord(String idempotencyKey, String responseBody, 
                            Integer httpStatus, LocalDateTime createdAt, LocalDateTime expiresAt) {
        this.idempotencyKey = idempotencyKey;
        this.responseBody = responseBody;
        this.httpStatus = httpStatus;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public Long getId() {
        return id;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public Integer getHttpStatus() {
        return httpStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String idempotencyKey;
        private String responseBody;
        private Integer httpStatus;
        private LocalDateTime createdAt;
        private LocalDateTime expiresAt;

        public Builder idempotencyKey(String idempotencyKey) {
            this.idempotencyKey = idempotencyKey;
            return this;
        }

        public Builder responseBody(String responseBody) {
            this.responseBody = responseBody;
            return this;
        }

        public Builder httpStatus(Integer httpStatus) {
            this.httpStatus = httpStatus;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder expiresAt(LocalDateTime expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        public IdempotencyRecord build() {
            return new IdempotencyRecord(idempotencyKey, responseBody, httpStatus, createdAt, expiresAt);
        }
    }
}
