package com.sni.bokaticowork.features.payment.compliance.service;

import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.features.payment.compliance.detector.ComplianceDetectors;
import com.sni.bokaticowork.features.payment.compliance.detector.DetectionContext;
import com.sni.bokaticowork.features.payment.compliance.model.ComplianceCase;
import com.sni.bokaticowork.features.payment.compliance.model.ComplianceRule;
import com.sni.bokaticowork.features.payment.compliance.repository.ComplianceRuleRepository;
import com.sni.bokaticowork.features.payment.control.model.WalletRiskFlag;
import com.sni.bokaticowork.features.payment.control.repository.WalletRiskFlagRepository;
import com.sni.bokaticowork.features.payment.control.service.WalletRiskFlagService;
import com.sni.bokaticowork.features.payment.enums.WalletStatus;
import com.sni.bokaticowork.features.payment.model.WalletAccount;
import com.sni.bokaticowork.features.payment.repository.WalletAccountRepository;
import com.sni.bokaticowork.features.payment.repository.WalletLedgerEntryRepository;
import com.sni.bokaticowork.features.payment.transfer.repository.WalletDeviceRepository;
import com.sni.bokaticowork.features.payment.transfer.repository.WalletTransferRepository;
import com.sni.bokaticowork.features.payment.transfer.service.WalletNotifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Fait tourner les regles · sans qu'on les lance.
 *
 * <p>Deux moments. <b>Apres</b> une operation, une fois validee : les regles regardent ce qui vient
 * de se passer, signalent, ouvrent un dossier ou gelent selon ce que chacune prescrit. <b>Avant</b>
 * une operation, pour les seules regles qui bloquent : si l'une d'elles parle, l'operation ne
 * s'execute pas, et le refus dit pourquoi.</p>
 *
 * <p>Le passage d'apres tourne dans sa propre transaction, apres le commit de l'operation. Une
 * regle qui echoue n'annule rien : ce serait offrir a qui veut echapper a la surveillance un moyen
 * simple d'y parvenir · la faire planter.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ComplianceRuleEngine {

    private final ComplianceRuleRepository ruleRepository;
    private final ComplianceDetectors detectors;
    private final WalletAccountRepository walletRepository;
    private final WalletLedgerEntryRepository ledgerRepository;
    private final WalletTransferRepository transferRepository;
    private final WalletDeviceRepository deviceRepository;
    private final WalletRiskFlagRepository flagRepository;
    private final WalletRiskFlagService flagService;
    private final ComplianceCaseService caseService;
    private final ComplianceAuditService auditService;
    private final WalletNotifier notifier;

    /** Ce qu'un passage a produit · pour le journal et les tests. */
    public record Outcome(String ruleCode, ComplianceRule.Action action, String details, boolean newFlag) {
    }

    // -----------------------------------------------------------------------------------------
    // Apres l'operation
    // -----------------------------------------------------------------------------------------

    /** Evalue apres le commit de la transaction en cours · ou tout de suite s'il n'y en a pas. */
    public void evaluateAfterCommit(Long walletId, String deviceId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            evaluate(walletId, deviceId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    evaluate(walletId, deviceId);
                } catch (RuntimeException ex) {
                    log.error("Surveillance · evaluation du portefeuille {} en echec", walletId, ex);
                }
            }
        });
    }

    /** Passe toutes les regles actives sur un portefeuille. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<Outcome> evaluate(Long walletId, String deviceId) {
        WalletAccount wallet = walletRepository.findById(walletId).orElse(null);
        if (wallet == null || wallet.getStatus() == WalletStatus.CLOSED) {
            return List.of();
        }
        Instant now = Instant.now();
        List<Outcome> outcomes = new ArrayList<>();
        for (ComplianceRule rule : ruleRepository.findByActiveTrueOrderByRuleCodeAsc()) {
            if (!rule.activeAt(now) || !detectors.knows(rule.getDetector())) {
                continue;
            }
            try {
                DetectionContext context = new DetectionContext(wallet, rule, now, deviceId,
                        ledgerRepository, transferRepository, deviceRepository, flagRepository);
                detectors.run(rule.getDetector(), context)
                        .ifPresent(detection -> apply(wallet, rule, detection).ifPresent(outcomes::add));
            } catch (RuntimeException ex) {
                // Une regle qui plante ne doit pas faire taire les autres, ni l'operation.
                log.error("Surveillance · regle {} en echec sur {}", rule.getRuleCode(), wallet.getWalletNumber(), ex);
            }
        }
        return outcomes;
    }

    /**
     * Ce que la regle prescrit, une fois qu'elle a parle.
     *
     * <p>Le signalement est leve d'abord, quoi que la regle prescrive ensuite : c'est lui que la
     * revue retrouvera. Un signalement deja ouvert pour le meme type n'est pas double, et la regle
     * ne compte pas une touche de plus · elle redit la meme chose, elle ne dit rien de nouveau.</p>
     */
    private Optional<Outcome> apply(WalletAccount wallet, ComplianceRule rule, ComplianceDetectors.Detection detection) {
        Optional<WalletRiskFlagService.RaiseResult> raised = flagService.raiseForRule(wallet, detection.flagType(),
                severity(rule), rule.getRuleCode() + " · " + detection.details(), detection.reference(), rule.getRuleCode());
        if (raised.isEmpty()) {
            return Optional.empty();
        }
        WalletRiskFlag flag = raised.get().flag();
        boolean created = raised.get().created();
        if (created) {
            rule.setHitCount(rule.getHitCount() + 1);
            ruleRepository.save(rule);
        }

        switch (rule.getAction()) {
            case FLAG -> {
                // Rien de plus · quelqu'un regardera.
            }
            case REQUIRE_REVIEW, BLOCK_OPERATION -> {
                if (created || flag.getCaseNumber() == null) {
                    caseService.openOrAttach(wallet, flag, ComplianceCaseService.priorityFor(rule.getSeverity()),
                            rule.getName(), "RULE:" + rule.getRuleCode());
                }
            }
            case FREEZE_WALLET -> freeze(wallet, rule, flag, detection);
        }
        return Optional.of(new Outcome(rule.getRuleCode(), rule.getAction(), detection.details(), created));
    }

    /**
     * Gel de protection · pas un gel sur instruction.
     *
     * <p>Le portefeuille est fige et un dossier critique s'ouvre. La levee est une decision humaine,
     * tracee comme action administrative · le moteur sait geler, il ne sait pas degeler, et c'est
     * volontaire.</p>
     */
    private void freeze(WalletAccount wallet, ComplianceRule rule, WalletRiskFlag flag, ComplianceDetectors.Detection detection) {
        WalletAccount locked = walletRepository.findByIdForUpdate(wallet.getId()).orElse(wallet);
        if (locked.getFrozenAt() == null) {
            locked.setFrozenAt(Instant.now());
            locked.setFrozenReason("Règle " + rule.getRuleCode() + " · " + detection.details());
            if (locked.getStatus() == WalletStatus.ACTIVE) {
                locked.setStatus(WalletStatus.UNDER_REVIEW);
            }
            walletRepository.save(locked);
            auditService.record("RULE:" + rule.getRuleCode(), "WALLET_FROZEN_BY_RULE", ComplianceCaseService.SUBJECT_WALLET,
                    locked.getWalletNumber(), detection.details());
            notifier.securityEvent(locked, "WALLET_FROZEN", "Votre portefeuille est temporairement suspendu",
                    Map.of("reason", "Vérification en cours"));
        }
        caseService.openOrAttach(locked, flag, ComplianceCase.Priority.CRITICAL, rule.getName(), "RULE:" + rule.getRuleCode());
    }

    // -----------------------------------------------------------------------------------------
    // Avant l'operation
    // -----------------------------------------------------------------------------------------

    /**
     * Refuse l'operation si une regle bloquante parle.
     *
     * <p>Seules les regles dont l'action est {@code BLOCK_OPERATION} sont passees ici : les autres
     * regardent apres, sans gener. Le refus nomme la regle · un refus qui ne dit pas pourquoi se
     * contourne en essayant autrement, celui-ci dit ce qu'il faudrait pour comprendre.</p>
     */
    @Transactional(readOnly = true)
    public void assertNotBlocked(WalletAccount wallet, String deviceId) {
        Instant now = Instant.now();
        for (ComplianceRule rule : ruleRepository.findByActiveTrueOrderByRuleCodeAsc()) {
            if (rule.getAction() != ComplianceRule.Action.BLOCK_OPERATION || !rule.activeAt(now)
                    || !detectors.knows(rule.getDetector())) {
                continue;
            }
            DetectionContext context = new DetectionContext(wallet, rule, now, deviceId,
                    ledgerRepository, transferRepository, deviceRepository, flagRepository);
            Optional<ComplianceDetectors.Detection> detection = detectors.run(rule.getDetector(), context);
            if (detection.isPresent()) {
                throw new ConflictException("wallet", "opération refusée par la règle de surveillance « "
                        + rule.getName() + " » · " + detection.get().details());
            }
        }
    }

    private WalletRiskFlag.Severity severity(ComplianceRule rule) {
        return switch (rule.getSeverity()) {
            case CRITICAL -> WalletRiskFlag.Severity.CRITICAL;
            case HIGH -> WalletRiskFlag.Severity.HIGH;
            case MEDIUM -> WalletRiskFlag.Severity.MEDIUM;
            case LOW, INFO -> WalletRiskFlag.Severity.LOW;
        };
    }
}
