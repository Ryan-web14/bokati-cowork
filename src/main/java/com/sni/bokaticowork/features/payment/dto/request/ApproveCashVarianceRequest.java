package com.sni.bokaticowork.features.payment.dto.request;


public record ApproveCashVarianceRequest(
        /** Ignore en contexte authentifie : l'auteur est l'utilisateur connecte. */
        String reviewedBy,
        String note
) {
}
