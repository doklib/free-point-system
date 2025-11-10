package com.musinsa.point.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 포인트 트랜잭션 엔티티
 * 포인트의 모든 변경 이력을 기록하는 핵심 엔티티
 */
@Entity
@Table(name = "point_transactions", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_point_key", columnList = "point_key", unique = true),
    @Index(name = "idx_user_type_created", columnList = "user_id, transaction_type, created_at"),
    // 포인트 사용 시 사용 가능한 포인트 조회 성능 최적화를 위한 복합 인덱스
    @Index(name = "idx_available_points", columnList = "user_id, transaction_type, available_balance, expiration_date, is_manual_grant, created_at")
})
public class PointTransaction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "point_key", nullable = false, unique = true, length = 50)
    private String pointKey;
    
    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 20)
    private TransactionType transactionType;
    
    @Column(name = "amount", nullable = false)
    private Long amount;
    
    @Column(name = "available_balance", nullable = false)
    private Long availableBalance;
    
    @Column(name = "is_manual_grant", nullable = false)
    private Boolean isManualGrant;
    
    @Column(name = "expiration_date")
    private LocalDateTime expirationDate;
    
    @Column(name = "order_number", length = 100)
    private String orderNumber;
    
    @Column(name = "reference_point_key", length = 50)
    private String referencePointKey;
    
    @Column(name = "description", length = 500)
    private String description;
    
    @Version
    @Column(name = "version", nullable = false)
    private Long version;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    public PointTransaction() {
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
    
    public String getPointKey() {
        return pointKey;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public TransactionType getTransactionType() {
        return transactionType;
    }
    
    public Long getAmount() {
        return amount;
    }
    
    public Long getAvailableBalance() {
        return availableBalance;
    }
    
    public Boolean getIsManualGrant() {
        return isManualGrant;
    }
    
    public LocalDateTime getExpirationDate() {
        return expirationDate;
    }
    
    public String getOrderNumber() {
        return orderNumber;
    }
    
    public String getReferencePointKey() {
        return referencePointKey;
    }
    
    public String getDescription() {
        return description;
    }
    
    public Long getVersion() {
        return version;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    // Setters
    public void setPointKey(String pointKey) {
        this.pointKey = pointKey;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }
    
    public void setAmount(Long amount) {
        this.amount = amount;
    }
    
    public void setAvailableBalance(Long availableBalance) {
        this.availableBalance = availableBalance;
    }
    
    public void setIsManualGrant(Boolean isManualGrant) {
        this.isManualGrant = isManualGrant;
    }
    
    public void setExpirationDate(LocalDateTime expirationDate) {
        this.expirationDate = expirationDate;
    }
    
    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }
    
    public void setReferencePointKey(String referencePointKey) {
        this.referencePointKey = referencePointKey;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
}
