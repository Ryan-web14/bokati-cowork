package com.sni.bokaticowork.features.inventory.admin.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record InventoryLabelBatchRequest(
        @Valid @NotEmpty List<Item> items
) {
    public record Item(String type, String code) {
    }
}
