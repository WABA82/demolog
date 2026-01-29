package com.examples.demolog.domains.auth.controller;

import com.examples.demolog.domains.auth.dto.request.LoginRequest;
import com.examples.demolog.domains.auth.dto.request.SignupRequest;
import com.examples.demolog.domains.auth.dto.response.AppUserResponse;
import com.examples.demolog.domains.auth.service.AuthApplicationService;
import com.examples.demolog.global.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthApplicationService appService;

    /**
     * 회원가입 API
     */
    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ApiResponse<AppUserResponse>> signup(@Valid @RequestBody SignupRequest request) {
        AppUserResponse signup = appService.signup(request);
        return ApiResponse.created(signup);
    }

    /**
     * 로그인 API
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AppUserResponse>> login(
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse,
            @Valid @RequestBody LoginRequest request
    ) {
        AppUserResponse response = appService.login(httpRequest, httpResponse, request);
        return ApiResponse.ok(response);
    }

    /**
     * 로그아웃 API
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        appService.logout(servletRequest, servletResponse);
        return ApiResponse.noContent();
    }

    /**
     * 현재 로그인된 사용자 정보 조회 API
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AppUserResponse>> getCurrentUser() {
        return ApiResponse.ok(appService.getCurrentUser());
    }
}
