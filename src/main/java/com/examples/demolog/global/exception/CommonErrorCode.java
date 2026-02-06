package com.examples.demolog.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CommonErrorCode implements ErrorCode {

    // Common
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "INVALID_INPUT_VALUE", "잘못된 입력값입니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "리소스를 찾을 수 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "서버 오류가 발생했습니다."),

    // Security
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "잘못된 입력값입니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "FORBIDDEN", "리소스를 찾을 수 없습니다."),

    // JsonUtil
    JSON_PROCESSING_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "JSON_PROCESSING_ERROR", "Json 데이터 처리 중 에러가 발생하였습니다.");

    // CommonErrorCode fields
    private final HttpStatus status;
    private final String code;
    private final String message;

}
