package com.musinsa.point.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 시스템 설정 엔티티
 * 하드코딩 없이 설정을 관리
 */
@Entity
@Table(name = "system_configs", indexes = {
    @Index(name = "idx_config_key", columnList = "config_key", unique = true)
})
public class SystemConfig {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "config_key", nullable = false, unique = true, length = 100)
    private String configKey;
    
    @Column(name = "config_value", nullable = false, length = 500)
    private String configValue;
    
    @Column(name = "description", length = 500)
    private String description;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    protected SystemConfig() {
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
    
    public String getConfigKey() {
        return configKey;
    }
    
    public String getConfigValue() {
        return configValue;
    }
    
    public String getDescription() {
        return description;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    // Setters
    public void setConfigKey(String configKey) {
        this.configKey = configKey;
    }
    
    public void setConfigValue(String configValue) {
        this.configValue = configValue;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
}
