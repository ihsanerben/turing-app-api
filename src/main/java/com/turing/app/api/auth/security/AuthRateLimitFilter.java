package com.turing.app.api.auth.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.time.*;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {
    private static final Set<String> LIMITED_PATHS = Set.of(
            "/api/auth/login", "/api/auth/register", "/api/auth/forgot-password", "/api/auth/resend-verification");
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final AuthProperties properties;
    private final Clock clock;
    public AuthRateLimitFilter(AuthProperties properties, Clock clock) { this.properties = properties; this.clock = clock; }

    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equals(request.getMethod()) || !LIMITED_PATHS.contains(request.getRequestURI());
    }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Instant now = clock.instant(); String key = request.getRemoteAddr() + ':' + request.getRequestURI();
        Window window = windows.compute(key, (ignored, current) -> current == null || !current.resetAt.isAfter(now)
                ? new Window(now.plus(properties.rateLimitWindow())) : current.increment());
        if (window.count.get() > properties.rateLimitMaxAttempts()) {
            long retryAfter = Math.max(1, Duration.between(now, window.resetAt).toSeconds());
            response.setStatus(429); response.setHeader("Retry-After", Long.toString(retryAfter));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"status\":429,\"code\":\"RATE_LIMITED\",\"message\":\"Çok fazla deneme yaptınız. Lütfen daha sonra tekrar deneyin.\"}");
            return;
        }
        chain.doFilter(request, response);
    }
    private static final class Window {
        private final Instant resetAt; private final AtomicInteger count = new AtomicInteger(1);
        private Window(Instant resetAt) { this.resetAt = resetAt; }
        private Window increment() { count.incrementAndGet(); return this; }
    }
}
