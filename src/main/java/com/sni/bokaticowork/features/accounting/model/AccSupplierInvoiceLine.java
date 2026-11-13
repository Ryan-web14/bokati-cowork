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

/** Ligne de facture fournisseur (lot F13). */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "acc_supplier_invoice_line")
public class AccSupplierInvoiceLine {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "invoice_id", nullable = false)
    private Long invoiceId;

    @Column(name = "line_no", nullable = false)
    private Integer lineNo;

    @Column(name = "designation", nullable = false)
    private String designation;

    @Column(name = "nature_code")
    private String natureCode;

    @Column(name = "account_number", nullable = false)
    private String accountNumber;

    @Column(name = "amount_ht", nullable = false)
    private BigDecimal amountHt;

    @Builder.Default
    @Column(name = "vat_rate", nullable = false)
    private BigDecimal vatRate = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "vat_amount", nullable = false)
    private BigDecimal vatAmount = BigDecimal.ZERO;

    @Column(name = "vat_account")
    private String vatAccount;

    @Column(name = "section_code")
    private String sectionCode;

    @Column(name = "order_line_no")
    private Integer orderLineNo;

    /** Quantite de la ligne (lot F18 : une fiche par unite selon la categorie). */
    @Column(name = "quantity")
    private BigDecimal quantity;

    /** Fiche d'immobilisation a laquelle la ligne se rattache (frais accessoires, avoir, en cours ; lot F18). */
    @Column(name = "fixed_asset_code")
    private String fixedAssetCode;

    @Column(name = "service_start")
    private LocalDate serviceStart;

    @Column(name = "service_end")
    private LocalDate serviceEnd;

    @Column(name = "recognition_method")
    private String recognitionMethod;
}
