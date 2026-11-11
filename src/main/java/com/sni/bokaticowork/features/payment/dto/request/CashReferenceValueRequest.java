package com.sni.bokaticowork.features.payment.dto.request;

import jakarta.validation.constraints.NotBlank;

/** Une valeur dans la liste complete envoyee par {@code PUT /cash-registers/reference-lists/{list}}. */
public record CashReferenceValueRequest(
        @NotBlank String code,
        @NotBlank String label,
        Integer sortOrder,
        Boolean active,
        Boolean requiresText
) {
}
