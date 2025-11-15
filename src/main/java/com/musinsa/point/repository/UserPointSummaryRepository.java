package com.musinsa.point.repository;

import com.musinsa.point.domain.UserPointSummary;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /**
     * 사용자 ID로 포인트 요약 조회 (Pessimistic Write Lock)
     * 동시성 제어를 위해 사용
     * 
     * @param userId 사용자 ID
     * @return 사용자 포인트 요약
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM UserPointSummary s WHERE s.userId = :userId")
    Optional<UserPointSummary> findByUserIdWithLock(@Param("userId") String userId);
}
