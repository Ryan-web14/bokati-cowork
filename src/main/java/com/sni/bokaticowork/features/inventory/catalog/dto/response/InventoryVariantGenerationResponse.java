package com.sni.bokaticowork.features.inventory.catalog.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class InventoryVariantGenerationResponse {

    private String templateCode;

    private boolean dryRun;

    /** Nombre de combinaisons demandees. */
    private int requestedCombinations;

    /** Variantes reellement creees par cet appel. */
    private int createdCount;

    /** Combinaisons deja existantes, laissees intactes. */
    private int skippedCount;

    private List<Variant> variants;

    @Data
    @Builder
    public static class Variant {
        private String itemCode;
        private String name;
        private String variantSignature;
        /** Vrai lorsque la variante existait deja avant cet appel. */
        private boolean alreadyExisted;
    }
}
