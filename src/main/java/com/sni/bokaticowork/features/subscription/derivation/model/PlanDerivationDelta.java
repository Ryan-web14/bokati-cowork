package com.sni.bokaticowork.features.subscription.derivation.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Ce qui a ete concede, ligne par ligne, et combien.
 *
 * <p>Sans le delta, on sait qu'un client a un traitement particulier ; personne ne peut dire ce
 * qui a ete concede ni ce que cela coute. Avec lui, on produit la liste des concessions par
 * commercial, par periode, par montant.</p>
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "plan_derivation_delta")
public class PlanDerivationDelta {

    public enum Type {
        PRICE, ENTITLEMENT, BILLING_CYCLE, COMMITMENT, NOTICE_PERIOD, TRIAL, SETUP_FEE, PAYMENT_TERMS, ADDON_INCLUDED
    }

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "derivation_id", nullable = false)
    private PlanDerivation derivation;

    @Enumerated(EnumType.STRING)
    @Column(name = "delta_type", nullable = false, length = 30)
    private Type deltaType;

    /** Cycle de facturation pour un prix, code de droit pour un droit, titre pour un avantage. */
    @Column(name = "target_code", length = 120)
    private String targetCode;

    @Column(name = "catalogue_value", length = 255)
    private String catalogueValue;

    @Column(name = "derived_value", length = 255)
    private String derivedValue;

    /** Ecart chiffre sur une periode · positif si concession. */
    @Column(name = "impact_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal impactAmount = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
    }
}
