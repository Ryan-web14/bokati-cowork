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

/** Declaration mensuelle de TVA (lot F11). */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "acc_vat_return")
public class AccVatReturn {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    /** AAAA-MM. */
    @Column(name = "period_code", nullable = false)
    private String periodCode;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    /** INVOICE (exigible a la facture) ou PAYMENT (exigible a l'encaissement). */
    @Column(name = "regime", nullable = false)
    private String regime;

    @Builder.Default
    @Column(name = "collected", nullable = false)
    private BigDecimal collected = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "deductible", nullable = false)
    private BigDecimal deductible = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "credit_brought_forward", nullable = false)
    private BigDecimal creditBroughtForward = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "due_amount", nullable = false)
    private BigDecimal dueAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "credit_carried_forward", nullable = false)
    private BigDecimal creditCarriedForward = BigDecimal.ZERO;

    /** Chiffre d'affaires hors taxe de la periode (classe 70). */
    @Builder.Default
    @Column(name = "revenue", nullable = false)
    private BigDecimal revenue = BigDecimal.ZERO;

    /** Somme des taxes des factures et avoirs de la periode (controle). */
    @Column(name = "invoice_tax_total")
    private BigDecimal invoiceTaxTotal;

    /** TVA collectee comptable moins taxes des factures ; nul attendu au regime des debits. */
    @Column(name = "control_difference")
    private BigDecimal controlDifference;

    /** TVA encore en attente d'exigibilite a la fin de la periode. */
    @Builder.Default
    @Column(name = "pending_balance", nullable = false)
    private BigDecimal pendingBalance = BigDecimal.ZERO;

    @Column(name = "detail_json", columnDefinition = "TEXT")
    private String detailJson;

    @Builder.Default
    @Column(name = "status", nullable = false)
    private String status = "DRAFT";

    @Column(name = "voucher_number")
    private String voucherNumber;

    @Column(name = "liquidation_entry_number")
    private String liquidationEntryNumber;

    @Column(name = "prepared_by", nullable = false)
    private String preparedBy;

    @Column(name = "prepared_at", nullable = false)
    private Instant preparedAt;

    @Column(name = "validated_by")
    private String validatedBy;

    @Column(name = "validated_at")
    private Instant validatedAt;

    @Column(name = "liquidated_by")
    private String liquidatedBy;

    @Column(name = "liquidated_at")
    private Instant liquidatedAt;
}
