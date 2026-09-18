package com.sni.bokaticowork.features.payment.service.support;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.core.generator.uuid.TimeOrderedUuid;
import com.sni.bokaticowork.features.payment.integrity.service.WalletLedgerChain;
import com.sni.bokaticowork.features.payment.enums.WalletEntryDirection;
import com.sni.bokaticowork.features.payment.enums.WalletEntryType;
import com.sni.bokaticowork.features.payment.model.WalletAccount;
import com.sni.bokaticowork.features.payment.model.WalletLedgerEntry;
import com.sni.bokaticowork.features.payment.repository.WalletAccountRepository;
import com.sni.bokaticowork.features.payment.repository.WalletLedgerEntryRepository;
import com.sni.bokaticowork.features.payment.transfer.service.WalletBalanceWatch;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Optional;

/**
 * Unique proprietaire des mutations de solde d'un portefeuille.
 * <p>
 * Toute operation suit le meme protocole, dans cet ordre :
 * <ol>
 *     <li>validation du montant et de la devise ;</li>
 *     <li>court-circuit si la cle d'idempotence a deja ete consommee ;</li>
 *     <li>verrou exclusif sur la ligne {@code wallet_account} ;</li>
 *     <li>mutation des soldes puis ecriture au grand livre.</li>
 * </ol>
 * L'invariant {@code ledger_balance = available_balance + held_balance} est preserve par
 * construction et verrouille en base par {@code ck_wallet_account_balance_split}.
 * <p>
 * Aucune autre classe ne doit ecrire dans les colonnes de solde : le rapprochement
 * quotidien ({@code WalletReconciliationWorker}) suppose que le grand livre est exhaustif.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WalletLedgerService {

    private final WalletAccountRepository walletRepository;
    private final WalletLedgerEntryRepository ledgerRepository;
    private final SequenceGeneratorFacade sequenceGenerator;
    private final WalletBalanceWatch balanceWatch;

    @PersistenceContext
    private EntityManager entityManager;

    /** Credite le solde comptable et le solde disponible. */
    public WalletLedgerEntry credit(WalletAccount wallet, BigDecimal amount, WalletEntryType type,
                                    String sourceType, String sourceCode, String reference,
                                    String createdBy, String idempotencyKey) {
        return apply(wallet, amount, type, WalletEntryDirection.CREDIT, sourceType, sourceCode,
                reference, createdBy, idempotencyKey, (account, value) -> {
                    account.setLedgerBalance(account.getLedgerBalance().add(value));
                    account.setAvailableBalance(account.getAvailableBalance().add(value));
                });
    }

    /** Debite le solde comptable et le solde disponible. */
    public WalletLedgerEntry debit(WalletAccount wallet, BigDecimal amount, WalletEntryType type,
                                   String sourceType, String sourceCode, String reference,
                                   String createdBy, String idempotencyKey) {
        return apply(wallet, amount, type, WalletEntryDirection.DEBIT, sourceType, sourceCode,
                reference, createdBy, idempotencyKey, (account, value) -> {
                    requireAvailable(account, value);
                    account.setLedgerBalance(account.getLedgerBalance().subtract(value));
                    account.setAvailableBalance(account.getAvailableBalance().subtract(value));
                });
    }

    /** Bloque des fonds : deplace du solde disponible vers le solde bloque, sans toucher au solde comptable. */
    public WalletLedgerEntry placeHold(WalletAccount wallet, BigDecimal amount, String sourceType,
                                       String sourceCode, String reference, String createdBy,
                                       String idempotencyKey) {
        return apply(wallet, amount, WalletEntryType.HOLD, WalletEntryDirection.DEBIT, sourceType,
                sourceCode, reference, createdBy, idempotencyKey, (account, value) -> {
                    requireAvailable(account, value);
                    account.setAvailableBalance(account.getAvailableBalance().subtract(value));
                    account.setHeldBalance(account.getHeldBalance().add(value));
                });
    }

    /** Encaisse des fonds bloques : sort du solde bloque et du solde comptable. */
    public WalletLedgerEntry captureHold(WalletAccount wallet, BigDecimal amount, String sourceType,
                                         String sourceCode, String reference, String createdBy,
                                         String idempotencyKey) {
        return apply(wallet, amount, WalletEntryType.PAYMENT, WalletEntryDirection.DEBIT, sourceType,
                sourceCode, reference, createdBy, idempotencyKey, (account, value) -> {
                    requireHeld(account, value);
                    account.setHeldBalance(account.getHeldBalance().subtract(value));
                    account.setLedgerBalance(account.getLedgerBalance().subtract(value));
                });
    }

    /** Libere des fonds bloques : les rend au solde disponible. */
    public WalletLedgerEntry releaseHold(WalletAccount wallet, BigDecimal amount, String sourceType,
                                         String sourceCode, String reference, String createdBy,
                                         String idempotencyKey) {
        return apply(wallet, amount, WalletEntryType.HOLD_RELEASE, WalletEntryDirection.CREDIT, sourceType,
                sourceCode, reference, createdBy, idempotencyKey, (account, value) -> {
                    requireHeld(account, value);
                    account.setHeldBalance(account.getHeldBalance().subtract(value));
                    account.setAvailableBalance(account.getAvailableBalance().add(value));
                });
    }

    @FunctionalInterface
    private interface BalanceMutation {
        void apply(WalletAccount wallet, BigDecimal amount);
    }

    private WalletLedgerEntry apply(WalletAccount wallet, BigDecimal amount, WalletEntryType type,
                                    WalletEntryDirection direction, String sourceType, String sourceCode,
                                    String reference, String createdBy, String idempotencyKey,
                                    BalanceMutation mutation) {
        validateAmount(amount);
        String key = trim(idempotencyKey);

        // Court-circuit avant tout verrou : une operation deja appliquee renvoie son ecriture
        // d'origine au lieu de rejouer la mutation. Le rejeu d'un callback n'est pas une erreur.
        Optional<WalletLedgerEntry> replayed = findReplay(key);
        if (replayed.isPresent()) {
            log.info("Wallet operation {} ignoree · cle d'idempotence {} deja consommee par l'ecriture {}",
                    type, key, replayed.get().getEntryNumber());
            return replayed.get();
        }

        WalletAccount locked = lock(wallet);
        BigDecimal normalized = money(amount);
        mutation.apply(locked, normalized);
        locked.setLastActivityAt(Instant.now());
        locked.setDormantSince(null);
        // L'alerte de solde bas se decide ici, sous le verrou, sur le solde reellement obtenu :
        // ailleurs, elle lirait un solde deja depasse par l'ecriture suivante.
        balanceWatch.afterBalanceChange(locked);
        WalletAccount saved = walletRepository.save(locked);

        WalletLedgerEntry entry = WalletLedgerEntry.builder()
                .entryNumber(sequenceGenerator.next("wallet_entry"))
                .wallet(saved)
                .direction(direction)
                .amount(normalized)
                .currency(saved.getCurrency())
                .balanceAfter(saved.getLedgerBalance())
                .entryType(type)
                .sourceType(trim(sourceType))
                .sourceCode(trim(sourceCode))
                .reference(trim(reference))
                .createdBy(trim(createdBy))
                .idempotencyKey(key)
                .transactionUuid(TimeOrderedUuid.next())
                .transactionNumber(sequenceGenerator.next("wallet_transaction"))
                .createdAt(Instant.now())
                .build();
        chain(entry, saved.getId());
        try {
            return ledgerRepository.saveAndFlush(entry);
        } catch (DataIntegrityViolationException ex) {
            // Une instance concurrente a gagne la course sur l'index unique partiel.
            // La mutation de solde de cette transaction sera annulee par le rollback.
            return findReplay(key).orElseThrow(() -> ex);
        }
    }

    /**
     * Accroche l'ecriture au dernier maillon du portefeuille.
     *
     * <p>Le calcul a lieu ici et nulle part ailleurs, sous le verrou exclusif deja pose sur
     * {@code wallet_account}. C'est ce verrou qui fait la chaine : sans lui, deux ecritures
     * concurrentes liraient le meme maillon precedent et se declareraient toutes deux legitimes
     * derriere lui · la chaine se dedoublerait sans que rien ne paraisse rompu.</p>
     */
    private void chain(WalletLedgerEntry entry, Long walletId) {
        String previous = ledgerRepository.findLastHash(walletId).orElse(WalletLedgerChain.GENESIS);
        entry.setPreviousHash(previous);
        entry.setCurrentHash(WalletLedgerChain.hash(entry, previous));
    }

    /**
     * Recharge le portefeuille sous {@code SELECT ... FOR UPDATE}.
     * <p>
     * Le {@code refresh} est indispensable : sans lui, Hibernate rendrait l'instance deja
     * presente dans le contexte de persistance avec ses valeurs d'origine, donc on poserait
     * le verrou mais on calculerait sur un solde perime. Le {@code flush} prealable couvre
     * le cas d'un portefeuille cree dans la meme transaction et pas encore ecrit en base.
     */
    private WalletAccount lock(WalletAccount wallet) {
        if (wallet == null || wallet.getId() == null) {
            throw new BadRequestException("Wallet is required");
        }
        entityManager.flush();
        WalletAccount managed = entityManager.find(WalletAccount.class, wallet.getId());
        if (managed == null) {
            throw new ResourceNotFoundException("Wallet not found");
        }
        entityManager.refresh(managed, LockModeType.PESSIMISTIC_WRITE);
        return managed;
    }

    private Optional<WalletLedgerEntry> findReplay(String key) {
        return key == null ? Optional.empty() : ledgerRepository.findByIdempotencyKey(key);
    }

    private void requireAvailable(WalletAccount wallet, BigDecimal amount) {
        if (wallet.getAvailableBalance().compareTo(amount) < 0) {
            throw new BadRequestException("Insufficient wallet balance");
        }
    }

    private void requireHeld(WalletAccount wallet, BigDecimal amount) {
        if (wallet.getHeldBalance().compareTo(amount) < 0) {
            throw new BadRequestException("Insufficient held balance on wallet");
        }
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new BadRequestException("Wallet amount must be positive");
        }
    }

    private BigDecimal money(BigDecimal amount) {
        return amount.setScale(4, RoundingMode.HALF_UP);
    }

    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
