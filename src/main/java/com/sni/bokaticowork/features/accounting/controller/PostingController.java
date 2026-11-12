package com.sni.bokaticowork.features.accounting.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.accounting.dto.PostingDtos;
import com.sni.bokaticowork.features.accounting.enums.PostingEventStatus;
import com.sni.bokaticowork.features.accounting.service.implementation.PostingEngine;
import com.sni.bokaticowork.features.accounting.service.implementation.PostingRuleService;
import com.sni.bokaticowork.features.accounting.service.support.PostingTemplate;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/** Moteur d'imputation : schemas d'ecriture, evenements comptables, reprise (lot F2). */
@RestController
@RequestMapping(ApiPath.V1 + "/accounting/posting")
@RequiredArgsConstructor
public class PostingController {

    private final PostingRuleService rules;
    private final PostingEngine engine;
    private final PostingTemplate template;

    // --------------------------------------------------------------- schemas

    @GetMapping("/rules")
    public ResponseEntity<List<PostingDtos.Rule>> rules(@RequestParam(required = false) String eventType) {
        return ResponseEntity.ok(rules.rules(eventType));
    }

    @GetMapping("/rules/{code}/versions")
    public ResponseEntity<List<PostingDtos.Rule>> versions(@PathVariable String code) {
        return ResponseEntity.ok(rules.versions(code));
    }

    @Audited(module = "ACCOUNTING", action = "POSTING_RULE_SAVE", ressource = "acc_posting_rule")
    @PostMapping("/rules")
    public ResponseEntity<PostingDtos.Rule> save(@Valid @RequestBody PostingDtos.RuleRequest request) {
        return ResponseEntity.ok(rules.save(request));
    }

    @Audited(module = "ACCOUNTING", action = "POSTING_RULE_ACTIVATE", ressource = "acc_posting_rule")
    @PatchMapping("/rules/{id}/activate")
    public ResponseEntity<PostingDtos.Rule> activate(@PathVariable Long id) {
        return ResponseEntity.ok(rules.activate(id));
    }

    @Audited(module = "ACCOUNTING", action = "POSTING_RULE_RETIRE", ressource = "acc_posting_rule")
    @PatchMapping("/rules/{id}/retire")
    public ResponseEntity<PostingDtos.Rule> retire(@PathVariable Long id) {
        return ResponseEntity.ok(rules.retire(id));
    }

    @PostMapping("/preview")
    public ResponseEntity<PostingDtos.Preview> preview(@RequestBody PostingDtos.PreviewRequest request) {
        return ResponseEntity.ok(rules.preview(request));
    }

    @GetMapping("/resolvers")
    public ResponseEntity<List<PostingDtos.Resolver>> resolvers() {
        return ResponseEntity.ok(rules.resolvers());
    }

    // ------------------------------------------------------------ evenements

    @GetMapping("/events")
    public ResponseEntity<List<PostingDtos.Event>> events(@RequestParam(required = false) PostingEventStatus status,
                                                          @RequestParam(required = false) String eventType,
                                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                                          @RequestParam(defaultValue = "100") int limit) {
        return ResponseEntity.ok(rules.events(status, eventType, from, to, limit));
    }

    @GetMapping("/events/summary")
    public ResponseEntity<PostingDtos.EventSummary> summary() {
        return ResponseEntity.ok(rules.summary());
    }

    @GetMapping("/events/{id}")
    public ResponseEntity<PostingDtos.Event> event(@PathVariable Long id) {
        return ResponseEntity.ok(engine.toResponse(engine.event(id)));
    }

    @Audited(module = "ACCOUNTING", action = "POSTING_EVENT_REGENERATE", ressource = "acc_posting_event")
    @PatchMapping("/events/{id}/regenerate")
    public ResponseEntity<PostingDtos.Event> regenerate(@PathVariable Long id) {
        return ResponseEntity.ok(engine.toResponse(engine.regenerate(id)));
    }

    @Audited(module = "ACCOUNTING", action = "POSTING_EVENT_IGNORE", ressource = "acc_posting_event")
    @PatchMapping("/events/{id}/ignore")
    public ResponseEntity<PostingDtos.Event> ignore(@PathVariable Long id, @RequestBody PostingDtos.Reason request) {
        return ResponseEntity.ok(engine.toResponse(engine.ignore(id, request.reason())));
    }

    /** Evenement soumis a la main : test d'un schema de bout en bout, reprise ponctuelle. */
    @Audited(module = "ACCOUNTING", action = "POSTING_EVENT_SUBMIT", ressource = "acc_posting_event")
    @PostMapping("/events")
    public ResponseEntity<PostingDtos.Event> submit(@Valid @RequestBody PostingDtos.SubmitEvent request) {
        var event = engine.receive(request.eventType(), request.reference(), request.family(), request.eventDate(),
                request.payload() == null ? "{}" : template.write(request.payload()), Boolean.TRUE.equals(request.replay()));
        return ResponseEntity.ok(engine.toResponse(event));
    }

    @Audited(module = "ACCOUNTING", action = "POSTING_RUN", ressource = "acc_posting_event")
    @PostMapping("/run")
    public ResponseEntity<PostingDtos.BatchResult> run() {
        return ResponseEntity.ok(engine.processPending());
    }

    /** Validation en bloc des brouillards produits par la reprise (ou par un journal sans validation automatique). */
    @Audited(module = "ACCOUNTING", action = "POSTING_VALIDATE_DRAFTS", ressource = "acc_entry")
    @PostMapping("/drafts/validate")
    public ResponseEntity<PostingDtos.BatchResult> validateDrafts(@RequestParam(required = false) String journalCode,
                                                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(engine.validateDrafts(journalCode, to));
    }
}
