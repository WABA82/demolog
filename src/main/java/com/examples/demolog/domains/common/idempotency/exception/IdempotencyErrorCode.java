package com.examples.demolog.domains.common.idempotency.exception;

import com.examples.demolog.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum IdempotencyErrorCode implements ErrorCode {
    HEADER_MISSING(HttpStatus.BAD_REQUEST, "IDEMPOTENCY_HEADER_MISSING", "Idempotency-Key 헤더가 필수입니다."),
    REQUEST_MISMATCH(HttpStatus.UNPROCESSABLE_ENTITY, "IDEMPOTENCY_REQUEST_MISMATCH", "동일한 멱등 키로 다른 요청이 전송되었습니다."),
    LOCK_FAILED(HttpStatus.CONFLICT, "IDEMPOTENCY_LOCK_FAILED", "동일한 요청이 처리 중입니다. 잠시 후 다시 시도해주세요."),
    PROCESSING(HttpStatus.CONFLICT, "IDEMPOTENCY_PROCESSING", "멱등 키에 대한 요청이 현재 처리 중입니다."),
    EXPIRED(HttpStatus.GONE, "IDEMPOTENCY_EXPIRED", "멱등 키의 유효 시간이 만료되었습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
