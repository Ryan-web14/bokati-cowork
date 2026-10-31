package com.sni.bokaticowork.features.company.wallet.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.company.sharing.service.CompanySharingService;
import com.sni.bokaticowork.features.company.wallet.dto.CompanyWalletDtos;
import com.sni.bokaticowork.features.company.wallet.model.CompanyWalletSpend;
import com.sni.bokaticowork.features.company.wallet.service.CompanyWalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
 * Le portefeuille d'une organisation et les autorisations de depense · cote administration.
 */
@RestController
@RequestMapping(ApiPath.V1 + "/companies/{customerId}")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER')")
public class CompanyWalletController {

    private final CompanySharingService sharingService;
    private final CompanyWalletService service;

    @GetMapping("/wallet")
    public ResponseEntity<CompanyWalletDtos.WalletView> wallet(@PathVariable String customerId,
                                                               @RequestParam(required = false) String currency) {
        return ResponseEntity.ok(service.view(sharingService.company(customerId), currency));
    }

    @GetMapping("/wallet/spends")
    public ResponseEntity<List<CompanyWalletDtos.SpendView>> spends(@PathVariable String customerId,
                                                                    @RequestParam(required = false) String currency,
                                                                    @RequestParam(required = false) CompanyWalletSpend.Status status) {
        return ResponseEntity.ok(service.spends(sharingService.company(customerId), currency, status));
    }

    /** Especes recues a l'accueil ou virement constate · au-dela du seuil, une seconde personne valide. */
    @PostMapping("/wallet/top-ups")
    @Audited(module = "COMPANY", action = "COMPANY_WALLET_TOPUP", ressource = "wallet_account")
    public ResponseEntity<CompanyWalletDtos.AdminTopUpResult> topUp(@PathVariable String customerId,
                                                                    @Valid @RequestBody CompanyWalletDtos.AdminTopUpRequest request,
                                                                    Authentication authentication) {
        return ResponseEntity.ok(service.adminTopUp(sharingService.company(customerId), request, actor(authentication)));
    }

    @GetMapping("/spending-authorizations")
    public ResponseEntity<List<CompanyWalletDtos.AuthorizationView>> authorizations(@PathVariable String customerId) {
        return ResponseEntity.ok(service.authorizations(sharingService.company(customerId)));
    }

    @PostMapping("/spending-authorizations")
    @Audited(module = "COMPANY", action = "SPENDING_AUTHORIZATION_CREATE", ressource = "wallet_spending_authorization")
    public ResponseEntity<CompanyWalletDtos.AuthorizationView> authorize(@PathVariable String customerId,
                                                                         @Valid @RequestBody CompanyWalletDtos.AuthorizationRequest request,
                                                                         Authentication authentication) {
        return ResponseEntity.ok(service.authorize(sharingService.company(customerId), request, actor(authentication)));
    }

    @PatchMapping("/spending-authorizations/{authorizationId}")
    @Audited(module = "COMPANY", action = "SPENDING_AUTHORIZATION_UPDATE", ressource = "wallet_spending_authorization")
    public ResponseEntity<CompanyWalletDtos.AuthorizationView> update(@PathVariable String customerId,
                                                                      @PathVariable Long authorizationId,
                                                                      @Valid @RequestBody CompanyWalletDtos.AuthorizationUpdateRequest request,
                                                                      Authentication authentication) {
        return ResponseEntity.ok(service.updateAuthorization(sharingService.company(customerId), authorizationId, request,
                actor(authentication)));
    }

    private static String actor(Authentication authentication) {
        return authentication == null ? "SYSTEM" : authentication.getName();
    }
}
