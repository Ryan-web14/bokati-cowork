package com.sni.bokaticowork.features.inventory.stock.mapper.decorator;

import com.sni.bokaticowork.features.inventory.stock.dto.response.StockLotResponse;
import com.sni.bokaticowork.features.inventory.stock.model.StockLot;
import org.springframework.stereotype.Component;

/**
 * Traduction d un lot vers sa representation d API.
 *
 * <p>Extraite des services parce qu elle y etait dupliquee : trois copies de la meme correspondance
 * auraient diverge des le premier champ ajoute.</p>
 */
@Component
public class StockLotResponseFactory {

    public StockLotResponse toResponse(StockLot lot) {
        return StockLotResponse.builder()
                .itemCode(lot.getItem().getItemCode())
                .itemName(lot.getItem().getName())
                .locationCode(lot.getLocation().getLocationCode())
                .locationName(lot.getLocation().getName())
                .lotNumber(lot.getLotNumber())
                .expiryDate(lot.getExpiryDate())
                .receivedAt(lot.getReceivedAt())
                .initialQuantity(lot.getInitialQuantity())
                .remainingQuantity(lot.getRemainingQuantity())
                .quarantined(lot.getQuarantined())
                .quarantineReason(lot.getQuarantineReason())
                .blocked(lot.getBlocked())
                .blockReasonType(lot.getBlockReasonType() == null ? null : lot.getBlockReasonType().name())
                .blockReason(lot.getBlockReason())
                .ownershipType(lot.getOwnershipType())
                .ownerCode(lot.getOwnerCode())
                .active(lot.getActive())
                .build();
    }
}
