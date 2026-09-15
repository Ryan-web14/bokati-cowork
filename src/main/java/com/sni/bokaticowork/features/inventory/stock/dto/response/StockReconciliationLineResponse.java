package com.sni.bokaticowork.features.inventory.stock.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Une divergence entre le stock enregistre et le stock recalcule depuis le journal de mouvements.
 */
@Data
@Builder
public class StockReconciliationLineResponse {

    private String itemCode;

    private String itemName;

    private String locationCode;

    private String locationName;

    /** Quantite actuellement portee par le niveau de stock. */
    private BigDecimal recordedQuantity;

    /** Quantite recalculee a partir de la somme des mouvements. */
    private BigDecimal expectedQuantity;

    /** recordedQuantity moins expectedQuantity. Positif : le niveau est trop haut. */
    private BigDecimal difference;

    private Instant lastMovementAt;
}
