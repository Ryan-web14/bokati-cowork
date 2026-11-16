package com.sni.bokaticowork.core.audit.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.core.observability.RequestContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Enregistre les evenements de securite (lot A2). Chaque evenement est ecrit dans sa propre
 * transaction : il reste meme quand l'operation qui l'a produit est annulee (un echec de connexion
 * annule la sienne). Une ecriture en echec est journalisee et n'interrompt jamais l'operation.
 */
@Slf4j
@Component
public class SecurityEvents {

    private final SecurityEventRepository repository;
    private final TransactionTemplate separate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SecurityEvents(SecurityEventRepository repository, PlatformTransactionManager transactions) {
        this.repository = repository;
        this.separate = new TransactionTemplate(transactions);
        this.separate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /** Detail facultatif : paires cle, valeur ({@code "reason", "BAD_CREDENTIALS"}). */
    public void record(SecurityEventType type, Long userId, String userCode, String login, Object... details) {
        HttpServletRequest request = currentRequest();
        record(type, userId, userCode, login, request, details);
    }

    public void record(SecurityEventType type, Long userId, String userCode, String login, HttpServletRequest request, Object... details) {
        try {
            Map<String, Object> map = new LinkedHashMap<>();
            for (int index = 0; index + 1 < details.length; index += 2) {
                if (details[index + 1] != null) {
                    map.put(String.valueOf(details[index]), details[index + 1]);
                }
            }
            SecurityEvent event = SecurityEvent.builder()
                    .eventType(type.name())
                    .userId(userId)
                    .userCode(userCode)
                    .login(cut(login == null ? null : login.trim().toLowerCase(java.util.Locale.ROOT), 250))
                    .ipAddress(cut(request == null ? RequestContext.currentClientIp() : RequestContext.clientIp(request), 80))
                    .userAgent(cut(request == null ? null : request.getHeader("User-Agent"), 500))
                    .requestId(RequestContext.currentRequestId())
                    .path(cut(request == null ? null : request.getMethod() + " " + request.getRequestURI(), 300))
                    .detailsJson(map.isEmpty() ? null : objectMapper.writeValueAsString(map))
                    .build();
            separate.executeWithoutResult(status -> repository.save(event));
        } catch (Exception ex) {
            log.warn("Evenement de securite {} non enregistre : {}", type, ex.getMessage());
        }
    }

    /** Derniere ecriture par cle (refus, limitations) : au plus une par minute et par cle. */
    private final java.util.concurrent.ConcurrentHashMap<String, java.time.Instant> throttle = new java.util.concurrent.ConcurrentHashMap<>();
    static final java.time.Duration THROTTLE = java.time.Duration.ofMinutes(1);
    static final int THROTTLE_MAX_KEYS = 10_000;

    /**
     * Comme {@link #record}, mais au plus une fois par minute pour une meme cle (adresse, route,
     * compte) : un ecran qui boucle ou une attaque ne remplit pas la base.
     */
    public void recordThrottled(String key, SecurityEventType type, Long userId, String userCode, String login, HttpServletRequest request,
                                Object... details) {
        java.time.Instant now = java.time.Instant.now();
        if (throttle.size() > THROTTLE_MAX_KEYS) {
            throttle.clear();
        }
        java.time.Instant previous = throttle.get(key);
        if (previous != null && previous.plus(THROTTLE).isAfter(now)) {
            return;
        }
        throttle.put(key, now);
        record(type, userId, userCode, login, request, details);
    }

    /** Refus d'acces (403) : compte connecte s'il y en a un. */
    public void accessDenied(HttpServletRequest request, String reason) {
        org.springframework.security.core.Authentication authentication =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        Long userId = null;
        String userCode = null;
        String login = null;
        if (authentication != null && authentication.getPrincipal() instanceof com.sni.bokaticowork.security.admin.user.model.UserPrincipal principal) {
            userId = principal.getUser().getId();
            userCode = principal.getUser().getUserId();
            login = principal.getUsername();
        }
        String path = request == null ? "" : request.getMethod() + " " + request.getRequestURI();
        recordThrottled("403|" + RequestContext.clientIp(request) + "|" + login + "|" + path, SecurityEventType.ACCESS_DENIED, userId, userCode,
                login, request, "reason", reason);
    }

    private static HttpServletRequest currentRequest() {
        return RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes ? attributes.getRequest() : null;
    }

    private static String cut(String value, int max) {
        return value == null ? null : value.length() > max ? value.substring(0, max) : value;
    }
}
