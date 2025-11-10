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
    
    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;
    
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
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    // Getters
    public Long getId() {
        return id;
    }
    
    public String getIdempotencyKey() {
        return idempotencyKey;
    }
    
    public String getRequestHash() {
        return requestHash;
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
    
    // Setters
    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }
    
    public void setRequestHash(String requestHash) {
        this.requestHash = requestHash;
    }
    
    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }
    
    public void setHttpStatus(Integer httpStatus) {
        this.httpStatus = httpStatus;
    }
    
    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
}
