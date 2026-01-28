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
        log.error("BusinessException: {}", e.getMessage());
        ErrorResponse response = ErrorResponse.of(e.getCommonErrorCode());
        return ResponseEntity.status(e.getCommonErrorCode().getStatus()).body(response);
    }

    /*
     * - ( @ModelAttribute ) 바인딩 실패
     * - 유효성 검사 실패 시
     */
    @ExceptionHandler(BindException.class)
    protected ResponseEntity<ErrorResponse> handleBindException(BindException e) {
        log.error("BindException: {}", e.getMessage());
        ErrorResponse response = ErrorResponse.of(CommonErrorCode.INVALID_INPUT_VALUE);
        // 필드 오류 추가
        e.getBindingResult().getFieldErrors()
                .forEach(error -> response.addFieldError(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.status(CommonErrorCode.INVALID_INPUT_VALUE.getStatus()).body(response);
    }

    /**
     * 서버 에러
     */
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Exception: {}", e.getMessage(), e);
        CommonErrorCode commonErrorCode = CommonErrorCode.INTERNAL_SERVER_ERROR;
        ErrorResponse response = ErrorResponse.of(CommonErrorCode.INTERNAL_SERVER_ERROR);
        return ResponseEntity.status(commonErrorCode.getStatus()).body(response);
    }
}
