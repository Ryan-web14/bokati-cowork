package com.sni.bokaticowork.features.subscription.lifecycle.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * La politique du cycle de vie · une ligne, des seuils qui sont des donnees.
 *
 * <p>Le prorata, le preavis par defaut, la formule de rupture d'engagement, les bornes du gel, la
 * tolerance avant suspension et la validite d'un devis. Modifier un seuil ne demande pas de livrer.</p>
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "subscription_policy")
public class SubscriptionPolicy {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "policy_code", nullable = false, unique = true, length = 40)
    private String policyCode;

    @Column(name = "name", nullable = false, length = 160)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "proration_policy", nullable = false, length = 20)
    @Builder.Default
    private ProrationPolicy prorationPolicy = ProrationPolicy.DAILY;

    @Column(name = "default_notice_days", nullable = false)
    @Builder.Default
    private Integer defaultNoticeDays = 30;

    @Enumerated(EnumType.STRING)
    @Column(name = "early_termination_formula", nullable = false, length = 30)
    @Builder.Default
    private EarlyTerminationFormula earlyTerminationFormula = EarlyTerminationFormula.PERCENT_OF_REMAINING;

    @Column(name = "early_termination_percent", nullable = false, precision = 9, scale = 4)
    @Builder.Default
    private BigDecimal earlyTerminationPercent = BigDecimal.valueOf(50);

    @Column(name = "early_termination_fixed_fee", precision = 19, scale = 4)
    private BigDecimal earlyTerminationFixedFee;

    @Column(name = "freeze_max_per_year", nullable = false)
    @Builder.Default
    private Integer freezeMaxPerYear = 2;

    @Column(name = "freeze_max_days", nullable = false)
    @Builder.Default
    private Integer freezeMaxDays = 60;

    @Column(name = "freeze_notice_days", nullable = false)
    @Builder.Default
    private Integer freezeNoticeDays = 0;

    /** Part du prix de la periode facturee pendant le gel · 0 rien, 100 tout. */
    @Column(name = "freeze_fee_percent", nullable = false, precision = 9, scale = 4)
    @Builder.Default
    private BigDecimal freezeFeePercent = BigDecimal.ZERO;

    /** Jours apres une echeance impayee avant d'entrer en tolerance. */
    /**
     * Jours avant qu'une echeance impayee entre en tolerance · un seul.
     *
     * <p>Une echeance qui n'est pas reglee le jour dit se signale des le lendemain. Attendre une
     * semaine pour le dire ne servait personne : ni le client, qui l'apprenait trop tard, ni la
     * maison, qui laissait courir des droits sans contrepartie.</p>
     */
    @Column(name = "grace_period_days", nullable = false)
    @Builder.Default
    private Integer gracePeriodDays = 1;

    /**
     * Jours de tolerance avant suspension · trois.
     *
     * <p>Avec une tolerance qui s'ouvre des le lendemain, deux semaines de suspension laissaient
     * quinze jours de droits ouverts sur une facture impayee · le rythme des deux reglages n'allait
     * plus ensemble.</p>
     */
    @Column(name = "suspension_after_grace_days", nullable = false)
    @Builder.Default
    private Integer suspensionAfterGraceDays = 3;

    /**
     * Combien de jours un abonnement survit a une reconduction automatique qui a echoue.
     *
     * <p>Ce delai emprunta un temps {@code gracePeriodDays}, qui sert a decider quand une
     * echeance impayee entre en tolerance. Les deux notions n ont rien a voir · un chiffre qui
     * convient a l une convient mal a l autre. Celui-ci est le sien.</p>
     *
     * <p>La reconduction a echoue de notre fait, l abonne n y est pour rien · il est prevenu des
     * le lendemain de la fin de periode, et ferme au bout de ce delai.</p>
     */
    @Column(name = "renewal_expiry_days", nullable = false)
    @Builder.Default
    private Integer renewalExpiryDays = 7;

    @Column(name = "quote_validity_days", nullable = false)
    @Builder.Default
    private Integer quoteValidityDays = 30;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = Boolean.TRUE;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

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
