package com.sni.bokaticowork.core.audit.security;

import com.sni.bokaticowork.core.audit.model.UserSession;
import com.sni.bokaticowork.core.audit.repository.UserSessionRepository;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.security.admin.user.model.Users;
import com.sni.bokaticowork.security.admin.user.repository.UserRepository;
import com.sni.bokaticowork.security.repository.RefreshTokenRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Centre d'audit, lot A2 : lecture des evenements de securite, sessions d'un utilisateur et leur
 * fermeture par un administrateur. Une session fermee coupe aussitot ses jetons (le filtre et le
 * rafraichissement verifient la session).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class SecurityAuditService {

    private final SecurityEventRepository events;
    private final UserSessionRepository sessions;
    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final SecurityEvents securityEvents;

    public record EventView(Long id, Instant createdAt, String eventType, String userCode, String login, String ipAddress, String userAgent,
                            String requestId, String path, String detailsJson) {
    }

    public record SessionView(String sessionId, Instant createdAt, Instant lastAccessAt, Instant expiredAt, boolean active, boolean revoked,
                              String ipAddress, String deviceType, String userAgent, String location, String roles, Instant revokedAt,
                              String revokedBy, String revokedReason) {
    }

    /** Filtres facultatifs ; periode bornee a 366 jours. */
    @Transactional(readOnly = true)
    public Page<EventView> events(String type, String login, String userCode, String ip, String requestId, Instant from, Instant to,
                                  int page, int size) {
        if (from != null && to != null && from.plus(java.time.Duration.ofDays(366)).isBefore(to)) {
            throw BadRequestException.coded("PERIODE_TROP_LONGUE", "Période de 366 jours au plus");
        }
        Specification<SecurityEvent> spec = (root, query, cb) -> {
            List<Predicate> where = new ArrayList<>();
            if (StringUtils.hasText(type)) {
                where.add(root.get("eventType").in(List.of(type.trim().toUpperCase(Locale.ROOT).split("\\s*,\\s*"))));
            }
            if (StringUtils.hasText(login)) {
                where.add(cb.like(cb.lower(root.get("login")), "%" + login.trim().toLowerCase(Locale.ROOT) + "%"));
            }
            if (StringUtils.hasText(userCode)) {
                where.add(cb.equal(root.get("userCode"), userCode.trim()));
            }
            if (StringUtils.hasText(ip)) {
                where.add(cb.equal(root.get("ipAddress"), ip.trim()));
            }
            if (StringUtils.hasText(requestId)) {
                where.add(cb.equal(root.get("requestId"), requestId.trim()));
            }
            if (from != null) {
                where.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            }
            if (to != null) {
                where.add(cb.lessThan(root.get("createdAt"), to));
            }
            return cb.and(where.toArray(Predicate[]::new));
        };
        PageRequest pageable = PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 200)), Sort.by(Sort.Direction.DESC, "createdAt"));
        return events.findAll(spec, pageable).map(event -> new EventView(event.getId(), event.getCreatedAt(), event.getEventType(),
                event.getUserCode(), event.getLogin(), event.getIpAddress(), event.getUserAgent(), event.getRequestId(), event.getPath(),
                event.getDetailsJson()));
    }

    @Transactional(readOnly = true)
    public List<SessionView> sessions(String userCode, boolean activeOnly) {
        Users user = user(userCode);
        List<UserSession> found = activeOnly ? sessions.findAllByUsers_IdAndRevokedFalseAndIsActiveTrueOrderByCreatedAtDesc(user.getId())
                : sessions.findAllByUsers_IdOrderByCreatedAtAsc(user.getId()).reversed();
        return found.stream().map(SecurityAuditService::view).toList();
    }

    /** Ferme une session : ses jetons d'acces et de rafraichissement ne servent plus. */
    public SessionView revoke(String sessionId, String reason) {
        UserSession session;
        try {
            session = sessions.findBySessionId(UUID.fromString(sessionId.trim()))
                    .orElseThrow(() -> new ResourceNotFoundException("Session introuvable : " + sessionId));
        } catch (IllegalArgumentException ex) {
            throw new ResourceNotFoundException("Session introuvable : " + sessionId);
        }
        close(session, reason);
        Users user = session.getUsers();
        securityEvents.record(SecurityEventType.SESSION_REVOKED, user.getId(), user.getUserId(), user.getEmail(),
                "sessionId", sessionId, "reason", reason, "by", actor());
        return view(session);
    }

    /** Ferme toutes les sessions ouvertes d'un utilisateur ; rend leur nombre. */
    public int revokeAll(String userCode, String reason) {
        Users user = user(userCode);
        List<UserSession> open = sessions.findAllByUsers_IdAndRevokedFalseOrderByCreatedAtDesc(user.getId());
        open.forEach(session -> close(session, reason));
        securityEvents.record(SecurityEventType.SESSION_REVOKED, user.getId(), user.getUserId(), user.getEmail(),
                "sessions", open.size(), "scope", "ALL", "reason", reason, "by", actor());
        return open.size();
    }

    private void close(UserSession session, String reason) {
        Instant now = Instant.now();
        session.setRevoked(true);
        session.setActive(false);
        session.setRevokedAt(now);
        session.setRevokedBy(actor());
        session.setRevokedReason(reason == null ? null : reason.length() > 300 ? reason.substring(0, 300) : reason);
        sessions.save(session);
        if (StringUtils.hasText(session.getRefreshTokenHash())) {
            refreshTokens.findByTokenAndRevokedFalse(session.getRefreshTokenHash()).ifPresent(token -> {
                token.setRevoked(true);
                token.setRevokedAt(now);
                refreshTokens.save(token);
            });
        }
    }

    private Users user(String userCode) {
        return users.findByUserId(userCode.trim()).orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable : " + userCode));
    }

    private static String actor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? "SYSTEM" : authentication.getName();
    }

    private static SessionView view(UserSession session) {
        return new SessionView(session.getSessionId() == null ? null : session.getSessionId().toString(), session.getCreatedAt(),
                session.getLastAccessAt(), session.getExpiredAt(), session.isActive(), session.isRevoked(), session.getIpAddress(),
                session.getDeviceType(), session.getUserAgent(), session.getLocationGuess(), session.getRoleSnapshot(), session.getRevokedAt(),
                session.getRevokedBy(), session.getRevokedReason());
    }
}
