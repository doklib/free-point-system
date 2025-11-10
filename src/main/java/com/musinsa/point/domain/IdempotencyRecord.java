package com.musinsa.point.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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
    
    @Builder
    public IdempotencyRecord(String idempotencyKey, String responseBody, 
                            Integer httpStatus, LocalDateTime createdAt, LocalDateTime expiresAt) {
        this.idempotencyKey = idempotencyKey;
        this.responseBody = responseBody;
        this.httpStatus = httpStatus;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }
}
