package com.examples.demolog.domains.common.idempotency.service;

import com.examples.demolog.domains.common.idempotency.dto.CachedResponse;
import com.examples.demolog.domains.common.idempotency.exception.IdempotencyErrorCode;
import com.examples.demolog.domains.common.idempotency.exception.IdempotencyException;
import com.examples.demolog.domains.common.idempotency.model.IdempotencyKey;
import com.examples.demolog.domains.common.idempotency.repository.IdempotencyKeyRedisRepository;
import com.examples.demolog.domains.common.idempotency.repository.IdempotencyKeyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final IdempotencyKeyRedisRepository idempotencyKeyRedisRepository;

    /**
     * 멱등 키를 검증하고 캐시된 응답을 반환한다.
     * 캐시된 응답이 있으면 CachedResponse를 반환하고, 없으면 null을 반환한다.
     */
    public CachedResponse validateAndGetCachedResponse(
            UUID userId,
            UUID idempotencyKeyValue,
            String requestHttpMethod,
            String requestPath,
            String requestBody
    ) {
        // 멱등 키 검증
        if (idempotencyKeyValue == null) {
            throw new IdempotencyException(IdempotencyErrorCode.HEADER_MISSING);
        }

        // 요청 본문의 해시 계산
        String requestHash = calculateRequestHash(requestBody);

        // 1. Redis 캐시에서 조회 (빠른 경로)
        CachedResponse cachedResponse = idempotencyKeyRedisRepository.findCachedResponse(userId, idempotencyKeyValue);
        if (cachedResponse != null) {
            if (cachedResponse.isInProgress()) {
                throw new IdempotencyException(IdempotencyErrorCode.PROCESSING);
            }
            return cachedResponse;
        }

        // 2. DB에서 조회 (Redis 장애 대비)
        Optional<IdempotencyKey> optIdempotencyKey = idempotencyKeyRepository.findByUserIdAndKey(userId, idempotencyKeyValue);

        if (optIdempotencyKey.isPresent()) {
            IdempotencyKey idempotencyKey = optIdempotencyKey.get();

            // 멱등 키 유효성 검사
            idempotencyKey.validateIdempotencyKey(requestHash);

            // 성공한 요청이면 캐시된 응답 반환
            if (idempotencyKey.isSuccess()) {
                return new CachedResponse(idempotencyKey.getResponseBody(), idempotencyKey.getHttpStatusCode(), false);
            }
        }

        return null;
    }

    /**
     * IN_PROGRESS 상태로 멱등 키를 저장한다.
     * 성공하면 true, 다른 요청이 이미 처리 중이면 false를 반환한다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean saveInProgress(
            UUID userId,
            UUID idempotencyKey,
            String requestHttpMethod,
            String requestPath,
            String requestBody
    ) {
        String requestHash = calculateRequestHash(requestBody);

        try {
            IdempotencyKey entity = IdempotencyKey.create(
                    idempotencyKey, userId, requestHttpMethod, requestPath, requestHash
            );

            idempotencyKeyRepository.save(entity);
            idempotencyKeyRedisRepository.cacheInProgressStatus(userId, idempotencyKey, 30);

            return true; // Successfully claimed
        } catch (DataIntegrityViolationException e) {
            return false; // Another request already processing
        }
    }

    /**
     * @deprecated Replaced by write-first caching pattern (saveInProgress).
     * Kept for backward compatibility.
     */
    @Deprecated(forRemoval = false)
    public boolean tryAcquireLock(UUID userId, UUID idempotencyKeyValue) {
        return idempotencyKeyRedisRepository.tryAcquireLock(userId, idempotencyKeyValue);
    }

    /**
     * 응답을 DB와 Redis에 저장한다.
     * 별도의 트랜잭션(REQUIRES_NEW)으로 실행되어 원본 트랜잭션이 롤백되어도 저장된다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveResponse(
            UUID userId,
            UUID idempotencyKey,
            String requestHttpMethod,
            String requestPath,
            String requestBody,
            UUID resourceId,
            String responseBody,
            Integer httpStatusCode
    ) {
        String requestHash = calculateRequestHash(requestBody);

        // DB에서 기존 멱등 키 조회
        Optional<IdempotencyKey> existingKey = idempotencyKeyRepository.findByUserIdAndKey(userId, idempotencyKey);

        IdempotencyKey entity;
        if (existingKey.isPresent()) {
            // 기존 키 업데이트
            entity = existingKey.get();
            entity.markSuccess(resourceId, responseBody, httpStatusCode);
        } else {
            // 새 키 생성
            entity = IdempotencyKey.create(idempotencyKey, userId, requestHttpMethod, requestPath, requestHash);
            entity.markSuccess(resourceId, responseBody, httpStatusCode);
        }

        try {
            idempotencyKeyRepository.save(entity);
        } catch (DataIntegrityViolationException e) {
            // Unique constraint 위반: 다른 스레드가 동시에 저장했을 가능성 -> 이 경우 DB에는 저장되어 있으므로 무시
        }

        // Redis 캐싱 (24시간 TTL)
        idempotencyKeyRedisRepository.cacheResponse(userId, idempotencyKey, responseBody, httpStatusCode, 24 * 60 * 60);
    }

    /**
     * 실패한 응답을 저장한다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveFailed(
            UUID userId,
            UUID idempotencyKey,
            String requestHttpMethod,
            String requestPath,
            String requestBody,
            String responseBody,
            Integer httpStatusCode
    ) {
        String requestHash = calculateRequestHash(requestBody);

        // DB에서 기존 멱등 키 조회
        Optional<IdempotencyKey> existingEntity = idempotencyKeyRepository.findByUserIdAndKey(userId, idempotencyKey);

        IdempotencyKey entity;

        // 기존 멱등 키 엔티티 업데이트
        if (existingEntity.isPresent()) {
            entity = existingEntity.get();
            entity.markFailed(responseBody, httpStatusCode);
        }
        // 새 멱등 키 엔티티 생성
        else {
            entity = IdempotencyKey.create(idempotencyKey, userId, requestHttpMethod, requestPath, requestHash);
            entity.markFailed(responseBody, httpStatusCode);
        }

        // DB 저장
        try {
            idempotencyKeyRepository.save(entity);
        } catch (DataIntegrityViolationException e) {
            // Unique constraint 위반: 다른 스레드가 동시에 저장했을 가능성 -> 이 경우 DB에는 저장되어 있으므로 무시
        }

        // Redis 캐싱 (5분 TTL)
        idempotencyKeyRedisRepository.cacheResponse(userId, idempotencyKey, responseBody, httpStatusCode, 5 * 60);
    }

    /**
     * 만료된 멱등 키를 삭제한다. (배치 작업용)
     */
    @Transactional
    public void deleteExpiredKeys() {
        idempotencyKeyRepository.deleteExpiredKeys(LocalDateTime.now());
    }

    /**
     * Request Body의 SHA-256 해시를 계산한다.
     */
    private String calculateRequestHash(String requestBody) {
        if (requestBody == null || requestBody.isBlank()) {
            return calculateHash("");
        }
        return calculateHash(requestBody);
    }

    private String calculateHash(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 알고리즘을 찾을 수 없습니다.", e);
        }
    }

}
