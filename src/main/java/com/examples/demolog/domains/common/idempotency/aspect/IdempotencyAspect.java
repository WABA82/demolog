package com.examples.demolog.domains.common.idempotency.aspect;

import com.examples.demolog.domains.common.idempotency.annotation.Idempotent;
import com.examples.demolog.domains.common.idempotency.exception.IdempotencyErrorCode;
import com.examples.demolog.domains.common.idempotency.exception.IdempotencyException;
import com.examples.demolog.domains.common.idempotency.service.IdempotencyApplicationService;
import com.examples.demolog.global.exception.BusinessException;
import com.examples.demolog.global.exception.CommonErrorCode;
import com.examples.demolog.global.exception.ErrorCode;
import com.examples.demolog.global.response.ErrorResponse;
import com.examples.demolog.global.utils.JsonUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Objects;
import java.util.UUID;

/**
 * 멱등성 검증 Aspect
 * <p>
 * {@code @Idempotent} 어노테이션이 적용된 메서드를 가로채서
 * Idempotency-Key 헤더를 통해 멱등성을 검증합니다.
 * <p>
 * 동작 흐름:
 * <p>
 * 1. 캐시된 멱등성 확인 및 응답 쓰기
 * - 기존 요청 발견 & 응답 정보 있으면 → 완료 응답
 * - 기존 요청 발견 & 요청 불일치 → REQUEST_MISMATCH 예외
 * - 기존 요청 발견 & 응답 정보 없으면 → 처리중 응답
 * <p>
 * 2. 신규 멱등성 저장 또는 캐시 확인
 * <p>
 * 3. 비즈니스 로직 실행 및 결과 저장
 * - 비즈니스 로직 성공 → 성공 응답 저장(캐싱).
 * - 비즈니스 로직 예외 → 예외 응답 저장(캐싱).
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class IdempotencyAspect {

    private final IdempotencyApplicationService idempotencyApplicationService;

    @Around("@annotation(idempotent)")
    public Object checkIdempotency(ProceedingJoinPoint pjp, Idempotent idempotent) throws Throwable {
        // 1단계: 환경 및 요청 정보 추출
        // HttpServletResponse 얻기: 멱등성 상태에 따라 다른 응답을 생성 용도.
        HttpServletResponse response = getHttpServletResponse();
        if (response == null) {
            return pjp.proceed();
        }

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        UUID idempotencyToken = extractIdempotencyToken(Objects.requireNonNull(attributes), idempotent.value());
        String requestMethod = attributes.getRequest().getMethod();
        String requestUri = attributes.getRequest().getRequestURI();

        // 2단계: 캐시된 멱등성 확인 및 응답 쓰기
        if (idempotencyApplicationService.hasCachedIdempotency(idempotencyToken, requestMethod, requestUri, response)) return null;

        // 3단계: 신규 멱등성 저장 또는 캐시 확인
        String status = idempotencyApplicationService.createIfNotCached(idempotencyToken, requestMethod, requestUri, response);
        if (Objects.equals("CACHED", status)) return null;

        // 4단계: 비즈니스 로직 실행 및 결과 저장
        return executeAndSaveResponse(pjp, idempotencyToken, response);
    }

    // 서블릿 응답 얻기.
    private HttpServletResponse getHttpServletResponse() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return (attributes == null) ? null : attributes.getResponse();
    }

    // IdempotencyKey 추출: UUID 타입으로 형식 검증 및 파싱
    private UUID extractIdempotencyToken(ServletRequestAttributes attributes, String headerName) {
        try {
            // 헤더에서 Idempotency-Key 문자열 추출
            String idempotencyKeyValue = attributes.getRequest().getHeader(headerName);
            if (idempotencyKeyValue == null || idempotencyKeyValue.isBlank()) {
                throw new IdempotencyException(IdempotencyErrorCode.MISSING_IDEMPOTENCY_TOKEN);
            }

            // UUID 형식으로 변환
            return UUID.fromString(idempotencyKeyValue);
        } catch (IllegalArgumentException e) {
            throw new IdempotencyException(IdempotencyErrorCode.INVALID_IDEMPOTENCY_TOKEN);
        }
    }

    // 비즈니스 로직 실행 및 결과 저장(멱등성 엔티티에 응답 결과 반영)
    private Object executeAndSaveResponse(ProceedingJoinPoint pjp, UUID idempotencyToken, HttpServletResponse response) throws Throwable {
        try {
            Object responseResult = pjp.proceed();
            idempotencyApplicationService.updateResponse(idempotencyToken, response.getStatus(), JsonUtil.toJsonStr(responseResult));
            return responseResult;

        } catch (BusinessException be) {
            int responseCode = be.getErrorCode().getStatus().value();
            ErrorResponse errorResponse = ErrorResponse.of(be.getErrorCode());
            idempotencyApplicationService.updateResponse(idempotencyToken, responseCode, JsonUtil.toJsonStr(errorResponse));
            throw be;

        } catch (Exception e) {
            ErrorCode errorCode = CommonErrorCode.INTERNAL_SERVER_ERROR;
            ErrorResponse errorResponse = ErrorResponse.of(errorCode);
            idempotencyApplicationService.updateResponse(idempotencyToken, errorCode.getStatus().value(), JsonUtil.toJsonStr(errorResponse));
            throw e;
        }
    }

}
