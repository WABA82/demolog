package com.examples.demolog.domains.common.idempotency.aspect;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class IdempotencyContext {

    private UUID userId;
    private UUID idempotencyKey;
    private String requestMethod;
    private String requestUri;
    private String requestBody;

    /**
     * 빈 멱등성 컨텍스트 생성
     */
    public static IdempotencyContext empty() {
        return IdempotencyContext.builder().build();
    }

    /**
     * 멱등성 컨텍스트 유효성 검사
     */
    public boolean isValid() {
        // 멱등성 키, 사용자 ID, 요청 본문이 모두 존재하는지 확인
        return idempotencyKey != null && userId != null && requestBody != null;
    }

}
