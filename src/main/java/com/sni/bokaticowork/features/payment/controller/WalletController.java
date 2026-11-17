package com.sni.bokaticowork.features.payment.controller;

import com.sni.bokaticowork.core.idempotency.aop.Idempotent;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.payment.dto.request.WalletLookupRequest;
import com.sni.bokaticowork.features.payment.dto.request.WalletTopUpRequest;
import com.sni.bokaticowork.features.payment.dto.response.WalletLedgerEntryResponse;
import com.sni.bokaticowork.features.payment.dto.response.WalletResponse;
import com.sni.bokaticowork.features.payment.service.interfaces.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPath.V1 + "/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;
    private final com.sni.bokaticowork.features.payment.control.service.WalletAdminActionService adminActionService;
    private final com.sni.bokaticowork.features.payment.service.support.PaymentOriginResolver paymentOrigin;

    @GetMapping
    public ResponseEntity<PaginatedResponse<WalletResponse>> list(
            @RequestParam(required = false) String ownerType,
            @RequestParam(required = false) String ownerCode,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(walletService.list(ownerType, ownerCode, pageable));
    }

    /**
     * Recharge administrative. Sous le seuil de double visa ({@code bokati.wallet.admin.four-eyes-threshold},
     * 100 000) : creditee aussitot, {@code 200} avec le portefeuille. Au-dela : une action TOPUP est
     * creee en attente du second visa du centre de controle, {@code 202} avec l'action (rien n'est credite
     * avant le visa). L'auteur est le compte connecte.
     */
    @Idempotent(operation = "WALLET_ADMIN_TOPUP", requestBodyArgIndex = 0)
    @PostMapping("/admin/top-up")
    public ResponseEntity<?> adminTopUp(@Valid @RequestBody WalletTopUpRequest request) {
        String actor = paymentOrigin.currentActor() != null ? paymentOrigin.currentActor() : request.createdBy();
        if (actor == null || actor.isBlank()) {
            throw new com.sni.bokaticowork.core.exception.customs.BadRequestException("Impossible d'identifier l'auteur de la recharge");
        }
        WalletTopUpRequest stamped = new WalletTopUpRequest(request.ownerType(), request.ownerCode(), request.currency(),
                request.amount(), actor, request.reference(), request.metadataJson());
        if (adminActionService.exceedsFourEyesThreshold(request.amount())) {
            WalletResponse wallet = walletService.getOrCreate(request.ownerType(), request.ownerCode(), request.currency());
            String reason = "Recharge administrative" + (request.reference() == null || request.reference().isBlank()
                    ? "" : " (" + request.reference().trim() + ")");
            var action = adminActionService.request(wallet.walletNumber(), new com.sni.bokaticowork.features.payment.control.service.WalletAdminActionService.ActionRequest(
                    com.sni.bokaticowork.features.payment.control.model.WalletAdminAction.Type.TOPUP, request.amount(),
                    request.reference(), reason, null), actor);
            return ResponseEntity.accepted().body(com.sni.bokaticowork.features.payment.control.dto.WalletControlDtos.AdminActionView.of(action));
        }
        return ResponseEntity.ok(walletService.adminTopUp(stamped));
    }

    @PostMapping("/admin/get-or-create")
    public ResponseEntity<WalletResponse> getOrCreate(@Valid @RequestBody WalletLookupRequest request) {
        return ResponseEntity.ok(walletService.getOrCreate(request.ownerType(), request.ownerCode(), request.currency()));
    }

    @GetMapping("/{walletNumber}")
    public ResponseEntity<WalletResponse> get(@PathVariable String walletNumber) {
        return ResponseEntity.ok(walletService.get(walletNumber));
    }

    @GetMapping("/{walletNumber}/ledger")
    public ResponseEntity<PaginatedResponse<WalletLedgerEntryResponse>> ledger(
            @PathVariable String walletNumber,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(walletService.ledger(walletNumber, pageable));
    }
}
