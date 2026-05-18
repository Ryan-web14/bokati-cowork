package com.sni.bokaticowork.features.billing.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.billing.enums.BillingAdvanceStatus;
import com.sni.bokaticowork.features.billing.enums.BillingAdvanceType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "billing_document_advance")
public class BillingDocumentAdvance {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false, foreignKey = @ForeignKey(name = "fk_advance_document"))
    private BillingDocument document;

    @Enumerated(EnumType.STRING)
    @Column(name = "advance_type", nullable = false, length = 20)
    private BillingAdvanceType advanceType;

    /** Valeur saisie par l'utilisateur : pourcentage ou montant brut. */
    @Column(name = "advance_value", nullable = false, precision = 19, scale = 4)
    private BigDecimal advanceValue;

    /** Montant calculé et stocké pour éviter tout recalcul. */
    @Column(name = "computed_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal computedAmount;

    /** Numéros de ligne inclus dans la base de calcul (null = toutes les lignes). */
    @Column(name = "included_line_orders", columnDefinition = "text")
    private String includedLineOrders;

    /** Numéros de ligne explicitement exclus de la base de calcul. */
    @Column(name = "excluded_line_orders", columnDefinition = "text")
    private String excludedLineOrders;

    @Column(name = "payment_reference", length = 100)
    private String paymentReference;

    @Column(name = "reference_label", length = 200)
    private String referenceLabel;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private BillingAdvanceStatus status = BillingAdvanceStatus.PENDING;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

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