package com.sni.bokaticowork.features.payment.transfer.service;

import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.features.payment.dto.request.CreatePaymentIntentRequest;
import com.sni.bokaticowork.features.payment.enums.PaymentTransactionStatus;
import com.sni.bokaticowork.features.payment.enums.WalletEntryType;
import com.sni.bokaticowork.features.payment.enums.WalletStatus;
import com.sni.bokaticowork.features.payment.limit.service.WalletKycLevelResolver;
import com.sni.bokaticowork.features.payment.limit.service.WalletLimitService;
import com.sni.bokaticowork.features.payment.model.PaymentIntent;
import com.sni.bokaticowork.features.payment.model.PaymentTransaction;
import com.sni.bokaticowork.features.payment.model.WalletAccount;
import com.sni.bokaticowork.features.payment.provider.pawaypay.CongoCorrespondent;
import com.sni.bokaticowork.features.payment.repository.WalletAccountRepository;
import com.sni.bokaticowork.features.payment.service.interfaces.PaymentService;
import com.sni.bokaticowork.features.payment.service.support.WalletLedgerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Le rechargement en libre-service.
 *
 * <p>Deux choses comptent : les plafonds sont vérifiés avant d'engager l'opérateur, parce qu'un
 * refus après coup obligerait à rembourser ; et le règlement est idempotent par la clé dérivée du
 * numéro de transaction, parce que le flux qui l'appelle est rejoué.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WalletTopUpServiceTest {

    @Mock private PaymentService paymentService;
    @Mock private WalletAccountRepository walletRepository;
    @Mock private WalletLedgerService ledgerService;
    @Mock private WalletLimitService limitService;
    @Mock private WalletKycLevelResolver kycLevelResolver;
    @Mock private WalletNotifier notifier;

    @InjectMocks
    private WalletTopUpService service;

    private WalletAccount wallet;

    @BeforeEach
    void setUp() {
        wallet = WalletAccount.builder()
                .id(1L).walletNumber("WAL-1").ownerType("MEMBER").ownerCode("MBR-1").currency("XAF")
                .status(WalletStatus.ACTIVE)
                .availableBalance(BigDecimal.ZERO).ledgerBalance(BigDecimal.ZERO).heldBalance(BigDecimal.ZERO)
                .notifyOnCredit(true).build();
        when(kycLevelResolver.levelOf(any())).thenReturn(1);
        when(limitService.checkTopUp(any(), anyInt(), any()))
                .thenReturn(new WalletLimitService.LimitVerdict(true, null, null));
        when(walletRepository.findByWalletNumber("WAL-1")).thenReturn(Optional.of(wallet));
        when(walletRepository.findById(1L)).thenReturn(Optional.of(wallet));
    }

    @Test
    void lesPlafondsSontVerifiesAvantDEngagerLOperateur() {
        Mockito.doThrow(new ConflictException("wallet", "Plafond par rechargement atteint"))
                .when(limitService).assertAllowed(any());

        assertThrows(ConflictException.class, () -> service.initiate(wallet,
                new WalletTopUpService.TopUpOrder(new BigDecimal("1000000"), "+242060000000", CongoCorrespondent.MTN_MOMO_COG), "MBR-1"));

        verify(paymentService, never()).createIntent(any());
        verify(paymentService, never()).initiateMobileMoneyDeposit(any(), any());
    }

    @Test
    void lIntentionEstMarqueeCommeRechargementEtNonCommeVente() {
        com.sni.bokaticowork.features.payment.dto.response.PaymentIntentResponse created =
                Mockito.mock(com.sni.bokaticowork.features.payment.dto.response.PaymentIntentResponse.class);
        when(created.intentNumber()).thenReturn("PIT-1");
        when(paymentService.createIntent(any())).thenReturn(created);

        service.initiate(wallet, new WalletTopUpService.TopUpOrder(new BigDecimal("5000"), "+242060000000",
                CongoCorrespondent.MTN_MOMO_COG), "MBR-1");

        ArgumentCaptor<CreatePaymentIntentRequest> intent = ArgumentCaptor.forClass(CreatePaymentIntentRequest.class);
        verify(paymentService).createIntent(intent.capture());
        assertEquals("WALLET_TOPUP", intent.getValue().sourceType(),
                "C'est cette nature qui empêche la facturation automatique de traiter le rechargement comme une vente");
        assertEquals("WAL-1", intent.getValue().sourceCode());
    }

    @Test
    void unPortefeuilleGeleNeSeRechargePas() {
        wallet.setFrozenAt(Instant.now());

        assertThrows(ConflictException.class, () -> service.initiate(wallet,
                new WalletTopUpService.TopUpOrder(new BigDecimal("5000"), "+242060000000", CongoCorrespondent.MTN_MOMO_COG), "MBR-1"));
    }

    @Test
    void leReglementCrediteAvecUneCleDeriveeDuNumeroDeTransaction() {
        PaymentTransaction transaction = succeeded("WALLET_TOPUP", "WAL-1");

        service.settle(transaction);

        verify(ledgerService).credit(eq(wallet), eq(new BigDecimal("5000")), eq(WalletEntryType.TOPUP),
                any(), eq("TXN-1"), any(), any(), eq("WALLET_TOPUP:TXN-1"));
        verify(notifier).topUpCompleted(any(), eq(new BigDecimal("5000")), eq("TXN-1"));
    }

    @Test
    void leReglementIgnoreCeQuiNEstPasUnRechargement() {
        service.settle(succeeded("BILLING_DOCUMENT", "INV-1"));

        verify(ledgerService, never()).credit(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void leReglementIgnoreUneTransactionNonAboutie() {
        PaymentTransaction transaction = succeeded("WALLET_TOPUP", "WAL-1");
        transaction.setStatus(PaymentTransactionStatus.PROCESSING);

        service.settle(transaction);

        verify(ledgerService, never()).credit(any(), any(), any(), any(), any(), any(), any(), any());
    }

    private PaymentTransaction succeeded(String sourceType, String sourceCode) {
        PaymentIntent intent = PaymentIntent.builder().intentNumber("PIT-1").sourceType(sourceType).sourceCode(sourceCode).build();
        return PaymentTransaction.builder()
                .transactionNumber("TXN-1").paymentIntent(intent)
                .amount(new BigDecimal("5000")).currency("XAF")
                .status(PaymentTransactionStatus.SUCCEEDED).build();
    }
}
