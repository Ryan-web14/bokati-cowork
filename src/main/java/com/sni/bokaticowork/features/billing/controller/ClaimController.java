package com.sni.bokaticowork.features.billing.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.billing.dto.ClaimDtos;
import com.sni.bokaticowork.features.billing.service.implementation.ClaimService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Lot C11 : reclamations sur facture (equipe). Une reclamation ouverte suspend les relances ; elle
 * se regle par avoir ou refus motive. Les motifs se gerent avec le droit BILLING_CATALOG_REFERENCES.
 */
@RestController
@RequestMapping(ApiPath.V1 + "/billing")
@RequiredArgsConstructor
public class ClaimController {

    private static final String MANAGE_REASONS = "hasAuthority('BILLING:CATALOG_REFERENCES')";

    private final ClaimService claims;

    @PostMapping("/documents/{documentNumber}/claims")
    public ResponseEntity<ClaimDtos.ClaimView> open(@PathVariable String documentNumber, @Valid @RequestBody ClaimDtos.OpenRequest request) {
        return ResponseEntity.ok(claims.openByStaff(documentNumber, request));
    }

    /** Reclamations d'une facture, d'un client, ou toutes les reclamations en cours. */
    @GetMapping("/claims")
    public ResponseEntity<List<ClaimDtos.ClaimView>> list(@RequestParam(required = false) String documentNumber,
                                                          @RequestParam(required = false) String customerType,
                                                          @RequestParam(required = false) String customerCode,
                                                          @RequestParam(defaultValue = "false") boolean openOnly) {
        return ResponseEntity.ok(claims.list(documentNumber, customerType, customerCode, openOnly));
    }

    @GetMapping("/claims/{claimNumber}")
    public ResponseEntity<ClaimDtos.ClaimView> get(@PathVariable String claimNumber) {
        return ResponseEntity.ok(claims.get(claimNumber));
    }

    /** Acceptation : avoir du montant accorde. */
    @PatchMapping("/claims/{claimNumber}/accept")
    public ResponseEntity<ClaimDtos.ClaimView> accept(@PathVariable String claimNumber,
                                                      @Valid @RequestBody(required = false) ClaimDtos.AcceptRequest request) {
        return ResponseEntity.ok(claims.accept(claimNumber, request));
    }

    @PatchMapping("/claims/{claimNumber}/reject")
    public ResponseEntity<ClaimDtos.ClaimView> reject(@PathVariable String claimNumber, @Valid @RequestBody ClaimDtos.RejectRequest request) {
        return ResponseEntity.ok(claims.reject(claimNumber, request.comment()));
    }

    @PatchMapping("/claims/{claimNumber}/withdraw")
    public ResponseEntity<ClaimDtos.ClaimView> withdraw(@PathVariable String claimNumber) {
        return ResponseEntity.ok(claims.withdraw(claimNumber));
    }

    // ------------------------------------------------------------------ motifs

    @GetMapping("/claim-reasons")
    public ResponseEntity<List<ClaimDtos.ReasonView>> reasons(@RequestParam(defaultValue = "false") boolean includeInactive) {
        return ResponseEntity.ok(claims.reasons(includeInactive));
    }

    /** Ajoute ou renomme un motif (le code ne change pas). */
    @PutMapping("/claim-reasons")
    @PreAuthorize(MANAGE_REASONS)
    public ResponseEntity<ClaimDtos.ReasonView> saveReason(@Valid @RequestBody ClaimDtos.ReasonRequest request,
                                                           @RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok(claims.saveReason(request, active));
    }
}
