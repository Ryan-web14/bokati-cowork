package com.sni.bokaticowork.features.inventory.stock.dto.request;

import com.sni.bokaticowork.features.inventory.stock.enums.NonConformanceDisposition;
import com.sni.bokaticowork.features.inventory.stock.enums.NonConformanceSeverity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class NonConformanceRequest {

    @NotBlank
    private String itemCode;

    private Long lotId;

    private BigDecimal quantity;

    @NotNull
    private NonConformanceSeverity severity;

    @NotBlank
    private String description;

    private NonConformanceDisposition disposition;

    private String correctiveAction;

    private String responsibleCode;

    private LocalDate dueDate;

    private String supplierClaimCode;

    private Long costImpact;

    private String detectedBy;
}
