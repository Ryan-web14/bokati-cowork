package com.sni.bokaticowork.features.accounting.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.accounting.dto.AccountingResponses;
import com.sni.bokaticowork.features.accounting.dto.PostingDtos;
import com.sni.bokaticowork.features.accounting.repository.AccPostingEventRepository;
import com.sni.bokaticowork.features.accounting.service.implementation.AccountingLetteringService;
import com.sni.bokaticowork.features.accounting.service.implementation.PostingEngine;
import com.sni.bokaticowork.features.accounting.service.implementation.SalesAccountingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Ventes et encaissements (lot F4) : controles, lettrage automatique, propositions, etat comptable d'un fait. */
@RestController
@RequestMapping(ApiPath.V1 + "/accounting/sales")
@RequiredArgsConstructor
public class SalesAccountingController {

    private final SalesAccountingService sales;
    private final AccountingLetteringService lettering;
    private final AccPostingEventRepository events;
    private final PostingEngine engine;

    /** 4191 face aux portefeuilles, reste du des factures face au 411, faits non comptabilises. */
    @GetMapping("/checks")
    public ResponseEntity<SalesAccountingService.Checks> checks(@RequestParam(defaultValue = "100") int limit) {
        return ResponseEntity.ok(sales.checks(Math.max(1, Math.min(1000, limit))));
    }

    /** Etat comptable d'un fait (facture, avoir, transaction, ecriture de portefeuille) par sa reference. */
    @GetMapping("/status/{reference}")
    public ResponseEntity<List<PostingDtos.Event>> status(@PathVariable String reference) {
        return ResponseEntity.ok(events.findByReferenceOrderByIdAsc(reference.trim()).stream().map(engine::toResponse).toList());
    }

    /** Propositions de lettrage (meme tiers, meme montant, sans reference commune). */
    @GetMapping("/lettering-proposals")
    public ResponseEntity<List<AccountingLetteringService.Proposal>> proposals(@RequestParam(defaultValue = "411") String accountNumber,
                                                                              @RequestParam(defaultValue = "100") int limit) {
        return ResponseEntity.ok(lettering.proposals(accountNumber, Math.max(1, Math.min(500, limit))));
    }

    @Audited(module = "ACCOUNTING", action = "AUTO_LETTER", ressource = "acc_lettering")
    @PostMapping("/auto-letter")
    public ResponseEntity<PostingDtos.BatchResult> autoLetter() {
        return ResponseEntity.ok(sales.autoLetter());
    }

    @Audited(module = "ACCOUNTING", action = "SALES_FEED", ressource = "acc_posting_event")
    @PostMapping("/feed")
    public ResponseEntity<PostingDtos.BatchResult> feed() {
        return ResponseEntity.ok(sales.feed());
    }
}
