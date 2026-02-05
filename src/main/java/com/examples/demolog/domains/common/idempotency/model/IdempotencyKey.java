package com.examples.demolog.domains.common.idempotency.model;

import com.examples.demolog.domains.common.idempotency.exception.IdempotencyErrorCode;
import com.examples.demolog.domains.common.idempotency.exception.IdempotencyException;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "IDEMPOTENCY_KEY",
        uniqueConstraints = {
                // 사용자별 멱등 키 중복 방지
                @UniqueConstraint(name = "uk_idempotency_user_key", columnNames = {"user_id", "client_key"})
        },
        indexes = {
                // 사용자별 멱등 키 조회 성능
                @Index(name = "idx_idempotency_user_id", columnList = "user_id"),
                // 목적 배치 삭제 성능
                @Index(name = "idx_idempotency_expires_at", columnList = "expires_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class IdempotencyKey {

    // 내부 식별자
    @Id
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    // 클라이언트 멱등 키
    @Column(nullable = false, columnDefinition = "BINARY(16)")
    private UUID key;

    // 요청 사용자 식별자
    @Column(nullable = false, columnDefinition = "BINARY(16)")
    private UUID userId;

    // 요청 http 메서드 (예: POST, PUT)
    @Column(nullable = false, length = 20)
    private String requestHttpMethod;

    // 요청 경로 (예: /api/v1/resources)
    @Column(nullable = false, length = 200)
    private String requestUri;

    // 리소스 식별자
    @Column(columnDefinition = "BINARY(16)")
    private UUID resourceId;

    // 요청 바디 해시값
    @Column(nullable = false, length = 64)
    private String requestHash;

    // 응답 바디
    @Column(nullable = false, columnDefinition = "TEXT")
    private String responseBody;

    // 응답 HTTP 상태 코드
    @Column(nullable = false)
    private Integer httpStatusCode;

    // 멱등성 처리 상태
    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private IdempotencyStatus status;

    // 생성 시각
    @Column(nullable = false)
    private LocalDateTime createdAt;

    // 만료 시각
    @Column(nullable = false)
    private LocalDateTime expiresAt;

    public static IdempotencyKey create(
            UUID key,
            UUID userId,
            String requestHttpMethod,
            String requestUri,
            String requestHash
    ) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusHours(24);

        return IdempotencyKey.builder()
                .key(key)
                .userId(userId)
                .requestHttpMethod(requestHttpMethod)
                .requestUri(requestUri)
                .requestHash(requestHash)
                .status(IdempotencyStatus.IN_PROGRESS)
                .createdAt(now)
                .expiresAt(expiresAt)
                .build();
    }

    /**
     * 요청이 성공했음을 표시한다.
     */
    public void markSuccess(UUID resourceId, String responseBody, Integer httpStatusCode) {
        this.resourceId = resourceId;
        this.responseBody = responseBody;
        this.httpStatusCode = httpStatusCode;
        this.status = IdempotencyStatus.SUCCESS;
    }

    /**
     * 요청이 실패했음을 표시한다.
     */
    public void markFailed(String responseBody, Integer httpStatusCode) {
        this.responseBody = responseBody;
        this.httpStatusCode = httpStatusCode;
        this.status = IdempotencyStatus.FAILED;
    }

    /**
     * 멱등 키 유효성을 검증한다.
     */
    public void validateIdempotencyKey(String requestHash) {
        // 멱등 키가 만료되었는지 확인
        if (isExpired()) {
            throw new IdempotencyException(IdempotencyErrorCode.EXPIRED);
        }

        // 요청 내용이 일치하는지 확인
        if (!isSameRequestHash(requestHash)) {
            throw new IdempotencyException(IdempotencyErrorCode.REQUEST_MISMATCH);
        }

        // Allow retry if IN_PROGRESS is stale (server crash recovery)
        if (isProcessing() && isStale()) {
            throw new IdempotencyException(IdempotencyErrorCode.EXPIRED);
        }

        // 요청이 처리 중이면 대기
        if (isProcessing()) {
            throw new IdempotencyException(IdempotencyErrorCode.PROCESSING);
        }
    }

    /**
     * 멱등 키의 유효기간이 만료되었는지 여부를 반환한다.
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * 요청 해시가 일치하는지 여부를 반환한다.
     */
    public boolean isSameRequestHash(String incomingRequestHash) {
        return this.requestHash.equals(incomingRequestHash);
    }

    /**
     * 요청이 처리 중인지 여부를 반환한다.
     */
    public boolean isProcessing() {
        return status == IdempotencyStatus.IN_PROGRESS;
    }

    /**
     * 요청이 성공적으로 처리되었는지 여부를 반환한다.
     */
    public boolean isSuccess() {
        return status == IdempotencyStatus.SUCCESS;
    }

    /**
     * IN_PROGRESS 상태가 30초를 초과했는지 여부를 반환한다.
     */
    public boolean isStale() {
        return LocalDateTime.now().isAfter(createdAt.plusSeconds(30));
    }
}
