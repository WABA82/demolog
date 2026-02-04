package com.examples.demolog.domains.common.idempotency2.exception;

import com.examples.demolog.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum IdempotencyErrorCode implements ErrorCode {

    MISSING_IDEMPOTENCY_KEY(HttpStatus.BAD_REQUEST, "MISSING_IDEMPOTENCY_KEY", "멱등성 키(Idempotency-Key) 헤더가 필요합니다."),
    INVALID_IDEMPOTENCY_KEY(HttpStatus.BAD_REQUEST, "INVALID_IDEMPOTENCY_KEY", "멱등성 키(Idempotency-Key) 형식이 올바르지 않습니다."),
    REQUEST_MISMATCH(HttpStatus.UNPROCESSABLE_ENTITY, "REQUEST_MISMATCH", "동일한 멱등성 키로 다른 요청이 전송되었습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

}
