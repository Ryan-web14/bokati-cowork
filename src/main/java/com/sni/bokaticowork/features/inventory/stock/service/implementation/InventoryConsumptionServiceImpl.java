package com.sni.bokaticowork.features.inventory.stock.service.implementation;

import com.sni.bokaticowork.features.inventory.stock.dto.request.StockReservationRequest;
import com.sni.bokaticowork.features.inventory.stock.dto.response.StockMovementResponse;
import com.sni.bokaticowork.features.inventory.stock.dto.response.StockReservationResponse;
import com.sni.bokaticowork.features.inventory.stock.service.interfaces.InventoryConsumptionService;
import com.sni.bokaticowork.features.inventory.stock.service.interfaces.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class InventoryConsumptionServiceImpl implements InventoryConsumptionService {

    private final StockService stockService;

    @Override
    public StockReservationResponse reserveForBusinessEvent(StockReservationRequest request) {
        return stockService.reserve(request);
    }

    @Override
    public StockReservationResponse releaseBusinessReservation(String reservationCode) {
        return stockService.releaseReservation(reservationCode);
    }

    @Override
    public StockMovementResponse consumeBusinessReservation(String reservationCode, String performedBy) {
        return stockService.consumeReservation(reservationCode, performedBy);
    }
}
