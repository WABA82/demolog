package com.examples.demolog.domains.common.idempotency2.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "idempotency_record", indexes = {@Index(name = "idx_idempotency_expires_at" /*배치 삭제 성능 최적화 인덱스*/, columnList = "expires_at")})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class IdempotencyRecord {

    // 내부 식별자
    @Id
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    // 클라이언트 멱등 키
    @Column(nullable = false, unique = true, columnDefinition = "BINARY(16)")
    private UUID idempotencyKey;

    // 요청 HTTP 메서드 (예: POST, PUT)
    @Column(nullable = false, length = 20)
    private String requestMethod;

    // 요청 경로 (예: /api/v1/resources)
    @Column(nullable = false, length = 200)
    private String requestUri;

    // 요청 바디 해시값
    @Column(nullable = false, length = 64)
    private String requestHash;

    // 응답 바디
    @Column(nullable = false, columnDefinition = "TEXT")
    private String responseBody;

    // 응답 HTTP 상태 코드
    @Column(nullable = false)
    private Integer responseStatusCode;

    // 생성 시각
    @Column(nullable = false)
    private LocalDateTime createdAt;

    // 만료 시각
    @Column(nullable = false)
    private LocalDateTime expiresAt;

    public static IdempotencyRecord create(
            UUID key,
            String requestHttpMethod,
            String requestUri,
            String requestHash
    ) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusHours(24);

        return IdempotencyRecord.builder()
                .idempotencyKey(key)
                .requestMethod(requestHttpMethod)
                .requestUri(requestUri)
                .requestHash(requestHash)
                .createdAt(now)
                .expiresAt(expiresAt)
                .build();
    }

    /**
     * 응답 정보 설정
     */
    public void setResponse(String responseBody, Integer statusCode) {
        this.responseBody = responseBody;
        this.responseStatusCode = statusCode;
    }

}
