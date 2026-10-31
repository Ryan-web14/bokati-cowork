package com.sni.bokaticowork.features.portal.company.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.client.customer.model.Customer;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.company.membership.service.CompanyMembershipService;
import com.sni.bokaticowork.features.company.wallet.dto.CompanyWalletDtos;
import com.sni.bokaticowork.features.company.wallet.model.CompanyWalletSpend;
import com.sni.bokaticowork.features.company.wallet.service.CompanyWalletService;
import com.sni.bokaticowork.features.payment.dto.response.MobileMoneyDepositResponse;
import com.sni.bokaticowork.features.payment.security.service.WalletSecurityService;
import com.sni.bokaticowork.features.payment.transfer.dto.WalletUsageDtos;
import com.sni.bokaticowork.features.portal.context.ClientContextService;
import com.sni.bokaticowork.features.portal.wallet.dto.SetWalletPinRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Le portefeuille de l'organisation dans le portail.
 *
 * <p>Le gerant voit le solde et les mouvements, recharge, autorise ses collaborateurs et valide
 * leurs depenses au-dela du seuil. Un collaborateur autorise regle une facture de l'organisation
 * et ne voit que ce qu'il a lui-meme depense.</p>
 */
@RestController
@RequiredArgsConstructor
public class ClientCompanyWalletController {

    private final ClientContextService clientContextService;
    private final CompanyMembershipService membershipService;
    private final CompanyWalletService service;
    private final WalletSecurityService securityService;

    // ── Gerant ───────────────────────────────────────────────────────────────

    @GetMapping(ApiPath.V1 + "/client/companies/{customerId}/wallet")
    public ResponseEntity<CompanyWalletDtos.WalletView> wallet(@PathVariable String customerId,
                                                               @RequestParam(required = false) String currency) {
        return ResponseEntity.ok(service.view(owned(customerId), currency));
    }

    @GetMapping(ApiPath.V1 + "/client/companies/{customerId}/wallet/spends")
    public ResponseEntity<List<CompanyWalletDtos.SpendView>> spends(@PathVariable String customerId,
                                                                    @RequestParam(required = false) String currency,
                                                                    @RequestParam(required = false) CompanyWalletSpend.Status status) {
        return ResponseEntity.ok(service.spends(owned(customerId), currency, status));
    }

    /** Recharge par mobile money · un recu, pas une facture. */
    @PostMapping(ApiPath.V1 + "/client/companies/{customerId}/wallet/top-ups")
    @Audited(module = "COMPANY", action = "COMPANY_WALLET_TOPUP_PORTAL", ressource = "wallet_account")
    public ResponseEntity<MobileMoneyDepositResponse> topUp(@PathVariable String customerId,
                                                            @Valid @RequestBody WalletUsageDtos.TopUpRequest request) {
        Member owner = clientContextService.getAuthenticatedMember();
        return ResponseEntity.ok(service.ownerTopUp(membershipService.ownedCompany(owner, customerId), owner, request));
    }

    /** Le code secret du portefeuille de l'organisation · celui que le gerant donne pour payer. */
    @PostMapping(ApiPath.V1 + "/client/companies/{customerId}/wallet/security/pin")
    @Audited(module = "COMPANY", action = "COMPANY_WALLET_PIN", ressource = "wallet_account")
    public ResponseEntity<Void> setPin(@PathVariable String customerId, @Valid @RequestBody SetWalletPinRequest request) {
        securityService.setPin(service.wallet(owned(customerId), null), request.currentPin(), request.newPin());
        return ResponseEntity.noContent().build();
    }

    /** Le gerant regle une facture de l'organisation avec son portefeuille. */
    @PostMapping(ApiPath.V1 + "/client/companies/{customerId}/invoices/{documentNumber}/pay/wallet")
    @Audited(module = "COMPANY", action = "COMPANY_WALLET_PAY_OWNER", ressource = "billing_document")
    public ResponseEntity<CompanyWalletDtos.SpendResult> payAsOwner(@PathVariable String customerId,
                                                                    @PathVariable String documentNumber,
                                                                    @RequestBody(required = false) CompanyWalletDtos.PayRequest request) {
        Member owner = clientContextService.getAuthenticatedMember();
        return ResponseEntity.ok(service.payAsOwner(membershipService.ownedCompany(owner, customerId), owner, documentNumber,
                request == null ? null : request.pin()));
    }

    @GetMapping(ApiPath.V1 + "/client/companies/{customerId}/spending-authorizations")
    public ResponseEntity<List<CompanyWalletDtos.AuthorizationView>> authorizations(@PathVariable String customerId) {
        return ResponseEntity.ok(service.authorizations(owned(customerId)));
    }

