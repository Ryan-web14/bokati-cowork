package com.sni.bokaticowork.features.accounting.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.accounting.model.*;
import com.sni.bokaticowork.features.accounting.service.implementation.BankService;
import com.sni.bokaticowork.features.accounting.service.implementation.ChequeService;
import com.sni.bokaticowork.features.accounting.service.implementation.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Banque, rapprochement, cheques et remises (lot F10) ; devises des caisses (lot C5). */
@RestController
@RequestMapping(ApiPath.V1 + "/accounting")
@RequiredArgsConstructor
public class BankController {

    private final BankService banks;
    private final ChequeService cheques;
    private final ExchangeRateService rates;

    public record MatchRequest(List<Long> statementLineIds, List<Long> entryLineIds) {
    }

    public record Reason(String reason) {
    }

    // ------------------------------------------------------------------ comptes et modeles

    @GetMapping("/bank-accounts")
    public ResponseEntity<List<BankAccount>> accounts() {
        return ResponseEntity.ok(banks.accounts());
    }

    @Audited(module = "ACCOUNTING", action = "BANK_ACCOUNT_SAVE", ressource = "bank_account")
    @PostMapping("/bank-accounts")
    public ResponseEntity<BankAccount> saveAccount(@RequestBody BankAccount request) {
        return ResponseEntity.ok(banks.saveAccount(request));
    }

    @GetMapping("/bank-import-templates")
    public ResponseEntity<List<BankImportTemplate>> templates() {
        return ResponseEntity.ok(banks.templates());
    }

    @Audited(module = "ACCOUNTING", action = "BANK_TEMPLATE_SAVE", ressource = "bank_import_template")
    @PostMapping("/bank-import-templates")
    public ResponseEntity<BankImportTemplate> saveTemplate(@RequestBody BankImportTemplate request) {
        return ResponseEntity.ok(banks.saveTemplate(request));
    }

    @PostMapping(value = "/bank-import-templates/{code}/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<BankService.ManualLine>> preview(@PathVariable String code, @RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(banks.preview(code, file.getBytes()));
    }

    // ------------------------------------------------------------------ releves et pointage

