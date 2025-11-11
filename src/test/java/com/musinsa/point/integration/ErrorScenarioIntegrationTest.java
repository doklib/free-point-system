package com.musinsa.point.integration;

import com.musinsa.point.dto.*;
import com.musinsa.point.exception.PointBusinessException;
import com.musinsa.point.service.PointService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@DisplayName("오류 시나리오 통합 테스트")
class ErrorScenarioIntegrationTest {

    @Autowired
    private PointService pointService;

    private String userId;

    @BeforeEach
    void setUp() {
        userId = "test-user-" + UUID.randomUUID();
    }

    @Test
    @DisplayName("1회 최대 적립 한도 초과 - EXCEED_MAX_EARN_LIMIT 오류")
    void testExceedMaxEarnLimit() {
        EarnRequest request = EarnRequest.builder()
                .userId(userId)
                .amount(100001L) // 최대 한도 100,000 초과
                .isManualGrant(false)
                .description("한도 초과 테스트")
                .build();

        assertThatThrownBy(() -> pointService.earnPoints(request, UUID.randomUUID().toString()))
                .isInstanceOf(PointBusinessException.class)
                .hasMessageContaining("최대 적립 한도")
                .extracting("errorCode")
                .isEqualTo("EXCEED_MAX_EARN_LIMIT");
    }

    @Test
    @DisplayName("개인별 최대 보유 한도 초과 - EXCEED_USER_MAX_BALANCE 오류")
    void testExceedUserMaxBalance() {
        // 먼저 최대 한도에 가까운 포인트를 여러 번 적립 (1회 최대 적립 한도 100,000 이내)
        // 최대 보유 한도 10,000,000에 가까운 금액까지 적립
        for (int i = 0; i < 100; i++) {
            EarnRequest request = EarnRequest.builder()
                    .userId(userId)
                    .amount(99990L) // 100,000 이하
                    .isManualGrant(false)
                    .description("적립 " + (i + 1))
                    .build();
            pointService.earnPoints(request, UUID.randomUUID().toString());
        }
        // 현재 잔액: 9,999,000

        // 추가 적립으로 한도 초과 시도
        EarnRequest request2 = EarnRequest.builder()
                .userId(userId)
                .amount(2000L) // 총 10,001,000이 되어 한도 초과
                .isManualGrant(false)
                .description("한도 초과 적립")
                .build();

        assertThatThrownBy(() -> pointService.earnPoints(request2, UUID.randomUUID().toString()))
                .isInstanceOf(PointBusinessException.class)
                .hasMessageContaining("최대 보유 한도")
                .extracting("errorCode")
                .isEqualTo("EXCEED_USER_MAX_BALANCE");
    }

    @Test
    @DisplayName("사용 가능 포인트 부족 - INSUFFICIENT_POINT_BALANCE 오류")
    void testInsufficientPointBalance() {
        // 1000원만 적립
        EarnRequest earnRequest = EarnRequest.builder()
                .userId(userId)
                .amount(1000L)
                .isManualGrant(false)
                .description("적립")
                .build();

        pointService.earnPoints(earnRequest, UUID.randomUUID().toString());

        // 2000원 사용 시도
        UseRequest useRequest = UseRequest.builder()
                .userId(userId)
                .orderNumber("ORDER-001")
                .amount(2000L)
                .build();

        assertThatThrownBy(() -> pointService.usePoints(useRequest, UUID.randomUUID().toString()))
                .isInstanceOf(PointBusinessException.class)
                .hasMessageContaining("부족")
                .extracting("errorCode")
                .isEqualTo("INSUFFICIENT_POINT_BALANCE");
    }

    @Test
    @DisplayName("존재하지 않는 pointKey - POINT_KEY_NOT_FOUND 오류")
    void testPointKeyNotFound() {
        CancelEarnRequest request = CancelEarnRequest.builder()
                .pointKey("NON_EXISTENT_KEY")
                .reason("존재하지 않는 키 테스트")
                .build();

        assertThatThrownBy(() -> pointService.cancelEarn(request, UUID.randomUUID().toString()))
                .isInstanceOf(PointBusinessException.class)
                .hasMessageContaining("찾을 수 없")
                .extracting("errorCode")
                .isEqualTo("POINT_KEY_NOT_FOUND");
    }

