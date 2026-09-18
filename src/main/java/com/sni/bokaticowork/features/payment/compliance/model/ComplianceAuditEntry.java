package com.sni.bokaticowork.features.payment.compliance.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Une ligne de la piste d'audit de conformite · chainee, conservee dix ans.
 *
 * <p>Chainee comme le grand livre des portefeuilles : chaque ligne porte l'empreinte de la
 * precedente, une ligne modifiee ou retiree rompt la suivante. La duree de conservation n'est pas
 * negociable et se pose des la conception · d'ou {@code retainUntil}, en dessous duquel aucune
 * purge ne serait admissible. Et il n'y a pas de purge : une purge ecrite trop tot est
 * irreversible.</p>
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "compliance_audit_entry")
public class ComplianceAuditEntry {

    public static final int RETENTION_YEARS = 10;

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "entry_uuid", nullable = false, unique = true)
    private UUID entryUuid;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "actor", nullable = false, length = 120)
    private String actor;

    @Column(name = "actor_role", length = 60)
    private String actorRole;

    @Column(name = "action", nullable = false, length = 60)
    private String action;

    @Column(name = "subject_type", nullable = false, length = 40)
    private String subjectType;

    @Column(name = "subject_code", nullable = false, length = 120)
    private String subjectCode;

    @Column(name = "before_state", columnDefinition = "text")
    private String beforeState;

    @Column(name = "after_state", columnDefinition = "text")
    private String afterState;

    @Column(name = "rationale", columnDefinition = "text")
    private String rationale;

    @Column(name = "previous_hash", nullable = false, length = 128)
    private String previousHash;

    @Column(name = "current_hash", nullable = false, length = 128)
    private String currentHash;

    @Column(name = "retain_until", nullable = false)
    private LocalDate retainUntil;
}
