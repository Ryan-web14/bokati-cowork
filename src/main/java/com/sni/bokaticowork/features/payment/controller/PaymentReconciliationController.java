package com.sni.bokaticowork.features.payment.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.payment.dto.request.CreateReconciliationBatchRequest;
import com.sni.bokaticowork.features.payment.dto.response.ReconciliationBatchResponse;
import com.sni.bokaticowork.features.payment.service.interfaces.PaymentReconciliationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPath.V1 + "/payments/reconciliation-batches")
@RequiredArgsConstructor
public class PaymentReconciliationController {

    private final PaymentReconciliationService reconciliationService;

    @PostMapping
    public ResponseEntity<ReconciliationBatchResponse> create(@Valid @RequestBody CreateReconciliationBatchRequest request) {
        return ResponseEntity.ok(reconciliationService.create(request));
    }

    @PatchMapping("/{batchNumber}/complete")
    public ResponseEntity<ReconciliationBatchResponse> complete(@PathVariable String batchNumber) {
        return ResponseEntity.ok(reconciliationService.complete(batchNumber));
    }
}
