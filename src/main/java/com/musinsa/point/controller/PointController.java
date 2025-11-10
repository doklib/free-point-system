package com.musinsa.point.controller;

import com.musinsa.point.dto.BalanceResponse;
import com.musinsa.point.dto.CancelEarnRequest;
import com.musinsa.point.dto.CancelEarnResponse;
import com.musinsa.point.dto.CancelUseRequest;
import com.musinsa.point.dto.CancelUseResponse;
import com.musinsa.point.dto.EarnRequest;
import com.musinsa.point.dto.EarnResponse;
import com.musinsa.point.dto.HistoryResponse;
import com.musinsa.point.dto.UseRequest;
import com.musinsa.point.dto.UseResponse;
import com.musinsa.point.service.PointService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 포인트 API 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/points")
@RequiredArgsConstructor
public class PointController {

    private final PointService pointService;

    /**
     * 포인트 적립
     *
     * @param idempotencyKey 멱등성 키
     * @param request 적립 요청
     * @return 적립 응답
     */
    @PostMapping("/earn")
    public ResponseEntity<EarnResponse> earnPoints(
        @RequestHeader("Idempotency-Key") String idempotencyKey,
        @Valid @RequestBody EarnRequest request
    ) {
        log.debug("포인트 적립 요청 - userId: {}, amount: {}", request.getUserId(), request.getAmount());
        
        EarnResponse response = pointService.earnPoints(request, idempotencyKey);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 포인트 적립 취소
     *
     * @param idempotencyKey 멱등성 키
     * @param request 적립 취소 요청
     * @return 적립 취소 응답
     */
    @PostMapping("/cancel-earn")
    public ResponseEntity<CancelEarnResponse> cancelEarn(
        @RequestHeader("Idempotency-Key") String idempotencyKey,
        @Valid @RequestBody CancelEarnRequest request
    ) {
        log.debug("포인트 적립 취소 요청 - pointKey: {}", request.getPointKey());
        
        CancelEarnResponse response = pointService.cancelEarn(request, idempotencyKey);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 포인트 사용
     *
     * @param idempotencyKey 멱등성 키
     * @param request 사용 요청
     * @return 사용 응답
     */
    @PostMapping("/use")
    public ResponseEntity<UseResponse> usePoints(
        @RequestHeader("Idempotency-Key") String idempotencyKey,
        @Valid @RequestBody UseRequest request
    ) {
        log.debug("포인트 사용 요청 - userId: {}, orderNumber: {}, amount: {}", 
            request.getUserId(), request.getOrderNumber(), request.getAmount());
        
        UseResponse response = pointService.usePoints(request, idempotencyKey);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 포인트 사용 취소
     *
     * @param idempotencyKey 멱등성 키
     * @param request 사용 취소 요청
     * @return 사용 취소 응답
     */
    @PostMapping("/cancel-use")
    public ResponseEntity<CancelUseResponse> cancelUse(
        @RequestHeader("Idempotency-Key") String idempotencyKey,
        @Valid @RequestBody CancelUseRequest request
    ) {
        log.debug("포인트 사용 취소 요청 - usePointKey: {}, amount: {}", 
            request.getUsePointKey(), request.getAmount());
        
        CancelUseResponse response = pointService.cancelUse(request, idempotencyKey);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 포인트 잔액 조회
     *
     * @param userId 사용자 ID
     * @return 잔액 응답
     */
    @GetMapping("/balance/{userId}")
    public ResponseEntity<BalanceResponse> getBalance(
        @PathVariable String userId
    ) {
        log.debug("포인트 잔액 조회 요청 - userId: {}", userId);
        
        BalanceResponse response = pointService.getBalance(userId);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 포인트 이력 조회
     *
     * @param userId 사용자 ID
     * @param page 페이지 번호 (0부터 시작, 기본값: 0)
     * @param size 페이지 크기 (기본값: 20)
     * @return 이력 응답
     */
    @GetMapping("/history/{userId}")
    public ResponseEntity<HistoryResponse> getHistory(
        @PathVariable String userId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        log.debug("포인트 이력 조회 요청 - userId: {}, page: {}, size: {}", userId, page, size);
        
        HistoryResponse response = pointService.getHistory(userId, page, size);
        
        return ResponseEntity.ok(response);
    }
}
