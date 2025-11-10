package com.musinsa.point.controller;

import com.musinsa.point.dto.BalanceResponse;
import com.musinsa.point.dto.CancelEarnRequest;
import com.musinsa.point.dto.CancelEarnResponse;
import com.musinsa.point.dto.CancelUseRequest;
import com.musinsa.point.dto.CancelUseResponse;
import com.musinsa.point.dto.EarnRequest;
import com.musinsa.point.dto.EarnResponse;
import com.musinsa.point.dto.ErrorResponse;
import com.musinsa.point.dto.HistoryResponse;
import com.musinsa.point.dto.UseRequest;
import com.musinsa.point.dto.UseResponse;
import com.musinsa.point.service.PointService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Point API", description = "무료 포인트 관리 API")
public class PointController {

    private final PointService pointService;

    /**
     * 포인트 적립
     *
     * @param idempotencyKey 멱등성 키
     * @param request 적립 요청
     * @return 적립 응답
     */
    @Operation(summary = "포인트 적립", description = "사용자에게 포인트를 적립합니다. 멱등성 키를 통해 중복 적립을 방지합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "적립 성공",
            content = @Content(schema = @Schema(implementation = EarnResponse.class))),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 (한도 초과, 유효하지 않은 금액 등)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "동시성 충돌",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/earn")
    public ResponseEntity<EarnResponse> earnPoints(
        @Parameter(description = "멱등성 키 (UUID)", required = true)
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
    @Operation(summary = "포인트 적립 취소", description = "특정 적립을 취소합니다. 이미 사용된 포인트는 취소할 수 없습니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "취소 성공",
            content = @Content(schema = @Schema(implementation = CancelEarnResponse.class))),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 (사용된 포인트 취소 시도 등)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "포인트 키를 찾을 수 없음",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/cancel-earn")
    public ResponseEntity<CancelEarnResponse> cancelEarn(
        @Parameter(description = "멱등성 키 (UUID)", required = true)
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
    @Operation(summary = "포인트 사용", description = "주문 시 포인트를 사용합니다. 수기 지급 포인트가 우선 사용되며, 만료일이 짧은 순서로 차감됩니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "사용 성공",
            content = @Content(schema = @Schema(implementation = UseResponse.class))),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 (잔액 부족 등)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "동시성 충돌",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/use")
    public ResponseEntity<UseResponse> usePoints(
        @Parameter(description = "멱등성 키 (UUID)", required = true)
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
    @Operation(summary = "포인트 사용 취소", description = "주문 취소 시 사용한 포인트를 복구합니다. 만료된 포인트는 신규 적립으로 처리됩니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "취소 성공",
            content = @Content(schema = @Schema(implementation = CancelUseResponse.class))),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 (원래 사용 금액 초과 등)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "사용 포인트 키를 찾을 수 없음",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/cancel-use")
    public ResponseEntity<CancelUseResponse> cancelUse(
        @Parameter(description = "멱등성 키 (UUID)", required = true)
        @RequestHeader("Idempotency-Key") String idempotencyKey,
        @Valid @RequestBody CancelUseRequest request
    ) {
        log.debug("포인트 사용 취소 요청 - orderNumber: {}, amount: {}", 
            request.getOrderNumber(), request.getAmount());
        
        CancelUseResponse response = pointService.cancelUse(request, idempotencyKey);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 포인트 잔액 조회
     *
     * @param userId 사용자 ID
     * @return 잔액 응답
     */
    @Operation(summary = "포인트 잔액 조회", description = "사용자의 현재 포인트 잔액과 사용 가능한 포인트 목록을 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공",
            content = @Content(schema = @Schema(implementation = BalanceResponse.class)))
    })
    @GetMapping("/balance/{userId}")
    public ResponseEntity<BalanceResponse> getBalance(
        @Parameter(name = "userId", description = "사용자 ID", required = true)
        @PathVariable("userId") String userId
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
    @Operation(summary = "포인트 이력 조회", description = "사용자의 포인트 트랜잭션 이력을 페이징하여 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공",
            content = @Content(schema = @Schema(implementation = HistoryResponse.class)))
    })
    @GetMapping("/history/{userId}")
    public ResponseEntity<HistoryResponse> getHistory(
        @Parameter(name = "userId", description = "사용자 ID", required = true)
        @PathVariable("userId") String userId,
        @Parameter(name = "page", description = "페이지 번호 (0부터 시작)")
        @RequestParam(value = "page", defaultValue = "0") int page,
        @Parameter(name = "size", description = "페이지 크기")
        @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        log.debug("포인트 이력 조회 요청 - userId: {}, page: {}, size: {}", userId, page, size);
        
        HistoryResponse response = pointService.getHistory(userId, page, size);
        
        return ResponseEntity.ok(response);
    }
}
