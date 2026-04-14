package com.sni.bokaticowork.features.inventory.stock.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.inventory.stock.dto.response.StockLotResponse;
import com.sni.bokaticowork.features.inventory.stock.service.interfaces.StockLotService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/inventory/stock/lots")
public class StockLotController {

    private final StockLotService service;

    @GetMapping
    public ResponseEntity<Page<StockLotResponse>> search(@RequestParam(required = false) String itemCode,
                                                         @RequestParam(required = false) String locationCode,
                                                         @RequestParam(required = false) String lotNumber,
                                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiringBefore,
                                                         @RequestParam(required = false) Boolean active,
                                                         @RequestParam(required = false) Boolean remainingOnly,
                                                         @PageableDefault(size = 20, sort = "expiryDate") Pageable pageable) {
        return ResponseEntity.ok(service.search(itemCode, locationCode, lotNumber, expiringBefore, active, remainingOnly, pageable));
    }
}
