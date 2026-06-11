package com.sni.bokaticowork.features.inventory.stock.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.inventory.stock.dto.request.StockLotCreateRequest;
import com.sni.bokaticowork.features.inventory.stock.dto.response.StockLotResponse;
import com.sni.bokaticowork.features.inventory.stock.service.interfaces.StockLotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class StockLotController {

    private final StockLotService service;

    @GetMapping(ApiPath.V1 + "/inventory/stock/lots")
    public ResponseEntity<Page<StockLotResponse>> search(@RequestParam(required = false) String itemCode,
                                                         @RequestParam(required = false) String locationCode,
                                                         @RequestParam(required = false) String lotNumber,
                                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiringBefore,
                                                         @RequestParam(required = false) Boolean active,
                                                         @RequestParam(required = false) Boolean remainingOnly,
                                                         @PageableDefault(size = 20, sort = "expiryDate") Pageable pageable) {
        return ResponseEntity.ok(service.search(itemCode, locationCode, lotNumber, expiringBefore, active, remainingOnly, pageable));
    }

    @GetMapping(ApiPath.V1 + "/inventory/items/{itemCode}/lots")
    public ResponseEntity<List<StockLotResponse>> getLotsForItem(@PathVariable String itemCode,
                                                                  @RequestParam(required = false) String locationCode,
                                                                  @RequestParam(required = false) Boolean activeOnly) {
        return ResponseEntity.ok(service.getLotsForItem(itemCode, locationCode, activeOnly));
    }

    @PostMapping(ApiPath.V1 + "/inventory/items/{itemCode}/lots")
    @Audited(module = "INVENTORY", action = "CREATE_LOT", ressource = "stock_lot")
    public ResponseEntity<StockLotResponse> createLot(@PathVariable String itemCode,
                                                       @Valid @RequestBody StockLotCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createLot(itemCode, request));
    }
}
