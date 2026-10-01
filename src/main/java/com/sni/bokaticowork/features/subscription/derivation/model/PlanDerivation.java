package com.sni.bokaticowork.features.subscription.derivation.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.sni.bokaticowork.features.subscription.subscription.model.PlanVersion;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Une derivation · un prix et des avantages propres a un abonne, sans plan de catalogue dedie.
 *
 * <p>Elle garde le lien vers sa source, pour qu'on puisse afficher l'ecart quand le catalogue
 * evolue. Elle est versionnee, jamais modifiee : renegocier en laisse deux, chainees par
 * {@code supersedes}. Elle est datee et peut expirer. Son comportement au renouvellement est
 * explicite, pour que le geste ponctuel ne devienne pas un tarif a vie parce que personne n'a pense
 * a l'echeance.</p>
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "plan_derivation")
public class PlanDerivation {

    public enum Status {
        DRAFT, PENDING_APPROVAL, ACTIVE, SUPERSEDED, EXPIRED, REJECTED;

        public boolean open() {
            return this == DRAFT || this == PENDING_APPROVAL || this == ACTIVE;
        }
    }

    public enum Reason {
        NEGOTIATION, GOODWILL, PARTNERSHIP, PILOT, GRANDFATHERING, LOYALTY, CORRECTION
    }

    public enum RenewalBehaviour {
        /** Le tarif derive se renouvelle tel quel. */
        KEEP,
        /** Au prochain renouvellement, retour au catalogue. */
        REVERT_TO_CATALOGUE,
        /** Retour au catalogue apres N renouvellements. */
        REVERT_AFTER_PERIODS
    }

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "derivation_code", nullable = false, unique = true, length = 100)
    private String derivationCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_plan_version_id", nullable = false)
    private PlanVersion sourcePlanVersion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "derived_plan_version_id", nullable = false)
    private PlanVersion derivedPlanVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private Status status = Status.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 40)
    private Reason reason;

    @Column(name = "reason_details", length = 1000)
    private String reasonDetails;

    @Column(name = "effective_from")
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Enumerated(EnumType.STRING)
    @Column(name = "renewal_behaviour", nullable = false, length = 30)
    @Builder.Default
    private RenewalBehaviour renewalBehaviour = RenewalBehaviour.KEEP;

    @Column(name = "revert_after_periods")
    private Integer revertAfterPeriods;

    @Column(name = "periods_applied", nullable = false)
    @Builder.Default
    private Integer periodsApplied = 0;

    /** Faux par defaut sur un prix negocie · une promotion par-dessus une negociation cumule deux concessions. */
    @Column(name = "promotions_allowed", nullable = false)
    @Builder.Default
    private Boolean promotionsAllowed = Boolean.FALSE;

    /** Ecart chiffre sur une periode · positif si concession, negatif si majoration. */
    @Column(name = "total_impact_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal totalImpactAmount = BigDecimal.ZERO;

    @Column(name = "discount_percent", precision = 9, scale = 4)
    private BigDecimal discountPercent;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "requested_by", nullable = false, length = 120)
    private String requestedBy;

    @Column(name = "approved_by", length = 120)
    private String approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "applied_at")
    private Instant appliedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "supersedes_derivation_id")
    private Long supersedesDerivationId;

    /** Creation en lot · le meme lot pour toutes celles d'une protection tarifaire ou d'un partenariat. */
    @Column(name = "batch_code", length = 100)
    private String batchCode;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public boolean expiredOn(LocalDate day) {
        return effectiveTo != null && day.isAfter(effectiveTo);
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
