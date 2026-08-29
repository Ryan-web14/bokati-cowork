package com.sni.bokaticowork.features.billing.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "service_catalog_item")
public class ServiceCatalogItem {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "item_code", nullable = false, unique = true, length = 100)
    private String itemCode;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "unit", length = 50)
    private String unit;

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal unitPrice;

    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "XAF";

    @Column(name = "tax_rule_code", length = 50)
    private String taxRuleCode;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = Boolean.TRUE;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    // ── Economie de la ligne ────────────────────────────────────────────────────────────

    /** Prix de revient · base du plancher derive et du calcul de marge. */
    @Column(name = "cost_price", precision = 19, scale = 4)
    private BigDecimal costPrice;

    /** Plancher saisi explicitement · prime sur le plancher derive du cout. */
    @Column(name = "floor_price", precision = 19, scale = 4)
    private BigDecimal floorPrice;

    /** Marge minimale en pourcentage, rapportee au prix de vente (taux de marque). */
    @Column(name = "min_margin_rate", precision = 9, scale = 4)
    private BigDecimal minMarginRate;

    @Column(name = "max_discount_rate", precision = 9, scale = 4)
    private BigDecimal maxDiscountRate;

    @Column(name = "discount_policy", nullable = false, length = 20)
    @Builder.Default
    private String discountPolicy = "NONE";

    // ── Contenu de la prestation ────────────────────────────────────────────────────────

    @Column(name = "detailed_description", columnDefinition = "text")
    private String detailedDescription;

    /** Ce que la prestation comprend · liste JSON de libelles, rendue en sous-lignes. */
    @Column(name = "included_items", columnDefinition = "text")
    private String includedItems;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    // ── Aide a la saisie et classement ──────────────────────────────────────────────────

    @Column(name = "billing_mode", length = 20)
    private String billingMode;

    @Column(name = "default_quantity", precision = 19, scale = 4)
    private BigDecimal defaultQuantity;

    @Column(name = "min_quantity", precision = 19, scale = 4)
    private BigDecimal minQuantity;

    @Column(name = "max_quantity", precision = 19, scale = 4)
    private BigDecimal maxQuantity;

    @Column(name = "taxable_by_default")
    private Boolean taxableByDefault;

    @Column(name = "subcategory", length = 100)
    private String subcategory;

    @Column(name = "tags", columnDefinition = "text")
    private String tags;

    @Column(name = "external_reference", length = 120)
    private String externalReference;

    @Column(name = "valid_from")
    private LocalDate validFrom;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Plancher applicable a une ligne de ce service, resolu dans cet ordre :
     *
     * <ol>
     *   <li>{@code floorPrice} s'il est saisi · une valeur explicite prime toujours ;</li>
     *   <li>sinon derive de {@code costPrice} et {@code minMarginRate} ;</li>
     *   <li>sinon {@code costPrice} seul · on ne vend pas a perte ;</li>
     *   <li>sinon aucun plancher.</li>
     * </ol>
     *
     * <p>La derivation emploie le <b>taux de marque</b>, c'est-a-dire la marge rapportee au prix
     * de vente : {@code cout / (1 - taux)}. Avec un cout de 8 000 et une marge minimale de 20 %,
     * le plancher vaut 10 000, et la marge realisee a ce prix est bien de 20 % du prix de vente.
     * L'autre convention, la marge rapportee au cout, donnerait 9 600.
     *
     * @return le plancher, ou {@code null} si l'article n'en definit aucun
     */
    public BigDecimal effectiveFloorPrice() {
        if (floorPrice != null) {
            return floorPrice;
        }
        if (costPrice == null) {
            return null;
        }
        if (minMarginRate == null || minMarginRate.signum() <= 0) {
            return costPrice;
        }
        BigDecimal complement = BigDecimal.valueOf(100).subtract(minMarginRate);
        if (complement.signum() <= 0) {
            // Une marge de 100 % ou plus n'a pas de plancher fini · la contrainte de base
            // l'interdit, on se protege ici du cas ou une ligne ancienne y echapperait.
            return costPrice;
        }
        return costPrice.multiply(BigDecimal.valueOf(100))
                .divide(complement, 4, RoundingMode.HALF_UP);
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
