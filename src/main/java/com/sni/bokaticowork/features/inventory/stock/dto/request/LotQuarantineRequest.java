package com.sni.bokaticowork.features.inventory.stock.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LotQuarantineRequest {

    /** Motif obligatoire : un lot immobilise sans raison lisible ne se leve jamais. */
    @NotBlank
    private String reason;

    private String quarantinedBy;
}
