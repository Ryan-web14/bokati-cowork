package com.sni.bokaticowork.core.audit.ops;

import com.sni.bokaticowork.core.observability.RequestContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

/**
 * Erreurs serveur (5xx) gardees une par une (centre d'audit, lot A4), avec l'identifiant de requete,
 * l'utilisateur, la route et la trace tronquee.
 *
 * <p>Ecriture hors du fil de la requete, jamais bloquante ; une rafale de la meme erreur sur la meme
 * route n'est ecrite qu'une fois toutes les dix secondes.</p>
 */
@Slf4j
@Component
public class AppErrors {

    static final Duration THROTTLE = Duration.ofSeconds(10);
    static final int MAX_STACK = 8_000;

    private final JdbcTemplate jdbc;
    private final Executor executor;
    private final Map<String, Instant> lastWritten = new ConcurrentHashMap<>();

    public AppErrors(JdbcTemplate jdbc, @Qualifier("taskExecutor") Executor executor) {
        this.jdbc = jdbc;
        this.executor = executor;
    }

    public void record(int status, String errorCode, String traceId, HttpServletRequest request, Throwable error) {
        try {
            String path = request == null ? null : request.getRequestURI();
            String exception = error == null ? null : error.getClass().getName();
            String key = path + "|" + exception;
            Instant now = Instant.now();
            Instant previous = lastWritten.get(key);
            if (previous != null && Duration.between(previous, now).compareTo(THROTTLE) < 0) {
                return;
            }
            lastWritten.put(key, now);
            if (lastWritten.size() > 1_000) {
                lastWritten.clear();
            }
            Object[] row = {
                    traceId,
                    RequestContext.currentRequestId(),
                    request == null ? null : request.getMethod(),
                    cut(path, 300),
                    status,
                    cut(errorCode, 60),
                    cut(exception, 200),
                    cut(error == null ? null : error.getMessage(), 1000),
                    cut(rootCause(error), 500),
                    cut(stack(error), MAX_STACK),
                    cut(currentUser(), 250),
                    request == null ? null : cut(RequestContext.clientIp(request), 80)
            };
            executor.execute(() -> write(row));
        } catch (RuntimeException exception) {
            log.warn("Erreur serveur non enregistree : {}", exception.getMessage());
        }
    }

    private void write(Object[] row) {
        try {
            jdbc.update("""
                    INSERT INTO app_error (trace_id, request_id, http_method, path, status, error_code, exception_class,
                                           message, root_cause, stack_trace, user_email, client_ip)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, row);
        } catch (RuntimeException exception) {
            log.warn("Erreur serveur non enregistree : {}", exception.getMessage());
        }
    }

    private static String currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null || !authentication.isAuthenticated() ? null : authentication.getName();
    }

    static String rootCause(Throwable error) {
        if (error == null) {
            return null;
        }
        Throwable root = error;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root == error ? null : root.getClass().getSimpleName() + (root.getMessage() == null ? "" : ": " + root.getMessage());
    }

    static String stack(Throwable error) {
        if (error == null) {
            return null;
        }
        StringWriter writer = new StringWriter();
        error.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }

    static String cut(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }
}