    @Test
    @DisplayName("사용된 포인트 적립 취소 시도 - CANNOT_CANCEL_USED_POINT 오류")
    void testCannotCancelUsedPoint() {
        // 포인트 적립
        EarnRequest earnRequest = EarnRequest.builder()
                .userId(userId)
                .amount(1000L)
                .isManualGrant(false)
                .description("적립")
                .build();

        EarnResponse earnResponse = pointService.earnPoints(earnRequest, UUID.randomUUID().toString());

        // 포인트 사용
        UseRequest useRequest = UseRequest.builder()
                .userId(userId)
                .orderNumber("ORDER-001")
                .amount(500L)
                .build();

        pointService.usePoints(useRequest, UUID.randomUUID().toString());

        // 사용된 포인트 적립 취소 시도
        CancelEarnRequest cancelRequest = CancelEarnRequest.builder()
                .pointKey(earnResponse.getPointKey())
                .reason("사용된 포인트 취소 시도")
                .build();

        assertThatThrownBy(() -> pointService.cancelEarn(cancelRequest, UUID.randomUUID().toString()))
                .isInstanceOf(PointBusinessException.class)
                .hasMessageContaining("사용")
                .extracting("errorCode")
                .isEqualTo("CANNOT_CANCEL_USED_POINT");
    }

    @Test
    @DisplayName("원래 사용 금액 초과 취소 시도 - EXCEED_ORIGINAL_USE_AMOUNT 오류")
    void testExceedOriginalUseAmount() {
        // 포인트 적립
        EarnRequest earnRequest = EarnRequest.builder()
                .userId(userId)
                .amount(2000L)
                .isManualGrant(false)
                .description("적립")
                .build();

        pointService.earnPoints(earnRequest, UUID.randomUUID().toString());

        // 1000원 사용
        UseRequest useRequest = UseRequest.builder()
                .userId(userId)
                .orderNumber("ORDER-001")
                .amount(1000L)
                .build();

        UseResponse useResponse = pointService.usePoints(useRequest, UUID.randomUUID().toString());

        // 1500원 취소 시도 (원래 사용 금액 1000원 초과)
        CancelUseRequest cancelRequest = CancelUseRequest.builder()
                .orderNumber("ORDER-001")
                .amount(1500L)
                .reason("초과 취소 시도")
                .build();

        assertThatThrownBy(() -> pointService.cancelUse(cancelRequest, UUID.randomUUID().toString()))
                .isInstanceOf(PointBusinessException.class)
                .hasMessageContaining("초과")
                .extracting("errorCode")
                .isEqualTo("EXCEED_ORIGINAL_USE_AMOUNT");
    }

    @Test
    @DisplayName("부분 취소 후 남은 금액 초과 취소 시도")
    void testExceedRemainingAmountAfterPartialCancel() {
        // 포인트 적립
        EarnRequest earnRequest = EarnRequest.builder()
                .userId(userId)
                .amount(2000L)
                .isManualGrant(false)
                .description("적립")
                .build();

        pointService.earnPoints(earnRequest, UUID.randomUUID().toString());

        // 1000원 사용
        UseRequest useRequest = UseRequest.builder()
                .userId(userId)
                .orderNumber("ORDER-001")
                .amount(1000L)
                .build();

        UseResponse useResponse = pointService.usePoints(useRequest, UUID.randomUUID().toString());

        // 600원 부분 취소
        CancelUseRequest cancelRequest1 = CancelUseRequest.builder()
                .orderNumber("ORDER-001")
                .amount(600L)
                .reason("부분 취소")
                .build();

        pointService.cancelUse(cancelRequest1, UUID.randomUUID().toString());

        // 남은 400원을 초과하는 500원 취소 시도
        CancelUseRequest cancelRequest2 = CancelUseRequest.builder()
                .orderNumber("ORDER-001")
                .amount(500L)
                .reason("초과 취소 시도")
                .build();

        assertThatThrownBy(() -> pointService.cancelUse(cancelRequest2, UUID.randomUUID().toString()))
                .isInstanceOf(PointBusinessException.class)
                .hasMessageContaining("초과")
                .extracting("errorCode")
                .isEqualTo("EXCEED_ORIGINAL_USE_AMOUNT");
    }

    @Test
    @DisplayName("유효하지 않은 만료일 - INVALID_EXPIRATION_DAYS 오류")
    void testInvalidExpirationDays() {
        // 최소 만료일(1일) 미만
        EarnRequest request1 = EarnRequest.builder()
                .userId(userId)
                .amount(1000L)
                .isManualGrant(false)
                .expirationDays(0)
                .description("만료일 0일")
                .build();

        assertThatThrownBy(() -> pointService.earnPoints(request1, UUID.randomUUID().toString()))
                .isInstanceOf(PointBusinessException.class)
                .hasMessageContaining("만료일")
                .extracting("errorCode")
                .isEqualTo("INVALID_EXPIRATION_DAYS");

        // 최대 만료일(1825일, 5년) 초과
        EarnRequest request2 = EarnRequest.builder()
                .userId(userId)
                .amount(1000L)
                .isManualGrant(false)
                .expirationDays(2000)
                .description("만료일 2000일")
                .build();

        assertThatThrownBy(() -> pointService.earnPoints(request2, UUID.randomUUID().toString()))
                .isInstanceOf(PointBusinessException.class)
                .hasMessageContaining("만료일")
                .extracting("errorCode")
                .isEqualTo("INVALID_EXPIRATION_DAYS");
    }
}
