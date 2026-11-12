package com.sni.bokaticowork.features.payment.dto.response;

import com.sni.bokaticowork.features.payment.enums.CashDocumentType;
import com.sni.bokaticowork.features.payment.enums.CashFlowDirection;
import com.sni.bokaticowork.features.payment.enums.CashMovementChannel;
import com.sni.bokaticowork.features.payment.enums.CashMovementStatus;
import com.sni.bokaticowork.features.payment.enums.CashMovementType;
import com.sni.bokaticowork.features.payment.enums.CashSessionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

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
        CashMovementStatus status,
        String relatedMovementNumber,
        String batchId,
        BigDecimal runningBalance,
        BigDecimal exchangeRate,
        CashMovementChannel channel,
        String movementDeviceCode,
        String deviceIp,
        String subCategory,
        String tags,
        BigDecimal riskScore,
        Boolean requiresSignature,
        String signedBy,
        Instant signedAt,
        Instant printedAt,
        List<CashMovementAttachmentResponse> attachments,
        Instant createdAt,
        /** Nature d'operation (lot C3). */
        String natureCode,
        String transferOrderNumber,
        String approvedBy,
        Instant approvedAt,
        Instant expiresAt,
        String reversalReason,
        String cancelReason
) {
    /** Forme historique, sans les champs des operations (lot C3). */
    public CashMovementResponse(String movementNumber, String sessionNumber, String registerCode, String registerName, String businessEntityCode, String deviceCode, CashSessionStatus sessionStatus, String sessionOpenedBy, Instant sessionOpenedAt, CashMovementType movementType, CashFlowDirection flowDirection, BigDecimal amount, String currency, CashDocumentType documentType, String documentNumber, String flowCategory, String referenceType, String referenceCode, String counterpartyType, String counterpartyCode, String counterpartyName, String reason, String createdBy, String relatedTransactionNumber, String relatedIntentNumber, String relatedReceiptNumber, String relatedCustomerType, String relatedCustomerCode, String relatedCustomerName, String relatedSourceType, String relatedSourceCode, String relatedSourceLabel, String metadataJson, CashMovementStatus status, String relatedMovementNumber, String batchId, BigDecimal runningBalance, BigDecimal exchangeRate, CashMovementChannel channel, String movementDeviceCode, String deviceIp, String subCategory, String tags, BigDecimal riskScore, Boolean requiresSignature, String signedBy, Instant signedAt, Instant printedAt, List<CashMovementAttachmentResponse> attachments, Instant createdAt) {
        this(movementNumber, sessionNumber, registerCode, registerName, businessEntityCode, deviceCode, sessionStatus, sessionOpenedBy, sessionOpenedAt, movementType, flowDirection, amount, currency, documentType, documentNumber, flowCategory, referenceType, referenceCode, counterpartyType, counterpartyCode, counterpartyName, reason, createdBy, relatedTransactionNumber, relatedIntentNumber, relatedReceiptNumber, relatedCustomerType, relatedCustomerCode, relatedCustomerName, relatedSourceType, relatedSourceCode, relatedSourceLabel, metadataJson, status, relatedMovementNumber, batchId, runningBalance, exchangeRate, channel, movementDeviceCode, deviceIp, subCategory, tags, riskScore, requiresSignature, signedBy, signedAt, printedAt, attachments, createdAt, null, null, null, null, null, null, null);
    }
}
