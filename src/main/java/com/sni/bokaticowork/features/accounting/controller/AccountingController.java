package com.sni.bokaticowork.features.accounting.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.accounting.dto.AccountingRequests;
import com.sni.bokaticowork.features.accounting.dto.AccountingResponses;
import com.sni.bokaticowork.features.accounting.enums.EntryStatus;
import com.sni.bokaticowork.features.accounting.service.implementation.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/** Socle comptable SYSCOHADA (lot F1 du plan finance). */
@RestController
@RequestMapping(ApiPath.V1 + "/accounting")
@RequiredArgsConstructor
public class AccountingController {

    private final AccountingChartService chart;
    private final AccountingPeriodService periods;
    private final AccountingEntryService entries;
    private final AccountingLetteringService lettering;
    private final AccountingReportService reports;

    // ------------------------------------------------------------ plan de comptes

    @GetMapping("/accounts")
    public ResponseEntity<List<AccountingResponses.Account>> accounts(@RequestParam(required = false) String search,
                                                                      @RequestParam(required = false) String classCode,
                                                                      @RequestParam(defaultValue = "false") boolean includeInactive) {
        return ResponseEntity.ok(chart.list(search, classCode, includeInactive));
    }

    @GetMapping("/accounts/proposal")
    public ResponseEntity<AccountingResponses.AccountProposal> proposal(@RequestParam String parentNumber) {
        return ResponseEntity.ok(chart.proposal(parentNumber));
    }

    @GetMapping("/accounts/{number}")
    public ResponseEntity<AccountingResponses.Account> account(@PathVariable String number) {
        return ResponseEntity.ok(chart.get(number));
    }

    @Audited(module = "ACCOUNTING", action = "ACCOUNT_CREATE", ressource = "acc_account")
    @PostMapping("/accounts")
    public ResponseEntity<AccountingResponses.Account> createAccount(@Valid @RequestBody AccountingRequests.CreateAccount request) {
        return ResponseEntity.ok(chart.create(request));
    }

    @Audited(module = "ACCOUNTING", action = "ACCOUNT_UPDATE", ressource = "acc_account")
    @PatchMapping("/accounts/{number}")
    public ResponseEntity<AccountingResponses.Account> updateAccount(@PathVariable String number,
                                                                     @RequestBody AccountingRequests.UpdateAccount request) {
        return ResponseEntity.ok(chart.update(number, request));
    }

    @Audited(module = "ACCOUNTING", action = "ACCOUNT_DEACTIVATE", ressource = "acc_account")
    @PatchMapping("/accounts/{number}/deactivate")
    public ResponseEntity<AccountingResponses.Account> deactivate(@PathVariable String number, @RequestBody AccountingRequests.Reason request) {
        return ResponseEntity.ok(chart.deactivate(number, request.reason()));
    }

    @Audited(module = "ACCOUNTING", action = "ACCOUNT_ACTIVATE", ressource = "acc_account")
    @PatchMapping("/accounts/{number}/activate")
    public ResponseEntity<AccountingResponses.Account> activate(@PathVariable String number) {
        return ResponseEntity.ok(chart.reactivate(number));
    }

    @GetMapping("/accounts/{number}/history")
    public ResponseEntity<List<AccountingResponses.AccountHistory>> accountHistory(@PathVariable String number) {
        return ResponseEntity.ok(chart.history(number));
    }

    @GetMapping("/accounts/{number}/third-parties")
    public ResponseEntity<List<AccountingResponses.ThirdParty>> thirdParties(@PathVariable String number) {
        return ResponseEntity.ok(chart.thirdParties(number));
    }

    // ------------------------------------------------- exercices, periodes, journaux

    @GetMapping("/fiscal-years")
    public ResponseEntity<List<AccountingResponses.FiscalYear>> fiscalYears() {
        return ResponseEntity.ok(periods.fiscalYears());
    }

    @Audited(module = "ACCOUNTING", action = "FISCAL_YEAR_CREATE", ressource = "acc_fiscal_year")
    @PostMapping("/fiscal-years")
    public ResponseEntity<AccountingResponses.FiscalYear> createFiscalYear(@Valid @RequestBody AccountingRequests.CreateFiscalYear request) {
        return ResponseEntity.ok(periods.createFiscalYear(request));
    }

