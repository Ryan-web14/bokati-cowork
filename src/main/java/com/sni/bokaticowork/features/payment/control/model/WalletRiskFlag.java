package com.sni.bokaticowork.features.payment.control.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.payment.model.WalletAccount;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Un signalement sur un portefeuille.
 *
 * <p>Detection automatique, revue humaine. Le signalement ne decide rien : il dit qu'un humain doit
 * regarder, et garde la trace de ce qu'il a conclu. Un meme signal ne s'ouvre pas deux fois tant
 * que le premier n'est pas traite, sans quoi une rafale de codes faux produirait une rafale de
 * signalements et la revue s'y noierait.</p>
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "wallet_risk_flag")
public class WalletRiskFlag {

    public enum Type {
        UNUSUAL_VOLUME, RAPID_IN_OUT, STRUCTURING, MANY_COUNTERPARTIES, DORMANT_REACTIVATION,
        FAILED_PIN_BURST, LIMIT_BREACH_ATTEMPT, NEGATIVE_BALANCE, INTEGRITY_BREAK,
        REVOKED_DEVICE_USED, NEW_DEVICE_LARGE_TRANSFER, LIST_MATCH, IDENTITY_REVIEW_DUE, PHONE_CHANGED
    }

    public enum Severity {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    public enum Status {
        OPEN, UNDER_REVIEW, CONFIRMED, DISMISSED
    }

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "flag_number", nullable = false, unique = true, length = 100)
    private String flagNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wallet_id", nullable = false)
    private WalletAccount wallet;

    @Enumerated(EnumType.STRING)
    @Column(name = "flag_type", nullable = false, length = 60)
    private Type flagType;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    @Builder.Default
    private Severity severity = Severity.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    @Builder.Default
    private Status status = Status.OPEN;

    @Column(name = "details", length = 1000)
    private String details;

    @Column(name = "reference", length = 180)
    private String reference;

    @Column(name = "detected_at", nullable = false)
    private Instant detectedAt;

    @Column(name = "detected_by", nullable = false, length = 120)
    @Builder.Default
    private String detectedBy = "SYSTEM";

    @Column(name = "reviewed_by", length = 120)
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "resolution", length = 1000)
    private String resolution;

    /** La regle qui l'a leve · nulle pour les signalements que le systeme leve sans regle. */
    @Column(name = "rule_code", length = 60)
    private String ruleCode;

    /** Le dossier qui l'a pris · nul tant qu'aucun dossier ne l'a repris. */
    @Column(name = "case_number", length = 100)
    private String caseNumber;

    public boolean open() {
        return status == Status.OPEN || status == Status.UNDER_REVIEW;
    }

    @PrePersist
    public void prePersist() {
        detectedAt = detectedAt == null ? Instant.now() : detectedAt;
    }
}
