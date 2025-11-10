package com.musinsa.point.service;

import com.musinsa.point.domain.IdempotencyRecord;
import com.musinsa.point.repository.IdempotencyRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private static final int TTL_HOURS = 24;
    private final IdempotencyRecordRepository idempotencyRecordRepository;

    /**
     * 멱등성 키를 확인하고 기존 레코드가 있으면 저장된 응답을 반환합니다.
     * 없으면 null을 반환하여 비즈니스 로직을 실행하도록 합니다.
     *
     * @param idempotencyKey 멱등성 키
     * @return 기존 레코드가 있으면 IdempotencyRecord, 없으면 null
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public IdempotencyRecord checkExisting(String idempotencyKey) {
        Optional<IdempotencyRecord> existingRecord = idempotencyRecordRepository
                .findByIdempotencyKey(idempotencyKey);

        if (existingRecord.isPresent()) {
            log.info("멱등성 레코드 발견 - idempotencyKey: {}, 저장된 응답 반환", idempotencyKey);
            return existingRecord.get();
        }

        return null;
    }

    /**
     * 비즈니스 로직 실행 후 응답을 저장합니다.
     *
     * @param idempotencyKey 멱등성 키
     * @param responseBody 응답 본문 (JSON 문자열)
     * @param httpStatus HTTP 상태 코드
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveResponse(String idempotencyKey, String responseBody, int httpStatus) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusHours(TTL_HOURS);

        IdempotencyRecord record = IdempotencyRecord.builder()
                .idempotencyKey(idempotencyKey)
                .responseBody(responseBody)
                .httpStatus(httpStatus)
                .createdAt(now)
                .expiresAt(expiresAt)
                .build();

        idempotencyRecordRepository.save(record);
        log.info("멱등성 레코드 저장 완료 - idempotencyKey: {}, expiresAt: {}", idempotencyKey, expiresAt);
    }

    /**
     * 만료된 멱등성 레코드를 정리합니다.
     * 스케줄러나 배치 작업에서 주기적으로 호출할 수 있습니다.
     */
    @Transactional
    public void cleanupExpiredRecords() {
        LocalDateTime now = LocalDateTime.now();
        int deletedCount = idempotencyRecordRepository.deleteByExpiresAtBefore(now);
        log.info("만료된 멱등성 레코드 정리 완료 - 삭제된 레코드 수: {}", deletedCount);
    }
}
