package com.sni.bokaticowork.features.inventory.control.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class InventoryCountCreateRequest {
    @NotBlank
    private String locationCode;
    private String createdBy;
    private String notes;
}
