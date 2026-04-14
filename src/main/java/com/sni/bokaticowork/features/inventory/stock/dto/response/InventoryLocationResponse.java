package com.sni.bokaticowork.features.inventory.stock.dto.response;

import com.sni.bokaticowork.features.inventory.stock.enums.InventoryLocationType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InventoryLocationResponse {

    private String locationCode;

    private String name;

    private String description;

    private InventoryLocationType locationType;

    private String parentLocationCode;

    private String businessCode;

    private Boolean active;
}
