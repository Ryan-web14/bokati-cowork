package com.sni.bokaticowork.features.subscription.promotion.pricing.referral.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.subscription.promotion.model.Promotion;
import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.enums.ReferralProgramStatus;
import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.enums.ReferralQualificationRule;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Un programme de parrainage.
 *
 * <p>Les recompenses ne sont pas redefinies ici : le programme designe deux promotions porteuses,
 * celle du parrain et celle du filleul. Un parrainage qui offre dix pour cent, un mois, ou un credit
 * de portefeuille n'a aucune raison de s'exprimer autrement qu'une campagne offrant la meme chose,
 * et inventer un second vocabulaire aurait impose de le maintenir en parallele du premier.</p>
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "referral_program", indexes = {
        @Index(name = "idx_referral_program_status", columnList = "status,valid_from,valid_until")
})
public class ReferralProgram {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 100)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    @Builder.Default
    private ReferralProgramStatus status = ReferralProgramStatus.DRAFT;

    @Column(name = "valid_from")
    private Instant validFrom;

    @Column(name = "valid_until")
    private Instant validUntil;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referrer_promotion_id")
    private Promotion referrerPromotion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referee_promotion_id")
    private Promotion refereePromotion;

    @Enumerated(EnumType.STRING)
    @Column(name = "qualification_rule", nullable = false, length = 60)
    @Builder.Default
    private ReferralQualificationRule qualificationRule = ReferralQualificationRule.FIRST_PAYMENT;

    /** Anciennete exigee, en jours, lorsque la regle est {@code TENURE_REACHED}. */
    @Column(name = "qualification_delay_days")
    private Integer qualificationDelayDays;

    /**
     * Plafond par parrain. Sans lui, un programme genereux devient une source de revenus a lui seul,
     * et c'est le premier usage qu'on en fait.
     */
    @Column(name = "max_referrals_per_referrer")
    private Integer maxReferralsPerReferrer;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "created_by", length = 120)
    private String createdBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public boolean openAt(Instant moment) {
        return status == ReferralProgramStatus.ACTIVE
                && (validFrom == null || !moment.isBefore(validFrom))
                && (validUntil == null || !moment.isAfter(validUntil));
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
