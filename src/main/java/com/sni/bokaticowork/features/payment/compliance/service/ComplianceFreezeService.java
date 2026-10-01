package com.sni.bokaticowork.features.payment.compliance.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.payment.compliance.model.ComplianceCase;
import com.sni.bokaticowork.features.payment.compliance.model.ComplianceFreeze;
import com.sni.bokaticowork.features.payment.compliance.repository.ComplianceFreezeRepository;
import com.sni.bokaticowork.features.payment.enums.WalletStatus;
import com.sni.bokaticowork.features.payment.model.WalletAccount;
import com.sni.bokaticowork.features.payment.repository.WalletAccountRepository;
import com.sni.bokaticowork.features.payment.transfer.service.WalletNotifier;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Le gel sur instruction · distinct de la suspension commerciale.
 *
 * <p>Une suspension est une decision de l'etablissement, qu'il peut lever seul et qui se trace comme
 * action administrative. Un gel sur instruction vient d'une autorite, porte sa reference, et ne se
 * leve que sur une autre reference · la levee se trace aussi, avec la sienne. Les deux figent le
 * portefeuille de la meme facon ; ce qui differe est qui repond de quoi.</p>
 *
 * <p>Un gel sur instruction ne se leve pas par la levee d'une suspension : l'administrateur qui
 * degelerait un portefeuille sans savoir qu'une autorite l'a fige commettrait une faute qu'il
 * n'aurait pas voulue. Le gel se marque donc dans la raison du compte, et la levee ordinaire le
 * refuse.</p>
 */
@Service
@RequiredArgsConstructor
public class ComplianceFreezeService {

    public static final String REASON_PREFIX = "INSTRUCTION ";

    private final ComplianceFreezeRepository freezeRepository;
    private final WalletAccountRepository walletRepository;
    private final ComplianceCaseService caseService;
    private final ComplianceAuditService auditService;
    private final WalletNotifier notifier;
    private final SequenceGeneratorFacade sequenceGenerator;

    public record FreezeOrder(String authority, String instructionReference, LocalDate instructionDate, String rationale) {
    }

    @Transactional
    public ComplianceFreeze freeze(String walletNumber, FreezeOrder order, String actor) {
        if (!StringUtils.hasText(order.authority()) || !StringUtils.hasText(order.instructionReference())
                || !StringUtils.hasText(order.rationale())) {
            throw new BadRequestException("Un gel sur instruction porte l'autorité, la référence de l'instruction et son motif");
        }
        WalletAccount wallet = walletRepository.findByWalletNumber(walletNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Portefeuille introuvable"));
        if (freezeRepository.findFirstByWallet_IdAndLiftedAtIsNull(wallet.getId()).isPresent()) {
            throw new ConflictException("wallet", "un gel sur instruction est déjà en cours · une seconde instruction s'ajoute au dossier");
        }

        WalletAccount locked = walletRepository.findByIdForUpdate(wallet.getId()).orElseThrow();
        String before = locked.getStatus() + "/" + (locked.getFrozenAt() == null ? "libre" : "gele");
        locked.setFrozenAt(Instant.now());
        locked.setFrozenReason(REASON_PREFIX + order.authority().trim() + " · " + order.instructionReference().trim());
        locked.setStatus(WalletStatus.LOCKED);
        walletRepository.save(locked);

        ComplianceFreeze freeze = freezeRepository.save(ComplianceFreeze.builder()
                .freezeNumber(sequenceGenerator.next("compliance_freeze"))
                .wallet(locked)
                .authority(order.authority().trim())
                .instructionReference(order.instructionReference().trim())
                .instructionDate(order.instructionDate())
                .rationale(order.rationale().trim())
                .frozenBy(actor)
                .build());

        ComplianceCase complianceCase = caseService.openOrAttach(locked, null, ComplianceCase.Priority.CRITICAL,
                "Gel sur instruction · " + order.authority().trim(), actor);
        freeze.setCaseNumber(complianceCase.getCaseNumber());
        freezeRepository.save(freeze);

        auditService.record(actor, null, "WALLET_FROZEN_BY_INSTRUCTION", ComplianceCaseService.SUBJECT_WALLET,
                locked.getWalletNumber(), before, "LOCKED/gele",
                freeze.getFreezeNumber() + " · " + order.authority().trim() + " · " + order.instructionReference().trim()
                        + " · " + order.rationale().trim());
        notifier.securityEvent(locked, "WALLET_FROZEN", "Votre portefeuille est suspendu",
                Map.of("reason", "Suspension sur instruction · contactez l'accueil"));
        return freeze;
    }

    @Transactional
    public ComplianceFreeze lift(String freezeNumber, String liftReference, String liftRationale, String actor) {
        if (!StringUtils.hasText(liftReference) || !StringUtils.hasText(liftRationale)) {
            throw new BadRequestException("La levée d'un gel sur instruction porte sa propre référence et son motif");
        }
        ComplianceFreeze freeze = freezeRepository.findByFreezeNumber(freezeNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Gel introuvable"));
        if (!freeze.active()) {
            throw new BadRequestException("Ce gel est déjà levé");
        }
        WalletAccount locked = walletRepository.findByIdForUpdate(freeze.getWallet().getId()).orElseThrow();
        locked.setFrozenAt(null);
        locked.setFrozenReason(null);
        if (locked.getStatus() == WalletStatus.LOCKED) {
            locked.setStatus(WalletStatus.ACTIVE);
        }
        walletRepository.save(locked);

        freeze.setLiftedBy(actor);
        freeze.setLiftedAt(Instant.now());
        freeze.setLiftReference(liftReference.trim());
        freeze.setLiftRationale(liftRationale.trim());
        ComplianceFreeze saved = freezeRepository.save(freeze);

        auditService.record(actor, null, "WALLET_FREEZE_LIFTED", ComplianceCaseService.SUBJECT_WALLET,
                locked.getWalletNumber(), "LOCKED/gele", locked.getStatus() + "/libre",
                saved.getFreezeNumber() + " · " + liftReference.trim() + " · " + liftRationale.trim());
        notifier.securityEvent(locked, "WALLET_UNSUSPENDED", "Votre portefeuille est de nouveau actif",
                Map.of("freezeNumber", saved.getFreezeNumber()));
        return saved;
    }

    /** Un gel sur instruction en cours · ce que la levee ordinaire doit refuser de toucher. */
    @Transactional(readOnly = true)
    public Optional<ComplianceFreeze> activeFreeze(Long walletId) {
        return freezeRepository.findFirstByWallet_IdAndLiftedAtIsNull(walletId);
    }

    @Transactional(readOnly = true)
    public List<ComplianceFreeze> history(Long walletId) {
        return freezeRepository.findByWallet_IdOrderByFrozenAtDesc(walletId);
    }

    @Transactional(readOnly = true)
    public Page<ComplianceFreeze> active(Pageable pageable) {
        return freezeRepository.findByLiftedAtIsNullOrderByFrozenAtDesc(pageable);
    }
}
