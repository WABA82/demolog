package com.examples.demolog.global.utils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Arrays;
import java.util.Optional;

public final class CookieUtils {

    private CookieUtils() {
        // util class
    }

    /* ===================== 조회 ===================== */

    public static Optional<Cookie> getCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }

        return Arrays.stream(request.getCookies())
                .filter(cookie -> cookie.getName().equals(name))
                .findFirst();
    }

    /* ===================== 생성 ===================== */

    public static void addCookie(
            HttpServletResponse response,
            String name,
            String value,
            int maxAge,
            boolean httpOnly,
            boolean secure,
            String path,
            String domain
    ) {
        Cookie cookie = new Cookie(name, value);
        cookie.setMaxAge(maxAge);
        cookie.setHttpOnly(httpOnly);
        cookie.setSecure(secure);
        cookie.setPath(path);

        if (domain != null && !domain.isBlank()) {
            cookie.setDomain(domain);
        }

        response.addCookie(cookie);
    }

    /* ===================== 삭제 ===================== */

    /**
     * 쿠키 삭제
     */
    public static void deleteCookie(
            HttpServletResponse response,
            String name,
            String path
    ) {
        CookieUtils.deleteCookie(response, name, path, null);
    }

    /**
     * 쿠키 삭제
     */
    public static void deleteCookie(
            HttpServletResponse response,
            String name,
            String path,
            String domain
    ) {
        Cookie cookie = new Cookie(name, null);
        cookie.setMaxAge(0); // 즉시 만료
        cookie.setPath(path);

        if (domain != null && !domain.isBlank()) {
            cookie.setDomain(domain);
        }

        response.addCookie(cookie);
    }

    /* ===================== SameSite 대응 ===================== */

    /**
     * Servlet Cookie API는 SameSite를 직접 지원하지 않아서
     * 헤더로 직접 내려야 할 때 사용하는 메서드
     */
    public static void addSameSiteCookie(
            HttpServletResponse response,
            String name,
            String value,
            int maxAge,
            boolean httpOnly,
            boolean secure,
            String path,
            String sameSite
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append("=").append(value).append("; ");
        sb.append("Path=").append(path).append("; ");
        sb.append("Max-Age=").append(maxAge).append("; ");

        if (httpOnly) sb.append("HttpOnly; ");
        if (secure) sb.append("Secure; ");

        sb.append("SameSite=").append(sameSite);

        response.addHeader("Set-Cookie", sb.toString());
    }
}
