package com.examples.demolog.domains.common.idempotency2.web;

import com.examples.demolog.domains.common.idempotency2.annotation.Idempotent;
import com.examples.demolog.domains.common.idempotency2.exception.IdempotencyErrorCode;
import com.examples.demolog.domains.common.idempotency2.exception.IdempotencyException;
import com.examples.demolog.domains.common.idempotency2.model.IdempotencyRecord;
import com.examples.demolog.domains.common.idempotency2.service.IdempotencyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * 멱등성 검증 인터셉터
 * <p>
 * 동작 흐름:
 * 1. preHandle: Idempotency-Key 검증, 기존 레코드 확인
 * - 기존 레코드 있음 & 요청 일치 → 저장된 응답 반환, 컨트롤러 실행 안 함
 * - 기존 레코드 있음 & 요청 불일치 → REQUEST_MISMATCH 예외
 * - 기존 레코드 없음 → 컨트롤러 실행
 * <p>
 * 2. afterCompletion: 응답이 2xx이면 DB에 저장
 */
@Component
@RequiredArgsConstructor
public class IdempotencyInterceptor implements HandlerInterceptor {

    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    private static final String IDEMPOTENCY_KEY_ATTRIBUTE = "idempotencyKey";
    private static final String IDEMPOTENCY_RESPONSE_CACHED = "idempotencyCached";

    @Override
    public boolean preHandle(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler
    ) throws Exception {

        // 핸들러가 메서드가 아니면 통과
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        Idempotent idempotent = handlerMethod.getMethodAnnotation(Idempotent.class);

        // @Idempotent 어노테이션이 없으면 통과
        if (idempotent == null) {
            return true;
        }

        // Idempotency-Key 헤더 추출
        String idempotencyKeyStr = request.getHeader(IDEMPOTENCY_KEY_HEADER);

        // 헤더 필수 검증
        if (idempotent.required() && (idempotencyKeyStr == null || idempotencyKeyStr.isBlank())) {
            throw new IdempotencyException(IdempotencyErrorCode.MISSING_IDEMPOTENCY_KEY);
        }

        // 헤더 선택사항인데 없으면 통과
        if (idempotencyKeyStr == null || idempotencyKeyStr.isBlank()) {
            return true;
        }

        // UUID 형식 검증
        UUID idempotencyKey;
        try {
            idempotencyKey = UUID.fromString(idempotencyKeyStr);
        } catch (IllegalArgumentException e) {
            throw new IdempotencyException(IdempotencyErrorCode.INVALID_IDEMPOTENCY_KEY);
        }

        // 요청 정보 추출
        String requestMethod = request.getMethod();
        String requestUri = request.getRequestURI();
        String requestBody = getRequestBody(request);
        String requestHash = idempotencyService.generateRequestHash(requestBody);

        // 기존 레코드 조회
        Optional<IdempotencyRecord> existingRecord = idempotencyService.findByKey(idempotencyKey);

        if (existingRecord.isPresent()) {
            IdempotencyRecord record = existingRecord.get();

            // 만료 시간 확인
            if (record.getExpiresAt().isBefore(LocalDateTime.now())) {
                // 만료된 레코드는 새 요청으로 처리
                request.setAttribute(IDEMPOTENCY_KEY_ATTRIBUTE, idempotencyKey);
                return true;
            }

            // 요청 일치 검증
            if (!idempotencyService.isRequestMatch(record, requestMethod, requestUri, requestHash)) {
                throw new IdempotencyException(IdempotencyErrorCode.REQUEST_MISMATCH);
            }

            // 저장된 응답 반환
            returnCachedResponse(response, record);
            request.setAttribute(IDEMPOTENCY_RESPONSE_CACHED, true);
            return false;
        }

        // 멱등성 키를 요청 속성에 저장 (afterCompletion에서 사용)
        request.setAttribute(IDEMPOTENCY_KEY_ATTRIBUTE, idempotencyKey);
        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler,
            Exception ex
    ) {

        // 캐시된 응답은 저장하지 않음
        if (request.getAttribute(IDEMPOTENCY_RESPONSE_CACHED) != null) {
            return;
        }

        // 멱등성 키가 없으면 저장하지 않음
        Object keyObj = request.getAttribute(IDEMPOTENCY_KEY_ATTRIBUTE);
        if (keyObj == null) {
            return;
        }

        UUID idempotencyKey = (UUID) keyObj;

        // 2xx 응답만 저장
        if (response.getStatus() < 200 || response.getStatus() >= 300) {
            return;
        }

        // 예외가 발생했으면 저장하지 않음
        if (ex != null) {
            return;
        }

        // 응답 정보 추출
        String responseBody = getResponseBody(response);
        Integer responseStatusCode = response.getStatus();

        // 요청 정보 추출
        String requestMethod = request.getMethod();
        String requestUri = request.getRequestURI();
        String requestBody = getRequestBody(request);
        String requestHash = idempotencyService.generateRequestHash(requestBody);

        // 레코드 생성 및 저장
        IdempotencyRecord record = IdempotencyRecord.create(
                idempotencyKey,
                requestMethod,
                requestUri,
                requestHash
        );
        record.setResponse(responseBody, responseStatusCode);
        idempotencyService.saveRecord(record);
    }

    /**
     * 요청 바디 추출
     */
    private String getRequestBody(HttpServletRequest request) {
        if (request instanceof ContentCachingRequestWrapper wrapper) {
            byte[] buf = wrapper.getContentAsByteArray();
            if (buf.length == 0) {
                return "";
            }
            return new String(buf, StandardCharsets.UTF_8);
        }
        return "";
    }

    /**
     * 응답 바디 추출
     */
    private String getResponseBody(HttpServletResponse response) {
        if (response instanceof ContentCachingResponseWrapper wrapper) {
            byte[] buf = wrapper.getContentAsByteArray();
            if (buf.length == 0) {
                return "";
            }
            return new String(buf, StandardCharsets.UTF_8);
        }
        return "";
    }

    /**
     * 캐시된 응답을 클라이언트에 반환
     */
    private void returnCachedResponse(
            HttpServletResponse response,
            IdempotencyRecord record
    ) throws IOException {
        response.setStatus(record.getResponseStatusCode());
        response.setContentType("application/json;charset=UTF-8");

        // 이미 저장된 응답 바디 반환
        response.getWriter().write(record.getResponseBody());
    }

}
