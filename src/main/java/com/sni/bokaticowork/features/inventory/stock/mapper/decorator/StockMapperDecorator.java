package com.sni.bokaticowork.features.inventory.stock.mapper.decorator;

import com.sni.bokaticowork.features.inventory.stock.dto.response.StockLevelResponse;
import com.sni.bokaticowork.features.inventory.stock.dto.response.StockMovementLotResponse;
import com.sni.bokaticowork.features.inventory.stock.dto.response.StockMovementResponse;
import com.sni.bokaticowork.features.inventory.stock.dto.response.StockReservationResponse;
import com.sni.bokaticowork.features.inventory.stock.mapper.interfaces.StockMapper;
import com.sni.bokaticowork.features.inventory.stock.model.StockLevel;
import com.sni.bokaticowork.features.inventory.stock.model.StockMovement;
import com.sni.bokaticowork.features.inventory.stock.model.StockReservation;
import com.sni.bokaticowork.features.inventory.stock.repository.StockMovementLotRepository;
import com.sni.bokaticowork.features.inventory.stock.repository.StockMovementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public abstract class StockMapperDecorator implements StockMapper {

    @Autowired
    @Qualifier("delegate")
    private StockMapper delegate;

    @Autowired
    private StockMovementLotRepository movementLotRepository;

    @Autowired
    private StockMovementRepository movementRepository;

    @Override
    public StockLevelResponse toLevelResponse(StockLevel level) {
        StockLevelResponse response = delegate.toLevelResponse(level);
        if (level == null) return response;
        response.setItemCode(level.getItem().getItemCode());
        response.setItemName(level.getItem().getName());
        response.setLocationCode(level.getLocation().getLocationCode());
        response.setLocationName(level.getLocation().getName());
        return response;
    }

    @Override
    public StockMovementResponse toMovementResponse(StockMovement movement) {
        StockMovementResponse response = delegate.toMovementResponse(movement);
        if (movement == null) return response;
        response.setItemCode(movement.getItem().getItemCode());
        response.setItemName(movement.getItem().getName());
        response.setLocationFromCode(movement.getLocationFrom() == null ? null : movement.getLocationFrom().getLocationCode());
        response.setLocationToCode(movement.getLocationTo() == null ? null : movement.getLocationTo().getLocationCode());
        response.setReversalOfMovementCode(movement.getReversalOfMovement() == null ? null : movement.getReversalOfMovement().getMovementCode());
        response.setReversalMovementCode(movementRepository.findFirstByReversalOfMovementOrderByPerformedAtDesc(movement)
                .map(StockMovement::getMovementCode)
                .orElse(null));
        response.setOriginalMovementCode(movement.getReversalOfMovement() == null ? movement.getMovementCode() : movement.getReversalOfMovement().getMovementCode());
        response.setLots(movementLotRepository.findAllByMovement(movement).stream()
                .map(lot -> StockMovementLotResponse.builder()
                        .lotNumber(lot.getLotNumber())
                        .expiryDate(lot.getExpiryDate())
                        .quantity(lot.getQuantity())
                        .build())
                .toList());
        return response;
    }

    @Override
    public StockReservationResponse toReservationResponse(StockReservation reservation) {
        StockReservationResponse response = delegate.toReservationResponse(reservation);
        if (reservation == null) return response;
        response.setItemCode(reservation.getItem().getItemCode());
        response.setItemName(reservation.getItem().getName());
        response.setLocationCode(reservation.getLocation().getLocationCode());
        response.setLocationName(reservation.getLocation().getName());
        return response;
    }
}
