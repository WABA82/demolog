package com.examples.demolog.domains.notification.exception;

import com.examples.demolog.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum NotificationErrorCode implements ErrorCode {
    NOT_FOUND(HttpStatus.NOT_FOUND, "NOTIFICATION_NOT_FOUND", "해당 알림을 찾을 수 없습니다."),
    FORBIDDEN_ACCESS(HttpStatus.FORBIDDEN, "NOTIFICATION_FORBIDDEN_ACCESS", "해당 알림에 대한 권한이 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
