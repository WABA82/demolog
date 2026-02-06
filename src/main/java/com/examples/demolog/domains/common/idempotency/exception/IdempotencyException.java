package com.examples.demolog.domains.common.idempotency.exception;

import com.examples.demolog.global.exception.BusinessException;
import com.examples.demolog.global.exception.ErrorCode;

public class IdempotencyException extends BusinessException {

    public IdempotencyException(ErrorCode errorCode) {
        super(errorCode);
    }

    public IdempotencyException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

}
