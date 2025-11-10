package com.musinsa.point.repository;

import com.musinsa.point.domain.UserPointSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserPointSummaryRepository extends JpaRepository<UserPointSummary, Long> {
    
    /**
     * 사용자 ID로 포인트 요약 조회
     * 
     * @param userId 사용자 ID
     * @return 사용자 포인트 요약
     */
    Optional<UserPointSummary> findByUserId(String userId);
}
