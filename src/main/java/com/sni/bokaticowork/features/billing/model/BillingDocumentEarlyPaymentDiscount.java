package com.sni.bokaticowork.features.billing.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "billing_document_early_payment_discount")
public class BillingDocumentEarlyPaymentDiscount {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false, foreignKey = @ForeignKey(name = "fk_early_discount_document"))
    private BillingDocument document;

    /** Taux d'escompte en pourcentage (ex : 2.00 pour 2 %). */
    @Column(name = "discount_rate", nullable = false, precision = 9, scale = 4)
    private BigDecimal discountRate;

    /** Date limite pour bénéficier de l'escompte. */
    @Column(name = "if_paid_before", nullable = false)
    private LocalDate ifPaidBefore;

    /** Montant calculé : totalDocument × discountRate / 100. */
    @Column(name = "computed_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal computedAmount;

    /** Libellé affiché sur le document (ex : "Escompte 2% si règlement avant le 01/06/2026"). */
    @Column(name = "label", length = 200)
    private String label;

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
