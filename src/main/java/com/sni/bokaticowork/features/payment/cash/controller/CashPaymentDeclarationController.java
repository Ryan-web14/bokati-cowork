package com.sni.bokaticowork.features.payment.cash.controller;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.payment.cash.dto.request.CloseCashDeclarationRequest;
import com.sni.bokaticowork.features.payment.cash.dto.request.ConfirmCashPaymentRequest;
import com.sni.bokaticowork.features.payment.cash.dto.response.CashPaymentDeclarationResponse;
import com.sni.bokaticowork.features.payment.cash.service.CashPaymentDeclarationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * La file de la caisse · ce que des clients ont annonce vouloir regler en especes.
 *
 * <p>Confirmer, c est constater que les billets sont la. Tant que personne ne l a fait, rien n est
 * encaisse · la facture reste due et la reservation reste en attente.</p>
 */
@RestController
@RequestMapping(ApiPath.V1 + "/payments/cash-declarations")
@RequiredArgsConstructor
public class CashPaymentDeclarationController {

    private static final String DESK = "hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER') or hasRole('CASHIER')";

    private final CashPaymentDeclarationService declarationService;

    /** La file · {@code status=AWAITING_CONFIRMATION} donne ce qui reste a encaisser. */
    @GetMapping
    @PreAuthorize(DESK)
    public ResponseEntity<PaginatedResponse<CashPaymentDeclarationResponse>> list(
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(declarationService.list(status, pageable));
    }

    @GetMapping("/{declarationNumber}")
    @PreAuthorize(DESK)
    public ResponseEntity<CashPaymentDeclarationResponse> get(@PathVariable String declarationNumber) {
        return ResponseEntity.ok(declarationService.get(declarationNumber));
    }

    /**
     * Les billets sont comptes · c est maintenant, et seulement maintenant, que le paiement existe.
     *
     * <p>La session de caisse est exigee : un encaissement qui n atterrit dans aucune caisse rend
     * le comptage du soir faux et ne laisse personne responsable.</p>
     */
    @PostMapping("/{declarationNumber}/confirm")
    @PreAuthorize(DESK)
    public ResponseEntity<CashPaymentDeclarationResponse> confirm(
            @PathVariable String declarationNumber,
            @Valid @RequestBody ConfirmCashPaymentRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(declarationService.confirm(declarationNumber, request, actor(authentication)));
    }

    /** Le client ne viendra pas · on retire la ligne de la file, la facture reste due. */
    @PostMapping("/{declarationNumber}/cancel")
    @PreAuthorize(DESK)
    public ResponseEntity<CashPaymentDeclarationResponse> cancel(
            @PathVariable String declarationNumber,
            @RequestBody(required = false) CloseCashDeclarationRequest request,
            Authentication authentication) {
        String reason = request == null || request.reason() == null
                ? "Annulée par la caisse"
                : request.reason();
        return ResponseEntity.ok(declarationService.cancel(declarationNumber, reason, actor(authentication)));
    }

    private String actor(Authentication authentication) {
        return authentication == null ? "SYSTEM" : authentication.getName();
    }
}
