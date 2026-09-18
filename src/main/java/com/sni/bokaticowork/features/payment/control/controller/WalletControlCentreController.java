package com.sni.bokaticowork.features.payment.control.controller;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.payment.control.dto.WalletControlDtos.AdminActionRequest;
import com.sni.bokaticowork.features.payment.control.dto.WalletControlDtos.AdminActionView;
import com.sni.bokaticowork.features.payment.control.dto.WalletControlDtos.FlagResolveRequest;
import com.sni.bokaticowork.features.payment.control.dto.WalletControlDtos.FlagReviewRequest;
import com.sni.bokaticowork.features.payment.control.dto.WalletControlDtos.FlagView;
import com.sni.bokaticowork.features.payment.control.dto.WalletControlDtos.RejectRequest;
import com.sni.bokaticowork.features.payment.control.dto.WalletControlDtos.TreasuryRequest;
import com.sni.bokaticowork.features.payment.control.model.WalletRiskFlag;
import com.sni.bokaticowork.features.payment.control.model.WalletTreasuryReconciliation;
import com.sni.bokaticowork.features.payment.control.service.WalletAdminActionService;
import com.sni.bokaticowork.features.payment.control.service.WalletControlCentreService;
import com.sni.bokaticowork.features.payment.control.service.WalletRiskFlagService;
import com.sni.bokaticowork.features.payment.control.service.WalletTreasuryReconciliationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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

import java.time.LocalDate;
import java.util.List;

/**
 * Le centre de controle du portefeuille.
 *
 * <p>Un ecran, et les droits qui vont avec. Chaque action porte le nom de qui l'a demandee, tire
 * de l'authentification et jamais du corps de la requete : une action « au nom de » n'existe pas
 * ici, c'est ce qui donne un sens au second visa.</p>
 */
@RestController
@RequestMapping(ApiPath.V1 + "/wallets/control")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
public class WalletControlCentreController {

    private final WalletControlCentreService controlCentreService;
    private final WalletAdminActionService adminActionService;
    private final WalletRiskFlagService flagService;
    private final WalletTreasuryReconciliationService treasuryService;
    private final com.sni.bokaticowork.features.payment.repository.WalletAccountRepository walletRepository;

    // -------------------------------------------------------------------------------------
    // Vue d'ensemble
    // -------------------------------------------------------------------------------------

    @GetMapping("/dashboard")
    public ResponseEntity<WalletControlCentreService.Dashboard> dashboard(
            @RequestParam(required = false) String currency) {
        return ResponseEntity.ok(controlCentreService.dashboard(currency));
    }

    @GetMapping(value = "/export", produces = "text/csv")
    public ResponseEntity<byte[]> export(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        byte[] csv = controlCentreService.accountingExport(from, to);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"portefeuille-" + from + "-" + to + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }

    // -------------------------------------------------------------------------------------
    // Actions administratives
    // -------------------------------------------------------------------------------------

    @PostMapping("/wallets/{walletNumber}/actions")
    public ResponseEntity<AdminActionView> request(@PathVariable String walletNumber,
                                                   @Valid @RequestBody AdminActionRequest request,
                                                   Authentication authentication) {
        return ResponseEntity.ok(AdminActionView.of(adminActionService.request(walletNumber,
                new WalletAdminActionService.ActionRequest(request.type(), request.amount(), request.reference(),
                        request.reason(), request.until()),
                actor(authentication))));
    }

    @GetMapping("/wallets/{walletNumber}/actions")
    public ResponseEntity<PaginatedResponse<AdminActionView>> history(@PathVariable String walletNumber,
                                                                      @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(new PaginatedResponse<>(
                adminActionService.history(walletNumber, unsorted(pageable)).map(AdminActionView::of)));
    }

    @GetMapping("/actions/pending")
    public ResponseEntity<PaginatedResponse<AdminActionView>> pending(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(new PaginatedResponse<>(
                adminActionService.pendingApprovals(unsorted(pageable)).map(AdminActionView::of)));
    }

