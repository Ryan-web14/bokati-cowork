package com.sni.bokaticowork.features.inventory.stock.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.inventory.stock.dto.response.InventorySerialResponse;
import com.sni.bokaticowork.features.inventory.stock.enums.InventorySerialStatus;
import com.sni.bokaticowork.features.inventory.stock.service.interfaces.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Numeros de serie d'un article. Pas de prefixe de classe : la route vit sous /inventory/items,
 * comme les lots, et non sous /inventory/stock.
 */
@RestController
@RequiredArgsConstructor
public class StockSerialController {

    private final StockService service;

    @GetMapping(ApiPath.V1 + "/inventory/items/{itemCode}/serials")
    public ResponseEntity<List<InventorySerialResponse>> serials(@PathVariable String itemCode,
                                                                  @RequestParam(required = false) String locationCode,
                                                                  @RequestParam(required = false) InventorySerialStatus status) {
        return ResponseEntity.ok(service.getSerials(itemCode, locationCode, status));
    }
}
