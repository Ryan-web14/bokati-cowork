package com.sni.bokaticowork.features.company.account.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.company.account.dto.CompanyDtos;
import com.sni.bokaticowork.features.company.account.service.CompanyAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
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

/**
 * Le centre de gestion des entreprises · fiche d'une organisation cliente, en un appel, mise a jour par bloc.
 */
@RestController
@RequestMapping(ApiPath.V1 + "/companies")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER')")
public class CompanyAccountController {

    private final CompanyAccountService service;

    @Audited(module = "COMPANY", action = "CREATE", ressource = "customer")
    @PostMapping
    public ResponseEntity<CompanyDtos.CompanyResponse> create(@Valid @RequestBody CompanyDtos.CreateCompanyRequest request,
                                                              Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request, actor(authentication)));
    }

    @GetMapping
    public ResponseEntity<PaginatedResponse<CompanyDtos.CompanySummary>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(service.search(q, status, pageable));
    }

    @GetMapping("/{customerId}")
    public ResponseEntity<CompanyDtos.CompanyResponse> get(@PathVariable String customerId) {
        return ResponseEntity.ok(service.get(customerId));
    }

    @Audited(module = "COMPANY", action = "UPDATE_LEGAL", ressource = "customer")
    @PatchMapping("/{customerId}/legal")
    public ResponseEntity<CompanyDtos.CompanyResponse> updateLegal(@PathVariable String customerId,
                                                                   @Valid @RequestBody CompanyDtos.UpdateLegalRequest request,
                                                                   Authentication authentication) {
        return ResponseEntity.ok(service.updateLegal(customerId, request, actor(authentication)));
    }

    @Audited(module = "COMPANY", action = "UPDATE_BILLING", ressource = "customer")
    @PatchMapping("/{customerId}/billing")
    public ResponseEntity<CompanyDtos.CompanyResponse> updateBilling(@PathVariable String customerId,
                                                                     @Valid @RequestBody CompanyDtos.UpdateBillingRequest request) {
        return ResponseEntity.ok(service.updateBilling(customerId, request));
    }

    @Audited(module = "COMPANY", action = "UPDATE_CONTACT", ressource = "customer")
    @PatchMapping("/{customerId}/contact")
    public ResponseEntity<CompanyDtos.CompanyResponse> updateContact(@PathVariable String customerId,
                                                                     @Valid @RequestBody CompanyDtos.UpdateContactRequest request) {
        return ResponseEntity.ok(service.updateContact(customerId, request));
    }

    @Audited(module = "COMPANY", action = "BLOCK", ressource = "customer")
    @PostMapping("/{customerId}/block")
    public ResponseEntity<CompanyDtos.CompanyResponse> block(@PathVariable String customerId,
                                                             @Valid @RequestBody CompanyDtos.BlockRequest request,
                                                             Authentication authentication) {
        return ResponseEntity.ok(service.block(customerId, request.reason(), actor(authentication)));
    }

    @Audited(module = "COMPANY", action = "UNBLOCK", ressource = "customer")
    @PostMapping("/{customerId}/unblock")
    public ResponseEntity<CompanyDtos.CompanyResponse> unblock(@PathVariable String customerId) {
        return ResponseEntity.ok(service.unblock(customerId));
    }

    @Audited(module = "COMPANY", action = "LINK_LEGAL_PROFILE", ressource = "customer")
    @PostMapping("/{customerId}/legal-profile")
    public ResponseEntity<CompanyDtos.CompanyResponse> linkLegalProfile(@PathVariable String customerId,
                                                                        @Valid @RequestBody CompanyDtos.LinkLegalProfileRequest request) {
        return ResponseEntity.ok(service.linkLegalProfile(customerId, request.businessCode()));
    }

    private static String actor(Authentication authentication) {
        return authentication == null ? "SYSTEM" : authentication.getName();
    }
}
