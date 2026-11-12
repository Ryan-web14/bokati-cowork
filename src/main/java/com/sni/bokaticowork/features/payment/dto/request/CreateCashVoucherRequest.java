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
        String metadataJson,
        /** Nature d'operation (liste {@code GET /cash-registers/natures}) ; remplace {@code flowCategory}. */
        String natureCode
) {
    /** Forme historique, sans nature. */
    public CreateCashVoucherRequest(java.math.BigDecimal amount, String documentNumber, String flowCategory, String referenceType,
                                    String referenceCode, String counterpartyType, String counterpartyCode, String counterpartyName,
                                    String reason, String createdBy, com.sni.bokaticowork.features.payment.enums.CashDocumentType documentType,
                                    String metadataJson) {
        this(amount, documentNumber, flowCategory, referenceType, referenceCode, counterpartyType, counterpartyCode, counterpartyName,
                reason, createdBy, documentType, metadataJson, null);
    }
}
