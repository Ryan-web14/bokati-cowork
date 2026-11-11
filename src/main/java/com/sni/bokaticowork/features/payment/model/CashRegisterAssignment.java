package com.sni.bokaticowork.features.payment.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;

/**
 * Affectation d'une personne a une caisse : role, qualite et periode.
 *
 * <p>Une personne sans affectation valide (role autre que {@code VIEWER}) ne peut pas ouvrir de
 * session sur la caisse. L'affectation n'est jamais supprimee : elle est revoquee
 * ({@code revokedAt}) ou se termine ({@code validTo}), ce qui garde la succession des titulaires.</p>
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "cash_register_assignment")
public class CashRegisterAssignment {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "register_code", nullable = false, length = 100)
    private String registerCode;

    /** Courriel de la personne : le meme identifiant que l'acteur tire du jeton. */
    @Column(name = "user_email", nullable = false, length = 180)
    private String userEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private com.sni.bokaticowork.features.payment.enums.CashAssignmentRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "quality", nullable = false, length = 20)
    private com.sni.bokaticowork.features.payment.enums.CashAssignmentQuality quality;

    /** Ordre de preference d'un remplacant (1 = premier propose). */
    @Column(name = "backup_rank")
    private Integer backupRank;

    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;

    @Column(name = "valid_to")
    private Instant validTo;

    /** Affectation d'urgence par un superviseur : courte et tracee. */
    @Builder.Default
    @Column(name = "emergency", nullable = false)
    private Boolean emergency = Boolean.FALSE;

    /** Origine : MANUAL, EMERGENCY, RESPONSIBILITY_TRANSFER, ABSENCE, MIGRATION. */
    @Column(name = "source", length = 40)
    private String source;

    @Column(name = "source_reference", length = 100)
    private String sourceReference;

    @Column(name = "granted_by", length = 180)
    private String grantedBy;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "revoked_by", length = 180)
    private String revokedBy;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (validFrom == null) {
            validFrom = createdAt;
        }
    }

    /** L'affectation est-elle en vigueur a cet instant ? */
    public boolean isEffectiveAt(Instant at) {
        return revokedAt == null
                && (validFrom == null || !validFrom.isAfter(at))
                && (validTo == null || validTo.isAfter(at));
    }
}