    @GetMapping("/periods")
    public ResponseEntity<List<AccountingResponses.Period>> periods() {
        return ResponseEntity.ok(periods.periods());
    }

    @Audited(module = "ACCOUNTING", action = "PERIOD_LOCK", ressource = "acc_period")
    @PatchMapping("/periods/{code}/lock")
    public ResponseEntity<AccountingResponses.Period> lock(@PathVariable String code) {
        return ResponseEntity.ok(periods.lock(code));
    }

    @Audited(module = "ACCOUNTING", action = "PERIOD_UNLOCK", ressource = "acc_period")
    @PatchMapping("/periods/{code}/unlock")
    public ResponseEntity<AccountingResponses.Period> unlock(@PathVariable String code) {
        return ResponseEntity.ok(periods.unlock(code));
    }

    @Audited(module = "ACCOUNTING", action = "PERIOD_CLOSE", ressource = "acc_period")
    @PatchMapping("/periods/{code}/close")
    public ResponseEntity<AccountingResponses.Period> close(@PathVariable String code) {
        return ResponseEntity.ok(periods.close(code));
    }

    @GetMapping("/journals")
    public ResponseEntity<List<AccountingResponses.Journal>> journals() {
        return ResponseEntity.ok(periods.journals());
    }

    @Audited(module = "ACCOUNTING", action = "JOURNAL_CREATE", ressource = "acc_journal")
    @PostMapping("/journals")
    public ResponseEntity<AccountingResponses.Journal> createJournal(@Valid @RequestBody AccountingRequests.CreateJournal request) {
        return ResponseEntity.ok(periods.createJournal(request));
    }

    @Audited(module = "ACCOUNTING", action = "JOURNAL_UPDATE", ressource = "acc_journal")
    @PatchMapping("/journals/{code}")
    public ResponseEntity<AccountingResponses.Journal> updateJournal(@PathVariable String code, @RequestBody AccountingRequests.UpdateJournal request) {
        return ResponseEntity.ok(periods.updateJournal(code, request));
    }

    // ---------------------------------------------------------------- pieces

    @GetMapping("/entries")
    public ResponseEntity<List<AccountingResponses.Entry>> entries(@RequestParam(required = false) String journalCode,
                                                                   @RequestParam(required = false) EntryStatus status,
                                                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(entries.search(journalCode, status, from, to));
    }

    @GetMapping("/entries/pending")
    public ResponseEntity<List<AccountingResponses.Entry>> pending() {
        return ResponseEntity.ok(entries.pending());
    }

    @GetMapping("/entries/{reference}")
    public ResponseEntity<AccountingResponses.Entry> entry(@PathVariable String reference) {
        return ResponseEntity.ok(entries.get(reference));
    }

    @Audited(module = "ACCOUNTING", action = "ENTRY_CREATE", ressource = "acc_entry")
    @PostMapping("/entries")
    public ResponseEntity<AccountingResponses.Entry> create(@Valid @RequestBody AccountingRequests.Entry request) {
        return ResponseEntity.ok(entries.create(request));
    }

    @Audited(module = "ACCOUNTING", action = "ENTRY_UPDATE", ressource = "acc_entry")
    @PutMapping("/entries/{reference}")
    public ResponseEntity<AccountingResponses.Entry> update(@PathVariable String reference, @Valid @RequestBody AccountingRequests.Entry request) {
        return ResponseEntity.ok(entries.update(reference, request));
    }

    @Audited(module = "ACCOUNTING", action = "ENTRY_DELETE_DRAFT", ressource = "acc_entry")
    @DeleteMapping("/entries/{reference}")
    public ResponseEntity<Void> delete(@PathVariable String reference) {
        entries.deleteDraft(reference);
        return ResponseEntity.noContent().build();
    }

    @Audited(module = "ACCOUNTING", action = "ENTRY_VALIDATE", ressource = "acc_entry")
    @PatchMapping("/entries/{reference}/validate")
    public ResponseEntity<AccountingResponses.Entry> validate(@PathVariable String reference) {
        return ResponseEntity.ok(entries.validate(reference));
    }

