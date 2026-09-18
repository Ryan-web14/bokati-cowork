package com.sni.bokaticowork.features.subscription.promotion.pricing.referral.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.enums.ReferralStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Un parrainage, du parrain vers un filleul.
 *
 * <p>Il se <b>qualifie</b>, il ne se declare pas. Entre l'enregistrement et la qualification, rien
 * n'est du : c'est ce qui evite de payer pour des comptes crees et jamais utilises, lesquels sont
 * precisement ce qu'attire un programme qui paie trop tot.</p>
 *
 * <p>Les deux recompenses sont suivies separement. Celle du filleul peut etre accordee alors que
 * celle du parrain est refusee parce qu'il a atteint son plafond ; les melanger dans un seul
 * drapeau rendrait cette situation, pourtant courante, indescriptible.</p>
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "referral", indexes = {
        @Index(name = "idx_referral_referrer", columnList = "referrer_type,referrer_code,status"),
        @Index(name = "idx_referral_status", columnList = "status,registered_at")
})
public class Referral {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "referral_number", nullable = false, unique = true, length = 100)
    private String referralNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "program_id", nullable = false)
    private ReferralProgram program;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "link_id")
    private ReferralLink link;

    @Column(name = "referrer_type", nullable = false, length = 60)
    private String referrerType;

    @Column(name = "referrer_code", nullable = false, length = 120)
    private String referrerCode;

    @Column(name = "referee_type", nullable = false, length = 60)
    private String refereeType;

    @Column(name = "referee_code", nullable = false, length = 120)
    private String refereeCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    @Builder.Default
    private ReferralStatus status = ReferralStatus.PENDING;

    @Column(name = "registered_at", nullable = false)
    private Instant registeredAt;

    @Column(name = "qualified_at")
    private Instant qualifiedAt;

    @Column(name = "rejected_at")
    private Instant rejectedAt;

    @Column(name = "rejection_reason", length = 255)
    private String rejectionReason;

    @Column(name = "referrer_reward_granted", nullable = false)
    @Builder.Default
    private Boolean referrerRewardGranted = Boolean.FALSE;

    @Column(name = "referrer_reward_amount", precision = 19, scale = 4)
    private BigDecimal referrerRewardAmount;

    @Column(name = "referrer_rewarded_at")
    private Instant referrerRewardedAt;

    @Column(name = "referee_reward_granted", nullable = false)
    @Builder.Default
    private Boolean refereeRewardGranted = Boolean.FALSE;

    @Column(name = "referee_reward_amount", precision = 19, scale = 4)
    private BigDecimal refereeRewardAmount;

    @Column(name = "referee_rewarded_at")
    private Instant refereeRewardedAt;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        if (registeredAt == null) {
            registeredAt = Instant.now();
        }
        updatedAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}
