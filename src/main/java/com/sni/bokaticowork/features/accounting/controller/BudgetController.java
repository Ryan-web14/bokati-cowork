package com.sni.bokaticowork.features.accounting.controller;

import com.sni.bokaticowork.core.accounting.BudgetControl;
import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.accounting.model.AccBudgetForecast;
import com.sni.bokaticowork.features.accounting.model.AccBudgetTransfer;
import com.sni.bokaticowork.features.accounting.service.implementation.BudgetEngine;
import com.sni.bokaticowork.features.accounting.service.implementation.BudgetReportService;
import com.sni.bokaticowork.features.accounting.service.implementation.BudgetService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

/** Gestion budgetaire (lot F16). */
@RestController
@RequestMapping(ApiPath.V1 + "/accounting/budget")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgets;
    private final BudgetReportService reports;
    private final BudgetEngine engine;

    // ------------------------------------------------------------------ nomenclature

    @GetMapping("/items")
    public ResponseEntity<List<BudgetService.Item>> items() {
        return ResponseEntity.ok(budgets.items());
    }

    @Audited(module = "ACCOUNTING", action = "BUDGET_ITEM_SAVE", ressource = "acc_budget_item")
    @PostMapping("/items")
    public ResponseEntity<BudgetService.Item> saveItem(@RequestBody BudgetService.ItemInput request) {
        return ResponseEntity.ok(budgets.saveItem(request));
    }

    @Audited(module = "ACCOUNTING", action = "BUDGET_ITEM_DECIDE", ressource = "acc_budget_item")
    @PatchMapping("/items/{code}/{decision}")
    public ResponseEntity<BudgetService.Item> decideItem(@PathVariable String code, @PathVariable String decision,
                                                         @RequestParam(required = false) String into) {
        return ResponseEntity.ok(budgets.decideItem(code, decision, into));
    }

    // ------------------------------------------------------------------ budgets

    @GetMapping
    public ResponseEntity<List<BudgetService.Budget>> budgets(@RequestParam String fiscalYearCode) {
        return ResponseEntity.ok(budgets.budgets(fiscalYearCode));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BudgetService.Budget> budget(@PathVariable Long id) {
        return ResponseEntity.ok(budgets.budget(id));
    }

    @Audited(module = "ACCOUNTING", action = "BUDGET_CREATE", ressource = "acc_budget")
    @PostMapping
    public ResponseEntity<BudgetService.Budget> create(@RequestBody BudgetService.CreateBudget request) {
        return ResponseEntity.ok(budgets.create(request));
    }

    @Audited(module = "ACCOUNTING", action = "BUDGET_LINE_SAVE", ressource = "acc_budget_line")
    @PostMapping("/{id}/lines")
    public ResponseEntity<BudgetService.Line> line(@PathVariable Long id, @RequestBody BudgetService.LineInput request) {
        return ResponseEntity.ok(budgets.upsertLine(id, request));
    }

    @Audited(module = "ACCOUNTING", action = "BUDGET_TRANSITION", ressource = "acc_budget")
    @PatchMapping("/{id}/{action}")
    public ResponseEntity<BudgetService.Budget> transition(@PathVariable Long id, @PathVariable String action,
                                                           @RequestBody(required = false) Map<String, String> body) {
        return ResponseEntity.ok(budgets.transition(id, action, body == null ? null : body.get("reason")));
    }

    @Audited(module = "ACCOUNTING", action = "BUDGET_LINE_FREEZE", ressource = "acc_budget_line")
    @PatchMapping("/lines/{lineId}/freeze")
    public ResponseEntity<BudgetService.Line> freeze(@PathVariable Long lineId, @RequestParam(defaultValue = "true") boolean frozen) {
        return ResponseEntity.ok(budgets.freeze(lineId, frozen));
    }

    // ------------------------------------------------------------------ virements

    @GetMapping("/transfers")
    public ResponseEntity<List<AccBudgetTransfer>> transfers() {
        return ResponseEntity.ok(budgets.transfers());
    }

    @Audited(module = "ACCOUNTING", action = "BUDGET_TRANSFER_REQUEST", ressource = "acc_budget_transfer")
    @PostMapping("/transfers")
    public ResponseEntity<AccBudgetTransfer> requestTransfer(@RequestBody BudgetService.TransferInput request) {
        return ResponseEntity.ok(budgets.requestTransfer(request));
    }

    @Audited(module = "ACCOUNTING", action = "BUDGET_TRANSFER_DECIDE", ressource = "acc_budget_transfer")
    @PatchMapping("/transfers/{id}/{decision}")
    public ResponseEntity<AccBudgetTransfer> decideTransfer(@PathVariable Long id, @PathVariable String decision) {
        return ResponseEntity.ok(budgets.decideTransfer(id, "approve".equalsIgnoreCase(decision)));
    }

    // ------------------------------------------------------------------ previsions

    @GetMapping("/forecasts")
    public ResponseEntity<List<AccBudgetForecast>> forecasts(@RequestParam(required = false) String itemCode) {
        return ResponseEntity.ok(budgets.forecasts(itemCode));
    }

    @Audited(module = "ACCOUNTING", action = "BUDGET_FORECAST_SAVE", ressource = "acc_budget_forecast")
    @PostMapping("/forecasts")
    public ResponseEntity<AccBudgetForecast> createForecast(@RequestBody BudgetService.ForecastInput request) {
        return ResponseEntity.ok(budgets.saveForecast(null, request));
    }

    @Audited(module = "ACCOUNTING", action = "BUDGET_FORECAST_SAVE", ressource = "acc_budget_forecast")
    @PutMapping("/forecasts/{number}")
    public ResponseEntity<AccBudgetForecast> updateForecast(@PathVariable String number, @RequestBody BudgetService.ForecastInput request) {
        return ResponseEntity.ok(budgets.saveForecast(number, request));
    }

    @Audited(module = "ACCOUNTING", action = "BUDGET_FORECAST_STATUS", ressource = "acc_budget_forecast")
    @PatchMapping("/forecasts/{number}/status")
    public ResponseEntity<AccBudgetForecast> forecastStatus(@PathVariable String number, @RequestParam String status) {
        return ResponseEntity.ok(budgets.forecastStatus(number, status));
    }

    @GetMapping("/unplanned-reasons")
    public ResponseEntity<List<Map<String, Object>>> unplannedReasons() {
        return ResponseEntity.ok(budgets.unplannedReasons());
    }

    // ------------------------------------------------------------------ controle et suivi

    /** Simulation : ce qu'une depense ferait aux lignes de budget (affichage avant saisie). */
    @PostMapping("/check")
    public ResponseEntity<BudgetControl.Impact> check(@RequestBody List<BudgetControl.Spend> spends,
                                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(engine.check(spends, date == null ? LocalDate.now() : date));
    }

    @GetMapping("/reports/follow-up")
    public ResponseEntity<List<BudgetReportService.FollowUp>> followUp(@RequestParam String fiscalYearCode, @RequestParam(required = false) String month) {
        return ResponseEntity.ok(reports.followUp(fiscalYearCode, month == null ? null : YearMonth.parse(month)));
    }

    @GetMapping("/reports/commitments")
    public ResponseEntity<List<BudgetReportService.Commitment>> commitments() {
        return ResponseEntity.ok(reports.openCommitments());
    }

    @GetMapping("/reports/planned")
    public ResponseEntity<List<BudgetReportService.PlannedSplit>> planned() {
        return ResponseEntity.ok(reports.plannedSplit());
    }

    @GetMapping("/reports/forecasts")
    public ResponseEntity<List<BudgetReportService.ForecastRow>> forecastFollowUp() {
        return ResponseEntity.ok(reports.forecastFollowUp());
    }

    @GetMapping("/reports/reserve")
    public ResponseEntity<Map<String, BigDecimal>> reserve(@RequestParam String fiscalYearCode) {
        return ResponseEntity.ok(reports.reserve(fiscalYearCode));
    }
}
