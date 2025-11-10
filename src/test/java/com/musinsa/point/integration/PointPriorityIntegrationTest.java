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
@DisplayName("포인트 사용 우선순위 통합 테스트")
class PointPriorityIntegrationTest {

    @Autowired
    private PointService pointService;

    private String userId;

    @BeforeEach
    void setUp() {
        userId = "test-user-" + UUID.randomUUID();
    }

    @Test
    @DisplayName("수기 지급 포인트가 일반 포인트보다 먼저 사용됨")
    void testManualGrantPriorityOverRegular() {
        // 일반 포인트 적립
        EarnRequest regularRequest = EarnRequest.builder()
                .userId(userId)
                .amount(1000L)
                .isManualGrant(false)
                .description("일반 적립")
                .build();

        EarnResponse regularResponse = pointService.earnPoints(regularRequest, UUID.randomUUID().toString());

        // 수기 지급 포인트 적립
        EarnRequest manualRequest = EarnRequest.builder()
                .userId(userId)
                .amount(500L)
                .isManualGrant(true)
                .description("수기 지급")
                .build();

        EarnResponse manualResponse = pointService.earnPoints(manualRequest, UUID.randomUUID().toString());

        // 800원 사용
        UseRequest useRequest = UseRequest.builder()
                .userId(userId)
                .orderNumber("ORDER-001")
                .amount(800L)
                .build();

        UseResponse useResponse = pointService.usePoints(useRequest, UUID.randomUUID().toString());

        // 수기 지급 포인트가 먼저 사용되어야 함
        assertThat(useResponse.getUsedFrom()).hasSize(2);
        assertThat(useResponse.getUsedFrom().get(0).getEarnPointKey()).isEqualTo(manualResponse.getPointKey());
        assertThat(useResponse.getUsedFrom().get(0).getUsedAmount()).isEqualTo(500L);
        assertThat(useResponse.getUsedFrom().get(1).getEarnPointKey()).isEqualTo(regularResponse.getPointKey());
        assertThat(useResponse.getUsedFrom().get(1).getUsedAmount()).isEqualTo(300L);
    }

    @Test
    @DisplayName("만료일이 짧은 포인트가 먼저 사용됨")
    void testExpirationDatePriority() {
        // 만료일 30일 포인트 적립
        EarnRequest request30Days = EarnRequest.builder()
                .userId(userId)
                .amount(1000L)
                .isManualGrant(false)
                .expirationDays(30)
                .description("30일 만료")
                .build();

        EarnResponse response30Days = pointService.earnPoints(request30Days, UUID.randomUUID().toString());

        // 만료일 365일 포인트 적립
        EarnRequest request365Days = EarnRequest.builder()
                .userId(userId)
                .amount(500L)
                .isManualGrant(false)
                .expirationDays(365)
                .description("365일 만료")
                .build();

        EarnResponse response365Days = pointService.earnPoints(request365Days, UUID.randomUUID().toString());

        // 만료일 10일 포인트 적립
        EarnRequest request10Days = EarnRequest.builder()
                .userId(userId)
                .amount(300L)
                .isManualGrant(false)
                .expirationDays(10)
                .description("10일 만료")
                .build();

        EarnResponse response10Days = pointService.earnPoints(request10Days, UUID.randomUUID().toString());

        // 1200원 사용
        UseRequest useRequest = UseRequest.builder()
                .userId(userId)
                .orderNumber("ORDER-001")
                .amount(1200L)
                .build();

        UseResponse useResponse = pointService.usePoints(useRequest, UUID.randomUUID().toString());

        // 만료일이 짧은 순서로 사용되어야 함: 10일 -> 30일
        // 1200원 사용: 10일(300) + 30일(900) = 1200
        assertThat(useResponse.getUsedFrom()).hasSize(2);
        assertThat(useResponse.getUsedFrom().get(0).getEarnPointKey()).isEqualTo(response10Days.getPointKey());
        assertThat(useResponse.getUsedFrom().get(0).getUsedAmount()).isEqualTo(300L);
        assertThat(useResponse.getUsedFrom().get(1).getEarnPointKey()).isEqualTo(response30Days.getPointKey());
        assertThat(useResponse.getUsedFrom().get(1).getUsedAmount()).isEqualTo(900L);
    }

