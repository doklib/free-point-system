package com.musinsa.point.repository;

import com.musinsa.point.domain.PointAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PointAccountRepository extends JpaRepository<PointAccount, Long> {
    
    /**
     * 사용 포인트 키로 계정 조회
     * 
     * @param usePointKey 사용 트랜잭션의 포인트 키
     * @return 해당 사용에 대한 계정 목록
     */
    List<PointAccount> findByUsePointKey(String usePointKey);
    
    /**
     * 적립 포인트 키로 계정 조회
     * 
     * @param earnPointKey 적립 트랜잭션의 포인트 키
     * @return 해당 적립에서 사용된 계정 목록
     */
    List<PointAccount> findByEarnPointKey(String earnPointKey);
}
