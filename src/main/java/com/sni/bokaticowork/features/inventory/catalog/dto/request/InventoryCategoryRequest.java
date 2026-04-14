package com.sni.bokaticowork.features.inventory.catalog.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class InventoryCategoryRequest {

    private String code;

    @NotBlank
    private String name;

    private String description;

    private String parentCategoryCode;

    private Boolean active;
}
