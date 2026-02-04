package com.examples.demolog.domains.common.idempotency2.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;

/**
 * 멱등성 검증을 위한 요청/응답 바디 캐싱 필터
 *
 * ContentCachingRequestWrapper와 ContentCachingResponseWrapper를 사용하여
 * 요청과 응답의 바디를 캐시하므로, Interceptor에서 여러 번 읽을 수 있습니다.
 */
@Component
@RequiredArgsConstructor
public class IdempotencyCachingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // 요청 바디를 캐시할 수 있도록 래핑
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        // 응답 바디를 캐시할 수 있도록 래핑
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            // 응답 바디를 실제로 클라이언트에 전송
            wrappedResponse.copyBodyToResponse();
        }
    }

}
