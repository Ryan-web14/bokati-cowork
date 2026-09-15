package com.sni.bokaticowork.features.inventory.catalog.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class InventoryItemTranslationRequest {

    /** Code langue sur deux lettres, par exemple fr ou en. */
    @NotBlank
    private String languageCode;

    @NotBlank
    private String name;

    private String description;
}
