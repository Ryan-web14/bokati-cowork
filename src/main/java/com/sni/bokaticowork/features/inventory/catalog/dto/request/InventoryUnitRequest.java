package com.sni.bokaticowork.features.inventory.catalog.dto.request;

import com.sni.bokaticowork.features.inventory.catalog.enums.InventoryUnitType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InventoryUnitRequest {

    private String code;

    @NotBlank
    private String name;

    @NotNull
    private InventoryUnitType unitType;

    private Boolean active;
}
