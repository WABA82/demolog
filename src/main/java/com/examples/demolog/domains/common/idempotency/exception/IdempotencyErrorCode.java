package com.examples.demolog.domains.common.idempotency.exception;

import com.examples.demolog.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum IdempotencyErrorCode implements ErrorCode {

    MISSING_IDEMPOTENCY_TOKEN(HttpStatus.BAD_REQUEST, "MISSING_IDEMPOTENCY_TOKEN", "멱등성 키(Idempotency-Key) 헤더가 필요합니다."),
    INVALID_IDEMPOTENCY_TOKEN(HttpStatus.BAD_REQUEST, "INVALID_IDEMPOTENCY_TOKEN", "멱등성 키(Idempotency-Key) 형식이 올바르지 않습니다."),
    REQUEST_MISMATCH(HttpStatus.UNPROCESSABLE_ENTITY, "REQUEST_MISMATCH", "동일한 멱등성 키로 다른 요청이 전송되었습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "NOT_FOUND_IDEMPOTENCY", "해당 멱등성 정보를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

}
