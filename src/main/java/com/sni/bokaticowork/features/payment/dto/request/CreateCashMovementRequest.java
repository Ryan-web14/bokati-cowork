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
        String natureCode,
        /** Journal comptable (lot F3) : un des journaux autorises pour la caisse ; a defaut, celui de la caisse. */
        String journalCode,
        /** TVA recuperable (sortie) ou collectee (entree) comprise dans le montant. */
        BigDecimal vatAmount,
        String vatCode,
        String supplierCode,
        /** Beneficiaire reel d'une sortie. */
        String beneficiaryName,
        String beneficiaryIdNumber,
        String beneficiaryPhone,
        /** Demande de fonds a l'origine du mouvement. */
        String requestNumber
) {
    /** Forme du lot C3, sans les champs comptables. */
    public CreateCashMovementRequest(CashMovementType movementType, BigDecimal amount, CashDocumentType documentType, String documentNumber,
                                     String flowCategory, String referenceType, String referenceCode, String counterpartyType,
                                     String counterpartyCode, String counterpartyName, String reason, String createdBy, String metadataJson,
                                     String relatedMovementNumber, String batchId, BigDecimal exchangeRate, CashMovementChannel channel,
                                     String deviceCode, String deviceIp, String subCategory, String tags,
                                     com.sni.bokaticowork.features.payment.enums.CashFlowDirection direction, String natureCode) {
        this(movementType, amount, documentType, documentNumber, flowCategory, referenceType, referenceCode, counterpartyType,
                counterpartyCode, counterpartyName, reason, createdBy, metadataJson, relatedMovementNumber, batchId, exchangeRate, channel,
                deviceCode, deviceIp, subCategory, tags, direction, natureCode, null, null, null, null, null, null, null, null);
    }

}
