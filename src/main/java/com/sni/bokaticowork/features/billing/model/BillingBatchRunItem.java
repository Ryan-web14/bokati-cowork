package com.sni.bokaticowork.features.billing.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** Lot C13 : un client d'un lancement de facturation groupee (ses consommations, sa facture). */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "billing_batch_run_item")
public class BillingBatchRunItem {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "run_id", nullable = false)
    private Long runId;

    @Column(name = "customer_type", nullable = false, length = 60)
    private String customerType;

    @Column(name = "customer_code", nullable = false, length = 120)
    private String customerCode;

    @Column(name = "customer_name", length = 255)
    private String customerName;

    @Column(name = "billing_company", length = 120)
    private String billingCompany;

    /** Numeros des elements facturables, separes par des virgules. */
    @Column(name = "billable_numbers", nullable = false, columnDefinition = "text")
    private String billableNumbers;

    @Column(name = "item_count", nullable = false)
    private Integer itemCount;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalAmount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "excluded", nullable = false)
    @Builder.Default
    private Boolean excluded = Boolean.FALSE;

    @Column(name = "invoice_number", length = 80)
    private String invoiceNumber;

    @Column(name = "error", length = 1000)
    private String error;
}
