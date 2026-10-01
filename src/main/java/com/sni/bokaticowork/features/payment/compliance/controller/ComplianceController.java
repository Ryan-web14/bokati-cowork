package com.sni.bokaticowork.features.payment.compliance.controller;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.payment.compliance.dto.ComplianceDtos.AssignRequest;
import com.sni.bokaticowork.features.payment.compliance.dto.ComplianceDtos.CaseView;
import com.sni.bokaticowork.features.payment.compliance.dto.ComplianceDtos.CheckView;
import com.sni.bokaticowork.features.payment.compliance.dto.ComplianceDtos.CloseRequest;
import com.sni.bokaticowork.features.payment.compliance.dto.ComplianceDtos.EscalateRequest;
import com.sni.bokaticowork.features.payment.compliance.dto.ComplianceDtos.FreezeRequest;
import com.sni.bokaticowork.features.payment.compliance.dto.ComplianceDtos.FreezeView;
import com.sni.bokaticowork.features.payment.compliance.dto.ComplianceDtos.LiftRequest;
import com.sni.bokaticowork.features.payment.compliance.dto.ComplianceDtos.ListEntryRequest;
import com.sni.bokaticowork.features.payment.compliance.dto.ComplianceDtos.LoadListRequest;
import com.sni.bokaticowork.features.payment.compliance.dto.ComplianceDtos.NoteRequest;
import com.sni.bokaticowork.features.payment.compliance.dto.ComplianceDtos.NoteView;
import com.sni.bokaticowork.features.payment.compliance.dto.ComplianceDtos.RuleUpdateRequest;
import com.sni.bokaticowork.features.payment.compliance.dto.ComplianceDtos.RuleView;
import com.sni.bokaticowork.features.payment.compliance.dto.ComplianceDtos.ScreenRequest;
import com.sni.bokaticowork.features.payment.compliance.dto.ComplianceDtos.ScreeningReviewRequest;
import com.sni.bokaticowork.features.payment.compliance.model.ScreeningCheck;
import com.sni.bokaticowork.features.payment.compliance.service.ComplianceAuditService;
import com.sni.bokaticowork.features.payment.compliance.service.ComplianceCaseService;
import com.sni.bokaticowork.features.payment.compliance.service.ComplianceDashboardService;
import com.sni.bokaticowork.features.payment.compliance.service.ComplianceFreezeService;
import com.sni.bokaticowork.features.payment.compliance.service.ComplianceRuleEngine;
import com.sni.bokaticowork.features.payment.compliance.service.ComplianceRuleService;
import com.sni.bokaticowork.features.payment.compliance.service.ScreeningService;
import com.sni.bokaticowork.features.payment.model.WalletAccount;
import com.sni.bokaticowork.features.payment.repository.WalletAccountRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * L'onglet conformite du centre de controle.
 *
 * <p>Chaque action porte le nom de qui l'a faite, tire de l'authentification et jamais du corps de
 * la requete. C'est ce nom qui entre dans la piste d'audit, et la piste d'audit est ce qu'on
 * montrera le jour ou on nous le demandera.</p>
 */
@RestController
@RequestMapping(ApiPath.V1 + "/wallets/compliance")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
public class ComplianceController {

    private final ComplianceDashboardService dashboardService;
    private final ComplianceRuleService ruleService;
    private final ComplianceRuleEngine ruleEngine;
    private final ComplianceCaseService caseService;
    private final ScreeningService screeningService;
    private final ComplianceFreezeService freezeService;
    private final ComplianceAuditService auditService;
    private final WalletAccountRepository walletRepository;

    // ---- Vue d'ensemble ---------------------------------------------------------------------

    @GetMapping("/dashboard")
    public ResponseEntity<ComplianceDashboardService.Dashboard> dashboard() {
        return ResponseEntity.ok(dashboardService.dashboard());
    }

    // ---- Regles ------------------------------------------------------------------------------

    @GetMapping("/rules")
    public ResponseEntity<List<RuleView>> rules() {
        return ResponseEntity.ok(ruleService.all().stream().map(RuleView::of).toList());
    }

    @PutMapping("/rules/{ruleCode}")
    public ResponseEntity<RuleView> updateRule(@PathVariable String ruleCode, @Valid @RequestBody RuleUpdateRequest request,
                                               Authentication authentication) {
        return ResponseEntity.ok(RuleView.of(ruleService.update(ruleCode, request, actor(authentication))));
    }

