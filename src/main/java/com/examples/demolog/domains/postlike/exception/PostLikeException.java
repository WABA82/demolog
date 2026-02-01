package com.examples.demolog.domains.postlike.exception;

import com.examples.demolog.global.exception.BusinessException;
import com.examples.demolog.global.exception.ErrorCode;

/**
 * PostLike 관련 비즈니스 예외를 나타내는 클래스입니다.
 */
public class PostLikeException extends BusinessException {

    public PostLikeException(ErrorCode errorCode) {
        super(errorCode);
    }

    public PostLikeException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

}
