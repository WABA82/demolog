package com.examples.demolog.domains.common.idempotency2.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * HTTP 메서드에 멱등성 검증을 적용하는 어노테이션
 *
 * 클라이언트가 전송한 "Idempotency-Key" 헤더를 통해 멱등성을 보장합니다.
 * 동일한 키로 재요청 시 이전 응답을 반환합니다.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {

    /**
     * Idempotency-Key 헤더 필수 여부
     * true: 헤더 없으면 400 Bad Request
     * false: 헤더 없어도 요청 진행 (멱등성 검증 스킵)
     */
    boolean required() default true;

    /**
     * 멱등성 레코드 만료 시간 (시간 단위)
     */
    int expirationHours() default 24;

}
