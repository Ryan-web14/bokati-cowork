package com.sni.bokaticowork.features.billing.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.billing.dto.CollectionDtos;
import com.sni.bokaticowork.features.billing.dto.response.PaymentScheduleResponse;
import com.sni.bokaticowork.features.billing.service.implementation.CollectionService;
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
 * Lot C12 : recouvrement. Promesse de paiement (relances suspendues jusqu'a sa date) et plan de
 * reglement transforme en echeancier. Le palier WhatsApp se regle dans les politiques de relance.
 */
@RestController
@RequestMapping(ApiPath.V1 + "/billing")
@RequiredArgsConstructor
public class CollectionController {

    private final CollectionService collection;

    @PostMapping("/documents/{documentNumber}/payment-promises")
    public ResponseEntity<CollectionDtos.PromiseView> promise(@PathVariable String documentNumber,
                                                              @Valid @RequestBody CollectionDtos.PromiseRequest request) {
        return ResponseEntity.ok(collection.promise(documentNumber, request));
    }

    /** Promesses d'une facture, ou toutes les promesses en cours. */
    @GetMapping("/payment-promises")
    public ResponseEntity<List<CollectionDtos.PromiseView>> promises(@RequestParam(required = false) String documentNumber,
                                                                     @RequestParam(defaultValue = "false") boolean openOnly) {
        return ResponseEntity.ok(collection.promises(documentNumber, openOnly));
    }

    @PatchMapping("/payment-promises/{promiseNumber}/cancel")
    public ResponseEntity<CollectionDtos.PromiseView> cancel(@PathVariable String promiseNumber) {
        return ResponseEntity.ok(collection.cancelPromise(promiseNumber));
    }

    /** Plan de reglement : le reste du en echeances egales (echeancier de la facture). */
    @PostMapping("/documents/{documentNumber}/payment-plan")
    public ResponseEntity<PaymentScheduleResponse> plan(@PathVariable String documentNumber, @Valid @RequestBody CollectionDtos.PlanRequest request) {
        return ResponseEntity.ok(collection.plan(documentNumber, request));
    }
}
