package com.sni.bokaticowork.features.accounting.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.accounting.model.AccProvisionReview;
import com.sni.bokaticowork.features.accounting.model.AccReceivableProposal;
import com.sni.bokaticowork.features.accounting.model.AccReceivableWriteoff;
import com.sni.bokaticowork.features.accounting.model.AccRetirementObligation;
import com.sni.bokaticowork.features.accounting.model.AccRisk;
import com.sni.bokaticowork.features.accounting.service.implementation.FixedAssetReportService;
import com.sni.bokaticowork.features.accounting.service.implementation.ProvisionReportService;
import com.sni.bokaticowork.features.accounting.service.implementation.ReceivableRiskService;
import com.sni.bokaticowork.features.accounting.service.implementation.RiskProvisionService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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

/** Creances douteuses, provisions pour risques et charges (lot F19). Les droits sont controles par les services. */
@RestController
@RequestMapping(ApiPath.V1 + "/accounting")
@RequiredArgsConstructor
public class ProvisionController {

    private final ReceivableRiskService receivables;
    private final RiskProvisionService provisions;
    private final ProvisionReportService reports;
    private final FixedAssetReportService exporter;

    public record RateRequest(BigDecimal rate, String reason) {
    }

    public record RejectRequest(String reason) {
    }

    // ------------------------------------------------------------------ creances

    @GetMapping("/receivables/aging")
    public ResponseEntity<List<ReceivableRiskService.ReceivableView>> aging(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String segment) {
        return ResponseEntity.ok(receivables.aging(date, segment));
    }

    @GetMapping("/receivables/{documentNumber}")
    public ResponseEntity<ReceivableRiskService.ReceivableView> receivable(@PathVariable String documentNumber) {
        return ResponseEntity.ok(receivables.receivable(documentNumber));
    }

    @GetMapping("/receivables/proposals")
    public ResponseEntity<List<AccReceivableProposal>> proposals() {
        return ResponseEntity.ok(receivables.queue());
    }

    @Audited(module = "ACCOUNTING", action = "RECEIVABLE_PROPOSAL_DECIDE", ressource = "acc_receivable_proposal")
    @PostMapping("/receivables/proposals/{id}/decision")
    public ResponseEntity<AccReceivableProposal> decide(@PathVariable Long id, @RequestBody ReceivableRiskService.Decision decision) {
        return ResponseEntity.ok(receivables.decide(id, decision));
    }

    @Audited(module = "ACCOUNTING", action = "RECEIVABLE_CLASSIFY", ressource = "acc_receivable_status")
    @PostMapping("/receivables/{documentNumber}/classify")
    public ResponseEntity<ReceivableRiskService.ReceivableView> classify(@PathVariable String documentNumber,
                                                                         @RequestBody ReceivableRiskService.ClassifyRequest request) {
        return ResponseEntity.ok(receivables.classify(documentNumber, request));
    }

    @Audited(module = "ACCOUNTING", action = "RECEIVABLE_BACK_TO_NORMAL", ressource = "acc_receivable_status")
    @PostMapping("/receivables/{documentNumber}/back-to-normal")
    public ResponseEntity<ReceivableRiskService.ReceivableView> backToNormal(@PathVariable String documentNumber,
                                                                             @RequestBody ReceivableRiskService.BackToNormalRequest request) {
        return ResponseEntity.ok(receivables.backToNormal(documentNumber, request));
    }

    @Audited(module = "ACCOUNTING", action = "RECEIVABLE_INDIVIDUAL_RATE", ressource = "acc_receivable_status")
    @PostMapping("/receivables/{documentNumber}/individual-rate")
    public ResponseEntity<ReceivableRiskService.ReceivableView> individualRate(@PathVariable String documentNumber, @RequestBody RateRequest request) {
        return ResponseEntity.ok(receivables.individualRate(documentNumber, request.rate(), request.reason()));
    }

    @GetMapping("/receivables/impairment-runs/preview")
    public ResponseEntity<ReceivableRiskService.ImpairmentRun> previewImpairment(@RequestParam String period) {
        return ResponseEntity.ok(receivables.previewImpairment(period));
    }

    @Audited(module = "ACCOUNTING", action = "RECEIVABLE_IMPAIRMENT_RUN", ressource = "acc_receivable_impairment")
    @PostMapping("/receivables/impairment-runs")
    public ResponseEntity<ReceivableRiskService.ImpairmentRun> runImpairment(@RequestParam String period) {
        return ResponseEntity.ok(receivables.runImpairment(period));
    }

    @GetMapping("/receivables/write-offs")
    public ResponseEntity<List<AccReceivableWriteoff>> writeOffs() {
        return ResponseEntity.ok(receivables.writeOffs());
    }

    @Audited(module = "ACCOUNTING", action = "RECEIVABLE_WRITE_OFF_PREPARE", ressource = "acc_receivable_writeoff")
    @PostMapping("/receivables/write-offs")
    public ResponseEntity<AccReceivableWriteoff> prepareWriteOff(@RequestBody ReceivableRiskService.WriteOffRequest request) {
        return ResponseEntity.ok(receivables.prepareWriteOff(request));
    }

