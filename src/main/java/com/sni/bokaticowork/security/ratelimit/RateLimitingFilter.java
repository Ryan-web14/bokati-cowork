package com.sni.bokaticowork.security.ratelimit;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    @Value("${app.security.rate-limit.enabled:true}")
    private boolean enabled;

    @Value("${app.security.rate-limit.login.max-requests:5}")
    private int loginMaxRequests;

    @Value("${app.security.rate-limit.login.window-seconds:60}")
    private long loginWindowSeconds;

    @Value("${app.security.rate-limit.otp.max-requests:3}")
    private int otpMaxRequests;

    @Value("${app.security.rate-limit.otp.window-seconds:600}")
    private long otpWindowSeconds;

    @Value("${app.security.rate-limit.callback.max-requests:120}")
    private int callbackMaxRequests;

    @Value("${app.security.rate-limit.callback.window-seconds:60}")
    private long callbackWindowSeconds;

    @Value("${app.security.rate-limit.admin.max-requests:600}")
    private int adminMaxRequests;

    @Value("${app.security.rate-limit.admin.window-seconds:60}")
    private long adminWindowSeconds;

    @Override
    protected void doFilterInternal(
            @NotNull HttpServletRequest request,
            @NotNull HttpServletResponse response,
            @NotNull FilterChain filterChain) throws ServletException, IOException {
        if (!enabled) {
            filterChain.doFilter(request, response);
            return;
        }

        Rule rule = ruleFor(request);
        if (rule == null || allow(rule.key(), rule.maxRequests(), Duration.ofSeconds(rule.windowSeconds()))) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.getWriter().write("""
                {"errorCode":"TOO_MANY_REQUESTS","status":429,"message":"Too many requests. Please try again later."}
                """);
    }

    private boolean allow(String key, int maxRequests, Duration duration) {
        Instant now = Instant.now();
        Window window = windows.compute(key, (ignored, current) -> {
            if (current == null || current.expiresAt().isBefore(now)) {
                return new Window(1, now.plus(duration));
            }
            return new Window(current.count() + 1, current.expiresAt());
        });
        return window.count() <= maxRequests;
    }

    private Rule ruleFor(HttpServletRequest request) {
        String path = request.getRequestURI() == null ? "" : request.getRequestURI().toLowerCase(Locale.ROOT);
        String ip = clientIp(request);

        if (path.equals(ApiPath.V1 + "/auth/login")) {
            return new Rule("login:" + ip, loginMaxRequests, loginWindowSeconds);
        }
        if (path.startsWith(ApiPath.V1 + "/auth/ott/") || path.startsWith(ApiPath.V1 + "/auth/password-reset/")) {
            return new Rule("otp:" + ip + ":" + path, otpMaxRequests, otpWindowSeconds);
        }
        // Les routes publiques du mobile money · rappels de l'operateur et retour du navigateur.
        // La route de retour declenche une lecture chez l'operateur a chaque appel : sans plafond,
        // une boucle sur cette URL consommait notre quota d'API et notre base.
        if (path.startsWith(ApiPath.V1 + "/payments/mobile-money/")
                && (path.contains("callback") || path.endsWith("/return"))) {
            return new Rule("callback:" + ip + ":" + path, callbackMaxRequests, callbackWindowSeconds);
        }
        if (path.startsWith(ApiPath.V1)) {
            return new Rule("admin:" + ip, adminMaxRequests, adminWindowSeconds);
        }
        return null;
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
    }

    private record Rule(String key, int maxRequests, long windowSeconds) {
    }

    private record Window(int count, Instant expiresAt) {
    }
}