    /** Passe les regles sur un portefeuille, a la demande · pour comprendre ce qu'elles diraient. */
    @PostMapping("/wallets/{walletNumber}/evaluate")
    public ResponseEntity<List<ComplianceRuleEngine.Outcome>> evaluate(@PathVariable String walletNumber) {
        WalletAccount wallet = wallet(walletNumber);
        return ResponseEntity.ok(ruleEngine.evaluate(wallet.getId(), null));
    }

    // ---- Dossiers ----------------------------------------------------------------------------

    @GetMapping("/cases")
    public ResponseEntity<PaginatedResponse<CaseView>> cases(@RequestParam(defaultValue = "open") String scope,
                                                             @PageableDefault(size = 20) Pageable pageable,
                                                             Authentication authentication) {
        Pageable unsorted = unsorted(pageable);
        var page = switch (scope) {
            case "all" -> caseService.all(unsorted);
            case "mine" -> caseService.mine(actor(authentication), unsorted);
            default -> caseService.open(unsorted);
        };
        return ResponseEntity.ok(new PaginatedResponse<>(page.map(CaseView::of)));
    }

    @GetMapping("/cases/{caseNumber}")
    public ResponseEntity<CaseView> getCase(@PathVariable String caseNumber) {
        return ResponseEntity.ok(CaseView.of(caseService.get(caseNumber)));
    }

    @GetMapping("/cases/{caseNumber}/notes")
    public ResponseEntity<List<NoteView>> notes(@PathVariable String caseNumber) {
        return ResponseEntity.ok(caseService.notes(caseNumber).stream().map(NoteView::of).toList());
    }

    @PostMapping("/cases/{caseNumber}/assign")
    public ResponseEntity<CaseView> assign(@PathVariable String caseNumber, @Valid @RequestBody AssignRequest request,
                                           Authentication authentication) {
        return ResponseEntity.ok(CaseView.of(caseService.assign(caseNumber, request.assignee(), actor(authentication))));
    }

    @PostMapping("/cases/{caseNumber}/notes")
    public ResponseEntity<NoteView> addNote(@PathVariable String caseNumber, @Valid @RequestBody NoteRequest request,
                                            Authentication authentication) {
        return ResponseEntity.ok(NoteView.of(caseService.addNote(caseNumber, actor(authentication), request.note())));
    }

    @PostMapping("/cases/{caseNumber}/escalate")
    public ResponseEntity<CaseView> escalate(@PathVariable String caseNumber, @Valid @RequestBody EscalateRequest request,
                                             Authentication authentication) {
        return ResponseEntity.ok(CaseView.of(caseService.escalate(caseNumber, request.escalatedTo(), request.reason(), actor(authentication))));
    }

    @PostMapping("/cases/{caseNumber}/close")
    public ResponseEntity<CaseView> close(@PathVariable String caseNumber, @Valid @RequestBody CloseRequest request,
                                          Authentication authentication) {
        return ResponseEntity.ok(CaseView.of(caseService.close(caseNumber, request.decision(), request.rationale(),
                request.findings(), actor(authentication))));
    }

