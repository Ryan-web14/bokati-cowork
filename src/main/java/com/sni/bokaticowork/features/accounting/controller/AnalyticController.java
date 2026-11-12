package com.sni.bokaticowork.features.accounting.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.accounting.model.AccAnalyticDistribution;
import com.sni.bokaticowork.features.accounting.model.AccAnalyticRule;
import com.sni.bokaticowork.features.accounting.service.implementation.AccountingEntryService;
import com.sni.bokaticowork.features.accounting.service.implementation.AnalyticReportService;
import com.sni.bokaticowork.features.accounting.service.implementation.AnalyticService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Comptabilite analytique et analyse des depenses (lot F9). */
@RestController
@RequestMapping(ApiPath.V1 + "/accounting/analytic")
@RequiredArgsConstructor
public class AnalyticController {

    private final AnalyticService analytic;
    private final AnalyticReportService reports;
    private final AccountingEntryService entries;

    // ------------------------------------------------------------------ parametrage

    @GetMapping("/axes")
    public ResponseEntity<List<AnalyticService.Axis>> axes() {
        return ResponseEntity.ok(analytic.axes());
    }

    @Audited(module = "ACCOUNTING", action = "ANALYTIC_AXIS_SAVE", ressource = "acc_analytic_axis")
    @PostMapping("/axes")
    public ResponseEntity<AnalyticService.Axis> saveAxis(@RequestBody AnalyticService.AxisInput request) {
        return ResponseEntity.ok(analytic.saveAxis(request));
    }

    @GetMapping("/axes/{axis}/sections")
    public ResponseEntity<List<AnalyticService.Section>> tree(@PathVariable String axis, @RequestParam(defaultValue = "false") boolean includeClosed) {
        return ResponseEntity.ok(analytic.tree(axis, includeClosed));
    }

    @Audited(module = "ACCOUNTING", action = "ANALYTIC_SECTION_CREATE", ressource = "acc_analytic_section")
    @PostMapping("/axes/{axis}/sections")
    public ResponseEntity<AnalyticService.Section> create(@PathVariable String axis, @RequestBody AnalyticService.SectionInput request) {
        return ResponseEntity.ok(analytic.createSection(axis, request));
    }

    @Audited(module = "ACCOUNTING", action = "ANALYTIC_SECTION_UPDATE", ressource = "acc_analytic_section")
    @PatchMapping("/axes/{axis}/sections/{code}")
    public ResponseEntity<AnalyticService.Section> update(@PathVariable String axis, @PathVariable String code,
                                                          @RequestBody AnalyticService.SectionInput request) {
        return ResponseEntity.ok(analytic.updateSection(axis, code, request));
    }

    @Audited(module = "ACCOUNTING", action = "ANALYTIC_SECTION_APPROVE", ressource = "acc_analytic_section")
    @PatchMapping("/axes/{axis}/sections/{code}/approve")
    public ResponseEntity<AnalyticService.Section> approve(@PathVariable String axis, @PathVariable String code) {
        return ResponseEntity.ok(analytic.approve(axis, code, true));
    }

    @Audited(module = "ACCOUNTING", action = "ANALYTIC_SECTION_REJECT", ressource = "acc_analytic_section")
    @PatchMapping("/axes/{axis}/sections/{code}/reject")
    public ResponseEntity<AnalyticService.Section> reject(@PathVariable String axis, @PathVariable String code) {
        return ResponseEntity.ok(analytic.approve(axis, code, false));
    }

    @Audited(module = "ACCOUNTING", action = "ANALYTIC_SECTION_CLOSE", ressource = "acc_analytic_section")
    @PatchMapping("/axes/{axis}/sections/{code}/close")
    public ResponseEntity<AnalyticService.Section> close(@PathVariable String axis, @PathVariable String code) {
        return ResponseEntity.ok(analytic.close(axis, code, true));
    }

    @Audited(module = "ACCOUNTING", action = "ANALYTIC_SECTION_REOPEN", ressource = "acc_analytic_section")
    @PatchMapping("/axes/{axis}/sections/{code}/reopen")
    public ResponseEntity<AnalyticService.Section> reopen(@PathVariable String axis, @PathVariable String code) {
        return ResponseEntity.ok(analytic.close(axis, code, false));
    }

