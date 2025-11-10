package com.musinsa.point.repository;

import com.musinsa.point.domain.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, Long> {
    
    /**
     * 멱등성 키로 레코드 조회
     * 
     * @param idempotencyKey 멱등성 키
     * @return 멱등성 레코드
     */
    Optional<IdempotencyRecord> findByIdempotencyKey(String idempotencyKey);
    
    /**
     * 만료된 레코드 삭제
     * 
     * @param expiresAt 만료 시간
     * @return 삭제된 레코드 수
     */
    int deleteByExpiresAtBefore(LocalDateTime expiresAt);
}
