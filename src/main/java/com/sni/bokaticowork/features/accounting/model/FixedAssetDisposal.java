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

/** Sortie d'une immobilisation et son PV (lot F18). */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "fixed_asset_disposal")
public class FixedAssetDisposal {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "asset_id", nullable = false)
    private Long assetId;

    @Column(name = "voucher_number")
    private String voucherNumber;

    @Column(name = "reason", nullable = false)
    private String reason;

    @Column(name = "disposal_date", nullable = false)
    private LocalDate disposalDate;

    @Builder.Default
    @Column(name = "gross_value", nullable = false)
    private BigDecimal grossValue = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "accumulated_depreciation", nullable = false)
    private BigDecimal accumulatedDepreciation = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "accumulated_impairment", nullable = false)
    private BigDecimal accumulatedImpairment = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "net_value", nullable = false)
    private BigDecimal netValue = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "price", nullable = false)
    private BigDecimal price = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "vat_rate", nullable = false)
    private BigDecimal vatRate = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "vat_amount", nullable = false)
    private BigDecimal vatAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "gain_loss", nullable = false)
    private BigDecimal gainLoss = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "vat_regularization", nullable = false)
    private BigDecimal vatRegularization = BigDecimal.ZERO;

    @Column(name = "buyer_name")
    private String buyerName;

    @Column(name = "justification_ref")
    private String justificationRef;

    @Builder.Default
    @Column(name = "remove_amenity", nullable = false)
    private Boolean removeAmenity = Boolean.FALSE;

    @Builder.Default
    @Column(name = "status", nullable = false)
    private String status = "PREPARED";

    @Column(name = "complement_entry")
    private String complementEntry;

    @Column(name = "exit_entry")
    private String exitEntry;

    @Column(name = "sale_entry")
    private String saleEntry;

    @Builder.Default
    @Column(name = "collected_amount", nullable = false)
    private BigDecimal collectedAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "indemnity_amount", nullable = false)
    private BigDecimal indemnityAmount = BigDecimal.ZERO;

    @Column(name = "indemnity_entry")
    private String indemnityEntry;

    @Column(name = "billing_document_id")
    private Long billingDocumentId;

    @Column(name = "prepared_by", nullable = false)
    private String preparedBy;

    @Column(name = "validated_by")
    private String validatedBy;

    @Column(name = "validated_at")
    private Instant validatedAt;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
