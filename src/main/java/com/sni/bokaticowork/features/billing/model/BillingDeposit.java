package com.sni.bokaticowork.features.billing.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Acompte d'un devis ou d'une facture en preparation, et sa facture d'acompte (lot B3). */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "billing_deposit")
public class BillingDeposit {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    /** Devis ou facture qui porte l'acompte. */
    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Column(name = "document_number", nullable = false, length = 60)
    private String documentNumber;

    /** Facture finale qui le deduit (facture issue du devis, ou la facture elle-meme). */
    @Column(name = "final_document_number", length = 60)
    private String finalDocumentNumber;

    /** Numero d'ordre de l'acompte sur la piece (acompte n° 1, 2...). */
    @Column(name = "rank", nullable = false)
    private Integer rank;

    @Column(name = "label")
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode", nullable = false, length = 20)
    private com.sni.bokaticowork.features.billing.enums.BillingDepositMode mode;

    @Column(name = "value", nullable = false, precision = 19, scale = 4)
    private BigDecimal value;

    /** Rangs des lignes servant de base au pourcentage ; vide : le total. */
    @Column(name = "base_line_orders", length = 500)
    private String baseLineOrders;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private com.sni.bokaticowork.features.billing.enums.BillingDepositStatus status;

    @Column(name = "deposit_invoice_number", length = 60)
    private String depositInvoiceNumber;

    @Builder.Default
    @Column(name = "paid_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(name = "deducted_amount", precision = 19, scale = 4)
    private BigDecimal deductedAmount;

    @Column(name = "deducted_on", length = 60)
    private String deductedOn;

    @Column(name = "deducted_at")
    private Instant deductedAt;

    /** Avoir qui solde un acompte encaisse puis rembourse ou garde (decision D4). */
    @Column(name = "settlement_document", length = 60)
    private String settlementDocument;

    @Column(name = "cancel_reason", length = 1000)
    private String cancelReason;

    @Column(name = "created_by", length = 180)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
