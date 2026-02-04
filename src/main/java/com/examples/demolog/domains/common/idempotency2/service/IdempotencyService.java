package com.examples.demolog.domains.common.idempotency2.service;

import com.examples.demolog.domains.common.idempotency2.model.IdempotencyRecord;
import com.examples.demolog.domains.common.idempotency2.repository.IdempotencyRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final IdempotencyRecordRepository idempotencyRecordRepository;

    /**
     * 요청 바디를 SHA-256 해시로 변환
     */
    public String generateRequestHash(String body) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(body.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 알고리즘을 사용할 수 없습니다.", e);
        }
    }

    /**
     * 바이트 배열을 16진수 문자열로 변환
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * 멱등성 키로 기존 레코드 조회
     */
    public Optional<IdempotencyRecord> findByKey(UUID key) {
        return idempotencyRecordRepository.findByIdempotencyKey(key);
    }

    /**
     * 요청 일치 여부 검증
     * (HTTP Method, Request URI, Request Body Hash를 비교)
     */
    public boolean isRequestMatch(
            IdempotencyRecord record,
            String requestMethod,
            String requestUri,
            String requestHash
    ) {
        return record.getRequestMethod().equals(requestMethod) &&
                record.getRequestUri().equals(requestUri) &&
                record.getRequestHash().equals(requestHash);
    }

    /**
     * 멱등성 레코드 저장 (2xx 응답만 저장)
     * 동시성으로 인한 중복 저장 시도 시 DataIntegrityViolationException 무시
     */
    @Transactional
    public void saveRecord(IdempotencyRecord record) {
        try {
            idempotencyRecordRepository.save(record);
        } catch (DataIntegrityViolationException e) {
            // 동시에 같은 키로 두 개 이상의 요청이 들어온 경우
            // 먼저 저장된 것은 성공, 나중 것은 예외 발생
            // 나중 요청은 이미 저장된 응답을 사용하므로 예외 무시
        }
    }

    /**
     * 만료된 레코드 삭제 (배치 작업용)
     */
    @Transactional
    public void deleteExpiredRecords() {
        idempotencyRecordRepository.deleteByExpiresAtBefore(LocalDateTime.now());
    }

}
