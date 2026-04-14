package com.sni.bokaticowork.features.inventory.catalog.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InventoryCategoryResponse {

    private String code;

    private String name;

    private String description;

    private String parentCategoryCode;

    private String parentCategoryName;

    private Boolean active;
}
