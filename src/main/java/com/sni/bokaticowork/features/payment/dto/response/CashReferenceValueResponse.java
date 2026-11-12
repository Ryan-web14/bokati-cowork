package com.sni.bokaticowork.features.payment.dto.response;

/** Une valeur d'une liste preenregistree. */
public record CashReferenceValueResponse(
        String listCode,
        String code,
        String label,
        Integer sortOrder,
        Boolean active,
        Boolean requiresText,
        Boolean systemValue,
        Long usageCount
) {
}
