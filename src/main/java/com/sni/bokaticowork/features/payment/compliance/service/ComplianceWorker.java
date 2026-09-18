package com.sni.bokaticowork.features.payment.compliance.service;

import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.client.member.repository.repo.MemberRepository;
import com.sni.bokaticowork.features.payment.compliance.model.ComplianceCase;
import com.sni.bokaticowork.features.payment.compliance.model.ScreeningCheck;
import com.sni.bokaticowork.features.payment.compliance.repository.ScreeningCheckRepository;
import com.sni.bokaticowork.features.payment.control.model.WalletRiskFlag;
import com.sni.bokaticowork.features.payment.control.service.WalletRiskFlagService;
import com.sni.bokaticowork.features.payment.model.WalletAccount;
import com.sni.bokaticowork.features.payment.repository.WalletAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Ce que la conformite fait sans qu'on la lance.
 *
 * <p>Trois passages quotidiens. Le <b>recontrole periodique</b> des listes, parce qu'un nom absent
 * hier peut etre present aujourd'hui. La <b>reverification d'identite</b> des dossiers les plus
 * exposes · les portefeuilles au-dessus d'un encours · a intervalle fixe. Et le <b>rappel des
 * dossiers hors delai</b>, qui ne disparaissent pas dans la pile : ils remontent.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ComplianceWorker {

    private final WalletAccountRepository walletRepository;
    private final MemberRepository memberRepository;
    private final ScreeningCheckRepository checkRepository;
    private final ScreeningService screeningService;
    private final ComplianceCaseService caseService;
    private final WalletRiskFlagService flagService;

    @Value("${bokati.compliance.screening.recheck-days:180}")
    private int screeningRecheckDays;

    @Value("${bokati.compliance.identity.review-days:365}")
    private int identityReviewDays;

    /** Au-dessus de cet encours, le dossier est « expose » et se reverifie periodiquement. */
    @Value("${bokati.compliance.identity.exposure-threshold:1000000}")
    private BigDecimal exposureThreshold;

    /** Recontrole des listes · les titulaires de portefeuille dont le dernier controle est ancien. */
    @Scheduled(cron = "${bokati.compliance.screening.cron:0 40 3 * * *}")
    @Transactional(readOnly = true)
    public void periodicScreening() {
        Instant threshold = Instant.now().minus(Duration.ofDays(Math.max(1, screeningRecheckDays)));
        int rechecked = 0;
        for (Long walletId : walletRepository.findAllOpenIds()) {
            WalletAccount wallet = walletRepository.findById(walletId).orElse(null);
            if (wallet == null || !"MEMBER".equalsIgnoreCase(wallet.getOwnerType())) {
                continue;
            }
            boolean stale = checkRepository
                    .findFirstBySubjectTypeAndSubjectCodeOrderByCheckedAtDesc(ScreeningService.SUBJECT_MEMBER, wallet.getOwnerCode())
                    .map(check -> check.getCheckedAt().isBefore(threshold))
                    .orElse(true);
            if (!stale) {
                continue;
            }
            Member member = memberRepository.findByMemberIdAndDeletedFalse(wallet.getOwnerCode()).orElse(null);
            if (member == null) {
                continue;
            }
            screeningService.screen(ScreeningService.SUBJECT_MEMBER, member.getMemberId(),
                    ComplianceSignals.fullName(member), null, ScreeningCheck.Trigger.PERIODIC, "SYSTEM");
            rechecked++;
        }
        if (rechecked > 0) {
            log.info("Conformite · {} titulaire(s) recontrole(s) sur les listes", rechecked);
        }
    }

    /**
     * Reverification d'identite des dossiers exposes.
     *
     * <p>Un portefeuille au-dessus du seuil recoit une echeance s'il n'en a pas ; une echeance
     * depassee leve un signalement et ouvre un dossier de priorite basse. La revue conclura par
     * une decision motivee, et l'echeance repartira pour un tour.</p>
     */
    @Scheduled(cron = "${bokati.compliance.identity.cron:0 50 3 * * *}")
    @Transactional
    public void identityReview() {
        Instant now = Instant.now();
        int scheduled = 0;
        int due = 0;
        for (Long walletId : walletRepository.findAllOpenIds()) {
            WalletAccount wallet = walletRepository.findById(walletId).orElse(null);
            if (wallet == null || wallet.getLedgerBalance() == null || wallet.getLedgerBalance().compareTo(exposureThreshold) < 0) {
                continue;
            }
            if (wallet.getIdentityReviewDueAt() == null) {
                Instant from = wallet.getIdentityReviewedAt() == null ? now : wallet.getIdentityReviewedAt();
                wallet.setIdentityReviewDueAt(from.plus(Duration.ofDays(Math.max(30, identityReviewDays))));
                walletRepository.save(wallet);
                scheduled++;
                continue;
            }
            if (now.isAfter(wallet.getIdentityReviewDueAt())) {
                flagService.raise(wallet, WalletRiskFlag.Type.IDENTITY_REVIEW_DUE, WalletRiskFlag.Severity.LOW,
                                "Encours " + wallet.getLedgerBalance().stripTrailingZeros().toPlainString() + " " + wallet.getCurrency()
                                        + " · reverification echue le " + wallet.getIdentityReviewDueAt(), null)
                        .ifPresent(flag -> caseService.openOrAttach(wallet, flag, ComplianceCase.Priority.LOW,
                                "Reverification periodique d'identite", "SYSTEM"));
                // L'echeance repart · on ne redemande pas chaque nuit ce qu'on a deja demande.
                wallet.setIdentityReviewDueAt(now.plus(Duration.ofDays(Math.max(30, identityReviewDays))));
                walletRepository.save(wallet);
                due++;
            }
        }
        if (scheduled > 0 || due > 0) {
            log.info("Conformite · {} echeance(s) de reverification posee(s), {} echue(s) signalee(s)", scheduled, due);
        }
    }

    /** Les dossiers hors delai remontent · dans le journal ici, au tableau de bord toujours. */
    @Scheduled(cron = "${bokati.compliance.overdue.cron:0 0 8 * * *}")
    public void overdueCases() {
        List<ComplianceCase> overdue = caseService.overdue();
        if (overdue.isEmpty()) {
            return;
        }
        log.warn("Conformite · {} dossier(s) hors delai : {}", overdue.size(),
                overdue.stream().limit(20).map(c -> c.getCaseNumber() + " (" + c.getPriority() + ", "
                        + (c.getAssignedTo() == null ? "non confie" : c.getAssignedTo()) + ")").toList());
    }
}
