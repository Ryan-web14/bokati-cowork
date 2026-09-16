package com.sni.bokaticowork.features.inventory.stock.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class InventoryPeriodReopenRequest {

    /** Motif obligatoire : rouvrir une periode close doit laisser une trace. */
    @NotBlank
    private String reason;

    private String reopenedBy;
}
