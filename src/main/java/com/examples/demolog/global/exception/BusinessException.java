package com.examples.demolog.global.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final CommonErrorCode commonErrorCode;

    public BusinessException(CommonErrorCode commonErrorCode) {
        super(commonErrorCode.getMessage());
        this.commonErrorCode = commonErrorCode;
    }

    public BusinessException(CommonErrorCode commonErrorCode, String message) {
        super(message);
        this.commonErrorCode = commonErrorCode;
    }
}
