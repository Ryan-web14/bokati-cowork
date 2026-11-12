package com.sni.bokaticowork.features.payment.dto.request;

import jakarta.validation.Valid;

import java.math.BigDecimal;
import java.util.List;

/**
 * Un comptage : billetage par coupure (ou un total si la caisse ne l'exige pas).
 *
 * @param currency          devise comptee ; XAF par defaut
 * @param lines             quantites par coupure ; le total se calcule
 * @param totalAmount       total, accepte seulement sans billetage et si la caisse ne l'exige pas
 * @param secondCounterEmail second compteur (recomptage, double comptage)
 * @param witnessEmail      temoin (cloture d'office, scelles)
 * @param keptFloat         fonds garde pour la session suivante (cloture) ; propose : fonds standard
 * @param varianceReasonCode motif d'ecart (liste {@code VARIANCE_REASON}), exige si l'ecart sort de la tolerance
 */
public record CashCountRequest(
        String currency,
        @Valid List<Line> lines,
        BigDecimal totalAmount,
        String secondCounterEmail,
        String witnessEmail,
        BigDecimal keptFloat,
        String varianceReasonCode,
        String varianceReasonText,
        String note
) {
    /** Quantite d'une coupure ; les billets abimes ou suspects a part, exclus du total disponible. */
    public record Line(BigDecimal value, com.sni.bokaticowork.features.payment.enums.CashDenominationKind kind, Integer quantity, Integer damagedQuantity) {
    }

    public static CashCountRequest ofTotal(BigDecimal total, String reasonText) {
        return new CashCountRequest(null, null, total, null, null, null, null, reasonText, null);
    }
}
