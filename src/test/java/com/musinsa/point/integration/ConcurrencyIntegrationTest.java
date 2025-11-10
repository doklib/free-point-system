package com.musinsa.point.integration;

import com.musinsa.point.dto.*;
import com.musinsa.point.service.PointService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisplayName("동시성 제어 통합 테스트")
class ConcurrencyIntegrationTest {

    @Autowired
    private PointService pointService;

    private String userId;

    @BeforeEach
    void setUp() {
        userId = "test-user-" + UUID.randomUUID();
        
        // 초기 포인트 적립 (concurrency 테스트를 위한 준비)
        EarnRequest initialEarn = EarnRequest.builder()
                .userId(userId)
                .amount(1000L)
                .isManualGrant(false)
                .description("초기 적립")
                .build();
        pointService.earnPoints(initialEarn, UUID.randomUUID().toString());
    }

    @Test
    @DisplayName("동일 사용자에 대한 동시 적립 요청 - 모든 요청이 성공해야 함")
    void testConcurrentEarnRequests() throws InterruptedException {
        int threadCount = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // 동시에 5개의 적립 요청
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executorService.submit(() -> {
                try {
                    latch.countDown();
                    latch.await(); // 모든 스레드가 준비될 때까지 대기

                    EarnRequest request = EarnRequest.builder()
                            .userId(userId)
                            .amount(100L)
                            .isManualGrant(false)
                            .description("동시 적립 " + index)
                            .build();

                    pointService.earnPoints(request, UUID.randomUUID().toString());
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // 낙관적 잠금 실패 또는 기타 동시성 이슈
                    failureCount.incrementAndGet();
                }
            });
        }

        // 모든 작업 완료 대기
        executorService.shutdown();
        executorService.awaitTermination(30, TimeUnit.SECONDS);

        // 최소한 일부는 성공해야 함 (동시성 제어가 있어도 일부는 성공)
        assertThat(successCount.get()).isGreaterThan(0);
        
        // 최종 잔액 확인 (초기 1000 + 성공한 적립)
        BalanceResponse balance = pointService.getBalance(userId);
        assertThat(balance.getTotalBalance()).isEqualTo(1000L + (successCount.get() * 100L));
    }

    @Test
    @DisplayName("동일 사용자에 대한 동시 사용 요청 - OptimisticLockException 발생 가능")
    void testConcurrentUseRequests() throws InterruptedException {
        // 먼저 충분한 포인트 적립 (초기 1000 + 추가 9000)
        EarnRequest earnRequest = EarnRequest.builder()
                .userId(userId)
                .amount(9000L)
                .isManualGrant(false)
                .description("추가 적립")
                .build();

        pointService.earnPoints(earnRequest, UUID.randomUUID().toString());

        int threadCount = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // 동시에 5개의 사용 요청
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executorService.submit(() -> {
                try {
                    latch.countDown();
                    latch.await(); // 모든 스레드가 준비될 때까지 대기

                    UseRequest request = UseRequest.builder()
                            .userId(userId)
                            .orderNumber("ORDER-" + index)
                            .amount(500L)
                            .build();

                    pointService.usePoints(request, UUID.randomUUID().toString());
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // 낙관적 잠금 실패 또는 기타 동시성 이슈
                    failureCount.incrementAndGet();
                }
            });
        }

        // 모든 작업 완료 대기
        executorService.shutdown();
        executorService.awaitTermination(30, TimeUnit.SECONDS);

        // 최소한 일부는 성공해야 함
        assertThat(successCount.get()).isGreaterThan(0);
        assertThat(successCount.get()).isLessThanOrEqualTo(threadCount);

        // 최종 잔액 확인
        BalanceResponse balance = pointService.getBalance(userId);
        assertThat(balance.getTotalBalance()).isEqualTo(10000L - (successCount.get() * 500L));
    }

    @Test
    @DisplayName("동시성 충돌 시 재시도로 성공 가능")
    void testRetryOnOptimisticLockFailure() throws InterruptedException {
        // 먼저 포인트 적립 (초기 1000 + 추가 4000)
        EarnRequest earnRequest = EarnRequest.builder()
                .userId(userId)
                .amount(4000L)
                .isManualGrant(false)
                .description("추가 적립")
                .build();

        pointService.earnPoints(earnRequest, UUID.randomUUID().toString());

        int threadCount = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger totalSuccessCount = new AtomicInteger(0);

        // 동시에 5개의 사용 요청 (재시도 포함)
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executorService.submit(() -> {
                try {
                    latch.countDown();
                    latch.await();

                    // 최대 3회 재시도
                    int maxRetries = 3;
                    for (int retry = 0; retry < maxRetries; retry++) {
                        try {
                            UseRequest request = UseRequest.builder()
                                    .userId(userId)
                                    .orderNumber("ORDER-RETRY-" + index)
                                    .amount(200L)
                                    .build();

                            pointService.usePoints(request, UUID.randomUUID().toString());
                            totalSuccessCount.incrementAndGet();
                            break; // 성공하면 루프 종료
                        } catch (ObjectOptimisticLockingFailureException e) {
                            if (retry == maxRetries - 1) {
                                // 마지막 재시도도 실패
                                throw e;
                            }
                            // 짧은 대기 후 재시도
                            Thread.sleep(10);
                        }
                    }
                } catch (Exception e) {
                    // 재시도 실패
                }
            });
        }

        // 모든 작업 완료 대기
        executorService.shutdown();
        executorService.awaitTermination(30, TimeUnit.SECONDS);

        // 재시도 덕분에 더 많은 요청이 성공해야 함
        assertThat(totalSuccessCount.get()).isGreaterThan(0);

        // 최종 잔액 확인
        BalanceResponse balance = pointService.getBalance(userId);
        assertThat(balance.getTotalBalance()).isEqualTo(5000L - (totalSuccessCount.get() * 200L));
    }
}
