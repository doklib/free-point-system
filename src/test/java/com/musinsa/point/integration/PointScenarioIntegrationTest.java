package com.musinsa.point.integration;

import com.musinsa.point.domain.PointTransaction;
import com.musinsa.point.dto.*;
import com.musinsa.point.repository.PointTransactionRepository;
import com.musinsa.point.service.PointService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@DisplayName("포인트 시스템 시나리오 통합 테스트")
class PointScenarioIntegrationTest {

    @Autowired
    private PointService pointService;

    @Autowired
    private PointTransactionRepository pointTransactionRepository;

    private String userId;

    @BeforeEach
    void setUp() {
        userId = "test-user-" + UUID.randomUUID();
    }

    @Test
    @DisplayName("과제 예시 시나리오: 적립 -> 사용 -> 만료 -> 부분 취소")
    void testCompleteScenario() {
        // 1. 1000원 적립 (pointKey: A)
        EarnRequest earnRequestA = EarnRequest.builder()
                .userId(userId)
                .amount(1000L)
                .isManualGrant(false)
                .description("첫 번째 적립")
                .build();

        EarnResponse earnResponseA = pointService.earnPoints(earnRequestA, UUID.randomUUID().toString());
        String pointKeyA = earnResponseA.pointKey();

        assertThat(earnResponseA.amount()).isEqualTo(1000L);
        assertThat(earnResponseA.availableBalance()).isEqualTo(1000L);
        assertThat(earnResponseA.totalBalance()).isEqualTo(1000L);

        // 2. 500원 적립 (pointKey: B)
        EarnRequest earnRequestB = EarnRequest.builder()
                .userId(userId)
                .amount(500L)
                .isManualGrant(false)
                .description("두 번째 적립")
                .build();

        EarnResponse earnResponseB = pointService.earnPoints(earnRequestB, UUID.randomUUID().toString());
        String pointKeyB = earnResponseB.pointKey();

        assertThat(earnResponseB.amount()).isEqualTo(500L);
        assertThat(earnResponseB.availableBalance()).isEqualTo(500L);
        assertThat(earnResponseB.totalBalance()).isEqualTo(1500L);

        // 3. 주문 A1234에서 1200원 사용 (A에서 1000원, B에서 200원)
        UseRequest useRequest = UseRequest.builder()
                .userId(userId)
                .orderNumber("A1234")
                .amount(1200L)
                .build();

        UseResponse useResponse = pointService.usePoints(useRequest, UUID.randomUUID().toString());
        String pointKeyC = useResponse.usePointKey();

        assertThat(useResponse.usedAmount()).isEqualTo(1200L);
        assertThat(useResponse.remainingBalance()).isEqualTo(300L);
        assertThat(useResponse.usedFrom()).hasSize(2);
        assertThat(useResponse.usedFrom().get(0).earnPointKey()).isEqualTo(pointKeyA);
        assertThat(useResponse.usedFrom().get(0).usedAmount()).isEqualTo(1000L);
        assertThat(useResponse.usedFrom().get(1).earnPointKey()).isEqualTo(pointKeyB);
        assertThat(useResponse.usedFrom().get(1).usedAmount()).isEqualTo(200L);

        // 4. A의 만료일을 과거로 설정
        PointTransaction transactionA = pointTransactionRepository.findByPointKey(pointKeyA)
                .orElseThrow();
        transactionA.setExpirationDate(LocalDateTime.now().minusDays(1));
        pointTransactionRepository.save(transactionA);

        // 5. 1100원 부분 사용 취소 (A는 만료되어 신규 적립, B는 복구)
        CancelUseRequest cancelUseRequest = CancelUseRequest.builder()
                .orderNumber("A1234")
                .amount(1100L)
                .reason("부분 취소 테스트")
                .build();

        CancelUseResponse cancelUseResponse = pointService.cancelUse(cancelUseRequest, UUID.randomUUID().toString());

        assertThat(cancelUseResponse.canceledAmount()).isEqualTo(1100L);
        assertThat(cancelUseResponse.totalBalance()).isEqualTo(1400L); // 300 + 1100

        // A는 만료되어 신규 적립되어야 함 (만료된 포인트는 원본 전체 금액을 신규 적립)
        assertThat(cancelUseResponse.newlyEarnedPoints()).hasSize(1);
        assertThat(cancelUseResponse.newlyEarnedPoints().get(0).amount()).isEqualTo(1000L);

        // B는 복구되어야 함
        assertThat(cancelUseResponse.restoredPoints()).hasSize(1);
        assertThat(cancelUseResponse.restoredPoints().get(0).earnPointKey()).isEqualTo(pointKeyB);
        assertThat(cancelUseResponse.restoredPoints().get(0).restoredAmount()).isEqualTo(100L);
        assertThat(cancelUseResponse.restoredPoints().get(0).isExpired()).isFalse();

        // 6. C는 이제 100원만 부분 취소 가능한지 검증
        CancelUseRequest cancelUseRequest2 = CancelUseRequest.builder()
                .orderNumber("A1234")
                .amount(100L)
                .reason("추가 부분 취소")
                .build();

        CancelUseResponse cancelUseResponse2 = pointService.cancelUse(cancelUseRequest2, UUID.randomUUID().toString());

        assertThat(cancelUseResponse2.canceledAmount()).isEqualTo(100L);
        assertThat(cancelUseResponse2.totalBalance()).isEqualTo(1500L); // 1400 + 100

        // B에서 100원 더 복구되어야 함
        assertThat(cancelUseResponse2.restoredPoints()).hasSize(1);
        assertThat(cancelUseResponse2.restoredPoints().get(0).earnPointKey()).isEqualTo(pointKeyB);
        assertThat(cancelUseResponse2.restoredPoints().get(0).restoredAmount()).isEqualTo(100L);

        // 최종 잔액 확인
        BalanceResponse balanceResponse = pointService.getBalance(userId);
        assertThat(balanceResponse.totalBalance()).isEqualTo(1500L);
    }
}
