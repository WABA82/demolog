package com.examples.demolog.global.exception;

import lombok.Getter;

@Getter
public class UtilsException extends RuntimeException {

    private final transient ErrorCode errorCode;

    public UtilsException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public UtilsException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
