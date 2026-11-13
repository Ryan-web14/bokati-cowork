package com.sni.bokaticowork.features.billing.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "billing_tax_rule")
public class TaxRule {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 120)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "country_code", length = 3)
    private String countryCode;

    @Column(name = "tax_type", nullable = false, length = 60)
    private String taxType;

    @Column(name = "rate", nullable = false, precision = 9, scale = 4)
    private BigDecimal rate;

    @Column(name = "applies_on", nullable = false, length = 60)
    private String appliesOn;

    @Column(name = "active", nullable = false)
    private Boolean active;

    @Column(name = "valid_from")
    private LocalDate validFrom;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    /**
     * Lot C0 : code stable que les articles referencent. Un changement de taux cree une nouvelle
     * version datee (code propre, meme base_code) ; l'ancienne s'arrete la veille.
     */
    @Column(name = "base_code", nullable = false, length = 120)
    private String baseCode;

    /** Mention imprimee sur la piece. */
    @Column(name = "label_on_document", length = 255)
    private String labelOnDocument;

    /** Motif legal d'une exoneration, imprime sous le tableau des lignes. */
    @Column(name = "exemption_reason", length = 500)
    private String exemptionReason;

    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    @Column(name = "treatment", nullable = false, length = 20)
    private com.sni.bokaticowork.features.billing.enums.TaxTreatment treatment;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @jakarta.persistence.PrePersist
    void defaults() {
        if (baseCode == null) {
            baseCode = code;
        }
        if (treatment == null) {
            treatment = com.sni.bokaticowork.features.billing.enums.TaxTreatment.STANDARD;
        }
        if (displayOrder == null) {
            displayOrder = 0;
        }
        if (active == null) {
            active = Boolean.TRUE;
        }
    }

    /** La regle s'applique-t-elle a cette date. */
    public boolean validOn(LocalDate date) {
        return Boolean.TRUE.equals(active)
                && (validFrom == null || !date.isBefore(validFrom))
                && (validUntil == null || !date.isAfter(validUntil));
    }
}
