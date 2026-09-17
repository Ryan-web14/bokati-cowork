package com.sni.bokaticowork.features.inventory.catalog.dto.request;

import com.sni.bokaticowork.features.inventory.catalog.enums.ItemLifecycleStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InventoryItemLifecycleRequest {

    @NotNull
    private ItemLifecycleStatus lifecycleStatus;

    private String reason;
}
