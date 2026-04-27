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
        @NotBlank String createdBy,
        CashDocumentType documentType,
        String metadataJson
) {
}
