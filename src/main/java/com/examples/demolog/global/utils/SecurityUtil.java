package com.examples.demolog.global.utils;

import com.examples.demolog.global.exception.BusinessException;
import com.examples.demolog.global.exception.CommonErrorCode;
import com.examples.demolog.global.security.CustomUserDetails;
import lombok.NoArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class SecurityUtil {

    /**
     * 현재 인증된 사용자의 CustomUserDetails를 반환합니다.
     */
    public static CustomUserDetails getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails principal)) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }

        return principal;
    }

    /**
     * 현재 인증된 사용자의 ID를 반환합니다.
     */
    public static UUID getCurrentUserId() {
        return getCurrentUser().getUserId();
    }

}