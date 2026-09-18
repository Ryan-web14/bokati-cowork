package com.sni.bokaticowork.features.subscription.promotion.pricing.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.subscription.promotion.pricing.enums.DiscountDocumentType;
import com.sni.bokaticowork.features.subscription.promotion.pricing.enums.DiscountSourceType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Trace d'une remise reellement accordee.
 *
 * <p>Jusqu'ici une remise ne laissait rien d'exploitable. {@code billing_document_discount} portait
 * un {@code discount_code} en texte libre, sans clef vers la campagne qui l'avait produite. On ne
 * pouvait donc repondre ni a « combien nous ont coute les promotions ce trimestre », ni a « quelle
 * campagne a produit cette remise », ni a « quel est le taux d'utilisation de ce lot de coupons ».</p>
 *
 * <p>Cette table ne remplace pas la remise de facture, qui reste la ou elle est : elle la double
 * d'une vue transverse, parce qu'une remise peut aussi porter sur un abonnement ou un pass, qui ne
 * sont pas des documents de facturation.</p>
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "applied_discount", indexes = {
        @Index(name = "idx_applied_discount_source", columnList = "source_type,source_code"),
        @Index(name = "idx_applied_discount_document", columnList = "document_type,document_code"),
        @Index(name = "idx_applied_discount_subscriber", columnList = "subscriber_type,subscriber_code,applied_at")
})
public class AppliedDiscount {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "discount_number", nullable = false, unique = true, length = 100)
    private String discountNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 40)
    private DiscountSourceType sourceType;

    @Column(name = "source_code", nullable = false, length = 120)
    private String sourceCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 40)
    private DiscountDocumentType documentType;

    @Column(name = "document_code", nullable = false, length = 120)
    private String documentCode;

    /** Ligne visee a l'interieur du document, nulle pour une remise portant sur l'ensemble. */
    @Column(name = "line_reference", length = 120)
    private String lineReference;

    @Column(name = "subscriber_type", length = 60)
    private String subscriberType;

    @Column(name = "subscriber_code", length = 120)
    private String subscriberCode;

    @Column(name = "original_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal originalAmount;

    @Column(name = "discount_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal discountAmount;

    @Column(name = "final_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal finalAmount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    /** Justification lisible, reprise telle quelle dans les etats et sur la facture. */
    @Column(name = "reason", length = 255)
    private String reason;

    @Column(name = "applied_at", nullable = false)
    private Instant appliedAt;

    @Column(name = "applied_by", length = 120)
    private String appliedBy;

    @Column(name = "reversed_at")
    private Instant reversedAt;

    @Column(name = "reversal_reason", length = 255)
    private String reversalReason;

    @Column(name = "reversed_by", length = 120)
    private String reversedBy;

    @PrePersist
    public void prePersist() {
        if (appliedAt == null) {
            appliedAt = Instant.now();
        }
    }
}
