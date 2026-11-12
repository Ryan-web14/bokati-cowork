package com.sni.bokaticowork.features.payment.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Position de tresorerie physique en temps reel (plan des caisses, section 13.1).
 *
 * <p>Le solde attendu d'une caisse a cloture a l'aveugle n'est montre qu'a qui peut decider d'un
 * ecart ({@code expectedHidden}).</p>
 */
public record CashPositionResponse(
        Instant generatedAt,
        List<RegisterPosition> registers,
        List<SiteTotal> sites,
        SiteTotal total
) {
    public record RegisterPosition(
            String registerCode,
            String name,
            String registerType,
            String status,
            String locationCode,
            String sessionNumber,
            String sessionStatus,
            String cashier,
            BigDecimal expectedBalance,
            Boolean expectedHidden,
            BigDecimal lastCountAmount,
            Instant lastCountAt,
            Long daysSinceCount,
            BigDecimal maxCashAmount,
            BigDecimal occupancyPercent,
            Integer transfersInCount,
            BigDecimal transfersInAmount,
            Integer transfersOutCount,
            BigDecimal transfersOutAmount,
            Integer pendingReviews,
            Integer pendingVisas,
            Instant lastSpotCheckAt,
            Boolean spotCheckOverdue,
            List<String> alerts
    ) {
    }

    public record SiteTotal(
            String locationCode,
            Integer registers,
            Integer openSessions,
            BigDecimal expectedBalance,
            BigDecimal lastCountTotal,
            BigDecimal inTransit,
            Integer pendingReviews,
            Integer alerts
    ) {
    }
}
