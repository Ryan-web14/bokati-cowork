package com.sni.bokaticowork.features.inventory.stock.dto.request;

import com.sni.bokaticowork.features.inventory.stock.enums.InventoryLocationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InventoryLocationRequest {

//    private String locationCode;

    @NotBlank
    private String name;

    private String description;

    @NotNull
    private InventoryLocationType locationType;

    private String parentLocationCode;

    private String businessCode;

    private Boolean active;
}
