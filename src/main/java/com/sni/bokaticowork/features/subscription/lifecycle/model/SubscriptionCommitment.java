package com.sni.bokaticowork.features.subscription.lifecycle.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * L'engagement · ce que l'abonne a promis, et ce que coute d'y renoncer.
 *
 * <p>Pose a la souscription quand le prix de plan porte un engagement, par un devis, ou a la main.
 * Il ne dit pas si l'on peut resilier · on peut toujours · il dit ce que cela coute avant son terme.</p>
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "subscription_commitment")
public class SubscriptionCommitment {

    public enum Source {
        PLAN, QUOTE, MANUAL
    }

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false, unique = true)
    private Subscription subscription;

    @Column(name = "commitment_months", nullable = false)
    private Integer commitmentMonths;

    @Column(name = "commitment_start", nullable = false)
    private LocalDate commitmentStart;

    @Column(name = "commitment_end", nullable = false)
    private LocalDate commitmentEnd;

    @Enumerated(EnumType.STRING)
    @Column(name = "early_termination_formula", nullable = false, length = 30)
    private EarlyTerminationFormula earlyTerminationFormula;

    @Column(name = "early_termination_percent", precision = 9, scale = 4)
    private BigDecimal earlyTerminationPercent;

    @Column(name = "early_termination_fixed_fee", precision = 19, scale = 4)
    private BigDecimal earlyTerminationFixedFee;

    /** Au terme, l'engagement se reconduit pour la meme duree · sinon l'abonnement devient sans engagement. */
    @Column(name = "auto_renew_commitment", nullable = false)
    @Builder.Default
    private Boolean autoRenewCommitment = Boolean.FALSE;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 20)
    @Builder.Default
    private Source source = Source.PLAN;

    @Column(name = "created_by", length = 120)
    private String createdBy;

    public boolean runningOn(LocalDate day) {
        return !day.isBefore(commitmentStart) && !day.isAfter(commitmentEnd);
    }

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
