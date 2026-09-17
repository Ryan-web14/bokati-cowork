package com.sni.bokaticowork.features.inventory.stock.dto.response;

import com.sni.bokaticowork.features.inventory.stock.enums.NonConformanceDisposition;
import com.sni.bokaticowork.features.inventory.stock.enums.NonConformanceSeverity;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
public class NonConformanceResponse {

    private String nonConformanceCode;

    private String itemCode;

    private Long lotId;

    private String lotNumber;

    private String sourceType;

    private String sourceCode;

    private BigDecimal quantity;

    private NonConformanceSeverity severity;

    private String description;

    private NonConformanceDisposition disposition;

    private String correctiveAction;

    private String responsibleCode;

    private LocalDate dueDate;

    private String supplierClaimCode;

    private Long costImpact;

    private String detectedBy;

    private Instant detectedAt;

    private String closedBy;

    private Instant closedAt;

    private boolean open;
}
