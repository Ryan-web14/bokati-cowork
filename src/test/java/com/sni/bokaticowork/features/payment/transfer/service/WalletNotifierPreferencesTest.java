package com.sni.bokaticowork.features.payment.transfer.service;

import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxService;
import com.sni.bokaticowork.features.billing.service.support.MemberBillingContact;
import com.sni.bokaticowork.features.payment.model.WalletAccount;
import com.sni.bokaticowork.features.payment.service.support.TransactionContextResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * « Factures seulement » : pas de recu par courriel a chaque operation du portefeuille, mais les
 * alertes de solde et de securite partent toujours.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WalletNotifierPreferencesTest {

    @Mock private OutboxService outboxService;
    @Mock private TransactionContextResolver contextResolver;
    @Mock private MemberBillingContact memberContact;

    private WalletNotifier notifier;
    private WalletAccount wallet;

    @BeforeEach
    void setUp() {
        notifier = new WalletNotifier(outboxService, contextResolver);
        notifier.setMemberContact(memberContact);
        wallet = WalletAccount.builder().id(1L).walletNumber("WAL-1").ownerType("MEMBER").ownerCode("MBR-1")
                .currency("XAF").availableBalance(new BigDecimal("500")).ledgerBalance(new BigDecimal("500"))
                .heldBalance(BigDecimal.ZERO).lowBalanceThreshold(new BigDecimal("1000"))
                .notifyOnCredit(true).notifyOnDebit(true).build();
        when(contextResolver.resolveParty("MEMBER", "MBR-1")).thenReturn(
                new TransactionContextResolver.PartyView("MEMBER", "MBR-1", "Joël", "joel@example.com", null, null, true));
        when(contextResolver.resolveBillingParty("MEMBER", "MBR-1")).thenReturn(
                new TransactionContextResolver.PartyView("MEMBER", "MBR-1", "Joël", "compta@example.com", null, null, true));
    }

    @Test
    @DisplayName("Par defaut, le recu de rechargement part a l'adresse de facturation")
    void receiptByDefault() {
        when(memberContact.wantsEveryWalletReceipt("MEMBER", "MBR-1")).thenReturn(true);

        notifier.topUpCompleted(wallet, new BigDecimal("2000"), "TXN-1");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> payload = ArgumentCaptor.forClass(Map.class);
        verify(outboxService).publish(eq("WALLET_TOPUP_COMPLETED"), anyString(), eq("TXN-1"), payload.capture());
        assertThat(payload.getValue()).containsEntry("recipientEmail", "compta@example.com");
    }

    @Test
    @DisplayName("Factures seulement : pas de recu, mais l'alerte de solde bas et la securite partent")
    void invoiceOnly() {
        when(memberContact.wantsEveryWalletReceipt("MEMBER", "MBR-1")).thenReturn(false);

        notifier.topUpCompleted(wallet, new BigDecimal("2000"), "TXN-1");
        verify(outboxService, never()).publish(eq("WALLET_TOPUP_COMPLETED"), anyString(), anyString(), any());

        notifier.lowBalance(wallet);
        notifier.securityEvent(wallet, "WALLET_NEW_DEVICE", "Nouvel appareil", Map.of());
        verify(outboxService).publish(eq("WALLET_LOW_BALANCE"), anyString(), anyString(), any());
        verify(outboxService).publish(eq("WALLET_NEW_DEVICE"), anyString(), anyString(), any());
    }
}
