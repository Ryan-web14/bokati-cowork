package com.sni.bokaticowork.features.accounting.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Facture ou avoir fournisseur (lot F13). */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "acc_supplier_invoice")
public class AccSupplierInvoice {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "number", nullable = false)
    private String number;

    @Builder.Default
    @Column(name = "kind", nullable = false)
    private String kind = "INVOICE";

    @Column(name = "supplier_code", nullable = false)
    private String supplierCode;

    @Column(name = "supplier_reference", nullable = false)
    private String supplierReference;

    @Column(name = "invoice_date", nullable = false)
    private LocalDate invoiceDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "order_number")
    private String orderNumber;

    @Column(name = "original_number")
    private String originalNumber;

    @Column(name = "subject")
    private String subject;

    @Builder.Default
    @Column(name = "total_ht", nullable = false)
    private BigDecimal totalHt = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "total_vat", nullable = false)
    private BigDecimal totalVat = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "total_ttc", nullable = false)
    private BigDecimal totalTtc = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "withholding_amount", nullable = false)
    private BigDecimal withholdingAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "net_to_pay", nullable = false)
    private BigDecimal netToPay = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "paid_amount", nullable = false)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "close_order", nullable = false)
    private Boolean closeOrder = Boolean.FALSE;

    @Column(name = "document_ref")
    private String documentRef;

    @Builder.Default
    @Column(name = "status", nullable = false)
    private String status = "DRAFT";

    @Column(name = "entry_number")
    private String entryNumber;

    /** Compte du fournisseur credite (481 pour une facture d'immobilisation, lot F18) ; vide : celui du fournisseur. */
    @Column(name = "payable_account")
    private String payableAccount;

    @Column(name = "posted_by")
    private String postedBy;

    @Column(name = "posted_at")
    private Instant postedAt;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
