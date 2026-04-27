package com.sni.bokaticowork.features.payment.dto.request;

import com.sni.bokaticowork.features.payment.enums.CashDocumentType;
import com.sni.bokaticowork.features.payment.enums.CashMovementType;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateCashMovementRequest(
        @NotNull CashMovementType movementType,
        @NotNull BigDecimal amount,
        CashDocumentType documentType,
        String documentNumber,
        String flowCategory,
        String referenceType,
        String referenceCode,
        String counterpartyType,
        String counterpartyCode,
        String counterpartyName,
        String reason,
        String createdBy,
        String metadataJson
) {
}
