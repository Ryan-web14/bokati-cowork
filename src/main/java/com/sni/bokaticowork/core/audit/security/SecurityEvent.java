package com.sni.bokaticowork.core.audit.security;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** Evenement de securite (centre d'audit, lot A2). Voir {@link SecurityEventType}. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "security_event")
public class SecurityEvent {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "event_type", nullable = false, length = 40)
    private String eventType;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "user_code", length = 80)
    private String userCode;

    /** Identifiant saisi (courriel) : connu meme quand le compte ne l'est pas. */
    @Column(name = "login", length = 250)
    private String login;

    @Column(name = "ip_address", length = 80)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "request_id", length = 64)
    private String requestId;

    @Column(name = "path", length = 300)
    private String path;

    @Column(name = "details_json", columnDefinition = "text")
    private String detailsJson;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
