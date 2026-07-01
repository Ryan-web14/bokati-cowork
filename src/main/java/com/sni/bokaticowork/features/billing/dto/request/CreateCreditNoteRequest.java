package com.sni.bokaticowork.features.billing.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.util.List;

public record CreateCreditNoteRequest(
        BigDecimal amount,
        @NotBlank String reason,
        Boolean applyImmediately,
        /** Si true, valide l'avoir (SEFC) dans la même transaction avant de l'appliquer. */
        Boolean validateImmediately,
        List<CreateBillingDocumentLineRequest> lines
) {
}
