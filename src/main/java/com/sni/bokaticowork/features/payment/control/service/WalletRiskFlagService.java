package com.sni.bokaticowork.features.payment.control.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.payment.control.model.WalletRiskFlag;
import com.sni.bokaticowork.features.payment.control.repository.WalletRiskFlagRepository;
import com.sni.bokaticowork.features.payment.enums.WalletStatus;
import com.sni.bokaticowork.features.payment.model.WalletAccount;
import com.sni.bokaticowork.features.payment.repository.WalletAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Les signalements · les lever, les revoir.
 *
 * <p>Lever un signalement ne fait jamais echouer l'operation qui l'a provoque : le signalement est
 * une information pour un humain, pas une decision. C'est pourquoi {@link #raise} tourne dans sa
 * propre transaction et avale ses propres erreurs · un transfert legitime ne doit pas tomber parce
 * que la table des signalements a un souci.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalletRiskFlagService {

    private static final List<WalletRiskFlag.Status> OPEN_STATUSES =
            List.of(WalletRiskFlag.Status.OPEN, WalletRiskFlag.Status.UNDER_REVIEW);

    private final WalletRiskFlagRepository flagRepository;
    private final WalletAccountRepository walletRepository;
    private final SequenceGeneratorFacade sequenceGenerator;

    /**
     * Leve un signalement, une seule fois par type tant qu'il est ouvert.
     *
     * <p>{@code REQUIRES_NEW} parce que l'appelant est au milieu d'une operation qu'on ne veut ni
     * bloquer ni annuler ; et l'index unique partiel tranche la course entre deux detections
     * simultanees, sans verrou.</p>
     *
     * <p><b>A ne pas appeler sous un verrou exclusif du compte.</b> L'insertion du signalement
     * reference le compte, donc en prend une cle partagee ; un {@code FOR UPDATE} tenu par la
     * transaction appelante la lui refuserait, et les deux s'attendraient. Depuis un chemin qui
     * tient le verrou, utiliser {@link #raiseAfterCommit}.</p>
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<WalletRiskFlag> raise(WalletAccount wallet, WalletRiskFlag.Type type,
                                          WalletRiskFlag.Severity severity, String details, String reference) {
        return raiseForRule(wallet, type, severity, details, reference, null).map(RaiseResult::flag);
    }

    /** Ce que {@link #raiseForRule} a fait · le signalement, et s'il vient d'etre cree ou existait deja. */
    public record RaiseResult(WalletRiskFlag flag, boolean created) {
    }

    /**
     * Comme {@link #raise}, en nommant la regle qui parle.
     *
     * <p>Un signalement deja ouvert pour le meme type n'est pas double, mais on dit qu'il existait :
     * une regle qui redetecte la meme situation a chaque operation n'a pas « touche » une seconde
     * fois, et ses compteurs ne doivent pas le croire.</p>
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<RaiseResult> raiseForRule(WalletAccount wallet, WalletRiskFlag.Type type,
                                              WalletRiskFlag.Severity severity, String details, String reference,
                                              String ruleCode) {
        try {
            Optional<WalletRiskFlag> existing = flagRepository
                    .findFirstByWallet_IdAndFlagTypeAndStatus(wallet.getId(), type, WalletRiskFlag.Status.OPEN);
            if (existing.isPresent()) {
                return Optional.of(new RaiseResult(existing.get(), false));
            }
            WalletRiskFlag flag = flagRepository.save(WalletRiskFlag.builder()
                    .flagNumber(sequenceGenerator.next("wallet_risk_flag"))
                    .wallet(wallet)
                    .flagType(type)
                    .severity(severity)
                    .status(WalletRiskFlag.Status.OPEN)
                    .details(truncate(details, 1000))
                    .reference(truncate(reference, 180))
                    .ruleCode(ruleCode)
                    .detectedBy(ruleCode == null ? "SYSTEM" : "RULE:" + ruleCode)
                    .build());
            log.warn("Portefeuille {} · signalement {} ({}) : {}", wallet.getWalletNumber(), type, severity, details);
            return Optional.of(new RaiseResult(flag, true));
        } catch (DataIntegrityViolationException ex) {
            // Une detection concurrente a gagne · le signalement existe, c'est ce qu'on voulait.
            return Optional.empty();
        } catch (RuntimeException ex) {
            log.error("Portefeuille {} · impossible de lever le signalement {} · l'operation continue",
                    wallet.getWalletNumber(), type, ex);
            return Optional.empty();
        }
    }

    /**
     * Leve le signalement une fois la transaction en cours validee.
     *
     * <p>Pour les chemins qui tiennent le verrou du compte · le grand livre, en premier lieu. Le
     * signalement part apres le commit, dans sa propre transaction ; si l'operation est annulee, il
     * ne part pas, et c'est juste : il n'y a plus rien a signaler.</p>
     */
    public void raiseAfterCommit(WalletAccount wallet, WalletRiskFlag.Type type,
                                 WalletRiskFlag.Severity severity, String details, String reference) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            raise(wallet, type, severity, details, reference);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                raise(wallet, type, severity, details, reference);
            }
        });
    }

    @Transactional(readOnly = true)
    public Page<WalletRiskFlag> search(Collection<WalletRiskFlag.Status> statuses, Pageable pageable) {
        return statuses == null || statuses.isEmpty()
                ? flagRepository.findAllByOrderBySeverityDescDetectedAtDesc(pageable)
                : flagRepository.findByStatusInOrderBySeverityDescDetectedAtDesc(statuses, pageable);
    }

    @Transactional(readOnly = true)
    public Page<WalletRiskFlag> forWallet(Long walletId, Pageable pageable) {
        return flagRepository.findByWallet_IdOrderByDetectedAtDesc(walletId, pageable);
    }

    @Transactional(readOnly = true)
    public long countOpen() {
        return flagRepository.countByStatusIn(OPEN_STATUSES);
    }

    /** Prend le signalement · et place le portefeuille en examen si demande. */
    @Transactional
    public WalletRiskFlag takeUnderReview(String flagNumber, String reviewer, boolean holdWallet) {
        WalletRiskFlag flag = open(flagNumber);
        flag.setStatus(WalletRiskFlag.Status.UNDER_REVIEW);
        flag.setReviewedBy(reviewer);
        flag.setReviewedAt(Instant.now());
        if (holdWallet) {
            WalletAccount wallet = walletRepository.findByIdForUpdate(flag.getWallet().getId()).orElseThrow();
            if (wallet.getStatus() == WalletStatus.ACTIVE) {
                wallet.setStatus(WalletStatus.UNDER_REVIEW);
                walletRepository.save(wallet);
            }
        }
        return flagRepository.save(flag);
    }

    /**
     * Conclut · confirme ou ecarte, avec une decision motivee.
     *
     * <p>Ecarter un signalement libere le portefeuille s'il n'etait en examen que pour lui. Le
     * confirmer ne fait rien de plus au portefeuille : ce qui suit une confirmation est une action
     * administrative, tracee comme telle, pas un effet de bord de la revue.</p>
     */
    @Transactional
    public WalletRiskFlag resolve(String flagNumber, String reviewer, boolean confirmed, String resolution) {
        if (!StringUtils.hasText(resolution) || resolution.trim().length() < 5) {
            throw new BadRequestException("Une décision motivée est requise");
        }
        WalletRiskFlag flag = open(flagNumber);
        flag.setStatus(confirmed ? WalletRiskFlag.Status.CONFIRMED : WalletRiskFlag.Status.DISMISSED);
        flag.setReviewedBy(reviewer);
        flag.setReviewedAt(Instant.now());
        flag.setResolution(resolution.trim());
        WalletRiskFlag saved = flagRepository.save(flag);

        if (!confirmed) {
            releaseIfNothingElseOpen(flag.getWallet().getId());
        }
        return saved;
    }

    private void releaseIfNothingElseOpen(Long walletId) {
        WalletAccount wallet = walletRepository.findByIdForUpdate(walletId).orElseThrow();
        if (wallet.getStatus() != WalletStatus.UNDER_REVIEW) {
            return;
        }
        boolean stillOpen = flagRepository.findByWallet_IdOrderByDetectedAtDesc(walletId, Pageable.ofSize(50))
                .stream().anyMatch(WalletRiskFlag::open);
        if (!stillOpen) {
            wallet.setStatus(WalletStatus.ACTIVE);
            walletRepository.save(wallet);
        }
    }

    private WalletRiskFlag open(String flagNumber) {
        WalletRiskFlag flag = flagRepository.findByFlagNumber(flagNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Signalement introuvable"));
        if (!flag.open()) {
            throw new BadRequestException("Ce signalement est déjà traité");
        }
        return flag;
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
