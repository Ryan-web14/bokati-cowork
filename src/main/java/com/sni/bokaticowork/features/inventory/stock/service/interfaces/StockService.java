package com.sni.bokaticowork.features.inventory.stock.service.interfaces;

import com.sni.bokaticowork.features.inventory.stock.dto.request.StockAdjustmentRequest;
import com.sni.bokaticowork.features.inventory.stock.dto.request.StockInRequest;
import com.sni.bokaticowork.features.inventory.stock.dto.request.StockOutRequest;
import com.sni.bokaticowork.features.inventory.stock.dto.request.StockReservationRequest;
import com.sni.bokaticowork.features.inventory.stock.dto.request.StockTransferRequest;
import com.sni.bokaticowork.features.inventory.stock.dto.response.StockLevelResponse;
import com.sni.bokaticowork.features.inventory.stock.dto.response.StockMovementResponse;
import com.sni.bokaticowork.features.inventory.stock.dto.response.StockReservationResponse;
import com.sni.bokaticowork.features.inventory.stock.enums.StockMovementType;
import com.sni.bokaticowork.features.inventory.stock.enums.StockReferenceType;
import com.sni.bokaticowork.features.inventory.stock.enums.StockReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;

public interface StockService {

    StockMovementResponse receive(StockInRequest request);

    StockMovementResponse issue(StockOutRequest request);

    StockMovementResponse transfer(StockTransferRequest request);

    StockMovementResponse adjust(StockAdjustmentRequest request);

    StockMovementResponse reverseMovement(String movementCode, String performedBy, String reason);

    StockReservationResponse reserve(StockReservationRequest request);

    StockReservationResponse releaseReservation(String reservationCode);

    StockMovementResponse consumeReservation(String reservationCode, String performedBy);

    Page<StockReservationResponse> searchReservations(StockReservationStatus status, Pageable pageable);

    Page<StockLevelResponse> searchLevels(String itemCode, String locationCode, String categoryCode, Boolean availableOnly, Boolean lowStock, Pageable pageable);

    Page<StockMovementResponse> searchMovements(String itemCode, String locationCode, StockMovementType movementType,
                                                StockReferenceType referenceType, String referenceCode,
                                                Instant fromDate, Instant toDate, Pageable pageable);
}
