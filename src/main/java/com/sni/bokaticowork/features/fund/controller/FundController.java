package com.sni.bokaticowork.features.fund.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.fund.dto.FundDtos;
import com.sni.bokaticowork.features.fund.enums.FundRequestStatus;
import com.sni.bokaticowork.features.fund.service.FundLifecycleService;
import com.sni.bokaticowork.features.fund.service.FundNeedService;
import com.sni.bokaticowork.features.fund.service.FundRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Etats de besoin, demandes de fonds, decaissements et justifications (lots F5 et F6). */
@RestController
@RequestMapping(ApiPath.V1)
@RequiredArgsConstructor
public class FundController {

    private final FundNeedService needs;
    private final FundRequestService requests;
    private final FundLifecycleService lifecycle;

    // ------------------------------------------------------------------ etats de besoin

    @GetMapping("/fund-needs/mine")
    public ResponseEntity<List<FundDtos.Need>> myNeeds() {
        return ResponseEntity.ok(needs.mine());
    }

    @GetMapping("/fund-needs/submitted")
    public ResponseEntity<List<FundDtos.Need>> submittedNeeds() {
        return ResponseEntity.ok(needs.submitted());
    }

    @GetMapping("/fund-needs/{number}")
    public ResponseEntity<FundDtos.Need> need(@PathVariable String number) {
        return ResponseEntity.ok(needs.get(number));
    }

    @Audited(module = "FUND", action = "NEED_CREATE", ressource = "fund_need_statement")
    @PostMapping("/fund-needs")
    public ResponseEntity<FundDtos.Need> createNeed(@Valid @RequestBody FundDtos.NeedRequest request) {
        return ResponseEntity.ok(needs.create(request));
    }

    @Audited(module = "FUND", action = "NEED_UPDATE", ressource = "fund_need_statement")
    @PutMapping("/fund-needs/{number}")
    public ResponseEntity<FundDtos.Need> updateNeed(@PathVariable String number, @Valid @RequestBody FundDtos.NeedRequest request) {
        return ResponseEntity.ok(needs.update(number, request));
    }

    @Audited(module = "FUND", action = "NEED_SUBMIT", ressource = "fund_need_statement")
    @PatchMapping("/fund-needs/{number}/submit")
    public ResponseEntity<FundDtos.Need> submitNeed(@PathVariable String number) {
        return ResponseEntity.ok(needs.submit(number));
    }

    @Audited(module = "FUND", action = "NEED_VALIDATE", ressource = "fund_need_statement")
    @PatchMapping("/fund-needs/{number}/validate")
    public ResponseEntity<FundDtos.Need> validateNeed(@PathVariable String number, @RequestBody(required = false) FundDtos.Decision decision) {
        return ResponseEntity.ok(needs.decide(number, true, decision == null ? null : decision.comment()));
    }

    @Audited(module = "FUND", action = "NEED_REJECT", ressource = "fund_need_statement")
    @PatchMapping("/fund-needs/{number}/reject")
    public ResponseEntity<FundDtos.Need> rejectNeed(@PathVariable String number, @RequestBody FundDtos.Decision decision) {
        return ResponseEntity.ok(needs.decide(number, false, decision.comment()));
    }

    @Audited(module = "FUND", action = "NEED_TRANSFORM", ressource = "fund_need_statement")
    @PostMapping("/fund-needs/{number}/transform")
    public ResponseEntity<FundDtos.Request> transform(@PathVariable String number, @Valid @RequestBody FundDtos.TransformNeed request) {
        return ResponseEntity.ok(needs.transform(number, request));
    }

    // ------------------------------------------------------------------ demandes

    @GetMapping("/fund-requests/mine")
    public ResponseEntity<List<FundDtos.Request>> mine() {
        return ResponseEntity.ok(requests.mine());
    }

    @GetMapping("/fund-requests/to-approve")
    public ResponseEntity<List<FundDtos.Request>> toApprove() {
        return ResponseEntity.ok(requests.toApprove());
    }

    @GetMapping("/fund-requests/to-disburse")
    public ResponseEntity<List<FundDtos.Request>> toDisburse(@RequestParam(required = false) String registerCode) {
        return ResponseEntity.ok(requests.toDisburse(registerCode));
    }

