package com.examples.demolog.domains.common.idempotency2.config;

import com.examples.demolog.domains.common.idempotency2.web.IdempotencyInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 멱등성 검증 인터셉터 등록 설정
 */
@Configuration
@RequiredArgsConstructor
public class IdempotencyWebConfig implements WebMvcConfigurer {

    private final IdempotencyInterceptor idempotencyInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(idempotencyInterceptor);
    }

}
