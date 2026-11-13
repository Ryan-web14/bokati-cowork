package com.sni.bokaticowork.features.billing.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.billing.dto.CustomerPriceDtos;
import com.sni.bokaticowork.features.billing.service.implementation.CustomerPriceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Lot C8 : tarifs negocies d'un client (organisation ou membre). Le prix applicable se lit par
 * {@code GET /billing/catalog/{itemCode}/price?customerType=&customerCode=} (source CONTRACT).
 */
@RestController
@RequestMapping(ApiPath.V1 + "/billing/customers/{customerType}/{customerCode}/prices")
@RequiredArgsConstructor
public class CustomerPriceController {

    private final CustomerPriceService prices;

    /** Tarifs du client ; {@code current=true} pour les seuls tarifs en vigueur. */
    @GetMapping
    public ResponseEntity<List<CustomerPriceDtos.PriceView>> list(@PathVariable String customerType, @PathVariable String customerCode,
                                                                  @RequestParam(defaultValue = "false") boolean current) {
        return ResponseEntity.ok(prices.list(customerType, customerCode, current));
    }

    /** Nouveau tarif ; le tarif en cours du meme article s'arrete la veille. */
    @PostMapping
    public ResponseEntity<CustomerPriceDtos.PriceView> create(@PathVariable String customerType, @PathVariable String customerCode,
                                                              @Valid @RequestBody CustomerPriceDtos.PriceRequest request) {
        return ResponseEntity.ok(prices.create(customerType, customerCode, request));
    }

    /** Fin d'un tarif a une date. */
    @PatchMapping("/{id}/end")
    public ResponseEntity<CustomerPriceDtos.PriceView> end(@PathVariable String customerType, @PathVariable String customerCode,
                                                           @PathVariable Long id, @Valid @RequestBody CustomerPriceDtos.EndRequest request) {
        return ResponseEntity.ok(prices.end(id, request.validUntil()));
    }
}
