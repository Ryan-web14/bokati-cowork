package com.sni.bokaticowork.features.inventory.stock.dto.request;

import com.sni.bokaticowork.features.inventory.stock.enums.ValuationMethod;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ItemValuationMethodRequest {

    @NotNull
    private ValuationMethod valuationMethod;

    /** Motif du changement de methode, conserve pour la permanence des methodes. */
    private String reason;

    private String changedBy;
}
