package com.examples.demolog.domains.follow.exception;

import com.examples.demolog.global.exception.BusinessException;
import com.examples.demolog.global.exception.ErrorCode;

public class FollowException extends BusinessException {

    public FollowException(ErrorCode errorCode) {
        super(errorCode);
    }

    public FollowException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
