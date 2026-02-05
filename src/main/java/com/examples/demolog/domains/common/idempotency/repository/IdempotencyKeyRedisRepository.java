package com.examples.demolog.domains.common.idempotency.repository;

import com.examples.demolog.domains.common.idempotency.dto.CachedResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Repository
@RequiredArgsConstructor
public class IdempotencyKeyRedisRepository {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final long CACHE_TTL_HOURS = 24;
    private static final long LOCK_TTL_SECONDS = 5;

    public CachedResponse findCachedResponse(UUID userId, UUID idempotencyKey) {
        try {
            String key = buildCacheKey(userId, idempotencyKey);
            String cachedData = redisTemplate.opsForValue().get(key);
            if (cachedData == null) {
                return null;
            }
            return objectMapper.readValue(cachedData, CachedResponse.class);
        } catch (Exception e) {
            return null;
        }
    }

    // IN_PROGRESS 상태 저장
    public void cacheInProgressStatus(UUID userId, UUID idempotencyKey, long ttlSeconds) {
        try {
            String key = buildCacheKey(userId, idempotencyKey);
            CachedResponse inProgressResponse = new CachedResponse(null, null, true);
            String jsonData = objectMapper.writeValueAsString(inProgressResponse);
            redisTemplate.opsForValue().set(key, jsonData, Duration.ofSeconds(ttlSeconds));
        } catch (Exception e) {
            log.warn("Failed to cache IN_PROGRESS status in Redis", e);
        }
    }

    // 캐시된 응답 저장
    public void cacheResponse(UUID userId, UUID idempotencyKey, String responseBody, Integer httpStatusCode) {
        cacheResponse(userId, idempotencyKey, responseBody, httpStatusCode, CACHE_TTL_HOURS * 3600);
    }

    // 캐시된 응답 저장 (TTL 지정 가능)
    public void cacheResponse(UUID userId, UUID idempotencyKey, String responseBody, Integer httpStatusCode, long ttlSeconds) {
        try {
            String key = buildCacheKey(userId, idempotencyKey);
            CachedResponse cachedResponse = new CachedResponse(responseBody, httpStatusCode, false);
            String jsonData = objectMapper.writeValueAsString(cachedResponse);
            redisTemplate.opsForValue().set(
                    key,
                    jsonData,
                    Duration.ofSeconds(ttlSeconds)
            );
        } catch (Exception e) {
            log.warn("Failed to cache response in Redis", e);
        }
    }

    /**
     * @deprecated Replaced by write-first caching pattern (cacheInProgressStatus).
     * Kept for backward compatibility.
     */
    @Deprecated(forRemoval = false)
    public boolean tryAcquireLock(UUID userId, UUID idempotencyKey) {
        try {
            String lockKey = buildLockKey(userId, idempotencyKey);
            Boolean success = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, "LOCKED", Duration.ofSeconds(LOCK_TTL_SECONDS));
            return Boolean.TRUE.equals(success);
        } catch (Exception e) {
            // Redis 장애 시 Lock 획득 실패로 처리
            return false;
        }
    }

    /**
     * @deprecated Replaced by write-first caching pattern.
     * Kept for backward compatibility.
     */
    @Deprecated(forRemoval = false)
    public void releaseLock(UUID userId, UUID idempotencyKey) {
        try {
            String lockKey = buildLockKey(userId, idempotencyKey);
            redisTemplate.delete(lockKey);
        } catch (Exception e) {
            // Lock 해제 실패는 무시 (TTL로 자동 해제됨)
        }
    }

    private String buildCacheKey(UUID userId, UUID idempotencyKey) {
        return "idempotency:" + userId + ":" + idempotencyKey;
    }

    private String buildLockKey(UUID userId, UUID idempotencyKey) {
        return "idempotency:lock:" + userId + ":" + idempotencyKey;
    }
}
