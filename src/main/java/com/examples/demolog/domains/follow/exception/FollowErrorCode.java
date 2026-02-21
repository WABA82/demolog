package com.examples.demolog.domains.follow.exception;

import com.examples.demolog.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum FollowErrorCode implements ErrorCode {
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "해당 사용자를 찾을 수 없습니다."),
    CANNOT_FOLLOW_SELF(HttpStatus.BAD_REQUEST, "CANNOT_FOLLOW_SELF", "자기 자신을 팔로우할 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
