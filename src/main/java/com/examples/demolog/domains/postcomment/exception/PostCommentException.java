package com.examples.demolog.domains.postcomment.exception;

import com.examples.demolog.global.exception.BusinessException;
import com.examples.demolog.global.exception.ErrorCode;

/**
 * PostComment 관련 비즈니스 예외를 나타내는 클래스입니다.
 */
public class PostCommentException extends BusinessException {

    public PostCommentException(ErrorCode errorCode) {
        super(errorCode);
    }

    public PostCommentException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

}
