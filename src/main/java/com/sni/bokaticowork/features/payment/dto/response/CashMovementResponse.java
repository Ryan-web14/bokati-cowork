package com.sni.bokaticowork.features.payment.dto.response;

import com.sni.bokaticowork.features.payment.enums.CashDocumentType;
import com.sni.bokaticowork.features.payment.enums.CashFlowDirection;
import com.sni.bokaticowork.features.payment.enums.CashMovementType;
import com.sni.bokaticowork.features.payment.enums.CashSessionStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record CashMovementResponse(
        String movementNumber,
        String sessionNumber,
        String registerCode,
        String registerName,
        String businessEntityCode,
        String deviceCode,
        CashSessionStatus sessionStatus,
        String sessionOpenedBy,
        Instant sessionOpenedAt,
        CashMovementType movementType,
        CashFlowDirection flowDirection,
        BigDecimal amount,
        String currency,
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
        String relatedTransactionNumber,
        String relatedIntentNumber,
        String relatedReceiptNumber,
        String relatedCustomerType,
        String relatedCustomerCode,
        String relatedCustomerName,
        String relatedSourceType,
        String relatedSourceCode,
        String relatedSourceLabel,
        String metadataJson,
        Instant createdAt
) {
}