    @GetMapping(value = "/cases/{caseNumber}/export", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<byte[]> export(@PathVariable String caseNumber,
                                         @RequestParam(defaultValue = "90") int activityDays,
                                         Authentication authentication) {
        byte[] json = dashboardService.exportCase(caseNumber, activityDays);
        auditService.record(actor(authentication), "CASE_EXPORTED", "COMPLIANCE_CASE", caseNumber,
                "export complet · " + activityDays + " jours d'activité");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"dossier-" + caseNumber + ".json\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(json);
    }

    @GetMapping("/wallets/{walletNumber}/cases")
    public ResponseEntity<List<CaseView>> walletCases(@PathVariable String walletNumber) {
        return ResponseEntity.ok(caseService.forSubject(walletNumber).stream().map(CaseView::of).toList());
    }

    // ---- Listes ------------------------------------------------------------------------------

    @PostMapping("/screening/lists")
    public ResponseEntity<java.util.Map<String, Object>> loadList(@Valid @RequestBody LoadListRequest request,
                                                                  Authentication authentication) {
        int loaded = screeningService.loadList(request.listCode(), request.listVersion(), request.type(),
                request.entries().stream().map(ListEntryRequest::toInput).toList(), actor(authentication));
        return ResponseEntity.ok(java.util.Map.of("listCode", request.listCode(), "listVersion", request.listVersion(),
                "loaded", loaded, "listsInForce", screeningService.listsChecked()));
    }

    @PostMapping("/screening/checks")
    public ResponseEntity<CheckView> screen(@Valid @RequestBody ScreenRequest request, Authentication authentication) {
        return ResponseEntity.ok(CheckView.of(screeningService.screen(request.subjectType(), request.subjectCode(),
                request.fullName(), request.birthYear(), ScreeningCheck.Trigger.MANUAL, actor(authentication))));
    }

    @GetMapping("/screening/checks")
    public ResponseEntity<PaginatedResponse<CheckView>> checks(@RequestParam(defaultValue = "pending") String scope,
                                                               @PageableDefault(size = 20) Pageable pageable) {
        var page = "all".equals(scope) ? screeningService.all(unsorted(pageable)) : screeningService.pendingReview(unsorted(pageable));
        return ResponseEntity.ok(new PaginatedResponse<>(page.map(CheckView::of)));
    }

    @GetMapping("/screening/subjects/{subjectType}/{subjectCode}")
    public ResponseEntity<List<CheckView>> subjectChecks(@PathVariable String subjectType, @PathVariable String subjectCode) {
        return ResponseEntity.ok(screeningService.history(subjectType, subjectCode).stream().map(CheckView::of).toList());
    }

    @PostMapping("/screening/checks/{checkNumber}/review")
    public ResponseEntity<CheckView> reviewCheck(@PathVariable String checkNumber, @Valid @RequestBody ScreeningReviewRequest request,
                                                 Authentication authentication) {
        return ResponseEntity.ok(CheckView.of(screeningService.review(checkNumber, request.trueMatch(), request.rationale(), actor(authentication))));
    }

    // ---- Gel sur instruction -----------------------------------------------------------------

    @PostMapping("/wallets/{walletNumber}/freeze")
    public ResponseEntity<FreezeView> freeze(@PathVariable String walletNumber, @Valid @RequestBody FreezeRequest request,
                                             Authentication authentication) {
        return ResponseEntity.ok(FreezeView.of(freezeService.freeze(walletNumber,
                new ComplianceFreezeService.FreezeOrder(request.authority(), request.instructionReference(),
                        request.instructionDate(), request.rationale()), actor(authentication))));
    }

    @PostMapping("/freezes/{freezeNumber}/lift")
    public ResponseEntity<FreezeView> lift(@PathVariable String freezeNumber, @Valid @RequestBody LiftRequest request,
                                           Authentication authentication) {
        return ResponseEntity.ok(FreezeView.of(freezeService.lift(freezeNumber, request.liftReference(), request.liftRationale(), actor(authentication))));
    }

    @GetMapping("/freezes")
    public ResponseEntity<PaginatedResponse<FreezeView>> freezes(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(new PaginatedResponse<>(freezeService.active(unsorted(pageable)).map(FreezeView::of)));
    }

    @GetMapping("/wallets/{walletNumber}/freezes")
    public ResponseEntity<List<FreezeView>> walletFreezes(@PathVariable String walletNumber) {
        return ResponseEntity.ok(freezeService.history(wallet(walletNumber).getId()).stream().map(FreezeView::of).toList());
    }

    // ---- Piste d'audit -----------------------------------------------------------------------

    @GetMapping("/audit/{subjectType}/{subjectCode}")
    public ResponseEntity<List<com.sni.bokaticowork.features.payment.compliance.model.ComplianceAuditEntry>> trail(
            @PathVariable String subjectType, @PathVariable String subjectCode) {
        return ResponseEntity.ok(auditService.trail(subjectType, subjectCode));
    }

    @GetMapping("/audit/verify")
    public ResponseEntity<ComplianceAuditService.ChainReport> verifyAudit() {
        return ResponseEntity.ok(auditService.verify());
    }

    // -----------------------------------------------------------------------------------------

    private WalletAccount wallet(String walletNumber) {
        return walletRepository.findByWalletNumber(walletNumber)
                .orElseThrow(() -> new com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException("Portefeuille introuvable"));
    }

    private String actor(Authentication authentication) {
        return authentication == null || authentication.getName() == null ? "ADMIN" : authentication.getName();
    }

    private Pageable unsorted(Pageable pageable) {
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
    }
}
