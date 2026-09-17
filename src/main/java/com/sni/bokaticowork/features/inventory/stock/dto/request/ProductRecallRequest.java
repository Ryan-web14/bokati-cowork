package com.sni.bokaticowork.features.inventory.stock.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProductRecallRequest {

    @NotBlank
    private String itemCode;

    /** Premier numero de lot concerne. Null pour rappeler tous les lots de l article. */
    private String lotNumberFrom;

    private String lotNumberTo;

    @NotBlank
    private String reason;
}
