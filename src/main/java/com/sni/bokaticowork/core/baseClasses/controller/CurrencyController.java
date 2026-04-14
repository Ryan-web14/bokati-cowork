package com.sni.bokaticowork.core.baseClasses.controller;


import com.sni.bokaticowork.core.baseClasses.dto.request.CurrencyRequest;
import com.sni.bokaticowork.core.baseClasses.dto.response.CurrencyResponse;
import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.idempotency.aop.Idempotent;
import com.sni.bokaticowork.core.baseClasses.service.interfaces.CurrencyService;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RequiredArgsConstructor
@RestController
@RequestMapping({ApiPath.V1 + "/currency", ApiPath.V1 + "/currencies"})
public class CurrencyController {

    private final CurrencyService currencyService;

    @Audited(module = "CURRENCY", action = "CREATE_CURRENCY")
    @Idempotent(operation = "CURRENCY_CREATE", required = false)
    @PostMapping
    public ResponseEntity<CurrencyResponse> createCurrency(@Valid @RequestBody CurrencyRequest request){

        CurrencyResponse response = currencyService.createCurrency(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Audited(module = "CURRENCY", action = "UPDATE_CURRENCY")
    @Idempotent(operation = "CURRENCY_UPDATE", required = false)
    @PutMapping("/{currencyCode}")
    public ResponseEntity<CurrencyResponse> updateCurrency(@PathVariable String currencyCode,
                                                           @Valid @RequestBody CurrencyRequest request) {
        return ResponseEntity.ok(currencyService.updateCurrency(currencyCode, request));
    }

    @GetMapping("/{currencyCode}")
    public ResponseEntity<CurrencyResponse> getCurrencyByCode(@PathVariable String currencyCode) {
        return ResponseEntity.ok(currencyService.getCurrencyByCode(currencyCode));
    }

    @GetMapping
    public ResponseEntity<List<CurrencyResponse>> getAllCurrencies() {
        return ResponseEntity.ok(currencyService.getAllCurrency());
    }

    @GetMapping("/all")
    public ResponseEntity<List<CurrencyResponse>> getAllCurrenciesLegacy() {
        return ResponseEntity.ok(currencyService.getAllCurrency());
    }

    @Audited(module = "CURRENCY", action = "DELETE_CURRENCY")
    @DeleteMapping("/{currencyCode}")
    public ResponseEntity<Void> deleteCurrency(@PathVariable String currencyCode) {
        currencyService.deleteCurrency(currencyCode);
        return ResponseEntity.noContent().build();
    }
}
