package com.sni.bokaticowork.features.billing.dto.response;

import java.math.BigDecimal;
import java.util.List;

/** Disponibilites et alertes des lignes (lot B7) ; {@code blocked} : l'emission sera refusee. */
public record CheckAvailabilityResponse(boolean blocked, List<SimulateBillingDocumentResponse.Alert> alerts, List<Stock> stock,
                                        List<Resource> resources) {

    /** Stock d'une ligne d'article : demande, disponible, manquant. */
    public record Stock(int lineIndex, String itemCode, String itemName, boolean active, BigDecimal requested, BigDecimal available,
                        BigDecimal missing, boolean negativeStockAllowed) {
    }

    public record Resource(Integer lineIndex, String resourceCode, String resourceName, boolean available, Integer remainingCapacity,
                           String message) {
    }
}