    @Audited(module = "ACCOUNTING", action = "ANALYTIC_SECTION_MERGE", ressource = "acc_analytic_section")
    @PatchMapping("/axes/{axis}/sections/{code}/merge")
    public ResponseEntity<AnalyticService.Section> merge(@PathVariable String axis, @PathVariable String code, @RequestParam String into,
                                                         @RequestParam(defaultValue = "false") boolean restate) {
        return ResponseEntity.ok(analytic.merge(axis, code, into, restate));
    }

    @GetMapping("/sections/pending")
    public ResponseEntity<List<AnalyticService.Section>> pending() {
        return ResponseEntity.ok(analytic.pending());
    }

    @GetMapping("/rules")
    public ResponseEntity<List<AccAnalyticRule>> rules() {
        return ResponseEntity.ok(analytic.rules());
    }

    @Audited(module = "ACCOUNTING", action = "ANALYTIC_RULE_SAVE", ressource = "acc_analytic_rule")
    @PostMapping("/rules")
    public ResponseEntity<AccAnalyticRule> createRule(@RequestBody AnalyticService.RuleInput request) {
        return ResponseEntity.ok(analytic.saveRule(null, request));
    }

    @Audited(module = "ACCOUNTING", action = "ANALYTIC_RULE_SAVE", ressource = "acc_analytic_rule")
    @PutMapping("/rules/{id}")
    public ResponseEntity<AccAnalyticRule> updateRule(@PathVariable Long id, @RequestBody AnalyticService.RuleInput request) {
        return ResponseEntity.ok(analytic.saveRule(id, request));
    }

    @Audited(module = "ACCOUNTING", action = "ANALYTIC_RULE_DELETE", ressource = "acc_analytic_rule")
    @DeleteMapping("/rules/{id}")
    public ResponseEntity<Void> deleteRule(@PathVariable Long id) {
        analytic.deleteRule(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/keys")
    public ResponseEntity<List<AnalyticService.Key>> keys() {
        return ResponseEntity.ok(analytic.keys());
    }

    @Audited(module = "ACCOUNTING", action = "ANALYTIC_KEY_SAVE", ressource = "acc_allocation_key")
    @PostMapping("/keys")
    public ResponseEntity<AnalyticService.Key> saveKey(@RequestBody AnalyticService.KeyInput request) {
        return ResponseEntity.ok(analytic.saveKey(request));
    }

    // ------------------------------------------------------------------ ventilation

    @GetMapping("/entries/{reference}/allocations")
    public ResponseEntity<List<AnalyticService.Allocation>> allocations(@PathVariable String reference) {
        return ResponseEntity.ok(analytic.allocations(entries.entry(reference).getId()));
    }

    @Audited(module = "ACCOUNTING", action = "ANALYTIC_RECLASS", ressource = "acc_analytic_reclass")
    @PostMapping("/reclass")
    public ResponseEntity<AnalyticService.Allocation> reclass(@RequestBody AnalyticService.Reclass request) {
        return ResponseEntity.ok(analytic.reclass(request));
    }

    @Audited(module = "ACCOUNTING", action = "ANALYTIC_DISTRIBUTE", ressource = "acc_analytic_distribution")
    @PostMapping("/distribution")
    public ResponseEntity<List<AccAnalyticDistribution>> distribute(@RequestParam String periodCode, @RequestParam String axisCode,
                                                                    @RequestParam String keyCode) {
        return ResponseEntity.ok(analytic.distribute(periodCode, axisCode, keyCode));
    }

    // ------------------------------------------------------------------ etats

    @GetMapping("/reports/result")
    public ResponseEntity<AnalyticReportService.Result> result(@RequestParam(defaultValue = "ACTIVITE") String axis,
                                                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(reports.result(axis.toUpperCase(), from, to));
    }

    @GetMapping("/reports/items/{item}")
    public ResponseEntity<AnalyticReportService.ItemAnalysis> item(@PathVariable String item,
                                                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(reports.item(item.toUpperCase(), from, to));
    }

    @GetMapping("/reports/dashboard")
    public ResponseEntity<List<AnalyticReportService.DashboardRow>> dashboard(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(reports.dashboard(from, to));
    }

    @GetMapping("/reports/balance")
    public ResponseEntity<List<AnalyticReportService.BalanceLine>> balance(@RequestParam(defaultValue = "ACTIVITE") String axis,
                                                                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(reports.balance(axis.toUpperCase(), from, to));
    }

    @GetMapping("/reports/control")
    public ResponseEntity<Map<String, java.math.BigDecimal>> control(@RequestParam(defaultValue = "ACTIVITE") String axis,
                                                                     @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                                     @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(reports.control(axis.toUpperCase(), from, to));
    }
}
