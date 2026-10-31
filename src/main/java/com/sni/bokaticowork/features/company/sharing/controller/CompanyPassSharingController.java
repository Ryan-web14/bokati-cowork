package com.sni.bokaticowork.features.company.sharing.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.company.sharing.dto.PassSharingDtos;
import com.sni.bokaticowork.features.company.sharing.service.CompanyPassSharingService;
import com.sni.bokaticowork.features.company.sharing.service.CompanySharingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Les passes d'une organisation partages avec ses collaborateurs · cote administration.
 */
@RestController
@RequestMapping(ApiPath.V1 + "/companies/{customerId}/passes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER')")
public class CompanyPassSharingController {

    private final CompanySharingService sharingService;
    private final CompanyPassSharingService service;

    @GetMapping
    public ResponseEntity<List<PassSharingDtos.PassSummary>> list(@PathVariable String customerId) {
        return ResponseEntity.ok(service.passes(sharingService.company(customerId)));
    }

    @GetMapping("/{passNumber}/beneficiaries")
    public ResponseEntity<PassSharingDtos.PassView> view(@PathVariable String customerId, @PathVariable String passNumber) {
        return ResponseEntity.ok(service.view(sharingService.company(customerId), passNumber));
    }

    @PostMapping("/{passNumber}/beneficiaries")
    @Audited(module = "COMPANY", action = "PASS_BENEFICIARY_ADD", ressource = "pass_beneficiary")
    public ResponseEntity<PassSharingDtos.PassView> add(@PathVariable String customerId, @PathVariable String passNumber,
                                                        @Valid @RequestBody PassSharingDtos.AddBeneficiaryRequest request,
                                                        Authentication authentication) {
        return ResponseEntity.ok(service.addBeneficiary(sharingService.company(customerId), passNumber, request,
                actor(authentication)));
    }

    @PostMapping("/{passNumber}/beneficiaries/{accessId}/revoke")
    @Audited(module = "COMPANY", action = "PASS_BENEFICIARY_REVOKE", ressource = "pass_beneficiary")
    public ResponseEntity<PassSharingDtos.RevokeResult> revoke(@PathVariable String customerId, @PathVariable String passNumber,
                                                               @PathVariable Long accessId,
                                                               @Valid @RequestBody(required = false) PassSharingDtos.RevokeBeneficiaryRequest request,
                                                               Authentication authentication) {
        return ResponseEntity.ok(service.revokeBeneficiary(sharingService.company(customerId), passNumber, accessId,
                request == null ? null : request.reason(), actor(authentication)));
    }

    private static String actor(Authentication authentication) {
        return authentication == null ? "SYSTEM" : authentication.getName();
    }
}
