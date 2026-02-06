package com.examples.demolog.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ApiResponse<T> implements CommonResponse {

    private boolean success;
    private String message;
    private T data;

    @Builder
    private ApiResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public static <T> ResponseEntity<ApiResponse<T>> ok(T data) {
        ApiResponse<T> response = ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .build();

        return ResponseEntity.ok(response);
    }

    public static <T> ResponseEntity<ApiResponse<T>> ok(T data, String message) {
        ApiResponse<T> response = ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .message(message)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * 201 Created: 리소스 생성 요청이 성공했을 때 사용 (POST)
     */
    public static <T> ResponseEntity<ApiResponse<T>> created() {
        ApiResponse<T> response = ApiResponse.<T>builder()
                .success(true)
                .message("데이터가 성공적으로 생성되었습니다.")
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 201 Created: 리소스 생성 요청이 성공했을 때 사용 (POST)
     */
    public static <T> ResponseEntity<ApiResponse<T>> created(T data) {
        ApiResponse<T> response = ApiResponse.<T>builder()
                .success(true)
                .message("데이터가 성공적으로 생성되었습니다.")
                .data(data)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 204 No Content: 요청은 성공적으로 처리되었으나, 클라이언트에게 돌려줄 콘텐츠(데이터)가 없을 때 사용 (DELETE)
     */
    public static <T> ResponseEntity<ApiResponse<T>> noContent() {
        ApiResponse<T> response = ApiResponse.<T>builder()
                .success(true)
                .message("요청이 성공적으로 처리되었습니다 (반환 데이터 없음).")
                .build();

        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(response);
    }

}