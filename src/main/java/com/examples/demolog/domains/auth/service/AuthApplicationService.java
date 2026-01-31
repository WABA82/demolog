package com.examples.demolog.domains.auth.service;

import com.examples.demolog.domains.auth.dto.request.LoginRequest;
import com.examples.demolog.domains.auth.dto.request.SignupRequest;
import com.examples.demolog.domains.auth.dto.response.AppUserResponse;
import com.examples.demolog.domains.auth.exception.AuthErrorCode;
import com.examples.demolog.domains.auth.exception.AuthException;
import com.examples.demolog.domains.auth.model.AppUser;
import com.examples.demolog.domains.auth.repository.AppUserRepository;
import com.examples.demolog.global.security.CustomUserDetails;
import com.examples.demolog.global.utils.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AuthApplicationService {

    private final AppUserRepository appUserRepository;

    // HttpSession 기반 SecurityContext 저장소
    private final SecurityContextRepository securityContextRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 회원가입
     */
    @Transactional
    public AppUserResponse signup(SignupRequest request) {
        // 이메일 중복 검사
        if (appUserRepository.existsByEmail(request.email())) {
            throw new AuthException(AuthErrorCode.DUPLICATE_EMAIL);
        }

        // 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(request.password());

        // 사용자 저장
        AppUser saved = appUserRepository.save(request.toEntity(encodedPassword));
        return AppUserResponse.from(saved);
    }

    /**
     * 로그인
     */
    @Transactional(readOnly = true)
    public AppUserResponse login(
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse,
            LoginRequest request
    ) {

        // 사용자 조회
        AppUser user = appUserRepository.findByEmail(request.email())
                .orElseThrow(() -> new AuthException(AuthErrorCode.NOT_FOUND_USER));

        // 비밀번호 검증
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new AuthException(AuthErrorCode.INVALID_PASSWORD);
        }

        // 1. 새로운 컨텍스트 생성 및 인증 정보 설정
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        CustomUserDetails userDetails = CustomUserDetails.from(user);
        context.setAuthentication(new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));
        SecurityContextHolder.setContext(context);

        // 2. 저장소(예: 세션)에 명시적으로 저장 (이 코드가 없으면 다음 페이지 이동 시 로그인이 풀림)
        securityContextRepository.saveContext(context, httpRequest, httpResponse);

        return AppUserResponse.from(user);
    }

    /**
     * 로그아웃
     */
    public void logout(HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        // 세션 객체를 완전히 제거(무효화)
        HttpSession session = servletRequest.getSession(false);
        if (!Objects.isNull(session)) session.invalidate();

        // 사큐리티 컨텍스트 비우기
        SecurityContextHolder.clearContext();

        // 쿠키 제거
        CookieUtil.deleteCookie(servletResponse, "JSESSIONID", "/");
    }

    /**
     * 현재 로그인된 사용자 정보 조회
     */
    @Transactional(readOnly = true)
    public AppUserResponse getCurrentUser() {
        CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();

        AppUser user = appUserRepository.findById(userDetails.getUserId())
                .orElseThrow(() -> new AuthException(AuthErrorCode.NOT_FOUND_USER));

        return AppUserResponse.from(user);
    }

}