    @PostMapping("/actions/{actionNumber}/approve")
    public ResponseEntity<AdminActionView> approve(@PathVariable String actionNumber, Authentication authentication) {
        return ResponseEntity.ok(AdminActionView.of(adminActionService.approve(actionNumber, actor(authentication))));
    }

    @PostMapping("/actions/{actionNumber}/reject")
    public ResponseEntity<AdminActionView> reject(@PathVariable String actionNumber,
                                                  @Valid @RequestBody RejectRequest request,
                                                  Authentication authentication) {
        return ResponseEntity.ok(AdminActionView.of(
                adminActionService.reject(actionNumber, actor(authentication), request.reason())));
    }

    // -------------------------------------------------------------------------------------
    // Signalements
    // -------------------------------------------------------------------------------------

    @GetMapping("/flags")
    public ResponseEntity<PaginatedResponse<FlagView>> flags(
            @RequestParam(required = false) List<WalletRiskFlag.Status> status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(new PaginatedResponse<>(flagService.search(status, unsorted(pageable)).map(FlagView::of)));
    }

    @GetMapping("/wallets/{walletNumber}/flags")
    public ResponseEntity<PaginatedResponse<FlagView>> walletFlags(@PathVariable String walletNumber,
                                                                   @PageableDefault(size = 20) Pageable pageable) {
        Long walletId = walletRepository.findByWalletNumber(walletNumber)
                .orElseThrow(() -> new com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException("Portefeuille introuvable"))
                .getId();
        return ResponseEntity.ok(new PaginatedResponse<>(flagService.forWallet(walletId, unsorted(pageable)).map(FlagView::of)));
    }

    @PostMapping("/flags/{flagNumber}/review")
    public ResponseEntity<FlagView> review(@PathVariable String flagNumber,
                                           @RequestBody(required = false) FlagReviewRequest request,
                                           Authentication authentication) {
        boolean hold = request != null && request.holdWallet();
        return ResponseEntity.ok(FlagView.of(flagService.takeUnderReview(flagNumber, actor(authentication), hold)));
    }

    @PostMapping("/flags/{flagNumber}/resolve")
    public ResponseEntity<FlagView> resolve(@PathVariable String flagNumber,
                                            @Valid @RequestBody FlagResolveRequest request,
                                            Authentication authentication) {
        return ResponseEntity.ok(FlagView.of(
                flagService.resolve(flagNumber, actor(authentication), request.confirmed(), request.resolution())));
    }

    // -------------------------------------------------------------------------------------
    // Tresorerie
    // -------------------------------------------------------------------------------------

    @GetMapping("/treasury/snapshot")
    public ResponseEntity<WalletTreasuryReconciliationService.Snapshot> snapshot(
            @RequestParam(required = false, defaultValue = "XAF") String currency) {
        return ResponseEntity.ok(treasuryService.snapshot(currency.trim().toUpperCase()));
    }

    @PostMapping("/treasury/reconciliations")
    public ResponseEntity<WalletTreasuryReconciliation> reconcile(@Valid @RequestBody TreasuryRequest request,
                                                                  Authentication authentication) {
        return ResponseEntity.ok(treasuryService.reconcile(
                new WalletTreasuryReconciliationService.Inputs(request.date(), request.currency(),
                        request.ledgerAccountBalance(), request.availableCash(), request.explanation()),
                actor(authentication)));
    }

    @GetMapping("/treasury/reconciliations")
    public ResponseEntity<PaginatedResponse<WalletTreasuryReconciliation>> reconciliations(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(new PaginatedResponse<>(treasuryService.history(unsorted(pageable))));
    }

    @GetMapping("/treasury/reconciliations/{number}")
    public ResponseEntity<WalletTreasuryReconciliation> reconciliation(@PathVariable String number) {
        return ResponseEntity.ok(treasuryService.get(number));
    }

    // -------------------------------------------------------------------------------------

    private String actor(Authentication authentication) {
        return authentication == null || authentication.getName() == null ? "ADMIN" : authentication.getName();
    }

    private Pageable unsorted(Pageable pageable) {
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
    }
}