    @Audited(module = "ACCOUNTING", action = "RECEIVABLE_WRITE_OFF_APPROVE", ressource = "acc_receivable_writeoff")
    @PostMapping("/receivables/write-offs/{id}/approve")
    public ResponseEntity<AccReceivableWriteoff> approveWriteOff(@PathVariable Long id) {
        return ResponseEntity.ok(receivables.approveWriteOff(id));
    }

    @Audited(module = "ACCOUNTING", action = "RECEIVABLE_WRITE_OFF_REJECT", ressource = "acc_receivable_writeoff")
    @PostMapping("/receivables/write-offs/{id}/reject")
    public ResponseEntity<AccReceivableWriteoff> rejectWriteOff(@PathVariable Long id, @RequestBody(required = false) RejectRequest request) {
        return ResponseEntity.ok(receivables.rejectWriteOff(id, request == null ? null : request.reason()));
    }

    @GetMapping("/receivables/controls")
    public ResponseEntity<List<ReceivableRiskService.Finding>> controls() {
        receivables.queue();
        return ResponseEntity.ok(receivables.controls(LocalDate.now()));
    }

    // ------------------------------------------------------------------ risques

    @GetMapping("/risks")
    public ResponseEntity<List<AccRisk>> risks(@RequestParam(required = false) String status) {
        return ResponseEntity.ok(provisions.list(status));
    }

    @Audited(module = "ACCOUNTING", action = "RISK_CREATE", ressource = "acc_risk")
    @PostMapping("/risks")
    public ResponseEntity<RiskProvisionService.RiskView> createRisk(@RequestBody RiskProvisionService.RiskRequest request) {
        return ResponseEntity.ok(provisions.create(request));
    }

    @GetMapping("/risks/{code}")
    public ResponseEntity<RiskProvisionService.RiskView> risk(@PathVariable String code) {
        return ResponseEntity.ok(provisions.get(code));
    }

    @Audited(module = "ACCOUNTING", action = "RISK_VALUATION", ressource = "acc_risk")
    @PostMapping("/risks/{code}/valuations")
    public ResponseEntity<RiskProvisionService.RiskView> valuate(@PathVariable String code, @RequestBody RiskProvisionService.ValuationRequest request) {
        return ResponseEntity.ok(provisions.valuate(code, request));
    }

    @Audited(module = "ACCOUNTING", action = "RISK_PROVISION", ressource = "acc_risk")
    @PostMapping("/risks/{code}/provision")
    public ResponseEntity<RiskProvisionService.RiskView> provision(@PathVariable String code, @RequestBody RiskProvisionService.ProvisionRequest request) {
        return ResponseEntity.ok(provisions.provision(code, request));
    }

    @Audited(module = "ACCOUNTING", action = "RISK_RECLASS_SHORT_TERM", ressource = "acc_risk")
    @PostMapping("/risks/reclass-short-term")
    public ResponseEntity<List<String>> reclass(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(provisions.reclassShortTerm(date != null ? date : LocalDate.now()));
    }

    @Audited(module = "ACCOUNTING", action = "RETIREMENT_OBLIGATION", ressource = "acc_retirement_obligation")
    @PostMapping("/risks/retirement-obligations")
    public ResponseEntity<AccRetirementObligation> retirement(@RequestBody RiskProvisionService.RetirementRequest request) {
        return ResponseEntity.ok(provisions.retirement(request));
    }

    // ------------------------------------------------------------------ revue et etats

    @Audited(module = "ACCOUNTING", action = "PROVISION_REVIEW_PREPARE", ressource = "acc_provision_review")
    @PostMapping("/provisions/reviews")
    public ResponseEntity<AccProvisionReview> prepareReview(@RequestBody RiskProvisionService.ReviewRequest request) {
        return ResponseEntity.ok(provisions.prepareReview(request));
    }

    @Audited(module = "ACCOUNTING", action = "PROVISION_REVIEW_VALIDATE", ressource = "acc_provision_review")
    @PostMapping("/provisions/reviews/{id}/validate")
    public ResponseEntity<AccProvisionReview> validateReview(@PathVariable Long id) {
        return ResponseEntity.ok(provisions.validateReview(id));
    }

    @GetMapping("/provisions/closing-check")
    public ResponseEntity<List<ReceivableRiskService.Finding>> closingCheck(@RequestParam int year) {
        receivables.queue();
        return ResponseEntity.ok(provisions.closingCheck(year));
    }

    /** Etats de la section 11 : {@code format} json (defaut), csv ou pdf. */
    @GetMapping("/provisions/reports/{report}")
    public ResponseEntity<?> report(@PathVariable String report, @RequestParam(required = false) Integer year,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                    @RequestParam(required = false) String segment,
                                    @RequestParam(required = false, defaultValue = "json") String format) {
        FixedAssetReportService.Table table = reports.report(report, year, date, segment);
        return switch (format.toLowerCase(Locale.ROOT)) {
            case "csv" -> ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"provisions-" + table.code() + ".csv\"")
                    .contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8)).body(exporter.csv(table));
            case "pdf" -> ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"provisions-" + table.code() + ".pdf\"")
                    .contentType(MediaType.APPLICATION_PDF).body(exporter.pdf(table));
            default -> ResponseEntity.ok(table);
        };
    }
}
