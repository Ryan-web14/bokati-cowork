package com.sni.bokaticowork.features.payment.compliance.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Une regle de detection · des donnees, pas du code.
 *
 * <p>Un seuil qui se change par deploiement ne se change jamais. La regle porte ses parametres ;
 * le detecteur Java nomme par {@code detector} ne porte que la facon de les lire. Les compteurs de
 * succes et de bruit sont tenus ici parce que c'est la mesure qui garde le module vivant : une
 * regle qui ne produit que du bruit finit par etre ignoree, et son bruit couvre les signaux qui
 * comptaient.</p>
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "compliance_rule")
public class ComplianceRule {

    public enum Category {
        VELOCITY, AMOUNT, PATTERN, COUNTERPARTY, IDENTITY, GEOGRAPHY
    }

    public enum Severity {
        INFO, LOW, MEDIUM, HIGH, CRITICAL
    }

    public enum Action {
        /** Signale · un humain regardera quand il pourra. */
        FLAG,
        /** Signale et ouvre un dossier avec echeance · quelqu'un doit regarder. */
        REQUIRE_REVIEW,
        /** Refuse l'operation avant qu'elle ne s'execute. */
        BLOCK_OPERATION,
        /** Gele le portefeuille · rien n'y entre ni n'en sort tant qu'un humain ne l'a pas leve. */
        FREEZE_WALLET
    }

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "rule_code", nullable = false, unique = true, length = 60)
    private String ruleCode;

    @Column(name = "name", nullable = false, length = 160)
    private String name;

    @Column(name = "description", length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 40)
    private Category category;

    /** Nom du detecteur qui sait lire cette regle · voir {@code ComplianceDetector}. */
    @Column(name = "detector", nullable = false, length = 60)
    private String detector;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    @Builder.Default
    private Severity severity = Severity.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 40)
    @Builder.Default
    private Action action = Action.FLAG;

    @Column(name = "amount_threshold", precision = 19, scale = 4)
    private BigDecimal amountThreshold;

    @Column(name = "count_threshold")
    private Integer countThreshold;

    @Column(name = "ratio_threshold", precision = 9, scale = 4)
    private BigDecimal ratioThreshold;

    @Column(name = "window_minutes")
    private Integer windowMinutes;

    @Column(name = "parameters_json", columnDefinition = "text")
    private String parametersJson;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = Boolean.TRUE;

    @Column(name = "effective_from")
    private Instant effectiveFrom;

    @Column(name = "effective_to")
    private Instant effectiveTo;

    @Column(name = "hit_count", nullable = false)
    @Builder.Default
    private Long hitCount = 0L;

    @Column(name = "confirmed_count", nullable = false)
    @Builder.Default
    private Long confirmedCount = 0L;

    @Column(name = "dismissed_count", nullable = false)
    @Builder.Default
    private Long dismissedCount = 0L;

    @Column(name = "created_by", length = 120)
    private String createdBy;

    @Column(name = "approved_by", length = 120)
    private String approvedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public boolean activeAt(Instant moment) {
        return Boolean.TRUE.equals(active)
                && (effectiveFrom == null || !moment.isBefore(effectiveFrom))
                && (effectiveTo == null || moment.isBefore(effectiveTo));
    }

    /**
     * Part des signalements de cette regle que la revue a ecartes · nulle tant que rien n'a ete revu.
     *
     * <p>C'est le chiffre a regarder avant de toucher un seuil : une regle a 90 % de bruit ne
     * protege plus, elle fatigue.</p>
     */
    public BigDecimal falsePositiveRate() {
        long reviewed = (confirmedCount == null ? 0 : confirmedCount) + (dismissedCount == null ? 0 : dismissedCount);
        if (reviewed == 0) {
            return null;
        }
        return BigDecimal.valueOf(dismissedCount).divide(BigDecimal.valueOf(reviewed), 4, java.math.RoundingMode.HALF_UP);
    }

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}
