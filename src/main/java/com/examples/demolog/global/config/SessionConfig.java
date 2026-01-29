package com.examples.demolog.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

@Configuration
@EnableRedisHttpSession
public class SessionConfig {

    /**
     * Redis 세션 객체의 직렬화 설정 (빈 이름 springSessionDefaultRedisSerializer 필수)
     */
    @Bean
    public RedisSerializer<Object> springSessionDefaultRedisSerializer() {
        // JSON 직렬화 사용: 가독성 향상 및 호환성 증가로 권장
        return new GenericJackson2JsonRedisSerializer();
    }

}
