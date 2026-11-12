package com.sni.bokaticowork.features.accounting.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.accounting.dto.AccountingResponses;
import com.sni.bokaticowork.features.accounting.dto.PostingDtos;
import com.sni.bokaticowork.features.accounting.enums.JournalType;
import com.sni.bokaticowork.features.accounting.service.implementation.CashAccountingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Caisse comptable (lot F3) : journaux d'une caisse, rapprochement, alimentation. */
@RestController
@RequestMapping(ApiPath.V1 + "/accounting/cash")
@RequiredArgsConstructor
public class CashAccountingController {

    private final CashAccountingService cash;

    public record NewJournal(String label, JournalType type) {
    }

    /** Journaux proposes a la caissiere (celui de la caisse en premier). */
    @GetMapping("/registers/{registerCode}/journals")
    public ResponseEntity<List<AccountingResponses.Journal>> journals(@PathVariable String registerCode) {
        return ResponseEntity.ok(cash.journalsFor(registerCode));
    }

    @Audited(module = "ACCOUNTING", action = "CASH_JOURNAL_CREATE", ressource = "acc_journal")
    @PostMapping("/registers/{registerCode}/journals")
    public ResponseEntity<AccountingResponses.Journal> createJournal(@PathVariable String registerCode, @RequestBody NewJournal request) {
        return ResponseEntity.ok(cash.createJournalFor(registerCode, request.label(), request.type()));
    }

    @Audited(module = "ACCOUNTING", action = "CASH_JOURNAL_AUTHORIZE", ressource = "acc_journal_register")
    @PutMapping("/registers/{registerCode}/journals/{journalCode}")
    public ResponseEntity<List<AccountingResponses.Journal>> authorize(@PathVariable String registerCode, @PathVariable String journalCode) {
        cash.authorize(journalCode, registerCode);
        return ResponseEntity.ok(cash.journalsFor(registerCode));
    }

    @Audited(module = "ACCOUNTING", action = "CASH_JOURNAL_REVOKE", ressource = "acc_journal_register")
    @DeleteMapping("/registers/{registerCode}/journals/{journalCode}")
    public ResponseEntity<List<AccountingResponses.Journal>> revoke(@PathVariable String registerCode, @PathVariable String journalCode) {
        cash.revoke(journalCode, registerCode);
        return ResponseEntity.ok(cash.journalsFor(registerCode));
    }

    /** Solde comptable de chaque caisse face a son solde operationnel. */
    @GetMapping("/reconciliation")
    public ResponseEntity<List<CashAccountingService.RegisterReconciliation>> reconciliation() {
        return ResponseEntity.ok(cash.reconciliation());
    }

    /** Transmet tout de suite les faits de caisse en attente au moteur. */
    @Audited(module = "ACCOUNTING", action = "CASH_FEED", ressource = "cash_movement")
    @PostMapping("/feed")
    public ResponseEntity<PostingDtos.BatchResult> feed() {
        return ResponseEntity.ok(cash.feed());
    }
}
