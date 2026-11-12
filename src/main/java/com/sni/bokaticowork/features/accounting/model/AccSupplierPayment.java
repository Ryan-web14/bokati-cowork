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

/** Reglement d'une facture fournisseur (lot F13). */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "acc_supplier_payment")
public class AccSupplierPayment {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "invoice_number", nullable = false)
    private String invoiceNumber;

    @Column(name = "supplier_code", nullable = false)
    private String supplierCode;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "treasury_account", nullable = false)
    private String treasuryAccount;

    @Column(name = "journal_code", nullable = false)
    private String journalCode;

    @Column(name = "reference")
    private String reference;

    @Column(name = "entry_number")
    private String entryNumber;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