    @Test
    @DisplayName("수기 지급 포인트 내에서도 만료일이 짧은 것이 먼저 사용됨")
    void testManualGrantWithExpirationPriority() {
        // 수기 지급 - 만료일 30일
        EarnRequest manual30Days = EarnRequest.builder()
                .userId(userId)
                .amount(500L)
                .isManualGrant(true)
                .expirationDays(30)
                .description("수기 30일")
                .build();

        EarnResponse response30Days = pointService.earnPoints(manual30Days, UUID.randomUUID().toString());

        // 수기 지급 - 만료일 10일
        EarnRequest manual10Days = EarnRequest.builder()
                .userId(userId)
                .amount(300L)
                .isManualGrant(true)
                .expirationDays(10)
                .description("수기 10일")
                .build();

        EarnResponse response10Days = pointService.earnPoints(manual10Days, UUID.randomUUID().toString());

        // 일반 적립 - 만료일 5일 (수기보다 짧지만 우선순위 낮음)
        EarnRequest regular5Days = EarnRequest.builder()
                .userId(userId)
                .amount(200L)
                .isManualGrant(false)
                .expirationDays(5)
                .description("일반 5일")
                .build();

        EarnResponse response5Days = pointService.earnPoints(regular5Days, UUID.randomUUID().toString());

        // 700원 사용
        UseRequest useRequest = UseRequest.builder()
                .userId(userId)
                .orderNumber("ORDER-001")
                .amount(700L)
                .build();

        UseResponse useResponse = pointService.usePoints(useRequest, UUID.randomUUID().toString());

        // 수기 지급이 먼저, 그 중에서도 만료일이 짧은 순서
        assertThat(useResponse.getUsedFrom()).hasSize(2);
        assertThat(useResponse.getUsedFrom().get(0).getEarnPointKey()).isEqualTo(response10Days.getPointKey());
        assertThat(useResponse.getUsedFrom().get(0).getUsedAmount()).isEqualTo(300L);
        assertThat(useResponse.getUsedFrom().get(1).getEarnPointKey()).isEqualTo(response30Days.getPointKey());
        assertThat(useResponse.getUsedFrom().get(1).getUsedAmount()).isEqualTo(400L);
    }

    @Test
    @DisplayName("복잡한 우선순위 시나리오 - 수기/일반, 다양한 만료일")
    void testComplexPriorityScenario() {
        // 일반 - 5일
        EarnRequest regular5 = EarnRequest.builder()
                .userId(userId)
                .amount(100L)
                .isManualGrant(false)
                .expirationDays(5)
                .description("일반 5일")
                .build();
        EarnResponse response5 = pointService.earnPoints(regular5, UUID.randomUUID().toString());

        // 수기 - 20일
        EarnRequest manual20 = EarnRequest.builder()
                .userId(userId)
                .amount(200L)
                .isManualGrant(true)
                .expirationDays(20)
                .description("수기 20일")
                .build();
        EarnResponse response20 = pointService.earnPoints(manual20, UUID.randomUUID().toString());

        // 일반 - 10일
        EarnRequest regular10 = EarnRequest.builder()
                .userId(userId)
                .amount(150L)
                .isManualGrant(false)
                .expirationDays(10)
                .description("일반 10일")
                .build();
        EarnResponse response10 = pointService.earnPoints(regular10, UUID.randomUUID().toString());

        // 수기 - 15일
        EarnRequest manual15 = EarnRequest.builder()
                .userId(userId)
                .amount(250L)
                .isManualGrant(true)
                .expirationDays(15)
                .description("수기 15일")
                .build();
        EarnResponse response15 = pointService.earnPoints(manual15, UUID.randomUUID().toString());

        // 일반 - 30일
        EarnRequest regular30 = EarnRequest.builder()
                .userId(userId)
                .amount(300L)
                .isManualGrant(false)
                .expirationDays(30)
                .description("일반 30일")
                .build();
        EarnResponse response30 = pointService.earnPoints(regular30, UUID.randomUUID().toString());

        // 600원 사용
        UseRequest useRequest = UseRequest.builder()
                .userId(userId)
                .orderNumber("ORDER-001")
                .amount(600L)
                .build();

        UseResponse useResponse = pointService.usePoints(useRequest, UUID.randomUUID().toString());

        // 예상 사용 순서: 수기 15일(250) -> 수기 20일(200) -> 일반 5일(100) -> 일반 10일(50)
        assertThat(useResponse.getUsedFrom()).hasSize(4);
        assertThat(useResponse.getUsedFrom().get(0).getEarnPointKey()).isEqualTo(response15.getPointKey());
        assertThat(useResponse.getUsedFrom().get(0).getUsedAmount()).isEqualTo(250L);
        assertThat(useResponse.getUsedFrom().get(1).getEarnPointKey()).isEqualTo(response20.getPointKey());
        assertThat(useResponse.getUsedFrom().get(1).getUsedAmount()).isEqualTo(200L);
        assertThat(useResponse.getUsedFrom().get(2).getEarnPointKey()).isEqualTo(response5.getPointKey());
        assertThat(useResponse.getUsedFrom().get(2).getUsedAmount()).isEqualTo(100L);
        assertThat(useResponse.getUsedFrom().get(3).getEarnPointKey()).isEqualTo(response10.getPointKey());
        assertThat(useResponse.getUsedFrom().get(3).getUsedAmount()).isEqualTo(50L);
    }
}
