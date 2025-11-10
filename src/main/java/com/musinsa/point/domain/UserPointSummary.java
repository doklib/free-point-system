package com.musinsa.point.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 사용자 포인트 요약 엔티티
 * 사용자별 포인트 잔액을 빠르게 조회하기 위한 집계 테이블
 */
@Entity
@Table(name = "user_point_summaries", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id", unique = true)
})
public class UserPointSummary {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false, unique = true, length = 100)
    private String userId;
    
    @Column(name = "total_balance", nullable = false)
    private Long totalBalance;
    
    @Version
    @Column(name = "version", nullable = false)
    private Long version;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    public UserPointSummary() {
    }
    
    @PrePersist
    protected void onCreate() {
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Getters
    public Long getId() {
        return id;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public Long getTotalBalance() {
        return totalBalance;
    }
    
    public Long getVersion() {
        return version;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    // Setters
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public void setTotalBalance(Long totalBalance) {
        this.totalBalance = totalBalance;
    }
}
