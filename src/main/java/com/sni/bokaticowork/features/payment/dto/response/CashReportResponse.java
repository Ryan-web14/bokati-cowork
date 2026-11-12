package com.sni.bokaticowork.features.payment.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Un etat de caisse : colonnes, lignes et totaux ; chaque etat porte sa date d'edition et son
 * auteur. Exportable en CSV ({@code ?format=csv}).
 */
public record CashReportResponse(
        String kind,
        String title,
        Instant generatedAt,
        String generatedBy,
        Map<String, Object> parameters,
        List<String> columns,
        List<Map<String, Object>> rows,
        Map<String, Object> totals
) {
}
