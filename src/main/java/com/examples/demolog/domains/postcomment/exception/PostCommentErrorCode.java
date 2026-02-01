package com.examples.demolog.domains.postcomment.exception;

import com.examples.demolog.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PostCommentErrorCode implements ErrorCode {
    NOT_FOUND(HttpStatus.NOT_FOUND, "POST_COMMENT_NOT_FOUND", "해당 댓글을 찾을 수 없습니다."),
    FORBIDDEN_ACCESS(HttpStatus.FORBIDDEN, "FORBIDDEN_ACCESS", "해당 게시글에 대한 권한이 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
