package com.sni.bokaticowork.features.inventory.stock.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdjustmentReasonRequest {

    @NotBlank
    private String reasonCode;

    @NotBlank
    private String label;

    /** Compte de contrepartie utilise pour l ecriture de stock. */
    @NotBlank
    private String counterpartAccount;

    private Boolean negativeOnly;

    private Boolean positiveOnly;

    private Boolean active;
}
