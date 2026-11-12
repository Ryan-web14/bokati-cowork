package com.sni.bokaticowork.features.accounting.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Profil d'amortissement d'une marque (et facultativement d'une categorie ou d'un modele) : mode,
 * taux ou duree, coefficient et valeur residuelle propres. Il l'emporte sur la categorie ; les
 * valeurs saisies sur la fiche l'emportent sur lui.
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "fa_depreciation_profile")
public class FaDepreciationProfile {

    @Id
    @com.sni.bokaticowork.core.annotation.IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "label", nullable = false)
    private String label;

    @Column(name = "category_code")
    private String categoryCode;

    @Column(name = "brand", nullable = false)
    private String brand;

    @Column(name = "model_pattern")
    private String modelPattern;

    @Column(name = "method")
    private String method;

    @Column(name = "annual_rate")
    private BigDecimal annualRate;

    @Column(name = "duration_months")
    private Integer durationMonths;

    @Column(name = "coefficient")
    private BigDecimal coefficient;

    @Column(name = "residual_rate")
    private BigDecimal residualRate;

    @Builder.Default
    @Column(name = "priority", nullable = false)
    private Integer priority = 0;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private Boolean active = Boolean.TRUE;

    @Column(name = "notes")
    private String notes;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_by")
    private String updatedBy;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
