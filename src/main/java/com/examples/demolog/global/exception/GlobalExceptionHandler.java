package com.examples.demolog.global.exception;

import com.examples.demolog.global.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 도메인 비즈니스 에러
     */
    @ExceptionHandler(BusinessException.class)
    protected ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        log.warn("[BusinessException]: {}", e.getMessage());
        ErrorResponse response = ErrorResponse.of(e.getErrorCode(), e.getMessage());
        return ResponseEntity.status(e.getErrorCode().getStatus()).body(response);
    }

    /*
     * 바인딩(@ModelAttribute) 에러 또는 유효성 검사 에러
     */
    @ExceptionHandler(BindException.class)
    protected ResponseEntity<ErrorResponse> handleBindException(BindException e) {
        log.warn("[BindException]: {}", e.getMessage());
        ErrorCode errorCode = CommonErrorCode.INVALID_INPUT_VALUE;
        ErrorResponse response = ErrorResponse.of(errorCode);
        // 응답에 필드 오류 추가
        e.getBindingResult().getFieldErrors().forEach(error ->
                response.addFieldError(error.getField(), error.getDefaultMessage())
        );
        return ResponseEntity.status(errorCode.getStatus()).body(response);
    }

    /**
     * 유틸 에러
     */
    @ExceptionHandler(UtilsException.class)
    protected ResponseEntity<ErrorResponse> handleUtilsException(UtilsException e) {
        log.error("[UtilsException]: ", e);
        ErrorResponse response = ErrorResponse.of(e.getErrorCode(), e.getMessage());
        return ResponseEntity.status(e.getErrorCode().getStatus()).body(response);
    }

    /**
     * 서버 에러
     */
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("[Exception]: ", e);
        ErrorCode errorCode = CommonErrorCode.INTERNAL_SERVER_ERROR;
        ErrorResponse response = ErrorResponse.of(errorCode);
        return ResponseEntity.status(errorCode.getStatus()).body(response);
    }
}