    @Audited(module = "ACCOUNTING", action = "BANK_STATEMENT_IMPORT", ressource = "bank_statement")
    @PostMapping(value = "/bank-accounts/{code}/statements", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BankService.ImportResult> importFile(@PathVariable String code, @RequestParam("file") MultipartFile file,
                                                               @RequestParam(required = false) BigDecimal openingBalance,
                                                               @RequestParam(required = false) BigDecimal closingBalance,
                                                               @RequestParam(required = false) String replace) throws IOException {
        return ResponseEntity.ok(banks.importFile(code, file.getBytes(), file.getOriginalFilename(), openingBalance, closingBalance, replace));
    }

    @Audited(module = "ACCOUNTING", action = "BANK_STATEMENT_MANUAL", ressource = "bank_statement")
    @PostMapping(value = "/bank-accounts/{code}/statements/manual", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BankService.ImportResult> importManual(@PathVariable String code, @RequestBody BankService.ManualStatement request) {
        return ResponseEntity.ok(banks.importManual(code, request));
    }

    @GetMapping("/bank-accounts/{code}/statements")
    public ResponseEntity<List<BankStatement>> statements(@PathVariable String code) {
        return ResponseEntity.ok(banks.statements(code));
    }

    @GetMapping("/bank-accounts/{code}/matching")
    public ResponseEntity<BankService.MatchingScreen> screen(@PathVariable String code) {
        return ResponseEntity.ok(banks.screen(code));
    }

    @Audited(module = "ACCOUNTING", action = "BANK_AUTO_MATCH", ressource = "bank_matching")
    @PostMapping("/bank-accounts/{code}/matching/auto")
    public ResponseEntity<Integer> auto(@PathVariable String code) {
        return ResponseEntity.ok(banks.autoMatch(code));
    }

    @Audited(module = "ACCOUNTING", action = "BANK_MATCH", ressource = "bank_matching")
    @PostMapping("/bank-accounts/{code}/matching")
    public ResponseEntity<BankMatching> match(@PathVariable String code, @RequestBody MatchRequest request) {
        return ResponseEntity.ok(banks.matchManual(code, request.statementLineIds(), request.entryLineIds()));
    }

    @Audited(module = "ACCOUNTING", action = "BANK_UNMATCH", ressource = "bank_matching")
    @DeleteMapping("/bank-accounts/{code}/matching/{matching}")
    public ResponseEntity<BankMatching> unmatch(@PathVariable String code, @PathVariable String matching, @RequestBody Reason request) {
        return ResponseEntity.ok(banks.unmatch(code, matching, request.reason()));
    }

    @Audited(module = "ACCOUNTING", action = "BANK_LINE_POST", ressource = "bank_statement_line")
    @PostMapping("/bank-accounts/{code}/statement-lines/{id}/post")
    public ResponseEntity<BankService.Line> post(@PathVariable String code, @PathVariable Long id, @RequestBody(required = false) BankService.PostLine request) {
        return ResponseEntity.ok(banks.post(code, id, request));
    }

    @GetMapping("/bank-assignment-rules")
    public ResponseEntity<List<BankAssignmentRule>> rules() {
        return ResponseEntity.ok(banks.rules());
    }

    @Audited(module = "ACCOUNTING", action = "BANK_RULE_SAVE", ressource = "bank_assignment_rule")
    @PostMapping("/bank-assignment-rules")
    public ResponseEntity<BankAssignmentRule> saveRule(@RequestBody BankAssignmentRule request) {
        return ResponseEntity.ok(banks.saveRule(request));
    }

    // ------------------------------------------------------------------ rapprochement

    @Audited(module = "ACCOUNTING", action = "BANK_RECONCILIATION_PREPARE", ressource = "bank_reconciliation")
    @PostMapping("/bank-accounts/{code}/reconciliations")
    public ResponseEntity<BankService.Reconciliation> prepare(@PathVariable String code,
                                                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(banks.prepare(code, date));
    }

    @GetMapping("/bank-accounts/{code}/reconciliations")
    public ResponseEntity<List<BankReconciliation>> reconciliations(@PathVariable String code) {
        return ResponseEntity.ok(banks.reconciliations(code));
    }

    @Audited(module = "ACCOUNTING", action = "BANK_RECONCILIATION_VALIDATE", ressource = "bank_reconciliation")
    @PatchMapping("/reconciliations/{id}/validate")
    public ResponseEntity<BankService.Reconciliation> validate(@PathVariable Long id) {
        return ResponseEntity.ok(banks.validate(id));
    }

    @GetMapping("/bank-dashboard")
    public ResponseEntity<List<BankService.Dashboard>> dashboard() {
        return ResponseEntity.ok(banks.dashboard());
    }

    // ------------------------------------------------------------------ cheques et remises

    @GetMapping("/cheques")
    public ResponseEntity<List<BankCheque>> cheques(@RequestParam(required = false) String status) {
        cheques.sync();
        return ResponseEntity.ok(cheques.cheques(status));
    }

    @Audited(module = "ACCOUNTING", action = "CHEQUE_REJECT", ressource = "bank_cheque")
    @PatchMapping("/cheques/{transactionNumber}/reject")
    public ResponseEntity<BankCheque> reject(@PathVariable String transactionNumber, @RequestParam String reason,
                                             @RequestParam(required = false) BigDecimal fees, @RequestParam(required = false) String bankAccountCode) {
        return ResponseEntity.ok(cheques.reject(transactionNumber, reason, fees, bankAccountCode));
    }

    @GetMapping("/deposit-slips")
    public ResponseEntity<List<BankDepositSlip>> slips() {
        return ResponseEntity.ok(cheques.slips());
    }

    @Audited(module = "ACCOUNTING", action = "DEPOSIT_SLIP_PREPARE", ressource = "bank_deposit_slip")
    @PostMapping("/deposit-slips")
    public ResponseEntity<ChequeService.Slip> prepareSlip(@RequestBody ChequeService.SlipInput request) {
        return ResponseEntity.ok(cheques.prepare(request));
    }

    @Audited(module = "ACCOUNTING", action = "DEPOSIT_SLIP_DEPOSIT", ressource = "bank_deposit_slip")
    @PatchMapping("/deposit-slips/{number}/deposit")
    public ResponseEntity<ChequeService.Slip> deposit(@PathVariable String number) {
        return ResponseEntity.ok(cheques.deposit(number));
    }

    @Audited(module = "ACCOUNTING", action = "DEPOSIT_SLIP_CREDIT", ressource = "bank_deposit_slip")
    @PatchMapping("/deposit-slips/{number}/credit")
    public ResponseEntity<ChequeService.Slip> credit(@PathVariable String number, @RequestParam(required = false) Long statementLineId,
                                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(cheques.credit(number, statementLineId, date));
    }

    // ------------------------------------------------------------------ devises (C5)

    @GetMapping("/exchange-rates")
    public ResponseEntity<List<CashExchangeRate>> rates() {
        return ResponseEntity.ok(rates.recent());
    }

    @Audited(module = "ACCOUNTING", action = "EXCHANGE_RATE_SET", ressource = "cash_exchange_rate")
    @PostMapping("/exchange-rates")
    public ResponseEntity<CashExchangeRate> setRate(@RequestParam String currency, @RequestParam BigDecimal rate,
                                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate validOn) {
        return ResponseEntity.ok(rates.setRate(currency, rate, validOn));
    }

    @GetMapping("/cash/registers/{registerCode}/balances")
    public ResponseEntity<List<ExchangeRateService.CurrencyBalance>> balances(@PathVariable String registerCode) {
        return ResponseEntity.ok(rates.balances(registerCode));
    }
}
