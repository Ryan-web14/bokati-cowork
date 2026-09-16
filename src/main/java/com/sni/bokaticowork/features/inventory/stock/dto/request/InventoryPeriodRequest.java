package com.sni.bokaticowork.features.inventory.stock.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class InventoryPeriodRequest {

    /** Code lisible, par exemple 2026-09. Genere si absent. */
    private String periodCode;

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;
}
