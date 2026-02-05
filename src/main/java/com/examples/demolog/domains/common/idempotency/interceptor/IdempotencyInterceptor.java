package com.examples.demolog.domains.common.idempotency.interceptor;

import com.examples.demolog.domains.common.idempotency.annotation.Idempotent;
import com.examples.demolog.domains.common.idempotency.dto.CachedResponse;
import com.examples.demolog.domains.common.idempotency.exception.IdempotencyErrorCode;
import com.examples.demolog.domains.common.idempotency.exception.IdempotencyException;
import com.examples.demolog.domains.common.idempotency.service.IdempotencyService;
import com.examples.demolog.global.response.ApiResponse;
import com.examples.demolog.global.security.CustomUserDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotencyInterceptor implements HandlerInterceptor {

    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler
    ) throws Exception {

        // 1. 핸들러가 Idempotent 어노테이션을 가졌는지 확인
        if (!isIdempotentHandler(handler)) {
            return true;
        }

        // 2. Request Body를 캐시할 수 있도록 Wrapper로 감싸기 (이미 감싸져 있지 않은 경우)
        if (!(request instanceof ContentCachingRequestWrapper)) {
            request = new ContentCachingRequestWrapper(request);
        }

        // 3. 인증된 사용자 정보 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return true;
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof CustomUserDetails userDetails)) {
            return true;
        }

        UUID userId = userDetails.getUserId();

        // 4. Idempotency-Key 헤더 가져오기
        UUID idempotencyKey = UUID.fromString(request.getHeader("Idempotency-Key"));

        // 5. Request Body 읽기
        ContentCachingRequestWrapper cachedRequest = (ContentCachingRequestWrapper) request;
        String requestBody = new String(cachedRequest.getContentAsByteArray());

        try {
            // 6. 멱등 키 검증 및 캐시된 응답 확인
            CachedResponse cachedResponse = idempotencyService.validateAndGetCachedResponse(
                    userId,
                    idempotencyKey,
                    request.getMethod(),
                    request.getRequestURI(),
                    requestBody
            );

            if (cachedResponse != null) {
                // 캐시된 응답이 있으면 즉시 반환
                returnCachedResponse(response, cachedResponse);
                return false; // 컨트롤러 실행 안 함
            }

            // 7. IN_PROGRESS 상태로 저장 시도
            boolean saved = idempotencyService.saveInProgress(
                    userId,
                    idempotencyKey,
                    request.getMethod(),
                    request.getRequestURI(),
                    requestBody
            );

            if (!saved) {
                // 다른 요청이 이미 처리 중
                throw new IdempotencyException(IdempotencyErrorCode.PROCESSING);
            }

            // 8. 요청 정보를 attribute에 저장 (aspect에서 사용)
            request.setAttribute("cachedRequestBody", requestBody);
            request.setAttribute("idempotencyKey", idempotencyKey);
            request.setAttribute("userId", userId);

            return true;

        } catch (IdempotencyException e) {
            // 멱등 키 검증 실패 시 에러 응답
            returnErrorResponse(response, e.getErrorCode().getStatus().value(), e.getErrorCode());
            return false;
        }
    }

    // 핸들러가 Idempotent 어노테이션을 가졌는지 확인
    private boolean isIdempotentHandler(Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return false;
        }

        Method method = handlerMethod.getMethod();
        return method.isAnnotationPresent(Idempotent.class);
    }


    // 캐시된 응답을 반환
    private void returnCachedResponse(HttpServletResponse response, CachedResponse cachedResponse) throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(cachedResponse.httpStatusCode());

        String responseJson = objectMapper.writeValueAsString(
                ApiResponse.builder()
                        .success(true)
                        .data(objectMapper.readTree(cachedResponse.responseBody()))
                        .build()
        );

        response.getWriter().write(responseJson);
        response.getWriter().flush();
    }

    // 에러 응답을 반환
    private void returnErrorResponse(HttpServletResponse response, int status, Object errorCode) throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(status);

        String errorJson = objectMapper.writeValueAsString(
                ApiResponse.builder()
                        .success(false)
                        .message(((com.examples.demolog.global.exception.ErrorCode) errorCode).getMessage())
                        .build()
        );

        response.getWriter().write(errorJson);
        response.getWriter().flush();
    }
}
