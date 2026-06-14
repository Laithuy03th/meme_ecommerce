package com.example.MyWeb.util;

import jakarta.servlet.http.Cookie;

public class CookieUtil {

    // Cookie names
    public static final String REFRESH_TOKEN_COOKIE = "refreshToken";
    public static final String ACCESS_TOKEN_COOKIE = "accessToken";

    public static final String ADMIN_REFRESH_TOKEN_COOKIE = "adminRefreshToken";

    // Cookie settings
    private static final int REFRESH_TOKEN_MAX_AGE = 7 * 24 * 60 * 60; // 7 days in seconds
    private static final int ACCESS_TOKEN_MAX_AGE = 15 * 60; // 15 minutes in seconds

    public static Cookie createRefreshTokenCookie(String refreshToken) {
        return createRefreshTokenCookie(refreshToken, REFRESH_TOKEN_MAX_AGE);
    }

    public static Cookie createRefreshTokenCookie(String refreshToken, int maxAge) {
        return createRefreshTokenCookie(refreshToken, maxAge, "/");
    }

    public static Cookie createRefreshTokenCookie(String refreshToken, int maxAge, String path) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE, refreshToken);

        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath(path);
        cookie.setMaxAge(maxAge);

        return cookie;
    }

    public static Cookie createAccessTokenCookie(String accessToken) {
        return createAccessTokenCookie(accessToken, ACCESS_TOKEN_MAX_AGE);
    }

    public static Cookie createAccessTokenCookie(String accessToken, int maxAge) {
        Cookie cookie = new Cookie(ACCESS_TOKEN_COOKIE, accessToken);

        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);

        return cookie;
    }

    public static Cookie deleteRefreshTokenCookie() {
        return deleteRefreshTokenCookie("/");
    }

    public static Cookie deleteRefreshTokenCookie(String path) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE, null);
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath(path);
        cookie.setMaxAge(0);

        return cookie;
    }

    public static Cookie createAdminRefreshTokenCookie(String refreshToken) {
        return createAdminRefreshTokenCookie(refreshToken, REFRESH_TOKEN_MAX_AGE, "/");
    }

    public static Cookie createAdminRefreshTokenCookie(String refreshToken, int maxAge, String path) {
        Cookie cookie = new Cookie(ADMIN_REFRESH_TOKEN_COOKIE, refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath(path);
        cookie.setMaxAge(maxAge);
        return cookie;
    }

    public static Cookie deleteAdminRefreshTokenCookie() {
        Cookie cookie = new Cookie(ADMIN_REFRESH_TOKEN_COOKIE, null);
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        return cookie;
    }

    public static String getAdminRefreshTokenFromCookies(Cookie[] cookies) {
        if (cookies == null || cookies.length == 0)
            return null;
        for (Cookie cookie : cookies) {
            if (ADMIN_REFRESH_TOKEN_COOKIE.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    public static Cookie deleteAccessTokenCookie() {
        Cookie cookie = new Cookie(ACCESS_TOKEN_COOKIE, null);
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge(0);

        return cookie;
    }

    public static String getRefreshTokenFromCookies(Cookie[] cookies) {
        if (cookies == null || cookies.length == 0) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (REFRESH_TOKEN_COOKIE.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }

    public static String getAccessTokenFromCookies(Cookie[] cookies) {
        if (cookies == null || cookies.length == 0) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (ACCESS_TOKEN_COOKIE.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }

    public static boolean hasCookie(Cookie[] cookies, String cookieName) {
        if (cookies == null || cookies.length == 0) {
            return false;
        }

        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName())) {
                return true;
            }
        }

        return false;
    }
}
