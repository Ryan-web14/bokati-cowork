package com.sni.bokaticowork.features.payment.compliance.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.payment.compliance.model.ComplianceAuditEntry;
import com.sni.bokaticowork.features.payment.compliance.model.ComplianceCase;
import com.sni.bokaticowork.features.payment.compliance.model.ComplianceCaseNote;
import com.sni.bokaticowork.features.payment.compliance.model.ComplianceFreeze;
import com.sni.bokaticowork.features.payment.compliance.model.ComplianceRule;
import com.sni.bokaticowork.features.payment.compliance.model.ScreeningCheck;
import com.sni.bokaticowork.features.payment.compliance.repository.ComplianceCaseRepository;
import com.sni.bokaticowork.features.payment.compliance.repository.ComplianceRuleRepository;
import com.sni.bokaticowork.features.payment.control.model.WalletRiskFlag;
import com.sni.bokaticowork.features.payment.control.repository.WalletRiskFlagRepository;
import com.sni.bokaticowork.features.payment.model.WalletAccount;
import com.sni.bokaticowork.features.payment.model.WalletLedgerEntry;
import com.sni.bokaticowork.features.payment.repository.WalletAccountRepository;
import com.sni.bokaticowork.features.payment.repository.WalletLedgerEntryRepository;
import com.sni.bokaticowork.features.payment.service.support.TransactionContextResolver;
import com.sni.bokaticowork.features.payment.transfer.model.WalletTransfer;
import com.sni.bokaticowork.features.payment.transfer.repository.WalletTransferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * L'onglet conformite du centre de controle, et l'export d'un dossier complet.
 *
 * <p>Le tableau de bord dit ce qui attend : dossiers ouverts par anciennete et priorite, dossiers
 * hors delai, regles les plus declenchantes, taux de faux positifs par regle. Cette derniere mesure
 * est celle qui garde le module vivant.</p>
 *
 * <p>L'export d'un dossier n'a pas besoin d'etre automatise, mais il doit exister avant qu'on le
 * demande, parce que le jour ou on le demande, le delai est court. Il reunit tout ce qu'un tiers
 * aura besoin de lire : le dossier, ses notes, ses signalements, la piste d'audit du sujet, les
 * controles de listes, les gels, et l'activite du portefeuille sur la periode.</p>
 */
@Service
@RequiredArgsConstructor
public class ComplianceDashboardService {

    private final ComplianceCaseRepository caseRepository;
    private final ComplianceRuleRepository ruleRepository;
    private final WalletRiskFlagRepository flagRepository;
    private final WalletAccountRepository walletRepository;
    private final WalletLedgerEntryRepository ledgerRepository;
    private final WalletTransferRepository transferRepository;
    private final ComplianceCaseService caseService;
    private final ComplianceAuditService auditService;
    private final ScreeningService screeningService;
    private final ComplianceFreezeService freezeService;
    private final TransactionContextResolver contextResolver;

    public record RuleStat(String ruleCode, String name, ComplianceRule.Action action, long hits, long confirmed,
                           long dismissed, BigDecimal falsePositiveRate, long hitsLast30Days) {
    }

    public record Dashboard(
            long openCases,
            long overdueCases,
            Map<String, Long> openByPriority,
            Map<String, Long> openByAge,
            long unreviewedListMatches,
            long activeFreezes,
            List<RuleStat> rules
    ) {
    }

    @Transactional(readOnly = true)
    public Dashboard dashboard() {
        Map<String, Long> byPriority = new LinkedHashMap<>();
        for (Object[] row : caseRepository.countOpenByPriority()) {
            byPriority.put(String.valueOf(row[0]), ((Number) row[1]).longValue());
        }
        Map<String, Long> byAge = new LinkedHashMap<>();
        Object[] age = unwrap(caseRepository.openByAge());
        String[] labels = {"moins d'un jour", "1 à 7 jours", "7 à 30 jours", "plus de 30 jours"};
        for (int i = 0; i < labels.length; i++) {
            byAge.put(labels[i], age == null || age.length <= i || age[i] == null ? 0L : ((Number) age[i]).longValue());
        }

        Map<String, Long> recent = new LinkedHashMap<>();
        for (Object[] row : flagRepository.countByRuleSince(Instant.now().minus(Duration.ofDays(30)))) {
            recent.put(String.valueOf(row[0]), ((Number) row[1]).longValue());
        }
        List<RuleStat> rules = new ArrayList<>();
        for (ComplianceRule rule : ruleRepository.findAllByOrderByRuleCodeAsc()) {
            rules.add(new RuleStat(rule.getRuleCode(), rule.getName(), rule.getAction(), rule.getHitCount(),
                    rule.getConfirmedCount(), rule.getDismissedCount(), rule.falsePositiveRate(),
                    recent.getOrDefault(rule.getRuleCode(), 0L)));
        }
        rules.sort((a, b) -> Long.compare(b.hitsLast30Days(), a.hitsLast30Days()));

        return new Dashboard(
                caseService.countOpen(),
                caseService.countOverdue(),
                byPriority,
                byAge,
                screeningService.countUnreviewedMatches(),
                freezeService.active(PageRequest.of(0, 1)).getTotalElements(),
                rules);
    }

