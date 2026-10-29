package com.sni.bokaticowork.features.company.membership.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.company.enums.InvoiceLinkMode;
import com.sni.bokaticowork.features.company.membership.dto.MembershipDtos;
import com.sni.bokaticowork.features.company.membership.service.CompanyMembershipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Le rattachement des membres aux organisations, cote administration.
 */
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER')")
public class CompanyMembershipController {

    private final CompanyMembershipService service;

    /** Les membres rattaches · actifs par defaut, {@code status=ENDED} ou {@code ALL} pour l'historique. */
    @GetMapping(ApiPath.V1 + "/customers/{customerId}/members")
    public ResponseEntity<List<MembershipDtos.LinkResponse>> list(@PathVariable String customerId,
                                                                  @RequestParam(required = false) String status) {
        return ResponseEntity.ok(service.list(customerId, status));
    }

    @PostMapping(ApiPath.V1 + "/customers/{customerId}/members")
    @Audited(module = "COMPANY", action = "MEMBER_LINK", ressource = "customer_member_link")
    public ResponseEntity<MembershipDtos.LinkResponse> link(@PathVariable String customerId,
                                                            @Valid @RequestBody MembershipDtos.LinkMemberRequest request,
                                                            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.link(customerId, request, actor(authentication)));
    }

    @PatchMapping(ApiPath.V1 + "/customers/{customerId}/members/{linkId}")
    @Audited(module = "COMPANY", action = "MEMBER_LINK_UPDATE", ressource = "customer_member_link")
    public ResponseEntity<MembershipDtos.LinkResponse> update(@PathVariable String customerId, @PathVariable Long linkId,
                                                              @Valid @RequestBody MembershipDtos.UpdateLinkRequest request) {
        return ResponseEntity.ok(service.update(customerId, linkId, request));
    }

    @PostMapping(ApiPath.V1 + "/customers/{customerId}/members/{linkId}/end")
    @Audited(module = "COMPANY", action = "MEMBER_LINK_END", ressource = "customer_member_link")
    public ResponseEntity<MembershipDtos.LinkResponse> end(@PathVariable String customerId, @PathVariable Long linkId,
                                                           @Valid @RequestBody MembershipDtos.EndLinkRequest request,
                                                           Authentication authentication) {
        return ResponseEntity.ok(service.end(customerId, linkId, request.reason(), actor(authentication)));
    }

    @GetMapping(ApiPath.V1 + "/members/{memberId}/companies")
    public ResponseEntity<List<MembershipDtos.LinkResponse>> companiesOf(@PathVariable String memberId) {
        return ResponseEntity.ok(service.companiesOf(memberId));
    }

    /** Le mode de rattachement des factures par defaut de l'organisation. */
    @PatchMapping(ApiPath.V1 + "/companies/{customerId}/invoice-link-mode")
    @Audited(module = "COMPANY", action = "INVOICE_LINK_MODE", ressource = "customer")
    public ResponseEntity<InvoiceLinkMode> setDefaultMode(@PathVariable String customerId,
                                                          @Valid @RequestBody MembershipDtos.CompanyModeRequest request) {
        return ResponseEntity.ok(service.setCompanyDefaultModeAsAdmin(customerId, request.mode()));
    }

    private static String actor(Authentication authentication) {
        return authentication == null ? "SYSTEM" : authentication.getName();
    }
}
