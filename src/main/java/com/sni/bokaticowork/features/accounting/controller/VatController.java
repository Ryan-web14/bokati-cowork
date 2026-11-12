package com.sni.bokaticowork.features.accounting.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.accounting.model.AccVatReturn;
import com.sni.bokaticowork.features.accounting.service.implementation.VatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Fiscalite : declarations mensuelles de TVA (lot F11). */
@RestController
@RequestMapping(ApiPath.V1 + "/accounting/vat-returns")
@RequiredArgsConstructor
public class VatController {

    private final VatService vat;

    @GetMapping
    public ResponseEntity<List<AccVatReturn>> list() {
        return ResponseEntity.ok(vat.list());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccVatReturn> get(@PathVariable Long id) {
        return ResponseEntity.ok(vat.get(id));
    }

    @GetMapping("/{id}/detail")
    public ResponseEntity<VatService.Detail> detail(@PathVariable Long id) {
        return ResponseEntity.ok(vat.detail(id));
    }

    /** Prepare ou recalcule la declaration d'un mois ({@code period=AAAA-MM}). */
    @Audited(module = "ACCOUNTING", action = "VAT_RETURN_PREPARE", ressource = "acc_vat_return")
    @PostMapping
    public ResponseEntity<AccVatReturn> prepare(@RequestParam String period) {
        return ResponseEntity.ok(vat.prepare(period));
    }

    @Audited(module = "ACCOUNTING", action = "VAT_RETURN_VALIDATE", ressource = "acc_vat_return")
    @PatchMapping("/{id}/validate")
    public ResponseEntity<AccVatReturn> validate(@PathVariable Long id) {
        return ResponseEntity.ok(vat.validate(id));
    }

    @Audited(module = "ACCOUNTING", action = "VAT_RETURN_LIQUIDATE", ressource = "acc_vat_return")
    @PostMapping("/{id}/liquidate")
    public ResponseEntity<AccVatReturn> liquidate(@PathVariable Long id) {
        return ResponseEntity.ok(vat.liquidate(id));
    }

    @Audited(module = "ACCOUNTING", action = "VAT_RETURN_DELETE", ressource = "acc_vat_return")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        vat.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable Long id) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"tva-" + id + ".pdf\"").body(vat.pdf(id));
    }

    @GetMapping("/{id}/export")
    public ResponseEntity<byte[]> export(@PathVariable Long id) {
        return ResponseEntity.ok().contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"tva-" + id + ".csv\"").body(vat.csv(id));
    }
}
