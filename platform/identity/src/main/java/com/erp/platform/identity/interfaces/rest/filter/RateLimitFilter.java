package com.erp.platform.identity.interfaces.rest.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple in-memory rate limiter for sensitive authentication endpoints.
 *
 * <p>Limits each client IP to a fixed number of requests per rolling window
 * on {@code /auth/login}, {@code /auth/refresh} and {@code /auth/forgot-password}
 * to slow down brute-force and credential-stuffing attacks. This is a
 * best-effort, single-instance guard; in a multi-instance deployment it should
 * be replaced by a distributed limiter (e.g. Bucket4j + Redis) at the gateway.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS = 10;
    private static final long WINDOW_MS = 60_000L;

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (isSensitive(path)) {
            String key = request.getRemoteAddr() + "|" + path;
            Window w = windows.compute(key, (k, existing) -> {
                long now = System.currentTimeMillis();
                if (existing == null || now - existing.start > WINDOW_MS) {
                    return new Window(now, new AtomicInteger(1));
                }
                existing.count.incrementAndGet();
                return existing;
            });
            if (w.count.get() > MAX_REQUESTS) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setHeader("Retry-After", "60");
                response.getWriter().write("Too many requests");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private boolean isSensitive(String path) {
        return path != null && (path.endsWith("/auth/login")
                || path.endsWith("/auth/refresh")
                || path.endsWith("/auth/forgot-password"));
    }

    private static final class Window {
        final long start;
        final AtomicInteger count;

        Window(long start, AtomicInteger count) {
            this.start = start;
            this.count = count;
        }
    }
}
