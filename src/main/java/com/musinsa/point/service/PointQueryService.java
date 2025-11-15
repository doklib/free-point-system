package com.musinsa.point.service;

import com.musinsa.point.domain.PointTransaction;
import com.musinsa.point.domain.UserPointSummary;
import com.musinsa.point.dto.AvailablePointDetail;
import com.musinsa.point.dto.BalanceResponse;
import com.musinsa.point.dto.HistoryResponse;
import com.musinsa.point.dto.PageInfo;
import com.musinsa.point.dto.TransactionDetail;
import com.musinsa.point.repository.PointTransactionRepository;
import com.musinsa.point.repository.UserPointSummaryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 포인트 조회 서비스
 * 잔액 조회 및 거래 내역 조회를 담당
 */
@Service
public class PointQueryService {

    private static final Logger log = LoggerFactory.getLogger(PointQueryService.class);

    private final PointTransactionRepository pointTransactionRepository;
    private final UserPointSummaryRepository userPointSummaryRepository;

    public PointQueryService(PointTransactionRepository pointTransactionRepository,
                            UserPointSummaryRepository userPointSummaryRepository) {
        this.pointTransactionRepository = pointTransactionRepository;
        this.userPointSummaryRepository = userPointSummaryRepository;
    }

    /**
     * 포인트 잔액 조회
     *
     * @param userId 사용자 ID
     * @return 잔액 응답
     */
    @Transactional(readOnly = true)
    public BalanceResponse getBalance(String userId) {
        String requestId = MDC.get("requestId");
        
        log.info("[{}] 포인트 잔액 조회 시작 - userId: {}", requestId, userId);

        try {
            // 1. UserPointSummary 조회
            UserPointSummary summary = userPointSummaryRepository.findByUserId(userId)
                .orElseGet(() -> {
                    UserPointSummary newSummary = new UserPointSummary();
                    newSummary.setUserId(userId);
                    newSummary.setTotalBalance(0L);
                    return newSummary;
                });

            // 2. 사용 가능한 PointTransaction 목록 조회 (만료되지 않고 availableBalance > 0)
            LocalDateTime now = LocalDateTime.now();
            List<PointTransaction> availableTransactions = pointTransactionRepository.findAvailablePointsForUse(
                userId, 
                now
            );

            // 3. AvailablePointDetail 목록 생성
            List<AvailablePointDetail> availablePoints = availableTransactions.stream()
                .map(transaction -> AvailablePointDetail.builder()
                    .pointKey(transaction.getPointKey())
                    .amount(transaction.getAmount())
                    .availableBalance(transaction.getAvailableBalance())
                    .isManualGrant(transaction.getIsManualGrant())
                    .expirationDate(transaction.getExpirationDate())
                    .build())
                .collect(Collectors.toList());

            // 4. BalanceResponse 생성
            BalanceResponse response = BalanceResponse.builder()
                .userId(userId)
                .totalBalance(summary.getTotalBalance())
                .availablePoints(availablePoints)
                .build();

            log.info("[{}] 포인트 잔액 조회 완료 - userId: {}, totalBalance: {}, availablePointsCount: {}",
                requestId, userId, summary.getTotalBalance(), availablePoints.size());

            return response;

        } catch (Exception ex) {
            log.error("[{}] 포인트 잔액 조회 중 예상치 못한 오류 발생", requestId, ex);
            throw new RuntimeException("포인트 잔액 조회 중 오류가 발생했습니다", ex);
        }
    }

    /**
     * 포인트 이력 조회
     *
     * @param userId 사용자 ID
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @return 이력 응답
     */
    @Transactional(readOnly = true)
    public HistoryResponse getHistory(String userId, int page, int size) {
        String requestId = MDC.get("requestId");
        
        log.info("[{}] 포인트 이력 조회 시작 - userId: {}, page: {}, size: {}", 
            requestId, userId, page, size);

        try {
            // 1. PointTransaction 목록 조회 (userId로, 페이징, 최신순 정렬)
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<PointTransaction> transactionPage = pointTransactionRepository.findByUserId(userId, pageable);

            // 2. TransactionDetail 목록 생성
            List<TransactionDetail> transactions = transactionPage.getContent().stream()
                .map(transaction -> TransactionDetail.builder()
                    .pointKey(transaction.getPointKey())
                    .type(transaction.getTransactionType())
                    .amount(transaction.getAmount())
                    .balance(transaction.getAvailableBalance())
                    .orderNumber(transaction.getOrderNumber())
                    .description(transaction.getDescription())
                    .createdAt(transaction.getCreatedAt())
                    .build())
                .collect(Collectors.toList());

            // 3. PageInfo 생성
            PageInfo pageInfo = PageInfo.builder()
                .number(transactionPage.getNumber())
                .size(transactionPage.getSize())
                .totalElements(transactionPage.getTotalElements())
                .totalPages(transactionPage.getTotalPages())
                .build();

            // 4. HistoryResponse 생성
            HistoryResponse response = HistoryResponse.builder()
                .userId(userId)
                .transactions(transactions)
                .page(pageInfo)
                .build();

            log.info("[{}] 포인트 이력 조회 완료 - userId: {}, transactionCount: {}, totalElements: {}",
                requestId, userId, transactions.size(), transactionPage.getTotalElements());

            return response;

        } catch (Exception ex) {
            log.error("[{}] 포인트 이력 조회 중 예상치 못한 오류 발생", requestId, ex);
            throw new RuntimeException("포인트 이력 조회 중 오류가 발생했습니다", ex);
        }
    }

}
