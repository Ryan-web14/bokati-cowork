package com.sni.bokaticowork.features.payment.dto.request;

import com.sni.bokaticowork.features.payment.enums.CashDocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateCashVoucherRequest(
        @NotNull BigDecimal amount,
        @NotBlank String documentNumber,
        String flowCategory,
        String referenceType,
        String referenceCode,
        String counterpartyType,
        String counterpartyCode,
        String counterpartyName,
        String reason,
        /** Ignore en contexte authentifie : l'auteur est l'utilisateur connecte. */
        String createdBy,
        CashDocumentType documentType,
        String metadataJson
) {
}
