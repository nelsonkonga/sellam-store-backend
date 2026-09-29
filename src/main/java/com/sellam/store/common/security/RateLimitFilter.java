package com.sellam.store.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Plafond fixe par minute et par adresse.
 * Les routes d'authentification sont plus serrées que le reste de l'API.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter
{
    private static final int AUTH_LIMIT = 20;
    private static final int GLOBAL_LIMIT = 300;
    private static final long WINDOW_MS = 60_000L;

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException
    {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod()))
        {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        boolean authRoute = path.startsWith("/api/auth/");
        int limit = authRoute ? AUTH_LIMIT : GLOBAL_LIMIT;
        String key = clientKey(request) + (authRoute ? "|auth" : "|api");

        if (!tryConsume(key, limit))
        {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"message\":\"Trop de requêtes. Patientez une minute avant de réessayer.\",\"error\":\"TOO_MANY_REQUESTS\",\"status\":429}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean tryConsume(String key, int limit)
    {
        long now = System.currentTimeMillis();
        Window window = windows.compute(key, (k, existing) -> {
            if (existing == null || now - existing.startedAt > WINDOW_MS)
            {
                return new Window(now, new AtomicInteger(0));
            }
            return existing;
        });
        return window.count.incrementAndGet() <= limit;
    }

    private String clientKey(HttpServletRequest request)
    {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank())
        {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private record Window(long startedAt, AtomicInteger count) {}
}
