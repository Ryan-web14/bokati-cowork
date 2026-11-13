package com.sni.bokaticowork.features.billing.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.billing.dto.WithholdingDtos;
import com.sni.bokaticowork.features.billing.service.implementation.WithholdingService;
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
 * Lot C6 : retenues a la source sur les reglements des clients (retenue de TVA, precompte) et
 * suivi des attestations. Les types de retenue se gerent avec le droit BILLING_TAX_RULES.
 */
@RestController
@RequestMapping(ApiPath.V1 + "/billing")
@RequiredArgsConstructor
public class WithholdingController {

    private static final String MANAGE_TYPES = "hasAuthority('BILLING:TAX_RULES')";

    private final WithholdingService withholdings;

    /** Retenue operee par le client sur une facture : solde la facture a hauteur de son montant. */
    @PostMapping("/documents/{documentNumber}/withholdings")
    public ResponseEntity<WithholdingDtos.WithholdingView> record(@PathVariable String documentNumber,
                                                                  @Valid @RequestBody WithholdingDtos.RecordRequest request) {
        return ResponseEntity.ok(withholdings.record(documentNumber, request));
    }

    @GetMapping("/documents/{documentNumber}/withholdings")
    public ResponseEntity<List<WithholdingDtos.WithholdingView>> ofDocument(@PathVariable String documentNumber) {
        return ResponseEntity.ok(withholdings.ofDocument(documentNumber));
    }

    /** Liste : {@code certificate=missing} pour les attestations attendues, ou par client. */
    @GetMapping("/withholdings")
    public ResponseEntity<List<WithholdingDtos.WithholdingView>> list(@RequestParam(required = false) String customerType,
                                                                      @RequestParam(required = false) String customerCode,
                                                                      @RequestParam(required = false) String certificate) {
        return ResponseEntity.ok(withholdings.list(customerType, customerCode, certificate));
    }

    @PatchMapping("/withholdings/{number}/certificate")
    public ResponseEntity<WithholdingDtos.WithholdingView> certificate(@PathVariable String number,
                                                                       @Valid @RequestBody WithholdingDtos.CertificateRequest request) {
        return ResponseEntity.ok(withholdings.certificate(number, request));
    }

    /** Annule une retenue saisie par erreur : la facture redevient due d'autant. */
    @PatchMapping("/withholdings/{number}/cancel")
    public ResponseEntity<WithholdingDtos.WithholdingView> cancel(@PathVariable String number,
                                                                  @Valid @RequestBody WithholdingDtos.CancelRequest request) {
        return ResponseEntity.ok(withholdings.cancel(number, request.reason()));
    }

    // ------------------------------------------------------------------ types

    @GetMapping("/withholding-types")
    public ResponseEntity<List<WithholdingDtos.TypeView>> types(@RequestParam(defaultValue = "false") boolean includeInactive) {
        return ResponseEntity.ok(withholdings.types(includeInactive));
    }

    @PostMapping("/withholding-types")
    @PreAuthorize(MANAGE_TYPES)
    public ResponseEntity<WithholdingDtos.TypeView> createType(@Valid @RequestBody WithholdingDtos.TypeRequest request) {
        return ResponseEntity.ok(withholdings.createType(request));
    }

    @PutMapping("/withholding-types/{code}")
    @PreAuthorize(MANAGE_TYPES)
    public ResponseEntity<WithholdingDtos.TypeView> updateType(@PathVariable String code,
                                                               @Valid @RequestBody WithholdingDtos.TypeUpdateRequest request) {
        return ResponseEntity.ok(withholdings.updateType(code, request));
    }

    @PatchMapping("/withholding-types/{code}/activate")
    @PreAuthorize(MANAGE_TYPES)
    public ResponseEntity<WithholdingDtos.TypeView> activateType(@PathVariable String code) {
        return ResponseEntity.ok(withholdings.setTypeActive(code, true));
    }

    @PatchMapping("/withholding-types/{code}/deactivate")
    @PreAuthorize(MANAGE_TYPES)
    public ResponseEntity<WithholdingDtos.TypeView> deactivateType(@PathVariable String code) {
        return ResponseEntity.ok(withholdings.setTypeActive(code, false));
    }
}
