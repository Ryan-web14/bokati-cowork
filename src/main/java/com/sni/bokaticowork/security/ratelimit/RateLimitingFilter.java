package com.sni.bokaticowork.security.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

/**
 * Le plafond de requetes · desormais sur toutes les routes, pas seulement l'API.
 *
 * <p>Le choix du plafond est dans {@link RateLimitRules}, le comptage dans
 * {@link RateLimitStore}, et l'adresse du client dans {@link ClientIpResolver} · chacun se lit et
 * se teste seul. Ce filtre ne fait que les mettre bout a bout et rendre le refus.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RateLimitRules rules;
    private final RateLimitStore store;
    private final ClientIpResolver clientIpResolver;

    @Value("${app.security.rate-limit.enabled:true}")
    private boolean enabled;

    @Override
    protected void doFilterInternal(
            @NotNull HttpServletRequest request,
            @NotNull HttpServletResponse response,
            @NotNull FilterChain filterChain) throws ServletException, IOException {
        if (!enabled) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = clientIpResolver.resolve(request);
        RateLimitRules.Rule rule = rules.ruleFor(request.getMethod(), request.getRequestURI(), clientIp);

        if (store.allow(rule.key(), rule.maxRequests(), Duration.ofSeconds(rule.windowSeconds()))) {
            filterChain.doFilter(request, response);
            return;
        }

        // Nommer l'adresse et la route · un plafond atteint est soit un abus, soit une interface
        // qui boucle. Les deux se diagnostiquent par la meme ligne.
        log.warn("Plafond atteint · {} {} depuis {}", request.getMethod(), request.getRequestURI(), clientIp);
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        // Retry-After · une interface qui respecte l'en-tete cesse de marteler d'elle-meme.
        response.setHeader("Retry-After", String.valueOf(rule.windowSeconds()));
        response.getWriter().write("""
                {"errorCode":"TOO_MANY_REQUESTS","status":429,"message":"Too many requests. Please try again later."}
                """);
    }
}
