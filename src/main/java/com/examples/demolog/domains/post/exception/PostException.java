package com.examples.demolog.domains.post.exception;

import com.examples.demolog.global.exception.BusinessException;
import com.examples.demolog.global.exception.ErrorCode;

public class PostException extends BusinessException {
    public PostException(ErrorCode errorCode) {
        super(errorCode);
    }
}
