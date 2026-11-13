package com.sni.bokaticowork.features.billing.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.billing.dto.BatchBillingDtos;
import com.sni.bokaticowork.features.billing.service.implementation.BatchBillingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Lot C13 : facturation groupee periodique (apercu, exclusions, emission). */
@RestController
@RequestMapping(ApiPath.V1 + "/billing/batch-runs")
@RequiredArgsConstructor
public class BatchBillingController {

    private final BatchBillingService batches;

    /** Nouvel apercu : les consommations en attente de la periode, client par client. */
    @PostMapping
    public ResponseEntity<BatchBillingDtos.RunView> preview(@Valid @RequestBody BatchBillingDtos.PreviewRequest request) {
        return ResponseEntity.ok(batches.preview(request));
    }

    @GetMapping
    public ResponseEntity<List<BatchBillingDtos.RunSummary>> list() {
        return ResponseEntity.ok(batches.list());
    }

    @GetMapping("/{runNumber}")
    public ResponseEntity<BatchBillingDtos.RunView> get(@PathVariable String runNumber) {
        return ResponseEntity.ok(batches.get(runNumber));
    }

    /** Exclut ou reintegre un client de l'apercu. */
    @PatchMapping("/{runNumber}/exclude")
    public ResponseEntity<BatchBillingDtos.RunView> exclude(@PathVariable String runNumber, @Valid @RequestBody BatchBillingDtos.ExcludeRequest request) {
        return ResponseEntity.ok(batches.exclude(runNumber, request.itemId(), request.excluded()));
    }

    /** Emission : une facture emise par client non exclu ; les erreurs sont notees client par client. */
    @PostMapping("/{runNumber}/issue")
    public ResponseEntity<BatchBillingDtos.RunView> issue(@PathVariable String runNumber) {
        return ResponseEntity.ok(batches.issue(runNumber));
    }

    @PostMapping("/{runNumber}/cancel")
    public ResponseEntity<BatchBillingDtos.RunView> cancel(@PathVariable String runNumber) {
        return ResponseEntity.ok(batches.cancel(runNumber));
    }
}
