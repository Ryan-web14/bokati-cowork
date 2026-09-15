package com.sni.bokaticowork.features.inventory.catalog.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InventoryItemTranslationResponse {

    private Long id;

    private String itemCode;

    private String languageCode;

    private String name;

    private String description;
}
