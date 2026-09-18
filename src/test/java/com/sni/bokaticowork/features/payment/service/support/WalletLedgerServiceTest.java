package com.sni.bokaticowork.features.payment.service.support;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.payment.enums.WalletEntryDirection;
import com.sni.bokaticowork.features.payment.enums.WalletEntryType;
import com.sni.bokaticowork.features.payment.enums.WalletStatus;
import com.sni.bokaticowork.features.payment.model.WalletAccount;
import com.sni.bokaticowork.features.payment.model.WalletLedgerEntry;
import com.sni.bokaticowork.features.payment.repository.WalletAccountRepository;
import com.sni.bokaticowork.features.payment.repository.WalletLedgerEntryRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WalletLedgerServiceTest {

    private static final Long WALLET_ID = 42L;

    private final WalletAccountRepository walletRepository = mock(WalletAccountRepository.class);
    private final WalletLedgerEntryRepository ledgerRepository = mock(WalletLedgerEntryRepository.class);
    private final SequenceGeneratorFacade sequenceGenerator = mock(SequenceGeneratorFacade.class);
    private final EntityManager entityManager = mock(EntityManager.class);

    private final com.sni.bokaticowork.features.payment.transfer.service.WalletBalanceWatch balanceWatch =
            mock(com.sni.bokaticowork.features.payment.transfer.service.WalletBalanceWatch.class);

    private final WalletLedgerService service =
            new WalletLedgerService(walletRepository, ledgerRepository, sequenceGenerator, balanceWatch);

    private WalletAccount wallet;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "entityManager", entityManager);
        wallet = wallet("1000.0000", "1000.0000", "0.0000");

        when(entityManager.find(WalletAccount.class, WALLET_ID)).thenReturn(wallet);
        when(walletRepository.save(any(WalletAccount.class))).thenAnswer(call -> call.getArgument(0));
        when(ledgerRepository.saveAndFlush(any(WalletLedgerEntry.class))).thenAnswer(call -> call.getArgument(0));
        when(sequenceGenerator.next(anyString())).thenReturn("WLE-001");
    }

    @Test
    void shouldCreditBothLedgerAndAvailableBalance() {
        WalletLedgerEntry entry = service.credit(wallet, new BigDecimal("250"), WalletEntryType.ADMIN_TOPUP,
                "ADMIN_TOPUP", "REF-1", "REF-1", "admin", "ADMIN_TOPUP:REF-1");

        assertThat(wallet.getLedgerBalance()).isEqualByComparingTo("1250");
        assertThat(wallet.getAvailableBalance()).isEqualByComparingTo("1250");
        assertThat(entry.getDirection()).isEqualTo(WalletEntryDirection.CREDIT);
        assertThat(entry.getBalanceAfter()).isEqualByComparingTo("1250");
        assertThat(entry.getIdempotencyKey()).isEqualTo("ADMIN_TOPUP:REF-1");
    }

    @Test
    void shouldRefuseToDebitMoreThanTheAvailableBalance() {
        Throwable thrown = catchThrowable(() -> service.debit(wallet, new BigDecimal("1200"),
                WalletEntryType.PAYMENT, "PAYMENT_INTENT", "PI-1", null, "system", "WALLET_PAYMENT:PI-1"));

        assertThat(thrown).isInstanceOf(BadRequestException.class);
        assertThat(wallet.getLedgerBalance()).isEqualByComparingTo("1000");
        verify(ledgerRepository, never()).saveAndFlush(any());
    }

    @Test
    void shouldNotApplyTheSameOperationTwiceWhenAnIdempotencyKeyIsReused() {
        WalletLedgerEntry original = WalletLedgerEntry.builder()
                .entryNumber("WLE-ORIGINAL")
                .idempotencyKey("WALLET_PAYMENT:PI-1")
                .build();
        when(ledgerRepository.findByIdempotencyKey("WALLET_PAYMENT:PI-1")).thenReturn(Optional.of(original));

        WalletLedgerEntry entry = service.debit(wallet, new BigDecimal("400"), WalletEntryType.PAYMENT,
                "PAYMENT_INTENT", "PI-1", null, "system", "WALLET_PAYMENT:PI-1");

        // Le rejeu renvoie l'ecriture d'origine et laisse le solde intact : c'est ce qui
        // empeche un callback duplique de debiter deux fois.
        assertThat(entry.getEntryNumber()).isEqualTo("WLE-ORIGINAL");
        assertThat(wallet.getAvailableBalance()).isEqualByComparingTo("1000");
        verify(walletRepository, never()).save(any());
        verify(ledgerRepository, never()).saveAndFlush(any());
    }

    @Test
    void shouldNeverDeduplicateWhenNoIdempotencyKeyIsSupplied() {
        service.credit(wallet, new BigDecimal("100"), WalletEntryType.REFUND,
                "PAYMENT_TRANSACTION", "TXN-1", "partiel 1", "admin", null);
        service.credit(wallet, new BigDecimal("100"), WalletEntryType.REFUND,
                "PAYMENT_TRANSACTION", "TXN-1", "partiel 2", "admin", null);

        // Deux remboursements partiels de la meme transaction sont legitimes : sans cle,
        // aucune deduplication ne doit intervenir.
        assertThat(wallet.getLedgerBalance()).isEqualByComparingTo("1200");
        verify(ledgerRepository, never()).findByIdempotencyKey(any());
    }

    @Test
    void shouldFallBackToTheWinningEntryWhenAConcurrentInstanceWinsTheUniqueIndexRace() {
        WalletLedgerEntry winner = WalletLedgerEntry.builder().entryNumber("WLE-WINNER").build();
        when(ledgerRepository.saveAndFlush(any(WalletLedgerEntry.class)))
                .thenThrow(new DataIntegrityViolationException("ux_wallet_ledger_idempotency_key"));
        when(ledgerRepository.findByIdempotencyKey("WALLET_PAYMENT:PI-1"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(winner));

        WalletLedgerEntry entry = service.debit(wallet, new BigDecimal("400"), WalletEntryType.PAYMENT,
                "PAYMENT_INTENT", "PI-1", null, "system", "WALLET_PAYMENT:PI-1");

        assertThat(entry.getEntryNumber()).isEqualTo("WLE-WINNER");
    }

    @Test
    void shouldMoveFundsBetweenAvailableAndHeldWithoutTouchingTheLedgerBalance() {
        service.placeHold(wallet, new BigDecimal("300"), "BOOKING", "BK-1", null, "system", "HOLD:WH-1");

        assertThat(wallet.getAvailableBalance()).isEqualByComparingTo("700");
        assertThat(wallet.getHeldBalance()).isEqualByComparingTo("300");
        assertThat(wallet.getLedgerBalance()).isEqualByComparingTo("1000");
        assertThat(invariantHolds(wallet)).isTrue();
    }

    @Test
    void shouldConsumeTheLedgerBalanceWhenAHoldIsCaptured() {
        service.placeHold(wallet, new BigDecimal("300"), "BOOKING", "BK-1", null, "system", "HOLD:WH-1");
        service.captureHold(wallet, new BigDecimal("300"), "BOOKING", "BK-1", "CAPTURE_HOLD", "system", "HOLD_CAPTURE:WH-1");

        assertThat(wallet.getAvailableBalance()).isEqualByComparingTo("700");
        assertThat(wallet.getHeldBalance()).isEqualByComparingTo("0");
        assertThat(wallet.getLedgerBalance()).isEqualByComparingTo("700");
        assertThat(invariantHolds(wallet)).isTrue();
    }

    @Test
    void shouldReturnHeldFundsToTheAvailableBalanceWhenAHoldIsReleased() {
        service.placeHold(wallet, new BigDecimal("300"), "BOOKING", "BK-1", null, "system", "HOLD:WH-1");
        service.releaseHold(wallet, new BigDecimal("300"), "BOOKING", "BK-1", "RELEASED", "system", "HOLD_RELEASE:WH-1");

        assertThat(wallet.getAvailableBalance()).isEqualByComparingTo("1000");
        assertThat(wallet.getHeldBalance()).isEqualByComparingTo("0");
        assertThat(wallet.getLedgerBalance()).isEqualByComparingTo("1000");
        assertThat(invariantHolds(wallet)).isTrue();
    }

    @Test
    void shouldRefuseToCaptureMoreThanWhatIsActuallyHeld() {
        Throwable thrown = catchThrowable(() -> service.captureHold(wallet, new BigDecimal("50"),
                "BOOKING", "BK-1", "CAPTURE_HOLD", "system", "HOLD_CAPTURE:WH-1"));

        assertThat(thrown).isInstanceOf(BadRequestException.class);
        assertThat(invariantHolds(wallet)).isTrue();
    }

    @Test
    void shouldRejectNonPositiveAmounts() {
        assertThat(catchThrowable(() -> service.credit(wallet, BigDecimal.ZERO, WalletEntryType.ADMIN_TOPUP,
                null, null, null, "admin", null))).isInstanceOf(BadRequestException.class);
        assertThat(catchThrowable(() -> service.credit(wallet, new BigDecimal("-5"), WalletEntryType.ADMIN_TOPUP,
                null, null, null, "admin", null))).isInstanceOf(BadRequestException.class);
        assertThat(catchThrowable(() -> service.credit(wallet, null, WalletEntryType.ADMIN_TOPUP,
                null, null, null, "admin", null))).isInstanceOf(BadRequestException.class);
    }

    @Test
    void shouldLockTheWalletRowBeforeMutatingIt() {
        service.credit(wallet, new BigDecimal("10"), WalletEntryType.ADMIN_TOPUP,
                "ADMIN_TOPUP", "REF-2", null, "admin", null);

        // flush puis refresh sous verrou exclusif : sans le refresh, Hibernate rendrait
        // l'instance deja en cache avec un solde perime malgre le verrou.
        verify(entityManager).flush();
        verify(entityManager).refresh(eq(wallet), eq(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE));
    }

    private boolean invariantHolds(WalletAccount account) {
        return account.getLedgerBalance()
                .compareTo(account.getAvailableBalance().add(account.getHeldBalance())) == 0;
    }

    private WalletAccount wallet(String ledger, String available, String held) {
        return WalletAccount.builder()
                .id(WALLET_ID)
                .walletNumber("WAL-001")
                .ownerType("MEMBER")
                .ownerCode("MBR-1")
                .currency("XAF")
                .status(WalletStatus.ACTIVE)
                .ledgerBalance(new BigDecimal(ledger))
                .availableBalance(new BigDecimal(available))
                .heldBalance(new BigDecimal(held))
                .build();
    }
}
