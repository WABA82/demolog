package com.examples.demolog.domains.common.idempotency.aspect;

import com.examples.demolog.domains.common.idempotency.service.IdempotencyService;
import com.examples.demolog.global.response.ApiResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class IdempotencyAspect {

    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    @Around("@annotation(com.examples.demolog.domains.common.idempotency.annotation.Idempotent)")
    public Object handleIdempotency(ProceedingJoinPoint joinPoint) throws Throwable {
        // Request 정보 추출
        IdempotencyContext idempotencyContext = this.extractRequestAttr();

        if (idempotencyContext.isValid()) {
            return joinPoint.proceed();
        }

        try {
            Object result = joinPoint.proceed();
            handleSuccess(result, idempotencyContext.getUserId(), idempotencyContext.getIdempotencyKey(), idempotencyContext.getRequestBody());
            return result;
        } catch (Exception e) {
            handleError(e, idempotencyContext.getUserId(), idempotencyContext.getIdempotencyKey(), idempotencyContext.getRequestBody());
            throw e;
        }
    }

    // Request 속성에서 IdempotencyContext 추출
    private IdempotencyContext extractRequestAttr() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes == null) {
            return IdempotencyContext.empty();
        }

        HttpServletRequest request = attributes.getRequest();
        return IdempotencyContext.builder()
                .idempotencyKey(UUID.fromString((String) request.getAttribute("idempotencyKey")))
                .userId((UUID) request.getAttribute("userId"))
                .requestBody((String) request.getAttribute("cachedRequestBody"))
                .requestMethod(request.getMethod())
                .requestUri(request.getRequestURI())
                .build();
    }

    // 응답 오류 처리
    private void handleError(Exception e, UUID userId, UUID idempotencyKey, String requestBody) {
        // 실패 응답 저장
        try {
            String errorBody = objectMapper.writeValueAsString(
                    ApiResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build()
            );

            // 상태 코드 추출 (기본값 500)
            int statusCode = 500;
            if (e.getCause() instanceof HttpStatusCodeException statusException) {
                statusCode = statusException.getStatusCode().value();
            }

            //
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            String requestMethod = attrs != null ? attrs.getRequest().getMethod() : "UNKNOWN";
            String requestUri = attrs != null ? attrs.getRequest().getRequestURI() : "UNKNOWN";

            idempotencyService.saveFailed(
                    userId,
                    idempotencyKey,
                    requestMethod,
                    requestUri,
                    requestBody,
                    errorBody,
                    statusCode
            );
        } catch (Exception savingException) {
            log.error("Failed to save idempotency error response", savingException);
        }
    }

    // 응답 성공 처리
    private void handleSuccess(Object result, UUID userId, UUID idempotencyKey, String requestBody) throws JsonProcessingException {
        if (result instanceof ResponseEntity<?> responseEntity) {

            // 리소스 ID 추출
            UUID resourceId = extractResourceId(responseEntity);
            String responseBody = objectMapper.writeValueAsString(responseEntity.getBody());
            int statusCode = responseEntity.getStatusCode().value();

            // 응답 저장 (별도 트랜잭션)
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            String requestMethod = attrs != null ? attrs.getRequest().getMethod() : "UNKNOWN";
            String requestUri = attrs != null ? attrs.getRequest().getRequestURI() : "UNKNOWN";

            idempotencyService.saveResponse(
                    userId,
                    idempotencyKey,
                    requestMethod,
                    requestUri,
                    requestBody,
                    resourceId,
                    responseBody,
                    statusCode
            );
        }
    }

    // 응답에서 리소스 ID 추출
    private UUID extractResourceId(ResponseEntity<?> responseEntity) {
        Object body = responseEntity.getBody();
        if (body instanceof ApiResponse<?> apiResponse) {
            Object data = apiResponse.getData();
            if (data != null) {
                try {
                    // DTO 객체를 Map으로 변환
                    Map<String, Object> dataMap = objectMapper.convertValue(data, new TypeReference<>() {
                    });

                    Object idValue = dataMap.get("id");
                    if (idValue instanceof String resourceId) {
                        return UUID.fromString(resourceId);
                    } else if (idValue instanceof UUID resourceId) {
                        return resourceId;
                    }
                } catch (Exception e) {
                    log.debug("Failed to extract resource ID from response", e);
                }
            }
        }
        return null;
    }

}
