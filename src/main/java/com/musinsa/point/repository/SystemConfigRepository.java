package com.musinsa.point.repository;

import com.musinsa.point.domain.SystemConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SystemConfigRepository extends JpaRepository<SystemConfig, Long> {
    
    /**
     * 설정 키로 시스템 설정 조회
     * 
     * @param configKey 설정 키
     * @return 시스템 설정
     */
    Optional<SystemConfig> findByConfigKey(String configKey);
}