    @Audited(module = "ACCOUNTING", action = "ENTRY_REVERSE", ressource = "acc_entry")
    @PatchMapping("/entries/{reference}/reverse")
    public ResponseEntity<AccountingResponses.Entry> reverse(@PathVariable String reference, @Valid @RequestBody AccountingRequests.Reverse request) {
        return ResponseEntity.ok(entries.reverse(reference, request));
    }

    @Audited(module = "ACCOUNTING", action = "ENTRY_REJECT", ressource = "acc_entry")
    @PatchMapping("/entries/{reference}/reject")
    public ResponseEntity<AccountingResponses.Entry> reject(@PathVariable String reference, @RequestBody AccountingRequests.Reason request) {
        return ResponseEntity.ok(entries.reject(reference, request.reason()));
    }

    @Audited(module = "ACCOUNTING", action = "ENTRY_REGULARIZE", ressource = "acc_entry")
    @PostMapping("/entries/{reference}/regularize")
    public ResponseEntity<AccountingResponses.Entry> regularize(@PathVariable String reference,
                                                                @Valid @RequestBody AccountingRequests.Regularize request) {
        return ResponseEntity.ok(entries.regularize(reference, request));
    }

    // --------------------------------------------------------------- lettrage

    @GetMapping("/lettering/open-lines")
    public ResponseEntity<List<AccountingResponses.OpenLine>> openLines(@RequestParam String accountNumber,
                                                                        @RequestParam(required = false) String thirdPartyCode) {
        return ResponseEntity.ok(lettering.openLines(accountNumber, thirdPartyCode));
    }

    @GetMapping("/lettering")
    public ResponseEntity<List<AccountingResponses.Lettering>> letterings(@RequestParam String accountNumber,
                                                                          @RequestParam(required = false) String thirdPartyCode) {
        return ResponseEntity.ok(lettering.letterings(accountNumber, thirdPartyCode));
    }

    @Audited(module = "ACCOUNTING", action = "LETTER", ressource = "acc_lettering")
    @PostMapping("/lettering")
    public ResponseEntity<AccountingResponses.Lettering> letter(@Valid @RequestBody AccountingRequests.Letter request) {
        return ResponseEntity.ok(lettering.letter(request));
    }

    @Audited(module = "ACCOUNTING", action = "UNLETTER", ressource = "acc_lettering")
    @PatchMapping("/lettering/{id}/unletter")
    public ResponseEntity<AccountingResponses.Lettering> unletter(@PathVariable Long id, @RequestBody AccountingRequests.Reason request) {
        return ResponseEntity.ok(lettering.unletter(id, request.reason()));
    }

    // ----------------------------------------------------------------- etats

    @GetMapping("/reports/journal")
    public ResponseEntity<AccountingResponses.JournalReport> journalReport(@RequestParam String journalCode,
                                                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(reports.journal(journalCode, from, to));
    }

    @GetMapping("/reports/general-ledger")
    public ResponseEntity<AccountingResponses.GeneralLedger> generalLedger(@RequestParam(required = false) String account,
                                                                           @RequestParam(required = false) String thirdPartyCode,
                                                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(reports.generalLedger(account, thirdPartyCode, from, to));
    }

    @GetMapping("/reports/trial-balance")
    public ResponseEntity<AccountingResponses.TrialBalance> trialBalance(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                                                         @RequestParam(required = false) String scope) {
        return ResponseEntity.ok(reports.trialBalance(from, to, scope));
    }

    @GetMapping("/reports/numbering-continuity")
    public ResponseEntity<AccountingResponses.NumberingCheck> continuity(@RequestParam String journalCode, @RequestParam String fiscalYearCode) {
        return ResponseEntity.ok(reports.checkContinuity(journalCode.trim().toUpperCase(), fiscalYearCode.trim()));
    }

    @PostMapping("/numbering-checks")
    public ResponseEntity<List<AccountingResponses.NumberingCheck>> checkAll() {
        return ResponseEntity.ok(reports.checkAll());
    }
}