    @GetMapping("/fund-requests")
    public ResponseEntity<List<FundDtos.Request>> all(@RequestParam(required = false) FundRequestStatus status) {
        return ResponseEntity.ok(requests.all(status));
    }

    @GetMapping("/fund-requests/advances")
    public ResponseEntity<List<FundDtos.AdvanceRow>> advances() {
        return ResponseEntity.ok(requests.openAdvances());
    }

    @GetMapping("/fund-requests/justifications-to-validate")
    public ResponseEntity<List<FundDtos.Request>> justificationsToValidate() {
        return ResponseEntity.ok(requests.justificationsToValidate());
    }

    @GetMapping("/fund-requests/{number}")
    public ResponseEntity<FundDtos.Request> get(@PathVariable String number) {
        return ResponseEntity.ok(requests.get(number));
    }

    @GetMapping("/fund-requests/{number}/insight")
    public ResponseEntity<FundDtos.Insight> insight(@PathVariable String number) {
        return ResponseEntity.ok(requests.insight(number));
    }

    @Audited(module = "FUND", action = "REQUEST_CREATE", ressource = "fund_request")
    @PostMapping("/fund-requests")
    public ResponseEntity<FundDtos.Request> create(@Valid @RequestBody FundDtos.RequestInput request) {
        return ResponseEntity.ok(requests.create(request));
    }

    @Audited(module = "FUND", action = "REQUEST_UPDATE", ressource = "fund_request")
    @PutMapping("/fund-requests/{number}")
    public ResponseEntity<FundDtos.Request> update(@PathVariable String number, @Valid @RequestBody FundDtos.RequestInput request) {
        return ResponseEntity.ok(requests.update(number, request));
    }

    @Audited(module = "FUND", action = "REQUEST_DUPLICATE", ressource = "fund_request")
    @PostMapping("/fund-requests/{number}/duplicate")
    public ResponseEntity<FundDtos.Request> duplicate(@PathVariable String number) {
        return ResponseEntity.ok(requests.duplicate(number));
    }

    @Audited(module = "FUND", action = "REQUEST_SUBMIT", ressource = "fund_request")
    @PatchMapping("/fund-requests/{number}/submit")
    public ResponseEntity<FundDtos.Request> submit(@PathVariable String number) {
        return ResponseEntity.ok(requests.submit(number));
    }

    @Audited(module = "FUND", action = "REQUEST_CANCEL", ressource = "fund_request")
    @PatchMapping("/fund-requests/{number}/cancel")
    public ResponseEntity<FundDtos.Request> cancel(@PathVariable String number, @RequestBody(required = false) FundDtos.Decision decision) {
        return ResponseEntity.ok(requests.cancel(number, decision));
    }

    @Audited(module = "FUND", action = "REQUEST_APPROVE", ressource = "fund_request")
    @PatchMapping("/fund-requests/{number}/approve")
    public ResponseEntity<FundDtos.Request> approve(@PathVariable String number, @RequestBody(required = false) FundDtos.Decision decision) {
        return ResponseEntity.ok(requests.approve(number, decision));
    }

    @Audited(module = "FUND", action = "REQUEST_REJECT", ressource = "fund_request")
    @PatchMapping("/fund-requests/{number}/reject")
    public ResponseEntity<FundDtos.Request> reject(@PathVariable String number, @RequestBody FundDtos.Decision decision) {
        return ResponseEntity.ok(requests.reject(number, decision));
    }

    @Audited(module = "FUND", action = "REQUEST_SEND_BACK", ressource = "fund_request")
    @PatchMapping("/fund-requests/{number}/send-back")
    public ResponseEntity<FundDtos.Request> sendBack(@PathVariable String number, @RequestBody FundDtos.Decision decision) {
        return ResponseEntity.ok(requests.sendBack(number, decision));
    }

    @Audited(module = "FUND", action = "REQUEST_DISBURSE", ressource = "fund_request")
    @PostMapping("/fund-requests/{number}/disburse")
    public ResponseEntity<FundDtos.Request> disburse(@PathVariable String number, @Valid @RequestBody FundDtos.Disburse request) {
        return ResponseEntity.ok(requests.disburse(number, request));
    }

