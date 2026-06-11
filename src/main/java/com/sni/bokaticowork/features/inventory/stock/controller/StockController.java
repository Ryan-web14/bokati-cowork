package com.sni.bokaticowork.features.inventory.stock.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.idempotency.aop.Idempotent;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.inventory.stock.dto.request.StockAdjustmentRequest;
import com.sni.bokaticowork.features.inventory.stock.dto.request.StockInRequest;
import com.sni.bokaticowork.features.inventory.stock.dto.request.StockOutRequest;
import com.sni.bokaticowork.features.inventory.stock.dto.request.StockReservationRequest;
import com.sni.bokaticowork.features.inventory.stock.dto.request.StockTransferRequest;
import com.sni.bokaticowork.features.inventory.stock.dto.response.InventorySerialResponse;
import com.sni.bokaticowork.features.inventory.stock.dto.response.StockLevelResponse;
import com.sni.bokaticowork.features.inventory.stock.dto.response.StockMovementResponse;
import com.sni.bokaticowork.features.inventory.stock.dto.response.StockReservationResponse;
import com.sni.bokaticowork.features.inventory.stock.enums.InventorySerialStatus;
import com.sni.bokaticowork.features.inventory.stock.enums.StockMovementType;
import com.sni.bokaticowork.features.inventory.stock.enums.StockReferenceType;
import com.sni.bokaticowork.features.inventory.stock.enums.StockReservationStatus;
import com.sni.bokaticowork.features.inventory.stock.service.interfaces.StockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/inventory/stock")
public class StockController {

    private final StockService service;

    @PostMapping("/in")
    @Audited(module = "INVENTORY", action = "STOCK_IN", ressource = "stock_movement")
    @Idempotent(operation = "INVENTORY_STOCK_IN", required = false)
    public ResponseEntity<StockMovementResponse> receive(@Valid @RequestBody StockInRequest request) {
        return ResponseEntity.ok(service.receive(request));
    }

    @PostMapping("/out")
    @Audited(module = "INVENTORY", action = "STOCK_OUT", ressource = "stock_movement")
    @Idempotent(operation = "INVENTORY_STOCK_OUT", required = false)
    public ResponseEntity<StockMovementResponse> issue(@Valid @RequestBody StockOutRequest request) {
        return ResponseEntity.ok(service.issue(request));
    }

    @PostMapping("/transfer")
    @Audited(module = "INVENTORY", action = "STOCK_TRANSFER", ressource = "stock_movement")
    @Idempotent(operation = "INVENTORY_STOCK_TRANSFER", required = false)
    public ResponseEntity<StockMovementResponse> transfer(@Valid @RequestBody StockTransferRequest request) {
        return ResponseEntity.ok(service.transfer(request));
    }

    @PostMapping("/adjust")
    @Audited(module = "INVENTORY", action = "STOCK_ADJUST", ressource = "stock_movement")
    @Idempotent(operation = "INVENTORY_STOCK_ADJUST", required = false)
    public ResponseEntity<StockMovementResponse> adjust(@Valid @RequestBody StockAdjustmentRequest request) {
        return ResponseEntity.ok(service.adjust(request));
    }

    @PostMapping("/movements/{movementCode}/reverse")
    @Audited(module = "INVENTORY", action = "STOCK_MOVEMENT_REVERSE", ressource = "stock_movement")
    @Idempotent(operation = "INVENTORY_STOCK_MOVEMENT_REVERSE", required = false)
    public ResponseEntity<StockMovementResponse> reverseMovement(@PathVariable String movementCode,
                                                                 @RequestParam(required = false) String performedBy,
                                                                 @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(service.reverseMovement(movementCode, performedBy, reason));
    }

    @PostMapping("/reservations")
    @Audited(module = "INVENTORY", action = "STOCK_RESERVE", ressource = "stock_reservation")
    @Idempotent(operation = "INVENTORY_STOCK_RESERVE", required = false)
    public ResponseEntity<StockReservationResponse> reserve(@Valid @RequestBody StockReservationRequest request) {
        return ResponseEntity.ok(service.reserve(request));
    }

    @PatchMapping("/reservations/{reservationCode}/release")
    @Audited(module = "INVENTORY", action = "STOCK_RESERVATION_RELEASE", ressource = "stock_reservation")
    @Idempotent(operation = "INVENTORY_STOCK_RESERVATION_RELEASE", required = false)
    public ResponseEntity<StockReservationResponse> releaseReservation(@PathVariable String reservationCode) {
        return ResponseEntity.ok(service.releaseReservation(reservationCode));
    }

    @PatchMapping("/reservations/{reservationCode}/consume")
    @Audited(module = "INVENTORY", action = "STOCK_RESERVATION_CONSUME", ressource = "stock_reservation")
    @Idempotent(operation = "INVENTORY_STOCK_RESERVATION_CONSUME", required = false)
    public ResponseEntity<StockMovementResponse> consumeReservation(@PathVariable String reservationCode,
                                                                    @RequestParam(required = false) String performedBy) {
        return ResponseEntity.ok(service.consumeReservation(reservationCode, performedBy));
    }

    @GetMapping("/reservations")
    public ResponseEntity<Page<StockReservationResponse>> reservations(@RequestParam(required = false) StockReservationStatus status,
                                                                       @PageableDefault(size = 20, sort = "reservedAt") Pageable pageable) {
        return ResponseEntity.ok(service.searchReservations(status, pageable));
    }

    @GetMapping("/levels")
    public ResponseEntity<Page<StockLevelResponse>> levels(@RequestParam(required = false) String itemCode,
                                                           @RequestParam(required = false) String locationCode,
                                                           @RequestParam(required = false) String categoryCode,
                                                           @RequestParam(required = false) Boolean availableOnly,
                                                           @RequestParam(required = false) Boolean lowStock,
                                                           @PageableDefault(size = 20, sort = "item.name") Pageable pageable) {
        return ResponseEntity.ok(service.searchLevels(itemCode, locationCode, categoryCode, availableOnly, lowStock, pageable));
    }

    @GetMapping(ApiPath.V1 + "/inventory/items/{itemCode}/serials")
    public ResponseEntity<List<InventorySerialResponse>> serials(@PathVariable String itemCode,
                                                                  @RequestParam(required = false) String locationCode,
                                                                  @RequestParam(required = false) InventorySerialStatus status) {
        return ResponseEntity.ok(service.getSerials(itemCode, locationCode, status));
    }

    @GetMapping("/movements")
    public ResponseEntity<Page<StockMovementResponse>> movements(@RequestParam(required = false) String itemCode,
                                                                 @RequestParam(required = false) String locationCode,
                                                                 @RequestParam(required = false) StockMovementType movementType,
                                                                 @RequestParam(required = false) StockReferenceType referenceType,
                                                                 @RequestParam(required = false) String referenceCode,
                                                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromDate,
                                                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toDate,
                                                                 @PageableDefault(size = 20, sort = "performedAt") Pageable pageable) {
        return ResponseEntity.ok(service.searchMovements(itemCode, locationCode, movementType, referenceType,
                referenceCode, fromDate, toDate, pageable));
    }
}
