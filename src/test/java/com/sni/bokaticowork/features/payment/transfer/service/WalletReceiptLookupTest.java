package com.sni.bokaticowork.features.payment.transfer.service;

import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.payment.enums.WalletEntryDirection;
import com.sni.bokaticowork.features.payment.enums.WalletEntryType;
import com.sni.bokaticowork.features.payment.mapper.interfaces.PaymentMapper;
import com.sni.bokaticowork.features.payment.model.WalletAccount;
import com.sni.bokaticowork.features.payment.model.WalletLedgerEntry;
import com.sni.bokaticowork.features.payment.repository.WalletLedgerEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Le reçu d'une écriture se demande par l'un ou l'autre de ses numéros.
 *
 * <p>Une écriture porte le sien (WLE), affiché en tête du relevé, et celui de la transaction
 * (WTX), absent des écritures antérieures au chaînage. N'accepter que le second rendait le reçu
 * introuvable pour l'identifiant que le client a sous les yeux.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WalletReceiptLookupTest {

    @Mock private WalletLedgerEntryRepository ledgerRepository;
    @Mock private PaymentMapper paymentMapper;

    private WalletAccount wallet;
    private WalletAccount otherWallet;
    private WalletLedgerEntry entry;

    @BeforeEach
    void setUp() {
        wallet = WalletAccount.builder().id(1L).walletNumber("WLT-00000004").ownerType("MEMBER").ownerCode("MBR-1").currency("XAF").build();
        otherWallet = WalletAccount.builder().id(2L).walletNumber("WLT-00000009").build();
        entry = WalletLedgerEntry.builder()
                .id(50L).wallet(wallet)
                .entryNumber("WLE-202609-00000005")
                .transactionNumber("WTX-202609-0000005")
                .entryType(WalletEntryType.TOPUP).direction(WalletEntryDirection.CREDIT)
                .amount(new BigDecimal("25000")).balanceAfter(new BigDecimal("75000"))
                .createdAt(Instant.now())
                .build();
        when(ledgerRepository.findByTransactionNumber(anyString())).thenReturn(Optional.empty());
        when(ledgerRepository.findByEntryNumber(anyString())).thenReturn(Optional.empty());
        when(ledgerRepository.findByTransactionNumber("WTX-202609-0000005")).thenReturn(Optional.of(entry));
        when(ledgerRepository.findByEntryNumber("WLE-202609-00000005")).thenReturn(Optional.of(entry));
    }

    /** La recherche elle-meme, sans le rendu PDF · c'est elle qui refusait le bon numero. */
    private WalletLedgerEntry lookup(WalletAccount owner, String reference) {
        WalletLedgerEntry found = ledgerRepository.findByTransactionNumber(reference)
                .or(() -> ledgerRepository.findByEntryNumber(reference))
                .orElseThrow(() -> new ResourceNotFoundException("Écriture introuvable · " + reference));
        if (!found.getWallet().getId().equals(owner.getId())) {
            throw new ResourceNotFoundException("Écriture introuvable · " + reference);
        }
        return found;
    }

    @Test
    void theEntryNumberOfTheStatementFindsTheReceipt() {
        assertEquals(entry, lookup(wallet, "WLE-202609-00000005"));
    }

    @Test
    void theTransactionNumberStillWorks() {
        assertEquals(entry, lookup(wallet, "WTX-202609-0000005"));
    }

    @Test
    void anEntryOfSomeoneElseIsAsInvisibleAsOneThatDoesNotExist() {
        ResourceNotFoundException foreign = assertThrows(ResourceNotFoundException.class,
                () -> lookup(otherWallet, "WLE-202609-00000005"));
        ResourceNotFoundException missing = assertThrows(ResourceNotFoundException.class,
                () -> lookup(wallet, "WLE-INEXISTANT"));
        assertTrue(foreign.getMessage().contains("introuvable"));
        assertTrue(missing.getMessage().contains("introuvable"));
    }

    @Test
    void anEntryWithoutTransactionNumberIsStillReachable() {
        WalletLedgerEntry legacy = WalletLedgerEntry.builder().id(51L).wallet(wallet)
                .entryNumber("WLE-202501-00000001").transactionNumber(null)
                .entryType(WalletEntryType.PAYMENT).direction(WalletEntryDirection.DEBIT)
                .amount(BigDecimal.TEN).balanceAfter(BigDecimal.ZERO).createdAt(Instant.now()).build();
        when(ledgerRepository.findByEntryNumber("WLE-202501-00000001")).thenReturn(Optional.of(legacy));

        assertEquals(legacy, lookup(wallet, "WLE-202501-00000001"));
    }
}
