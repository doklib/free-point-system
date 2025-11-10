package com.musinsa.point.repository;

import com.musinsa.point.domain.PointTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PointTransactionRepository extends JpaRepository<PointTransaction, Long> {
    
    /**
     * pointKey로 포인트 트랜잭션 조회
     */
    Optional<PointTransaction> findByPointKey(String pointKey);
    
    /**
     * userId로 포인트 트랜잭션 목록 조회 (페이징)
     */
    Page<PointTransaction> findByUserId(String userId, Pageable pageable);
    
    /**
     * orderNumber로 포인트 사용 트랜잭션 조회
     */
    Optional<PointTransaction> findByOrderNumber(String orderNumber);
    
    /**
     * 사용 가능한 포인트 조회 (수기 지급 우선, 만료일 순, 적립일 순)
     * 
     * 성능 최적화:
     * - idx_available_points 복합 인덱스를 사용하여 쿼리 성능 최적화
     * - WHERE 절의 모든 조건과 ORDER BY 절의 컬럼이 인덱스에 포함됨
     * - N+1 문제 없음: 단일 쿼리로 모든 데이터 조회
     * 
     * @param userId 사용자 ID
     * @param now 현재 시간
     * @return 사용 가능한 포인트 트랜잭션 목록 (우선순위 정렬)
     */
    @Query("""
        SELECT pt FROM PointTransaction pt
        WHERE pt.userId = :userId
        AND pt.transactionType = 'EARN'
        AND pt.availableBalance > 0
        AND pt.expirationDate > :now
        ORDER BY pt.isManualGrant DESC, pt.expirationDate ASC, pt.createdAt ASC
    """)
    List<PointTransaction> findAvailablePointsForUse(
        @Param("userId") String userId,
        @Param("now") LocalDateTime now
    );
}
