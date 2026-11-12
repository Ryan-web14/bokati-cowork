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

/** Ligne de commande fournisseur (lot F13). */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "acc_purchase_order_line")
public class AccPurchaseOrderLine {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "line_no", nullable = false)
    private Integer lineNo;

    @Column(name = "designation", nullable = false)
    private String designation;

    @Builder.Default
    @Column(name = "quantity", nullable = false)
    private BigDecimal quantity = BigDecimal.ONE;

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "nature_code")
    private String natureCode;

    @Column(name = "account_number", nullable = false)
    private String accountNumber;

    @Builder.Default
    @Column(name = "vat_rate", nullable = false)
    private BigDecimal vatRate = BigDecimal.ZERO;

    @Column(name = "amount_ht", nullable = false)
    private BigDecimal amountHt;

    @Builder.Default
    @Column(name = "vat_amount", nullable = false)
    private BigDecimal vatAmount = BigDecimal.ZERO;

    @Column(name = "section_code")
    private String sectionCode;

    @Builder.Default
    @Column(name = "invoiced_ht", nullable = false)
    private BigDecimal invoicedHt = BigDecimal.ZERO;

    @Column(name = "need_line_no")
    private Integer needLineNo;
}
