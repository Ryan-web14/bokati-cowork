package com.sni.bokaticowork.features.inventory.stock.dto.request;

import com.sni.bokaticowork.features.inventory.stock.enums.StockOutReasonCode;
import com.sni.bokaticowork.features.inventory.stock.enums.StockReferenceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class StockAdjustmentRequest {

    @NotBlank
    private String itemCode;

    @NotBlank
    private String locationCode;

    @NotNull
    private BigDecimal quantityDelta;

    private Long unitCost;

    private String lotNumber;

    private LocalDate expiryDate;

    private StockOutReasonCode reasonCode;

    /** Motif d ajustement codifie, choisi dans le referentiel des motifs. */
    private String adjustmentReasonCode;

    /** Auteur du visa, exige au-dela du seuil d approbation. */
    private String approvedBy;

    private StockReferenceType referenceType;

    private String referenceCode;

    private String reasonDetails;

    private Boolean allowNegativeOverride;

    @NotBlank
    private String performedBy;
}
