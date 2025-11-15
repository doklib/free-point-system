package com.musinsa.point.integration;

import com.musinsa.point.dto.*;
import com.musinsa.point.service.PointService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@DisplayName("멱등성 통합 테스트")
class IdempotencyIntegrationTest {

    @Autowired
    private PointService pointService;

    private String userId;

    @BeforeEach
    void setUp() {
        userId = "test-user-" + UUID.randomUUID();
    }

    @Test
    @DisplayName("포인트 적립 - 동일한 멱등성 키로 중복 요청 시 동일한 응답 반환")
    void testEarnPointsIdempotency() {
        String idempotencyKey = UUID.randomUUID().toString();

        EarnRequest request = EarnRequest.builder()
                .userId(userId)
                .amount(1000L)
                .isManualGrant(false)
                .description("멱등성 테스트")
                .build();

        // 첫 번째 요청
        EarnResponse response1 = pointService.earnPoints(request, idempotencyKey);

        // 두 번째 요청 (동일한 멱등성 키)
        EarnResponse response2 = pointService.earnPoints(request, idempotencyKey);

        // 동일한 응답 확인
        assertThat(response1.pointKey()).isEqualTo(response2.pointKey());
        assertThat(response1.amount()).isEqualTo(response2.amount());
        assertThat(response1.totalBalance()).isEqualTo(response2.totalBalance());
        assertThat(response1.createdAt()).isEqualTo(response2.createdAt());

        // 실제로 한 번만 적립되었는지 확인
        BalanceResponse balance = pointService.getBalance(userId);
        assertThat(balance.totalBalance()).isEqualTo(1000L);
    }

    @Test
    @DisplayName("포인트 적립 취소 - 동일한 멱등성 키로 중복 요청 시 동일한 응답 반환")
    void testCancelEarnIdempotency() {
        // 먼저 포인트 적립
        EarnRequest earnRequest = EarnRequest.builder()
                .userId(userId)
                .amount(1000L)
                .isManualGrant(false)
                .description("적립")
                .build();

        EarnResponse earnResponse = pointService.earnPoints(earnRequest, UUID.randomUUID().toString());

        // 적립 취소
        String idempotencyKey = UUID.randomUUID().toString();

        CancelEarnRequest request = CancelEarnRequest.builder()
                .pointKey(earnResponse.pointKey())
                .reason("멱등성 테스트")
                .build();

        // 첫 번째 취소 요청
        CancelEarnResponse response1 = pointService.cancelEarn(request, idempotencyKey);

        // 두 번째 취소 요청 (동일한 멱등성 키)
        CancelEarnResponse response2 = pointService.cancelEarn(request, idempotencyKey);

        // 동일한 응답 확인
        assertThat(response1.cancelPointKey()).isEqualTo(response2.cancelPointKey());
        assertThat(response1.originalPointKey()).isEqualTo(response2.originalPointKey());
        assertThat(response1.canceledAmount()).isEqualTo(response2.canceledAmount());
        assertThat(response1.totalBalance()).isEqualTo(response2.totalBalance());

        // 실제로 한 번만 취소되었는지 확인
        BalanceResponse balance = pointService.getBalance(userId);
        assertThat(balance.totalBalance()).isEqualTo(0L);
    }

    @Test
    @DisplayName("포인트 사용 - 동일한 멱등성 키로 중복 요청 시 동일한 응답 반환")
    void testUsePointsIdempotency() {
        // 먼저 포인트 적립
        EarnRequest earnRequest = EarnRequest.builder()
                .userId(userId)
                .amount(2000L)
                .isManualGrant(false)
                .description("적립")
                .build();

        pointService.earnPoints(earnRequest, UUID.randomUUID().toString());

        // 포인트 사용
        String idempotencyKey = UUID.randomUUID().toString();

        UseRequest request = UseRequest.builder()
                .userId(userId)
                .orderNumber("ORDER-001")
                .amount(1000L)
                .build();

        // 첫 번째 사용 요청
        UseResponse response1 = pointService.usePoints(request, idempotencyKey);

        // 두 번째 사용 요청 (동일한 멱등성 키)
        UseResponse response2 = pointService.usePoints(request, idempotencyKey);

        // 동일한 응답 확인
        assertThat(response1.usePointKey()).isEqualTo(response2.usePointKey());
        assertThat(response1.usedAmount()).isEqualTo(response2.usedAmount());
        assertThat(response1.remainingBalance()).isEqualTo(response2.remainingBalance());
        assertThat(response1.usedFrom()).hasSize(response2.usedFrom().size());

        // 실제로 한 번만 사용되었는지 확인
        BalanceResponse balance = pointService.getBalance(userId);
        assertThat(balance.totalBalance()).isEqualTo(1000L);
    }

    @Test
    @DisplayName("포인트 사용 취소 - 동일한 멱등성 키로 중복 요청 시 동일한 응답 반환")
    void testCancelUseIdempotency() {
        // 먼저 포인트 적립
        EarnRequest earnRequest = EarnRequest.builder()
                .userId(userId)
                .amount(2000L)
                .isManualGrant(false)
                .description("적립")
                .build();

        pointService.earnPoints(earnRequest, UUID.randomUUID().toString());

        // 포인트 사용
        UseRequest useRequest = UseRequest.builder()
                .userId(userId)
                .orderNumber("ORDER-001")
                .amount(1000L)
                .build();

        UseResponse useResponse = pointService.usePoints(useRequest, UUID.randomUUID().toString());

        // 사용 취소
        String idempotencyKey = UUID.randomUUID().toString();

        CancelUseRequest request = CancelUseRequest.builder()
                .orderNumber("ORDER-001")
                .amount(500L)
                .reason("멱등성 테스트")
                .build();

        // 첫 번째 취소 요청
        CancelUseResponse response1 = pointService.cancelUse(request, idempotencyKey);

        // 두 번째 취소 요청 (동일한 멱등성 키)
        CancelUseResponse response2 = pointService.cancelUse(request, idempotencyKey);

        // 동일한 응답 확인
        assertThat(response1.cancelUsePointKey()).isEqualTo(response2.cancelUsePointKey());
        assertThat(response1.originalUsePointKey()).isEqualTo(response2.originalUsePointKey());
        assertThat(response1.canceledAmount()).isEqualTo(response2.canceledAmount());
        assertThat(response1.totalBalance()).isEqualTo(response2.totalBalance());

        // 실제로 한 번만 취소되었는지 확인
        BalanceResponse balance = pointService.getBalance(userId);
        assertThat(balance.totalBalance()).isEqualTo(1500L); // 2000 - 1000 + 500
    }
}
