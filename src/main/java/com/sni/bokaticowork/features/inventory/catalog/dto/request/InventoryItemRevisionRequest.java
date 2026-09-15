package com.sni.bokaticowork.features.inventory.catalog.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class InventoryItemRevisionRequest {

    @NotBlank
    private String revision;

    private String reason;

    /** Document justificatif, gere par le module document. */
    private String documentCode;

    private String changedBy;
}
