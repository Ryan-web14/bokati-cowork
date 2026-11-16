package com.sni.bokaticowork.core.audit.traffic;

import com.sni.bokaticowork.core.observability.RequestContext;
import com.sni.bokaticowork.core.settings.platform.PlatformSettings;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;
import java.time.Instant;

/**
 * Mesure de chaque requete (centre d'audit, lot A3), juste apres l'identifiant de requete et avant
 * Spring Security : les refus (401, 403, 429) sont comptes aussi. La route est le gabarit trouve par
 * Spring MVC ({@code /sni/api/v1/billing/documents/{documentNumber}}), jamais l'adresse brute.
 *
 * <p>Non mesurees : WebSocket et SockJS ({@code /ws}, connexions longues) et Actuator.</p>
 */
@Component("bokatiTrafficFilter")
@Order(Ordered.HIGHEST_PRECEDENCE + 6)
public class TrafficFilter extends OncePerRequestFilter {

    static final String SLOW_MS_KEY = "bokati.audit.traffic.slow-ms";
    static final long DEFAULT_SLOW_MS = 2_000;
    static final String UNMATCHED = "(sans route)";
    /** Refus de la securite (401, 403, 429) avant que Spring MVC ait trouve la route. */
    static final String REFUSED = "(refusee avant la route)";

    private final TrafficRecorder recorder;
    private final PlatformSettings settings;

    public TrafficFilter(TrafficRecorder recorder, PlatformSettings settings) {
        this.recorder = recorder;
        this.settings = settings;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/ws/") || path.equals("/ws") || path.startsWith("/actuator");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long start = System.nanoTime();
        Instant at = Instant.now();
        boolean failed = false;
        try {
            chain.doFilter(request, response);
        } catch (IOException | ServletException | RuntimeException exception) {
            failed = true;
            throw exception;
        } finally {
            try {
                measure(request, response, at, (System.nanoTime() - start) / 1_000_000, failed);
            } catch (RuntimeException ignored) {
                // La mesure ne fait jamais echouer la requete.
            }
        }
    }

    private void measure(HttpServletRequest request, HttpServletResponse response, Instant at, long durationMs, boolean failed) {
        int status = failed && response.getStatus() < 400 ? 500 : response.getStatus();
        Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        String route = pattern != null ? String.valueOf(pattern)
                : status == 401 || status == 403 || status == 429 ? REFUSED : UNMATCHED;
        String method = request.getMethod();
        recorder.record(at, method, route, status, durationMs);

        long slowMs = settings.number(SLOW_MS_KEY, DEFAULT_SLOW_MS);
        String reason = status >= 500 ? "ERROR" : durationMs >= slowMs ? "SLOW" : null;
        if (reason != null) {
            Object user = request.getAttribute(RequestContext.ATTRIBUTE_USER);
            recorder.keep(new TrafficRecorder.SlowRequest(at, RequestContext.currentRequestId(), method,
                    cut(request.getRequestURI(), 300), cut(route, 300), status, durationMs,
                    user == null ? null : cut(String.valueOf(user), 250), cut(RequestContext.clientIp(request), 80), reason));
        }
    }

    private static String cut(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }
}