    @PostMapping(ApiPath.V1 + "/client/companies/{customerId}/spending-authorizations")
    @Audited(module = "COMPANY", action = "SPENDING_AUTHORIZATION_CREATE_PORTAL", ressource = "wallet_spending_authorization")
    public ResponseEntity<CompanyWalletDtos.AuthorizationView> authorize(@PathVariable String customerId,
                                                                         @Valid @RequestBody CompanyWalletDtos.AuthorizationRequest request) {
        Member owner = clientContextService.getAuthenticatedMember();
        return ResponseEntity.ok(service.authorize(membershipService.ownedCompany(owner, customerId), request, owner.getMemberId()));
    }

    @PatchMapping(ApiPath.V1 + "/client/companies/{customerId}/spending-authorizations/{authorizationId}")
    @Audited(module = "COMPANY", action = "SPENDING_AUTHORIZATION_UPDATE_PORTAL", ressource = "wallet_spending_authorization")
    public ResponseEntity<CompanyWalletDtos.AuthorizationView> update(@PathVariable String customerId,
                                                                      @PathVariable Long authorizationId,
                                                                      @Valid @RequestBody CompanyWalletDtos.AuthorizationUpdateRequest request) {
        Member owner = clientContextService.getAuthenticatedMember();
        return ResponseEntity.ok(service.updateAuthorization(membershipService.ownedCompany(owner, customerId), authorizationId,
                request, owner.getMemberId()));
    }

    @GetMapping(ApiPath.V1 + "/client/companies/{customerId}/approvals")
    public ResponseEntity<List<CompanyWalletDtos.SpendView>> approvals(@PathVariable String customerId) {
        return ResponseEntity.ok(service.pendingApprovals(owned(customerId)));
    }

    @PostMapping(ApiPath.V1 + "/client/companies/{customerId}/approvals/{spendId}/approve")
    @Audited(module = "COMPANY", action = "COMPANY_SPEND_APPROVE", ressource = "wallet_company_spend")
    public ResponseEntity<CompanyWalletDtos.SpendResult> approve(@PathVariable String customerId, @PathVariable Long spendId) {
        Member owner = clientContextService.getAuthenticatedMember();
        return ResponseEntity.ok(service.approve(membershipService.ownedCompany(owner, customerId), owner, spendId));
    }

    @PostMapping(ApiPath.V1 + "/client/companies/{customerId}/approvals/{spendId}/reject")
    @Audited(module = "COMPANY", action = "COMPANY_SPEND_REJECT", ressource = "wallet_company_spend")
    public ResponseEntity<CompanyWalletDtos.SpendResult> reject(@PathVariable String customerId, @PathVariable Long spendId,
                                                                @Valid @RequestBody(required = false) CompanyWalletDtos.DecisionRequest request) {
        Member owner = clientContextService.getAuthenticatedMember();
        return ResponseEntity.ok(service.reject(membershipService.ownedCompany(owner, customerId), owner, spendId,
                request == null ? null : request.reason()));
    }

    // ── Collaborateur ────────────────────────────────────────────────────────

    /** Mes autorisations sur les portefeuilles de mes organisations, et ce que j'y ai depense. */
    @GetMapping(ApiPath.V1 + "/client/me/company-wallets")
    public ResponseEntity<List<CompanyWalletDtos.MyCompanyWallet>> myWallets() {
        return ResponseEntity.ok(service.myWallets(clientContextService.getAuthenticatedMember()));
    }

    /** Regle une facture de l'organisation, ou reliee a elle, avec son portefeuille. */
    @PostMapping(ApiPath.V1 + "/client/me/company-wallets/{customerId}/invoices/{documentNumber}/pay")
    @Audited(module = "COMPANY", action = "COMPANY_WALLET_PAY_COLLABORATOR", ressource = "billing_document")
    public ResponseEntity<CompanyWalletDtos.SpendResult> pay(@PathVariable String customerId, @PathVariable String documentNumber) {
        return ResponseEntity.ok(service.payAsCollaborator(clientContextService.getAuthenticatedMember(), customerId, documentNumber));
    }

    @PostMapping(ApiPath.V1 + "/client/me/company-wallets/{customerId}/spends/{spendId}/cancel")
    @Audited(module = "COMPANY", action = "COMPANY_SPEND_CANCEL", ressource = "wallet_company_spend")
    public ResponseEntity<CompanyWalletDtos.SpendResult> cancel(@PathVariable String customerId, @PathVariable Long spendId) {
        return ResponseEntity.ok(service.cancelOwn(clientContextService.getAuthenticatedMember(), customerId, spendId));
    }

    private Customer owned(String customerId) {
        return membershipService.ownedCompany(clientContextService.getAuthenticatedMember(), customerId);
    }
}
