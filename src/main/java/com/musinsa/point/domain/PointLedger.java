package com.musinsa.point.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 포인트 원장 엔티티
 * 포인트 사용 시 어떤 적립에서 얼마씩 차감되었는지 기록
 */
@Entity
@Table(name = "point_ledgers", indexes = {
    @Index(name = "idx_use_point_key", columnList = "use_point_key"),
    @Index(name = "idx_earn_point_key", columnList = "earn_point_key")
})
public class PointLedger {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "use_point_key", nullable = false, length = 50)
    private String usePointKey;
    
    @Column(name = "earn_point_key", nullable = false, length = 50)
    private String earnPointKey;
    
    @Column(name = "used_amount", nullable = false)
    private Long usedAmount;
    
    @Column(name = "canceled_amount", nullable = false)
    private Long canceledAmount;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    protected PointLedger() {
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
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
    
    public String getUsePointKey() {
        return usePointKey;
    }
    
    public String getEarnPointKey() {
        return earnPointKey;
    }
    
    public Long getUsedAmount() {
        return usedAmount;
    }
    
    public Long getCanceledAmount() {
        return canceledAmount;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    // Setters
    public void setUsePointKey(String usePointKey) {
        this.usePointKey = usePointKey;
    }
    
    public void setEarnPointKey(String earnPointKey) {
        this.earnPointKey = earnPointKey;
    }
    
    public void setUsedAmount(Long usedAmount) {
        this.usedAmount = usedAmount;
    }
    
    public void setCanceledAmount(Long canceledAmount) {
        this.canceledAmount = canceledAmount;
    }
}
