package com.examples.demolog.domains.common.idempotency.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "idempotency", indexes = {@Index(name = "idx_idempotency_expires_at" /*배치 삭제 성능 최적화 인덱스*/, columnList = "expires_at")})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class Idempotency {

    // 내부 식별자
    @Id
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    // 클라이언트 멱등 키
    @Column(nullable = false, unique = true, columnDefinition = "BINARY(16)")
    private UUID idempotencyToken;

    // 요청 HTTP 메서드 (예: POST, PUT)
    @Column(nullable = false, length = 20)
    private String requestMethod;

    // 요청 경로 (예: /api/v1/resources)
    @Column(nullable = false, length = 200)
    private String requestUri;

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

    /**
     * 생성 메서드
     */
    public static Idempotency create(
            UUID key,
            String requestHttpMethod,
            String requestUri
    ) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusHours(24);

        return Idempotency.builder()
                .idempotencyToken(key)
                .requestMethod(requestHttpMethod)
                .requestUri(requestUri)
                .createdAt(now)
                .expiresAt(expiresAt)
                .build();
    }

    /**
     * 응답 정보 설정
     */
    public void setResponse(Integer statusCode, String responseBody) {
        this.responseStatusCode = statusCode;
        this.responseBody = responseBody;
    }

    /**
     * 완료 여부 검증 (응답이 설정되었는지 확인)
     */
    public boolean isNotCompleted() {
        return this.responseStatusCode == null || this.responseBody == null;
    }

    /**
     * 동일한 요청이 아닌지 검증 (HTTP Method, Request URI를 비교)
     */
    public boolean isNotRequestMatch(String incomingRequestMethod, String incomingRequestUri) {
        return !this.requestMethod.equals(incomingRequestMethod) || !this.requestUri.equals(incomingRequestUri);
    }


}
