package com.examples.demolog.global.config;

import com.examples.demolog.global.exception.CommonErrorCode;
import com.examples.demolog.global.response.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final ObjectMapper objectMapper;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                // 폼 로그인 비활성화 (REST API 사용)
                .formLogin(AbstractHttpConfigurer::disable)
                // HTTP Basic 인증 비활성화
                .httpBasic(AbstractHttpConfigurer::disable)
                // 시큐리티 로그아웃 비활성화
                .logout(AbstractHttpConfigurer::disable)

                // CSRF 보호 설정 (세션 기반 인증에서 필수)
                .csrf(csrf -> csrf
                        // CSRF 토큰을 쿠키에 저장 (JavaScript에서 접근 가능)
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        // 로그인/회원가입 API는 CSRF 검증 제외
                        .ignoringRequestMatchers("/api/auth/**")
                )

                // CORS 정책
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 세션 관리 정책
                .sessionManagement(session -> {
                    // 필요 시 세션 생성
                    session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED);
                    // 동시 세션 제어 (최대 1개, 이전 세션 만료)
                    session.maximumSessions(1)
                            .maxSessionsPreventsLogin(false)
                            .expiredUrl("/api/auth/session-expired");
                    // 세션 고정 공격 방지 (로그인 시 세션 ID 변경)
                    session.sessionFixation().changeSessionId();
                })

                // URL별 접근 권한 설정
                .authorizeHttpRequests(authorizeRequests -> {

                    // 리소스 접근 허용
                    authorizeRequests.requestMatchers("/public/**", "/favicon.ico").permitAll(); // 리소스
                    // 스웨거 접근 허용
                    authorizeRequests.requestMatchers(
                            "/v3/api-docs/**",
                            "/api/v3/api-docs/**",
                            "/api/swagger-ui/**",
                            "/swagger-ui/**",
                            "/swagger-ui.html",
                            "/swagger-resources/**"
                    ).permitAll();
                    // 헬스 체크 접근 허용
                    authorizeRequests.requestMatchers("/actuator/**").permitAll();
                    // 인증 API 접근 허용
                    authorizeRequests.requestMatchers("/api/auth/**").permitAll();
                    // 커스텀 API
                    authorizeRequests.requestMatchers("/external/**").permitAll();

                    /* :::::::: 이하 접근 제한 URL :::::::: */

                    // 관리자 전용 URL
                    authorizeRequests.requestMatchers("/api/admin/**").hasRole("ADMIN");
                    // 일반 사용자 API
                    authorizeRequests.requestMatchers("/api/**").hasRole("USER");
                    // 인증된 사용자만 접근 가능한 상태 조회 API
                    authorizeRequests.requestMatchers("/api/auth/status").authenticated();
                    // 그 외 모든 요청은 인증 필요
                    authorizeRequests.anyRequest().authenticated();
                })

                // 예외 처리 설정
                .exceptionHandling(configurer -> configurer
                        // 인증 실패 시 처리 (401 Unauthorized)
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write(objectMapper.writeValueAsString(ErrorResponse.of(CommonErrorCode.UNAUTHORIZED)));
                        })
                        // 권한 없음 처리 (403 Forbidden)
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write(objectMapper.writeValueAsString(CommonErrorCode.FORBIDDEN));
                        })
                );

        return http.build();
    }

    /**
     * 세션 이벤트 발행 빈 (Spring Security 사용 시 필수)
     * Spring Session(Redis)이 세션을 관리하더라도, Spring Security는 서블릿 컨테이너(Undertow) 수준에서 발생하는 세션 이벤트를 감지해야만 다음과 같은 기능을 수행
     * - 중복 로그인 방지
     * - 세션 만료 처리
     */
    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    /**
     * 시큐리티 컨텍스트 저장소 설정 (세션 기반)
     */
    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    /**
     * 비밀번호 암호화를 위한 인코더
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * CORS 설정
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // 허용할 출처(Origin)
        config.setAllowedOrigins(Arrays.asList(
                "http://localhost:3000",
                "https://www.your-frontend-domain.com"
        ));

        // 클라이언트로 부터 허용할 HTTP 메서드
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // 클라이언트로 부터 허용할 요청 헤더
        config.setAllowedHeaders(Arrays.asList(
                "Origin",
                "Accept",
                "X-Requested-With",
                "Content-Type",
                "Authorization",
                "X-CSRF-TOKEN",
                "Idempotency-Key"
        ));

        // 클라이언트에서 접근 가능한 응답 헤더
        config.setExposedHeaders(Arrays.asList(
                "Authorization",
                "X-CSRF-TOKEN"
        ));

        // 허용할 Origin (프론트엔드 도메인)
        config.setAllowCredentials(true);

        // Preflight 요청 캐시 1시간
        config.setMaxAge(3600L);

        // 모든 경로(/**)에 CORS 설정 적용
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
