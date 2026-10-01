package com.sni.bokaticowork.features.payment.control.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.payment.control.model.WalletAdminAction;
import com.sni.bokaticowork.features.payment.control.repository.WalletAdminActionRepository;
import com.sni.bokaticowork.features.payment.enums.WalletEntryDirection;
import com.sni.bokaticowork.features.payment.enums.WalletEntryType;
import com.sni.bokaticowork.features.payment.enums.WalletStatus;
import com.sni.bokaticowork.features.payment.limit.repository.WalletLimitPolicyRepository;
import com.sni.bokaticowork.features.payment.model.WalletAccount;
import com.sni.bokaticowork.features.payment.model.WalletLedgerEntry;
import com.sni.bokaticowork.features.payment.repository.WalletAccountRepository;
import com.sni.bokaticowork.features.payment.repository.WalletLedgerEntryRepository;
import com.sni.bokaticowork.features.payment.security.service.WalletSecurityService;
import com.sni.bokaticowork.features.payment.service.support.WalletLedgerService;
import com.sni.bokaticowork.features.payment.transfer.service.WalletNotifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Map;

/**
 * Les actions administratives sur un portefeuille.
 *
 * <p>Toutes passent par ici, et par ici seulement : c'est ce qui garantit qu'aucune n'echappe a la
 * trace. Deux principes, tenus par la base autant que par le code · aucune action sans motif, et un
 * second visa sur les operations sensibles. Le second visa n'est pas une formalite : l'action est
 * enregistree {@code PENDING_APPROVAL} et <em>rien n'est execute</em> tant qu'un autre que le
 * demandeur ne l'a pas approuvee.</p>
 *
 * <p>Ce qui exige un second visa : un mouvement d'argent au-dela du seuil configure, une
 * contre-passation, et toute cloture avec solde. Suspendre, lever la suspension, reinitialiser un
 * code ou accorder une derogation s'executent seul · ce sont des gestes de protection ou de
 * service, pas des mouvements d'argent.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalletAdminActionService {

    private static final String SOURCE_TYPE = "WALLET_ADMIN_ACTION";

    private final WalletAdminActionRepository actionRepository;
    private final WalletAccountRepository walletRepository;
    private final WalletLedgerEntryRepository ledgerRepository;
    private final WalletLedgerService ledgerService;
    private final WalletSecurityService securityService;
    private final WalletLimitPolicyRepository limitPolicyRepository;
    private final WalletNotifier notifier;
    private final SequenceGeneratorFacade sequenceGenerator;

    /** Au-dela, un mouvement d'argent exige un second visa. */
    @Value("${bokati.wallet.admin.four-eyes-threshold:100000}")
    private BigDecimal fourEyesThreshold;

    /**
     * Ce qu'un administrateur demande.
     *
     * @param reference ecriture a contre-passer, politique de plafond a accorder, reference de
     *                  restitution a la cloture · selon le type
     * @param until     fin de la derogation de plafond
     */
    public record ActionRequest(
            WalletAdminAction.Type type,
            BigDecimal amount,
            String reference,
            String reason,
            Instant until
    ) {
    }

    // -----------------------------------------------------------------------------------------
    // Demander
    // -----------------------------------------------------------------------------------------

    @Transactional
    public WalletAdminAction request(String walletNumber, ActionRequest request, String requestedBy) {
        if (request.type() == null) {
            throw new BadRequestException("Le type d'action est requis");
        }
        if (!StringUtils.hasText(request.reason()) || request.reason().trim().length() < 5) {
            throw new BadRequestException("Un motif d'au moins cinq caractères est requis pour toute action administrative");
        }
        if (!StringUtils.hasText(requestedBy)) {
            throw new BadRequestException("L'auteur de la demande est requis");
        }
        WalletAccount wallet = walletRepository.findByWalletNumber(walletNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Portefeuille introuvable"));

        validate(wallet, request);

        WalletAdminAction action = WalletAdminAction.builder()
                .actionNumber(sequenceGenerator.next("wallet_admin_action"))
                .wallet(wallet)
                .actionType(request.type())
                .amount(request.amount() == null ? null : request.amount().setScale(4, RoundingMode.HALF_UP))
                .currency(wallet.getCurrency())
                .reference(trim(request.reference()))
                .reason(request.reason().trim())
                .requestedBy(requestedBy.trim())
                .payloadJson(request.until() == null ? null : "{\"until\":\"" + request.until() + "\"}")
                .build();

        if (requiresSecondApproval(wallet, request)) {
            action.setStatus(WalletAdminAction.Status.PENDING_APPROVAL);
            return actionRepository.save(action);
        }
        action.setStatus(WalletAdminAction.Status.EXECUTED);
        action = actionRepository.save(action);
        execute(action, request.until());
        return actionRepository.save(action);
    }

    // -----------------------------------------------------------------------------------------
    // Second visa
    // -----------------------------------------------------------------------------------------

    /** Approuve et execute · par un autre que le demandeur, la base y veille aussi. */
    @Transactional
    public WalletAdminAction approve(String actionNumber, String approvedBy) {
        WalletAdminAction action = pending(actionNumber);
        if (!StringUtils.hasText(approvedBy) || approvedBy.trim().equalsIgnoreCase(action.getRequestedBy())) {
            throw new ConflictException("wallet", "le second visa doit venir d'une autre personne que le demandeur");
        }
        action.setApprovedBy(approvedBy.trim());
        action.setApprovedAt(Instant.now());
        action.setStatus(WalletAdminAction.Status.EXECUTED);
        execute(action, readUntil(action));
        return actionRepository.save(action);
    }

    @Transactional
    public WalletAdminAction reject(String actionNumber, String rejectedBy, String rejectionReason) {
        if (!StringUtils.hasText(rejectionReason)) {
            throw new BadRequestException("Un motif de refus est requis");
        }
        WalletAdminAction action = pending(actionNumber);
        action.setRejectedBy(trim(rejectedBy));
        action.setRejectedAt(Instant.now());
        action.setRejectionReason(rejectionReason.trim());
        action.setStatus(WalletAdminAction.Status.REJECTED);
        return actionRepository.save(action);
    }

    @Transactional(readOnly = true)
    public Page<WalletAdminAction> pendingApprovals(Pageable pageable) {
        return actionRepository.findByStatusOrderByCreatedAtDesc(WalletAdminAction.Status.PENDING_APPROVAL, pageable);
    }

    @Transactional(readOnly = true)
    public Page<WalletAdminAction> history(String walletNumber, Pageable pageable) {
        WalletAccount wallet = walletRepository.findByWalletNumber(walletNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Portefeuille introuvable"));
        return actionRepository.findByWallet_IdOrderByCreatedAtDesc(wallet.getId(), pageable);
    }

    @Transactional(readOnly = true)
    public long countPending() {
        return actionRepository.countByStatus(WalletAdminAction.Status.PENDING_APPROVAL);
    }

    // -----------------------------------------------------------------------------------------
    // Execution
    // -----------------------------------------------------------------------------------------

    private void execute(WalletAdminAction action, Instant until) {
        WalletAccount wallet = walletRepository.findByIdForUpdate(action.getWallet().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Portefeuille introuvable"));
        String actor = action.getApprovedBy() == null ? action.getRequestedBy() : action.getApprovedBy();
        String key = "ADMIN_ACTION:" + action.getActionNumber();
        String label = action.getActionType().name() + " " + action.getActionNumber() + " · " + action.getReason();

        switch (action.getActionType()) {
            case TOPUP -> action.setResultReference(ledgerService.credit(wallet, action.getAmount(),
                    WalletEntryType.ADMIN_TOPUP, SOURCE_TYPE, action.getActionNumber(), label, actor, key).getEntryNumber());

            case DEBIT -> action.setResultReference(ledgerService.debit(wallet, action.getAmount(),
                    WalletEntryType.ADMIN_DEBIT, SOURCE_TYPE, action.getActionNumber(), label, actor, key).getEntryNumber());

            case ADJUST -> {
                BigDecimal amount = action.getAmount();
                WalletLedgerEntry entry = amount.signum() > 0
                        ? ledgerService.credit(wallet, amount, WalletEntryType.ADJUSTMENT, SOURCE_TYPE, action.getActionNumber(), label, actor, key)
                        : ledgerService.debit(wallet, amount.negate(), WalletEntryType.ADJUSTMENT, SOURCE_TYPE, action.getActionNumber(), label, actor, key);
                action.setResultReference(entry.getEntryNumber());
            }

            case REVERSE -> action.setResultReference(reverse(wallet, action, actor).getEntryNumber());

            case SUSPEND -> {
                wallet.setFrozenAt(Instant.now());
                wallet.setFrozenReason(action.getReason());
                if (wallet.getStatus() == WalletStatus.ACTIVE) {
                    wallet.setStatus(WalletStatus.SUSPENDED);
                }
                walletRepository.save(wallet);
                notifier.securityEvent(wallet, "WALLET_SUSPENDED", "Votre portefeuille a été suspendu",
                        Map.of("actionNumber", action.getActionNumber()));
            }

            case UNSUSPEND -> {
                if (wallet.getFrozenReason() != null
                        && wallet.getFrozenReason().startsWith(com.sni.bokaticowork.features.payment.compliance.service.ComplianceFreezeService.REASON_PREFIX)) {
                    // Un gel sur instruction ne se leve pas comme une suspension · l'administrateur
                    // qui degelerait sans le savoir commettrait une faute qu'il n'a pas voulue.
                    throw new ConflictException("wallet", "ce portefeuille est gelé sur instruction · la levée passe par le module de conformité, avec sa référence");
                }
                wallet.setFrozenAt(null);
                wallet.setFrozenReason(null);
                if (wallet.getStatus() == WalletStatus.SUSPENDED || wallet.getStatus() == WalletStatus.UNDER_REVIEW) {
                    wallet.setStatus(WalletStatus.ACTIVE);
                }
                walletRepository.save(wallet);
                notifier.securityEvent(wallet, "WALLET_UNSUSPENDED", "Votre portefeuille est de nouveau actif",
                        Map.of("actionNumber", action.getActionNumber()));
            }

            case RESET_PIN -> securityService.resetPin(wallet, actor);

            case RAISE_LIMIT -> {
                wallet.setLimitPolicyCode(action.getReference());
                wallet.setLimitPolicyUntil(until);
                walletRepository.save(wallet);
            }

            case CLOSE -> close(wallet, action, actor, key, label);

            case REVIEW_FLAG -> {
                // La revue elle-meme est faite par le service des signalements · l'action ne fait
                // qu'en garder la trace dans l'historique du portefeuille.
            }
        }
        action.setExecutedAt(Instant.now());
        log.info("Portefeuille {} · action {} {} executee par {}", wallet.getWalletNumber(),
                action.getActionType(), action.getActionNumber(), actor);
    }

    /**
     * Contre-passe une ecriture · une ecriture opposee, jamais une modification.
     *
     * <p>La cle d'idempotence est derivee de l'ecriture d'origine, pas de l'action : deux actions
     * qui voudraient contre-passer la meme ecriture retombent sur la meme contre-passation, et une
     * ecriture ne peut donc etre annulee qu'une fois.</p>
     */
    private WalletLedgerEntry reverse(WalletAccount wallet, WalletAdminAction action, String actor) {
        WalletLedgerEntry original = ledgerRepository.findByTransactionNumber(action.getReference())
                .orElseThrow(() -> new ResourceNotFoundException("Écriture à contre-passer introuvable"));
        if (!original.getWallet().getId().equals(wallet.getId())) {
            throw new BadRequestException("Cette écriture n'appartient pas à ce portefeuille");
        }
        if (original.getEntryType() == WalletEntryType.HOLD || original.getEntryType() == WalletEntryType.HOLD_RELEASE) {
            throw new BadRequestException("Une retenue ne se contre-passe pas · elle se libère");
        }
        if (original.getEntryType() == WalletEntryType.REVERSAL) {
            throw new BadRequestException("Une contre-passation ne se contre-passe pas");
        }
        String key = "REVERSAL:" + original.getTransactionNumber();
        String label = "Contre-passation de " + original.getTransactionNumber() + " · " + action.getReason();
        return original.getDirection() == WalletEntryDirection.CREDIT
                ? ledgerService.debit(wallet, original.getAmount(), WalletEntryType.REVERSAL, SOURCE_TYPE, action.getActionNumber(), label, actor, key)
                : ledgerService.credit(wallet, original.getAmount(), WalletEntryType.REVERSAL, SOURCE_TYPE, action.getActionNumber(), label, actor, key);
    }

    /**
     * Cloture avec restitution.
     *
     * <p>Un solde ne disparait pas : s'il reste quelque chose, la reference de restitution dit par
     * quel chemin l'argent est reparti · le meme que celui par lequel il est entre, jamais en
     * especes. Sans reference, la cloture est refusee tant que le solde n'est pas nul.</p>
     */
    private void close(WalletAccount wallet, WalletAdminAction action, String actor, String key, String label) {
        if (wallet.getHeldBalance() != null && wallet.getHeldBalance().signum() > 0) {
            throw new ConflictException("wallet", "des fonds sont encore retenus · libérez-les avant de clôturer");
        }
        BigDecimal balance = wallet.getLedgerBalance() == null ? BigDecimal.ZERO : wallet.getLedgerBalance();
        if (balance.signum() > 0) {
            if (!StringUtils.hasText(action.getReference())) {
                throw new ConflictException("wallet", "le solde n'est pas nul · indiquez la référence de restitution");
            }
            WalletLedgerEntry restitution = ledgerService.debit(wallet, balance, WalletEntryType.ADMIN_DEBIT,
                    SOURCE_TYPE, action.getActionNumber(), "Restitution " + action.getReference() + " · " + label, actor, key);
            action.setResultReference(restitution.getEntryNumber());
            wallet = walletRepository.findById(wallet.getId()).orElse(wallet);
        }
        wallet.setStatus(WalletStatus.CLOSED);
        wallet.setClosedAt(Instant.now());
        walletRepository.save(wallet);
        notifier.securityEvent(wallet, "WALLET_CLOSED", "Votre portefeuille a été clôturé",
                Map.of("actionNumber", action.getActionNumber()));
    }

    // -----------------------------------------------------------------------------------------

    private void validate(WalletAccount wallet, ActionRequest request) {
        switch (request.type()) {
            case TOPUP, DEBIT -> requirePositive(request.amount());
            case ADJUST -> {
                if (request.amount() == null || request.amount().signum() == 0) {
                    throw new BadRequestException("Un ajustement porte un montant signé non nul");
                }
            }
            case REVERSE -> {
                if (!StringUtils.hasText(request.reference())) {
                    throw new BadRequestException("Indiquez la référence de l'écriture à contre-passer");
                }
            }
            case RAISE_LIMIT -> {
                if (!StringUtils.hasText(request.reference())
                        || limitPolicyRepository.findByCode(request.reference().trim()).isEmpty()) {
                    throw new BadRequestException("Indiquez le code d'une politique de plafond existante");
                }
                if (request.until() == null || !request.until().isAfter(Instant.now())) {
                    throw new BadRequestException("Une dérogation de plafond est bornée dans le temps · indiquez sa fin");
                }
            }
            case CLOSE -> {
                if (wallet.getStatus() == WalletStatus.CLOSED) {
                    throw new BadRequestException("Ce portefeuille est déjà clôturé");
                }
            }
            case SUSPEND, UNSUSPEND, RESET_PIN, REVIEW_FLAG -> {
                // Rien de plus que le motif, deja verifie.
            }
        }
        if (wallet.getStatus() == WalletStatus.CLOSED && request.type() != WalletAdminAction.Type.REVIEW_FLAG) {
            throw new ConflictException("wallet", "ce portefeuille est clôturé");
        }
    }

    private boolean requiresSecondApproval(WalletAccount wallet, ActionRequest request) {
        return switch (request.type()) {
            case REVERSE -> true;
            case CLOSE -> wallet.getLedgerBalance() != null && wallet.getLedgerBalance().signum() > 0;
            case TOPUP, DEBIT, ADJUST -> request.amount().abs().compareTo(fourEyesThreshold) >= 0;
            default -> false;
        };
    }

    private WalletAdminAction pending(String actionNumber) {
        WalletAdminAction action = actionRepository.findByActionNumber(actionNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Action introuvable"));
        if (action.getStatus() != WalletAdminAction.Status.PENDING_APPROVAL) {
            throw new BadRequestException("Cette action n'est pas en attente de visa");
        }
        return action;
    }

    private Instant readUntil(WalletAdminAction action) {
        String payload = action.getPayloadJson();
        if (payload == null || !payload.contains("\"until\"")) {
            return null;
        }
        String value = payload.replaceAll(".*\"until\":\"([^\"]+)\".*", "$1");
        return Instant.parse(value);
    }

    private void requirePositive(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new BadRequestException("Le montant doit être positif");
        }
    }

    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    /** Expose pour les tests. */
    void configureThreshold(BigDecimal threshold) {
        this.fourEyesThreshold = threshold;
    }
}
