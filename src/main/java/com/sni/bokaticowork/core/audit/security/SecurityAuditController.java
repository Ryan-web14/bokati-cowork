package com.sni.bokaticowork.core.audit.security;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Centre d'audit, lot A2 : evenements de securite et sessions. Droit {@code SYSTEM:AUDIT}
 * (chemin {@code /admin/audit}, {@code AdminApiAuthorizationManager}), donne a tous les
 * administrateurs.
 */
@RestController
@RequestMapping(ApiPath.V1 + "/admin/audit")
@RequiredArgsConstructor
public class SecurityAuditController {

    private final SecurityAuditService service;

    /** {@code type} : un ou plusieurs types separes par des virgules ({@link SecurityEventType}). */
    @GetMapping("/security-events")
    @Audited(module = "AUDIT", action = "VIEW_SECURITY_EVENTS", ressource = "security_event")
    public ResponseEntity<Page<SecurityAuditService.EventView>> events(
            @RequestParam(required = false) String type, @RequestParam(required = false) String login,
            @RequestParam(required = false) String userCode, @RequestParam(required = false) String ip,
            @RequestParam(required = false) String requestId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(service.events(type, login, userCode, ip, requestId, from, to, page, size));
    }

    @GetMapping("/users/{userCode}/sessions")
    @Audited(module = "AUDIT", action = "VIEW_USER_SESSIONS", ressource = "user_sessions")
    public ResponseEntity<List<SecurityAuditService.SessionView>> sessions(@PathVariable String userCode,
                                                                          @RequestParam(defaultValue = "true") boolean activeOnly) {
        return ResponseEntity.ok(service.sessions(userCode, activeOnly));
    }

    @DeleteMapping("/sessions/{sessionId}")
    @Audited(module = "SECURITY", action = "REVOKE_SESSION", ressource = "user_sessions")
    public ResponseEntity<SecurityAuditService.SessionView> revoke(@PathVariable String sessionId,
                                                                  @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(service.revoke(sessionId, reason));
    }

    @DeleteMapping("/users/{userCode}/sessions")
    @Audited(module = "SECURITY", action = "REVOKE_ALL_SESSIONS", ressource = "user_sessions")
    public ResponseEntity<Map<String, Integer>> revokeAll(@PathVariable String userCode, @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(Map.of("revoked", service.revokeAll(userCode, reason)));
    }
}
