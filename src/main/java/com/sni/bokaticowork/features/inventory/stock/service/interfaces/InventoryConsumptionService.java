package com.sni.bokaticowork.features.inventory.stock.service.interfaces;

import com.sni.bokaticowork.features.inventory.stock.dto.request.StockReservationRequest;
import com.sni.bokaticowork.features.inventory.stock.dto.response.StockMovementResponse;
import com.sni.bokaticowork.features.inventory.stock.dto.response.StockReservationResponse;

public interface InventoryConsumptionService {
    StockReservationResponse reserveForBusinessEvent(StockReservationRequest request);

    StockReservationResponse releaseBusinessReservation(String reservationCode);

    StockMovementResponse consumeBusinessReservation(String reservationCode, String performedBy);
}
