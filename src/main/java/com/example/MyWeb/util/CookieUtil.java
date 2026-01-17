package com.example.MyWeb.util;

import jakarta.servlet.http.Cookie;

/**
 * Utility class for working with HttpOnly cookies
 * Used for secure token storage (refresh tokens)
 */
public class CookieUtil {

    // Cookie names
    public static final String REFRESH_TOKEN_COOKIE = "refreshToken";
    public static final String ACCESS_TOKEN_COOKIE = "accessToken";

    // Cookie settings
    private static final int REFRESH_TOKEN_MAX_AGE = 7 * 24 * 60 * 60; // 7 days in seconds
    private static final int ACCESS_TOKEN_MAX_AGE = 15 * 60; // 15 minutes in seconds

    /**
     * Create HttpOnly cookie for refresh token
     * Protects against XSS attacks
     * 
     * @param refreshToken The JWT refresh token
     * @return Cookie object configured for security
     */
    public static Cookie createRefreshTokenCookie(String refreshToken) {
        return createRefreshTokenCookie(refreshToken, REFRESH_TOKEN_MAX_AGE);
    }

    /**
     * Create HttpOnly cookie for refresh token with custom max age
     */
    public static Cookie createRefreshTokenCookie(String refreshToken, int maxAge) {
        return createRefreshTokenCookie(refreshToken, maxAge, "/");
    }

    /**
     * Create HttpOnly cookie for refresh token with custom path
     * Used to isolate admin and client cookies on localhost
     * 
     * @param refreshToken The JWT refresh token
     * @param maxAge       Cookie lifetime in seconds
     * @param path         Cookie path (e.g., "/admin" or "/")
     * @return Cookie object configured for security
     */
    public static Cookie createRefreshTokenCookie(String refreshToken, int maxAge, String path) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE, refreshToken);

        cookie.setHttpOnly(true); // Cannot be accessed by JavaScript - XSS protection
        cookie.setSecure(false); // Set to true in production (HTTPS only) // TODO: change to true for production
        cookie.setPath(path); // Custom path for cookie isolation
        cookie.setMaxAge(maxAge); // 7 days by default
        // SameSite attribute for CSRF protection (requires Servlet 6.0+)
        // cookie.setAttribute("SameSite", "Strict");

        return cookie;
    }

    /**
     * Create HttpOnly cookie for access token (optional - if storing in cookie too)
     */
    public static Cookie createAccessTokenCookie(String accessToken) {
        return createAccessTokenCookie(accessToken, ACCESS_TOKEN_MAX_AGE);
    }

    /**
     * Create HttpOnly cookie for access token with custom max age
     */
    public static Cookie createAccessTokenCookie(String accessToken, int maxAge) {
        Cookie cookie = new Cookie(ACCESS_TOKEN_COOKIE, accessToken);

        cookie.setHttpOnly(true);
        cookie.setSecure(false); // Set to true in production
        cookie.setPath("/");
        cookie.setMaxAge(maxAge); // 15 minutes by default

        return cookie;
    }

    /**
     * Delete refresh token cookie by setting maxAge to 0
     */
    public static Cookie deleteRefreshTokenCookie() {
        return deleteRefreshTokenCookie("/");
    }

    /**
     * Delete refresh token cookie with custom path
     */
    public static Cookie deleteRefreshTokenCookie(String path) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE, null);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // Match creation settings
        cookie.setPath(path);
        cookie.setMaxAge(0); // Delete immediately

        return cookie;
    }

    /**
     * Delete access token cookie
     */
    public static Cookie deleteAccessTokenCookie() {
        Cookie cookie = new Cookie(ACCESS_TOKEN_COOKIE, null);
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge(0);

        return cookie;
    }

    /**
     * Extract refresh token from cookies array
     * 
     * @param cookies Array of cookies from HttpServletRequest
     * @return Refresh token value or null if not found
     */
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

    /**
     * Extract access token from cookies array
     */
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

    /**
     * Check if a specific cookie exists
     */
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
