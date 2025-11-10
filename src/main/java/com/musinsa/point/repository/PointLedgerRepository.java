package com.musinsa.point.repository;

import com.musinsa.point.domain.PointLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PointLedgerRepository extends JpaRepository<PointLedger, Long> {
    
    /**
     * 사용 포인트 키로 원장 조회
     * 
     * @param usePointKey 사용 트랜잭션의 포인트 키
     * @return 해당 사용에 대한 원장 목록
     */
    List<PointLedger> findByUsePointKey(String usePointKey);
    
    /**
     * 적립 포인트 키로 원장 조회
     * 
     * @param earnPointKey 적립 트랜잭션의 포인트 키
     * @return 해당 적립에서 사용된 원장 목록
     */
    List<PointLedger> findByEarnPointKey(String earnPointKey);
}
