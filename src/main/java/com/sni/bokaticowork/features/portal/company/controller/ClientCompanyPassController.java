package com.sni.bokaticowork.features.portal.company.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.client.customer.model.Customer;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.company.membership.service.CompanyMembershipService;
import com.sni.bokaticowork.features.company.sharing.dto.PassSharingDtos;
import com.sni.bokaticowork.features.company.sharing.service.CompanyPassSharingService;
import com.sni.bokaticowork.features.portal.context.ClientContextService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Les passes de l'organisation dans le portail · le gerant ouvre et retire les acces, chaque
 * collaborateur voit les passes auxquels il a acces.
 */
@RestController
@RequiredArgsConstructor
public class ClientCompanyPassController {

    private final ClientContextService clientContextService;
    private final CompanyMembershipService membershipService;
    private final CompanyPassSharingService service;
    private final com.sni.bokaticowork.features.portal.subscription.service.ClientCatalogService catalogService;

    /** Les passes de mes organisations auxquels j'ai acces. */
    @GetMapping(ApiPath.V1 + "/client/me/company-passes")
    public ResponseEntity<List<PassSharingDtos.MyPassAccess>> myAccesses() {
        return ResponseEntity.ok(service.myAccesses(clientContextService.getAuthenticatedMember()));
    }

    @GetMapping(ApiPath.V1 + "/client/companies/{customerId}/passes")
    public ResponseEntity<List<PassSharingDtos.PassSummary>> list(@PathVariable String customerId) {
        return ResponseEntity.ok(service.passes(owned(customerId)));
    }

    /**
     * Le gerant achete un pass du catalogue au nom de l'organisation · la facture est a elle, et se
     * regle par ses moyens (mobile money, especes, portefeuille de l'organisation).
     */
    @PostMapping(ApiPath.V1 + "/client/companies/{customerId}/passes/purchase")
    @Audited(module = "COMPANY", action = "COMPANY_PASS_PURCHASE_PORTAL", ressource = "subscription_pass")
    public ResponseEntity<com.sni.bokaticowork.features.subscription.subscription.dto.response.PassResponse> purchase(
            @PathVariable String customerId,
            @Valid @RequestBody com.sni.bokaticowork.features.portal.subscription.dto.request.ClientPurchasePassRequest request) {
        return ResponseEntity.ok(catalogService.purchasePassForCompany(owned(customerId), request));
    }

    @GetMapping(ApiPath.V1 + "/client/companies/{customerId}/passes/{passNumber}/beneficiaries")
    public ResponseEntity<PassSharingDtos.PassView> view(@PathVariable String customerId, @PathVariable String passNumber) {
        return ResponseEntity.ok(service.view(owned(customerId), passNumber));
    }

    @PostMapping(ApiPath.V1 + "/client/companies/{customerId}/passes/{passNumber}/beneficiaries")
    @Audited(module = "COMPANY", action = "PASS_BENEFICIARY_ADD_PORTAL", ressource = "pass_beneficiary")
    public ResponseEntity<PassSharingDtos.PassView> add(@PathVariable String customerId, @PathVariable String passNumber,
                                                        @Valid @RequestBody PassSharingDtos.AddBeneficiaryRequest request) {
        Member owner = clientContextService.getAuthenticatedMember();
        return ResponseEntity.ok(service.addBeneficiary(membershipService.ownedCompany(owner, customerId), passNumber,
                request, owner.getMemberId()));
    }

    @PostMapping(ApiPath.V1 + "/client/companies/{customerId}/passes/{passNumber}/beneficiaries/{accessId}/revoke")
    @Audited(module = "COMPANY", action = "PASS_BENEFICIARY_REVOKE_PORTAL", ressource = "pass_beneficiary")
    public ResponseEntity<PassSharingDtos.RevokeResult> revoke(@PathVariable String customerId, @PathVariable String passNumber,
                                                               @PathVariable Long accessId,
                                                               @Valid @RequestBody(required = false) PassSharingDtos.RevokeBeneficiaryRequest request) {
        Member owner = clientContextService.getAuthenticatedMember();
        return ResponseEntity.ok(service.revokeBeneficiary(membershipService.ownedCompany(owner, customerId), passNumber,
                accessId, request == null ? null : request.reason(), owner.getMemberId()));
    }

    private Customer owned(String customerId) {
        return membershipService.ownedCompany(clientContextService.getAuthenticatedMember(), customerId);
    }
}
