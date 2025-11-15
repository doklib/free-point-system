package com.musinsa.point.service;

import com.musinsa.point.dto.CancelEarnRequest;
import com.musinsa.point.dto.CancelEarnResponse;
import com.musinsa.point.dto.CancelUseRequest;
import com.musinsa.point.dto.BalanceResponse;
import com.musinsa.point.dto.CancelUseResponse;
import com.musinsa.point.dto.EarnRequest;
import com.musinsa.point.dto.EarnResponse;
import com.musinsa.point.dto.HistoryResponse;
import com.musinsa.point.dto.UseRequest;
import com.musinsa.point.dto.UseResponse;
import org.springframework.stereotype.Service;

/**
 * 포인트 비즈니스 로직을 처리하는 파사드 서비스
 * 실제 구현은 PointEarnService, PointUseService, PointQueryService에 위임
 */
@Service
public class PointService {

    private final PointEarnService pointEarnService;
    private final PointUseService pointUseService;
    private final PointQueryService pointQueryService;

    public PointService(PointEarnService pointEarnService,
                       PointUseService pointUseService,
                       PointQueryService pointQueryService) {
        this.pointEarnService = pointEarnService;
        this.pointUseService = pointUseService;
        this.pointQueryService = pointQueryService;
    }

    /**
     * 포인트 적립
     *
     * @param request 적립 요청
     * @param idempotencyKey 멱등성 키
     * @return 적립 응답
     */
    public EarnResponse earnPoints(EarnRequest request, String idempotencyKey) {
        return pointEarnService.earnPoints(request, idempotencyKey);
    }

    /**
     * 포인트 적립 취소
     *
     * @param request 적립 취소 요청
     * @param idempotencyKey 멱등성 키
     * @return 적립 취소 응답
     */
    public CancelEarnResponse cancelEarn(CancelEarnRequest request, String idempotencyKey) {
        return pointEarnService.cancelEarn(request, idempotencyKey);
    }

    /**
     * 포인트 사용
     *
     * @param request 사용 요청
     * @param idempotencyKey 멱등성 키
     * @return 사용 응답
     */
    public UseResponse usePoints(UseRequest request, String idempotencyKey) {
        return pointUseService.usePoints(request, idempotencyKey);
    }

    /**
     * 포인트 사용 취소
     *
     * @param request 사용 취소 요청
     * @param idempotencyKey 멱등성 키
     * @return 사용 취소 응답
     */
    public CancelUseResponse cancelUse(CancelUseRequest request, String idempotencyKey) {
        return pointUseService.cancelUse(request, idempotencyKey);
    }

    /**
     * 포인트 잔액 조회
     *
     * @param userId 사용자 ID
     * @return 잔액 응답
     */
    public BalanceResponse getBalance(String userId) {
        return pointQueryService.getBalance(userId);
    }

    /**
     * 포인트 이력 조회
     *
     * @param userId 사용자 ID
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @return 이력 응답
     */
    public HistoryResponse getHistory(String userId, int page, int size) {
        return pointQueryService.getHistory(userId, page, size);
    }
}
