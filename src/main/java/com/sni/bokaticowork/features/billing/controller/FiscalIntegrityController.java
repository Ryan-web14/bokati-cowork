package com.sni.bokaticowork.features.billing.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.billing.model.FiscalIntegrityReportLog;
import com.sni.bokaticowork.features.billing.service.fiscal.FiscalIntegrityCheckService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPath.V1 + "/admin/fiscal-integrity")
@RequiredArgsConstructor
public class FiscalIntegrityController {

    private final FiscalIntegrityCheckService integrityCheckService;

    @GetMapping("/check")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<FiscalIntegrityCheckService.FiscalIntegrityReport> check() {
        return ResponseEntity.ok(integrityCheckService.checkAndSave("MANUAL"));
    }

    @GetMapping("/last-report")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<FiscalIntegrityReportLog> lastReport() {
        return integrityCheckService.lastReport()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }
}
