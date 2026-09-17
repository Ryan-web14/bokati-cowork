package com.sni.bokaticowork.features.inventory.stock.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LotReleaseRequest {

    /** Qui demande la levee. */
    @NotBlank
    private String requestedBy;

    /** Qui l approuve. Doit differer du demandeur. */
    @NotBlank
    private String approvedBy;

    private String notes;
}
