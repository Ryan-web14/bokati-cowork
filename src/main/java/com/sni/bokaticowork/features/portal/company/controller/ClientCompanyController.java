package com.sni.bokaticowork.features.portal.company.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.company.membership.dto.MembershipDtos;
import com.sni.bokaticowork.features.company.membership.service.CompanyMembershipService;
import com.sni.bokaticowork.features.portal.context.ClientContextService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Les organisations du membre connecte · et, pour le gerant, ce qu'il pilote.
 *
 * <p>Un employe voit ses organisations et son role ; seul le gerant voit les membres et regle le
 * mode de rattachement des factures. Une organisation dont le membre n'est pas gerant repond 404,
 * sans dire qu'elle existe.</p>
 */
@RestController
@RequestMapping(ApiPath.V1 + "/client/companies")
@RequiredArgsConstructor
public class ClientCompanyController {

    private final ClientContextService clientContextService;
    private final CompanyMembershipService service;

    @GetMapping
    public ResponseEntity<List<MembershipDtos.MyCompany>> myCompanies() {
        return ResponseEntity.ok(service.myCompanies(clientContextService.getAuthenticatedMember()));
    }

    @GetMapping("/{customerId}/members")
    public ResponseEntity<List<MembershipDtos.LinkResponse>> members(@PathVariable String customerId) {
        return ResponseEntity.ok(service.membersForOwner(clientContextService.getAuthenticatedMember(), customerId));
    }

    @PatchMapping("/{customerId}/invoice-link-mode")
    @Audited(module = "COMPANY", action = "INVOICE_LINK_MODE_PORTAL", ressource = "customer")
    public ResponseEntity<MembershipDtos.MyCompany> setDefaultMode(@PathVariable String customerId,
                                                                   @Valid @RequestBody MembershipDtos.CompanyModeRequest request) {
        return ResponseEntity.ok(service.setCompanyDefaultMode(clientContextService.getAuthenticatedMember(), customerId, request.mode()));
    }

    @PatchMapping("/{customerId}/members/{linkId}/invoice-link-mode")
    @Audited(module = "COMPANY", action = "INVOICE_LINK_MODE_PORTAL", ressource = "customer_member_link")
    public ResponseEntity<MembershipDtos.LinkResponse> setLinkMode(@PathVariable String customerId, @PathVariable Long linkId,
                                                                   @Valid @RequestBody MembershipDtos.LinkModeRequest request) {
        return ResponseEntity.ok(service.setLinkMode(clientContextService.getAuthenticatedMember(), customerId, linkId, request));
    }
}
