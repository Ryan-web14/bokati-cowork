package com.sni.bokaticowork.features.document.kyc.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class KycBulkApproveRequest {
    /** Codes des documents (DOC-...). Ancien nom, garde ; {@code documentCodes} est prefere. */
    private List<String> documentIds;

    /** Codes des documents (DOC-...) a approuver (lot KG2). */
    private List<String> documentCodes;

    /** Les codes envoyes, sous l'un ou l'autre nom, sans doublon. */
    public List<String> codes() {
        java.util.LinkedHashSet<String> all = new java.util.LinkedHashSet<>();
        if (documentCodes != null) all.addAll(documentCodes);
        if (documentIds != null) all.addAll(documentIds);
        return List.copyOf(all);
    }

    /** Ignore pour un compte connecte : l\'auteur est toujours le compte connecte (lot KG0). */
    private Long reviewedBy;
}
