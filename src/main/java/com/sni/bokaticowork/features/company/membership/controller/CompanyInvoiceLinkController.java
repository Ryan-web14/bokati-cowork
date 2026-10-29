package com.sni.bokaticowork.features.company.membership.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.company.membership.service.CompanyInvoiceLinkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Relier d'anciennes factures de membres a leur organisation · previsualiser, relier, retirer.
 */
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER')")
public class CompanyInvoiceLinkController {

    private final CompanyInvoiceLinkService service;

    /** Ce qui serait relie, et pourquoi le reste ne le serait pas · rien n'est ecrit. */
    @PostMapping(ApiPath.V1 + "/billing/documents/company-links/preview")
    public ResponseEntity<CompanyInvoiceLinkService.Result> preview(@Valid @RequestBody CompanyInvoiceLinkService.LinkRequest request) {
        return ResponseEntity.ok(service.preview(request));
    }

    @PostMapping(ApiPath.V1 + "/billing/documents/company-links")
    @Audited(module = "COMPANY", action = "INVOICE_ATTACH", ressource = "billing_document")
    public ResponseEntity<CompanyInvoiceLinkService.Result> link(@Valid @RequestBody CompanyInvoiceLinkService.LinkRequest request,
                                                                 Authentication authentication) {
        return ResponseEntity.ok(service.link(request, authentication == null ? "SYSTEM" : authentication.getName()));
    }

    @DeleteMapping(ApiPath.V1 + "/billing/documents/{documentNumber}/company-link")
    @Audited(module = "COMPANY", action = "INVOICE_DETACH", ressource = "billing_document")
    public ResponseEntity<Void> unlink(@PathVariable String documentNumber) {
        service.unlink(documentNumber);
        return ResponseEntity.noContent().build();
    }
}
