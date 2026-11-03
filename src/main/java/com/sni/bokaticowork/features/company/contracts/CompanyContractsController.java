package com.sni.bokaticowork.features.company.contracts;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.company.membership.service.CompanyMembershipService;
import com.sni.bokaticowork.features.company.sharing.service.CompanySharingService;
import com.sni.bokaticowork.features.portal.context.ClientContextService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Les contrats d'une organisation · l'administration, et son gerant depuis le portail.
 */
@RestController
@RequiredArgsConstructor
public class CompanyContractsController {

    private final CompanyContractsService service;
    private final CompanySharingService sharingService;
    private final CompanyMembershipService membershipService;
    private final ClientContextService clientContextService;

    @GetMapping(ApiPath.V1 + "/companies/{customerId}/contracts")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<CompanyContractsService.CompanyContracts> list(@PathVariable String customerId) {
        return ResponseEntity.ok(service.list(sharingService.company(customerId)));
    }

    @GetMapping(ApiPath.V1 + "/companies/{customerId}/contracts/{contractCode}/pdf")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<byte[]> pdf(@PathVariable String customerId, @PathVariable String contractCode) {
        return pdf(contractCode, service.document(sharingService.company(customerId), contractCode));
    }

    @GetMapping(ApiPath.V1 + "/client/companies/{customerId}/contracts")
    public ResponseEntity<CompanyContractsService.CompanyContracts> ownerList(@PathVariable String customerId) {
        return ResponseEntity.ok(service.list(membershipService.ownedCompany(clientContextService.getAuthenticatedMember(), customerId)));
    }

    @GetMapping(ApiPath.V1 + "/client/companies/{customerId}/contracts/{contractCode}/pdf")
    public ResponseEntity<byte[]> ownerPdf(@PathVariable String customerId, @PathVariable String contractCode) {
        return pdf(contractCode, service.document(
                membershipService.ownedCompany(clientContextService.getAuthenticatedMember(), customerId), contractCode));
    }

    private static ResponseEntity<byte[]> pdf(String contractCode, byte[] bytes) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + contractCode + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(bytes);
    }
}
