package com.examples.demolog.domains.auth.exception;

import com.examples.demolog.global.exception.BusinessException;
import com.examples.demolog.global.exception.ErrorCode;

public class AuthException extends BusinessException {

    public AuthException(ErrorCode errorCode) {
        super(errorCode);
    }

    public AuthException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

}
