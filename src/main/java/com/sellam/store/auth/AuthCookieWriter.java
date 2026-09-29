package com.sellam.store.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class AuthCookieWriter
{
    public static final String ACCESS_COOKIE = "sellam_access";

    @Value("${jwt.expiration}")
    private long expirationMs;

    public void writeAccessCookie(HttpServletRequest request, HttpServletResponse response, String token)
    {
        boolean secure = request.isSecure();
        ResponseCookie cookie = ResponseCookie.from(ACCESS_COOKIE, token)
                .httpOnly(true)
                .secure(secure)
                .sameSite(secure ? "None" : "Lax")
                .path("/")
                .maxAge(Duration.ofMillis(expirationMs))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void clearAccessCookie(HttpServletRequest request, HttpServletResponse response)
    {
        boolean secure = request.isSecure();
        ResponseCookie cookie = ResponseCookie.from(ACCESS_COOKIE, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite(secure ? "None" : "Lax")
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
