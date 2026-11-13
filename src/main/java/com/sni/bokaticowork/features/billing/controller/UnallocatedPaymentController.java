package com.sni.bokaticowork.features.billing.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.billing.dto.UnallocatedPaymentDtos;
import com.sni.bokaticowork.features.billing.service.implementation.UnallocatedPaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Lot C9 : sommes encaissees en attente d'affectation, factures proposees, affectation. */
@RestController
@RequestMapping(ApiPath.V1 + "/billing/unallocated-payments")
@RequiredArgsConstructor
public class UnallocatedPaymentController {

    private final UnallocatedPaymentService unallocated;

    /** Sommes en attente, d'un client ou de tous, avec les factures candidates notees. */
    @GetMapping("/suggestions")
    public ResponseEntity<List<UnallocatedPaymentDtos.Suggestion>> suggestions(@RequestParam(required = false) String customerType,
                                                                               @RequestParam(required = false) String customerCode,
                                                                               @RequestParam(defaultValue = "100") int limit) {
        return ResponseEntity.ok(unallocated.suggestions(customerType, customerCode, limit));
    }

    /** Affectation a une ou plusieurs factures ; le reliquat reste en attente. */
    @PostMapping("/{transactionNumber}/allocate")
    public ResponseEntity<UnallocatedPaymentDtos.AllocationResult> allocate(@PathVariable String transactionNumber,
                                                                           @Valid @RequestBody UnallocatedPaymentDtos.AllocateRequest request) {
        return ResponseEntity.ok(unallocated.allocate(transactionNumber, request.allocations()));
    }
}