    /**
     * Le dossier complet, en JSON lisible.
     *
     * <p>Les entites sont recopiees champ a champ dans des cartes : un tiers lit un document, pas
     * un graphe d'objets, et l'export ne doit pas changer de forme parce qu'une entite a gagne une
     * relation.</p>
     */
    @Transactional(readOnly = true)
    public byte[] exportCase(String caseNumber, int activityDays) {
        ComplianceCase complianceCase = caseService.get(caseNumber);
        Map<String, Object> export = new LinkedHashMap<>();
        export.put("exportedAt", Instant.now().toString());
        export.put("case", caseView(complianceCase));
        export.put("notes", caseService.notes(caseNumber).stream().map(this::noteView).toList());
        export.put("flags", ComplianceCaseService.flagNumbers(complianceCase.getTriggeredByFlags()).stream()
                .map(number -> flagRepository.findByFlagNumber(number).map(this::flagView).orElse(Map.of("flagNumber", number, "missing", true)))
                .toList());

        WalletAccount wallet = complianceCase.getWallet();
        if (wallet != null) {
            export.put("wallet", walletView(wallet));
            export.put("holder", holderView(wallet));
            export.put("screeningChecks", screeningService.history(ScreeningService.SUBJECT_MEMBER, wallet.getOwnerCode())
                    .stream().map(this::checkView).toList());
            export.put("freezes", freezeService.history(wallet.getId()).stream().map(this::freezeView).toList());
            Instant from = Instant.now().minus(Duration.ofDays(Math.max(1, activityDays)));
            export.put("ledger", ledgerRepository.findBetween(wallet.getId(), from, Instant.now()).stream().map(this::entryView).toList());
            List<Map<String, Object>> transfers = new ArrayList<>();
            transferRepository.findCompletedFromSince(wallet.getId(), from).forEach(t -> transfers.add(transferView(t, "OUT")));
            transferRepository.findCompletedToSince(wallet.getId(), from).forEach(t -> transfers.add(transferView(t, "IN")));
            export.put("transfers", transfers);
            export.put("auditTrail", auditService.trail(ComplianceCaseService.SUBJECT_WALLET, wallet.getWalletNumber())
                    .stream().map(this::auditView).toList());
        }

        try {
            ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
            mapper.findAndRegisterModules();
            return mapper.writeValueAsBytes(export);
        } catch (Exception ex) {
            throw new BadRequestException("Impossible de produire l'export du dossier", ex);
        }
    }

    // -----------------------------------------------------------------------------------------

