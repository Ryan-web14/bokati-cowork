package com.sni.bokaticowork.features.document.kyc.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class KycDecisionRequest {

    /** Ignore pour un compte connecte : l\'auteur est toujours le compte connecte (lot KG0). */
    private Long reviewedBy;

    private String comment;

    /**
     * Lot KG7 : motif de la liste ({@code GET /kyc/reasons}) ; le commentaire le complete. Pour un
     * refus definitif, motif obligatoire.
     */
    private String reasonCode;
}
