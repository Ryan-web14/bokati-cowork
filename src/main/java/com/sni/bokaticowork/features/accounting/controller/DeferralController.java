package com.sni.bokaticowork.features.accounting.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.accounting.model.AccAccrual;
import com.sni.bokaticowork.features.accounting.model.AccDeferralSchedule;
import com.sni.bokaticowork.features.accounting.service.implementation.DeferralReportService;
import com.sni.bokaticowork.features.accounting.service.implementation.DeferralService;
import com.sni.bokaticowork.features.accounting.service.implementation.FixedAssetReportService;
import com.sni.bokaticowork.features.billing.model.BillingDocumentLine;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

/** Constatations d'avance et rattachement (lot F20). Les droits sont controles par le service. */
@RestController
@RequestMapping(ApiPath.V1)
@RequiredArgsConstructor
public class DeferralController {

    private final DeferralService deferrals;
    private final DeferralReportService reports;
    private final FixedAssetReportService exporter;

    public record MatchRequest(String actualReference, BigDecimal actualAmount) {
    }

    @GetMapping("/accounting/deferrals")
    public ResponseEntity<List<DeferralService.DetailLine>> detail(@RequestParam(required = false) String kind,
                                                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(deferrals.detail(kind, date));
    }

    @GetMapping("/accounting/deferrals/schedules/{id}")
    public ResponseEntity<DeferralService.ScheduleView> schedule(@PathVariable Long id) {
        return ResponseEntity.ok(deferrals.scheduleView(id));
    }

    @Audited(module = "ACCOUNTING", action = "DEFERRAL_EVENT", ressource = "acc_deferral_schedule")
    @PostMapping("/accounting/deferrals/schedules/{id}/events")
    public ResponseEntity<AccDeferralSchedule> event(@PathVariable Long id, @RequestBody DeferralService.EventRequest request) {
        return ResponseEntity.ok(deferrals.event(id, request));
    }

    @Audited(module = "ACCOUNTING", action = "DEFERRAL_USAGE", ressource = "acc_deferral_schedule")
    @PostMapping("/accounting/deferrals/schedules/{id}/usage")
    public ResponseEntity<AccDeferralSchedule> usage(@PathVariable Long id, @RequestBody DeferralService.UsageRequest request) {
        return ResponseEntity.ok(deferrals.usage(id, request));
    }

    @Audited(module = "ACCOUNTING", action = "SERVICE_PERIOD_EDIT", ressource = "billing_document_line")
    @PatchMapping("/billing/documents/{number}/lines/{lineId}/service-period")
    public ResponseEntity<BillingDocumentLine> servicePeriod(@PathVariable String number, @PathVariable Long lineId,
                                                             @RequestBody DeferralService.PeriodRequest request) {
        return ResponseEntity.ok(deferrals.editPeriod(lineId, request));
    }

    @PostMapping("/accounting/deferral-runs/preview")
    public ResponseEntity<DeferralService.RunResult> preview(@RequestParam String period, @RequestParam(required = false) String kind) {
        return ResponseEntity.ok(deferrals.preview(period, kind));
    }

    @Audited(module = "ACCOUNTING", action = "DEFERRAL_RUN", ressource = "acc_deferral_run")
    @PostMapping("/accounting/deferral-runs")
    public ResponseEntity<DeferralService.RunResult> run(@RequestParam String period, @RequestParam(required = false) String kind) {
        return ResponseEntity.ok(deferrals.run(period, kind));
    }

    @GetMapping("/accounting/accruals")
    public ResponseEntity<List<AccAccrual>> accruals(@RequestParam(required = false) String period) {
        return ResponseEntity.ok(deferrals.accruals(period));
    }

    @Audited(module = "ACCOUNTING", action = "ACCRUAL_PROPOSE", ressource = "acc_accrual")
    @PostMapping("/accounting/accruals")
    public ResponseEntity<AccAccrual> propose(@RequestBody DeferralService.AccrualRequest request) {
        return ResponseEntity.ok(deferrals.proposeAccrual(request));
    }

    @Audited(module = "ACCOUNTING", action = "ACCRUAL_POST", ressource = "acc_accrual")
    @PostMapping("/accounting/accruals/{id}/post")
    public ResponseEntity<AccAccrual> post(@PathVariable Long id) {
        return ResponseEntity.ok(deferrals.postAccrual(id));
    }

    @Audited(module = "ACCOUNTING", action = "ACCRUAL_MATCH", ressource = "acc_accrual")
    @PostMapping("/accounting/accruals/{id}/match")
    public ResponseEntity<AccAccrual> match(@PathVariable Long id, @RequestBody MatchRequest request) {
        return ResponseEntity.ok(deferrals.matchAccrual(id, request.actualReference(), request.actualAmount()));
    }

    @GetMapping("/accounting/deferrals/controls")
    public ResponseEntity<List<DeferralService.Finding>> controls() {
        deferrals.accruals(null);
        return ResponseEntity.ok(deferrals.controls(LocalDate.now()));
    }

    @GetMapping("/accounting/deferrals/closing-check")
    public ResponseEntity<List<DeferralService.Finding>> closingCheck(@RequestParam int year) {
        deferrals.accruals(null);
        return ResponseEntity.ok(deferrals.closingCheck(year));
    }

    /** Etats de la section 10 : {@code format} json (defaut), csv ou pdf. */
    @GetMapping("/accounting/deferrals/reports/{report}")
    public ResponseEntity<?> report(@PathVariable String report, @RequestParam(required = false) Integer year,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                    @RequestParam(required = false, defaultValue = "json") String format) {
        FixedAssetReportService.Table table = reports.report(report, year, date);
        return switch (format.toLowerCase(Locale.ROOT)) {
            case "csv" -> ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"constatations-" + table.code() + ".csv\"")
                    .contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8)).body(exporter.csv(table));
            case "pdf" -> ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"constatations-" + table.code() + ".pdf\"")
                    .contentType(MediaType.APPLICATION_PDF).body(exporter.pdf(table));
            default -> ResponseEntity.ok(table);
        };
    }
}
