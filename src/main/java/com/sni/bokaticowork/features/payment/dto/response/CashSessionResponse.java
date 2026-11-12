package com.sni.bokaticowork.features.payment.dto.response;

import com.sni.bokaticowork.features.payment.enums.CashSessionStatus;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Session de caisse.
 *
 * <p>Cloture a l'aveugle : tant que la session n'est pas comptee, l'attendu, l'ecart et le solde ne
 * sont pas montres a qui n'a pas le droit de decider d'un ecart ({@code expectedHidden} vaut alors
 * vrai et ces montants sont vides).</p>
 */
public record CashSessionResponse(
        String sessionNumber,
        String registerCode,
        CashSessionStatus status,
        String openedBy,
        String closedBy,
        String reviewedBy,
        BigDecimal openingAmount,
        BigDecimal closingAmount,
        BigDecimal expectedClosingAmount,
        BigDecimal countedClosingAmount,
        BigDecimal varianceAmount,
        String varianceReason,
        Instant openedAt,
        Instant closingRequestedAt,
        Instant closedAt,
        BigDecimal expectedOpeningAmount,
        BigDecimal openingVariance,
        String openingVarianceReason,
        Instant pausedAt,
        Long pauseSeconds,
        String suspendedBy,
        String suspensionReason,
        BigDecimal keptFloat,
        BigDecimal amountToRemit,
        String closingCountNumber,
        com.sni.bokaticowork.features.payment.enums.CashVarianceDecision varianceDecision,
        String varianceDecidedBy,
        Boolean forcedClose,
        String handoverNumber,
        String outOfHoursReason,
        Boolean expectedHidden
) {
    /** Forme historique. */
    public CashSessionResponse(String sessionNumber, String registerCode, CashSessionStatus status, String openedBy,
                               String closedBy, String reviewedBy, BigDecimal openingAmount, BigDecimal closingAmount,
                               BigDecimal expectedClosingAmount, BigDecimal countedClosingAmount, BigDecimal varianceAmount,
                               String varianceReason, Instant openedAt, Instant closingRequestedAt, Instant closedAt) {
        this(sessionNumber, registerCode, status, openedBy, closedBy, reviewedBy, openingAmount, closingAmount,
                expectedClosingAmount, countedClosingAmount, varianceAmount, varianceReason, openedAt, closingRequestedAt,
                closedAt, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, Boolean.FALSE);
    }
}
