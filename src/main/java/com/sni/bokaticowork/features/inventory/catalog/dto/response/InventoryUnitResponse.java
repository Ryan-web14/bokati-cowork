package com.sni.bokaticowork.features.inventory.catalog.dto.response;

import com.sni.bokaticowork.features.inventory.catalog.enums.InventoryUnitType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InventoryUnitResponse {

    private String code;

    private String name;

    private InventoryUnitType unitType;

    private Boolean active;
}