    private Map<String, Object> caseView(ComplianceCase c) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("caseNumber", c.getCaseNumber());
        view.put("subjectType", c.getSubjectType());
        view.put("subjectCode", c.getSubjectCode());
        view.put("title", c.getTitle());
        view.put("status", c.getStatus());
        view.put("priority", c.getPriority());
        view.put("openedBy", c.getOpenedBy());
        view.put("createdAt", c.getCreatedAt());
        view.put("dueAt", c.getDueAt());
        view.put("assignedTo", c.getAssignedTo());
        view.put("assignedAt", c.getAssignedAt());
        view.put("escalatedTo", c.getEscalatedTo());
        view.put("escalatedAt", c.getEscalatedAt());
        view.put("findings", c.getFindings());
        view.put("decision", c.getDecision());
        view.put("decisionRationale", c.getDecisionRationale());
        view.put("decidedBy", c.getDecidedBy());
        view.put("decidedAt", c.getDecidedAt());
        return view;
    }

    private Map<String, Object> noteView(ComplianceCaseNote n) {
        return Map.of("author", n.getAuthor(), "note", n.getNote(), "createdAt", n.getCreatedAt());
    }

    private Map<String, Object> flagView(WalletRiskFlag f) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("flagNumber", f.getFlagNumber());
        view.put("type", f.getFlagType());
        view.put("severity", f.getSeverity());
        view.put("status", f.getStatus());
        view.put("ruleCode", f.getRuleCode());
        view.put("details", f.getDetails());
        view.put("reference", f.getReference());
        view.put("detectedAt", f.getDetectedAt());
        view.put("detectedBy", f.getDetectedBy());
        view.put("reviewedBy", f.getReviewedBy());
        view.put("reviewedAt", f.getReviewedAt());
        view.put("resolution", f.getResolution());
        return view;
    }

    private Map<String, Object> walletView(WalletAccount w) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("walletNumber", w.getWalletNumber());
        view.put("ownerType", w.getOwnerType());
        view.put("ownerCode", w.getOwnerCode());
        view.put("currency", w.getCurrency());
        view.put("status", w.getStatus());
        view.put("ledgerBalance", w.getLedgerBalance());
        view.put("availableBalance", w.getAvailableBalance());
        view.put("heldBalance", w.getHeldBalance());
        view.put("openedAt", w.getOpenedAt());
        view.put("frozenAt", w.getFrozenAt());
        view.put("frozenReason", w.getFrozenReason());
        view.put("lockedByOwnerAt", w.getLockedByOwnerAt());
        view.put("dormantSince", w.getDormantSince());
        view.put("limitPolicyCode", w.getLimitPolicyCode());
        return view;
    }

    private Map<String, Object> holderView(WalletAccount w) {
        TransactionContextResolver.PartyView party = contextResolver.resolveParty(w.getOwnerType(), w.getOwnerCode());
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("type", party.type());
        view.put("code", party.code());
        view.put("name", party.name());
        view.put("email", party.email());
        view.put("phone", party.phone());
        return view;
    }

    private Map<String, Object> checkView(ScreeningCheck c) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("checkNumber", c.getCheckNumber());
        view.put("subjectName", c.getSubjectName());
        view.put("trigger", c.getTriggerReason());
        view.put("listsChecked", c.getListsChecked());
        view.put("result", c.getResult());
        view.put("matchDetails", c.getMatchDetails());
        view.put("checkedAt", c.getCheckedAt());
        view.put("checkedBy", c.getCheckedBy());
        view.put("reviewedBy", c.getReviewedBy());
        view.put("reviewedAt", c.getReviewedAt());
        view.put("reviewOutcome", c.getReviewOutcome());
        return view;
    }

    private Map<String, Object> freezeView(ComplianceFreeze f) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("freezeNumber", f.getFreezeNumber());
        view.put("authority", f.getAuthority());
        view.put("instructionReference", f.getInstructionReference());
        view.put("instructionDate", f.getInstructionDate());
        view.put("rationale", f.getRationale());
        view.put("frozenBy", f.getFrozenBy());
        view.put("frozenAt", f.getFrozenAt());
        view.put("liftedBy", f.getLiftedBy());
        view.put("liftedAt", f.getLiftedAt());
        view.put("liftReference", f.getLiftReference());
        view.put("liftRationale", f.getLiftRationale());
        return view;
    }

    private Map<String, Object> entryView(WalletLedgerEntry e) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("transactionNumber", e.getTransactionNumber());
        view.put("entryNumber", e.getEntryNumber());
        view.put("direction", e.getDirection());
        view.put("type", e.getEntryType());
        view.put("amount", e.getAmount());
        view.put("balanceAfter", e.getBalanceAfter());
        view.put("sourceType", e.getSourceType());
        view.put("sourceCode", e.getSourceCode());
        view.put("reference", e.getReference());
        view.put("createdAt", e.getCreatedAt());
        view.put("hash", e.getCurrentHash());
        return view;
    }

    private Map<String, Object> transferView(WalletTransfer t, String direction) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("transferNumber", t.getTransferNumber());
        view.put("direction", direction);
        view.put("from", t.getSourceWallet().getWalletNumber());
        view.put("to", t.getTargetWallet().getWalletNumber());
        view.put("amount", t.getAmount());
        view.put("fee", t.getFeeAmount());
        view.put("message", t.getMessage());
        view.put("deviceId", t.getDeviceId());
        view.put("ipAddress", t.getIpAddress());
        view.put("completedAt", t.getCompletedAt());
        return view;
    }

    private Map<String, Object> auditView(ComplianceAuditEntry a) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("entryUuid", a.getEntryUuid());
        view.put("occurredAt", a.getOccurredAt());
        view.put("actor", a.getActor());
        view.put("action", a.getAction());
        view.put("beforeState", a.getBeforeState());
        view.put("afterState", a.getAfterState());
        view.put("rationale", a.getRationale());
        view.put("hash", a.getCurrentHash());
        return view;
    }

    private Object[] unwrap(Object[] row) {
        if (row != null && row.length == 1 && row[0] instanceof Object[] inner) {
            return inner;
        }
        return row;
    }
}
