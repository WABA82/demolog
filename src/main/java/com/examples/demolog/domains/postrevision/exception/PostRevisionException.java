package com.examples.demolog.domains.postrevision.exception;

import com.examples.demolog.global.exception.BusinessException;
import com.examples.demolog.global.exception.ErrorCode;

public class PostRevisionException extends BusinessException {
    public PostRevisionException(ErrorCode errorCode) {
        super(errorCode);
    }
}
