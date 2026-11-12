package com.sni.bokaticowork.features.payment.dto.request;

import com.sni.bokaticowork.features.payment.enums.CashDocumentType;
import com.sni.bokaticowork.features.payment.enums.CashMovementChannel;
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
        /** Ignore en contexte authentifie : l'auteur est l'utilisateur connecte. */
        String createdBy,
        String metadataJson,
        String relatedMovementNumber,
        String batchId,
        BigDecimal exchangeRate,
        CashMovementChannel channel,
        String deviceCode,
        String deviceIp,
        String subCategory,
        String tags,
        /** Sens d'un ajustement : obligatoire pour {@code ADJUSTMENT}, ignore pour les autres types. */
        com.sni.bokaticowork.features.payment.enums.CashFlowDirection direction,
        /** Nature d'operation (liste {@code GET /cash-registers/natures}) ; remplace {@code flowCategory}. */
        String natureCode
) {
}