    @Audited(module = "FUND", action = "REQUEST_LINK_TRANSFER", ressource = "fund_request")
    @PatchMapping("/fund-requests/{number}/transfer")
    public ResponseEntity<FundDtos.Request> linkTransfer(@PathVariable String number, @Valid @RequestBody FundDtos.LinkTransfer request) {
        return ResponseEntity.ok(requests.linkTransfer(number, request));
    }

    // ------------------------------------------------------------------ justification

    @Audited(module = "FUND", action = "EXPENSE_ADD", ressource = "expense_justification_line")
    @PostMapping("/fund-requests/{number}/expenses")
    public ResponseEntity<FundDtos.Request> addExpense(@PathVariable String number, @Valid @RequestBody FundDtos.ExpenseInput request) {
        return ResponseEntity.ok(requests.addExpense(number, request));
    }

    @Audited(module = "FUND", action = "EXPENSE_DELETE", ressource = "expense_justification_line")
    @DeleteMapping("/fund-requests/{number}/expenses/{line}")
    public ResponseEntity<FundDtos.Request> deleteExpense(@PathVariable String number, @PathVariable String line) {
        return ResponseEntity.ok(requests.deleteExpense(number, line));
    }

    @Audited(module = "FUND", action = "EXPENSE_VALIDATE", ressource = "expense_justification_line")
    @PatchMapping("/fund-requests/{number}/expenses/{line}/validate")
    public ResponseEntity<FundDtos.Request> validateExpense(@PathVariable String number, @PathVariable String line) {
        return ResponseEntity.ok(requests.validateExpense(number, line));
    }

    @Audited(module = "FUND", action = "EXPENSE_REJECT", ressource = "expense_justification_line")
    @PatchMapping("/fund-requests/{number}/expenses/{line}/reject")
    public ResponseEntity<FundDtos.Request> rejectExpense(@PathVariable String number, @PathVariable String line,
                                                          @Valid @RequestBody FundDtos.Reject request) {
        return ResponseEntity.ok(requests.rejectExpense(number, line, request));
    }

    @Audited(module = "FUND", action = "ADVANCE_RETURN", ressource = "fund_request")
    @PostMapping("/fund-requests/{number}/return")
    public ResponseEntity<FundDtos.Request> returnRemainder(@PathVariable String number, @Valid @RequestBody FundDtos.CashBack request) {
        return ResponseEntity.ok(requests.returnRemainder(number, request));
    }

    @Audited(module = "FUND", action = "ADVANCE_COMPLEMENT", ressource = "fund_request")
    @PostMapping("/fund-requests/{number}/complement")
    public ResponseEntity<FundDtos.Request> complement(@PathVariable String number, @Valid @RequestBody FundDtos.CashBack request) {
        return ResponseEntity.ok(requests.payComplement(number, request));
    }

    // ------------------------------------------------------------------ delegations, reprise

    @GetMapping("/fund-requests/delegations")
    public ResponseEntity<List<FundDtos.DelegationView>> delegations() {
        return ResponseEntity.ok(requests.delegations());
    }

    @Audited(module = "FUND", action = "DELEGATION_CREATE", ressource = "fund_approval_delegation")
    @PostMapping("/fund-requests/delegations")
    public ResponseEntity<FundDtos.DelegationView> delegate(@Valid @RequestBody FundDtos.Delegation request) {
        return ResponseEntity.ok(requests.delegate(request));
    }

    @Audited(module = "FUND", action = "DELEGATION_CANCEL", ressource = "fund_approval_delegation")
    @DeleteMapping("/fund-requests/delegations/{id}")
    public ResponseEntity<Void> cancelDelegation(@PathVariable Long id) {
        requests.cancelDelegation(id);
        return ResponseEntity.noContent().build();
    }

    @Audited(module = "FUND", action = "LEGACY_IMPORT", ressource = "fund_request")
    @PostMapping("/fund-requests/import-legacy")
    public ResponseEntity<Map<String, Integer>> importLegacy() {
        return ResponseEntity.ok(lifecycle.importLegacy());
    }
}
