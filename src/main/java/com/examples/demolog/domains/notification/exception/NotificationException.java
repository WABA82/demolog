package com.examples.demolog.domains.notification.exception;

import com.examples.demolog.global.exception.BusinessException;
import com.examples.demolog.global.exception.ErrorCode;

public class NotificationException extends BusinessException {

    public NotificationException(ErrorCode errorCode) {
        super(errorCode);
    }

    public NotificationException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
