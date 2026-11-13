package com.sni.bokaticowork.features.accounting.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.accounting.model.AccAccount;
import com.sni.bokaticowork.features.accounting.service.implementation.ClosingService;
import com.sni.bokaticowork.features.accounting.service.implementation.FixedAssetReportService;
import com.sni.bokaticowork.features.accounting.service.implementation.LiasseService;
import com.sni.bokaticowork.features.accounting.service.implementation.ManagementReportService;
import com.sni.bokaticowork.features.accounting.service.implementation.TaxResultService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Liasse fiscale, resultat fiscal et etats de gestion (lot F15). Les droits sont controles par les services. */
@RestController
@RequestMapping(ApiPath.V1)
@RequiredArgsConstructor
public class ManagementController {

    private final ManagementReportService management;
    private final TaxResultService taxes;
    private final LiasseService liasse;
    private final FixedAssetReportService exporter;

    public record RateRequest(BigDecimal rate) {
    }

    // ------------------------------------------------------------------ etats de gestion

    @GetMapping("/accounting/management/sig")
    public ResponseEntity<?> sig(@RequestParam(required = false) String year, @RequestParam(required = false, defaultValue = "json") String format) {
        ManagementReportService.Sig sig = management.sig(year);
        return "json".equalsIgnoreCase(format) ? ResponseEntity.ok(sig) : table(management.sigTable(sig), format);
    }

    @GetMapping("/accounting/management/dashboard")
    public ResponseEntity<ManagementReportService.Dashboard> dashboard(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(management.dashboard(date));
    }

    @GetMapping("/accounting/management/ratios")
    public ResponseEntity<List<ManagementReportService.Ratio>> ratios(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(management.ratios(date));
    }

    @GetMapping("/accounting/management/cash-forecast")
    public ResponseEntity<?> forecast(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                      @RequestParam(required = false, defaultValue = "WEEKS") String horizon,
                                      @RequestParam(required = false, defaultValue = "json") String format) {
        ManagementReportService.Forecast forecast = management.forecast(date, horizon);
        return "json".equalsIgnoreCase(format) ? ResponseEntity.ok(forecast) : table(management.forecastTable(forecast), format);
    }

    // ------------------------------------------------------------------ resultat fiscal

    @GetMapping("/accounting/tax-result/{year}")
    public ResponseEntity<?> taxResult(@PathVariable String year, @RequestParam(required = false, defaultValue = "json") String format) {
        TaxResultService.TaxResult result = taxes.compute(year);
        return "json".equalsIgnoreCase(format) ? ResponseEntity.ok(result) : table(taxes.table(result), format);
    }

    @Audited(module = "ACCOUNTING", action = "TAX_ADJUSTMENT_ADD", ressource = "acc_tax_adjustment")
    @PostMapping("/accounting/tax-result/{year}/adjustments")
    public ResponseEntity<TaxResultService.Adjustment> addAdjustment(@PathVariable String year, @RequestBody TaxResultService.AdjustmentRequest request) {
        return ResponseEntity.ok(taxes.add(year, request));
    }

    @Audited(module = "ACCOUNTING", action = "TAX_ADJUSTMENT_DELETE", ressource = "acc_tax_adjustment")
    @DeleteMapping("/accounting/tax-result/adjustments/{id}")
    public ResponseEntity<Void> deleteAdjustment(@PathVariable Long id) {
        taxes.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Audited(module = "ACCOUNTING", action = "TAX_RESULT_SAVE", ressource = "acc_tax_result")
    @PostMapping("/accounting/tax-result/{year}/save")
    public ResponseEntity<TaxResultService.TaxResult> saveTaxResult(@PathVariable String year) {
        return ResponseEntity.ok(taxes.save(year));
    }

    @Audited(module = "ACCOUNTING", action = "TAX_RESULT_POST", ressource = "acc_tax_result")
    @PostMapping("/accounting/tax-result/{year}/post")
    public ResponseEntity<TaxResultService.TaxResult> postTax(@PathVariable String year) {
        return ResponseEntity.ok(taxes.post(year));
    }

    @Audited(module = "ACCOUNTING", action = "ACCOUNT_NON_DEDUCTIBLE", ressource = "acc_account")
    @PatchMapping("/accounting/accounts/{number}/non-deductible")
    public ResponseEntity<Map<String, Object>> nonDeductible(@PathVariable String number, @RequestBody RateRequest request) {
        AccAccount account = taxes.nonDeductible(number, request == null ? null : request.rate());
        return ResponseEntity.ok(Map.of("accountNumber", account.getNumber(), "nonDeductibleRate",
                account.getNonDeductibleRate() == null ? BigDecimal.ZERO : account.getNonDeductibleRate()));
    }

    // ------------------------------------------------------------------ liasse

    @GetMapping("/accounting/liasse/{year}")
    public ResponseEntity<?> liasse(@PathVariable String year, @RequestParam(required = false, defaultValue = "json") String format) {
        LiasseService.Liasse built = liasse.build(year);
        return switch (format.toLowerCase(Locale.ROOT)) {
            case "pdf" -> ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"liasse-" + year + ".pdf\"")
                    .contentType(MediaType.APPLICATION_PDF).body(liasse.pdf(built));
            case "xls", "excel" -> ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"liasse-" + year + ".xls\"")
                    .contentType(MediaType.parseMediaType("application/vnd.ms-excel")).body(liasse.excel(built));
            default -> ResponseEntity.ok(built);
        };
    }

    @Audited(module = "ACCOUNTING", action = "LIASSE_FINALIZE", ressource = "acc_generated_report")
    @PostMapping("/accounting/liasse/{year}/finalize")
    public ResponseEntity<ClosingService.Edition> finalizeLiasse(@PathVariable String year) {
        return ResponseEntity.ok(liasse.finalize(year));
    }

    private ResponseEntity<?> table(FixedAssetReportService.Table table, String format) {
        return "csv".equalsIgnoreCase(format)
                ? ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + table.code() + ".csv\"")
                .contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8)).body(exporter.csv(table))
                : ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + table.code() + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF).body(exporter.pdf(table));
    }
}
