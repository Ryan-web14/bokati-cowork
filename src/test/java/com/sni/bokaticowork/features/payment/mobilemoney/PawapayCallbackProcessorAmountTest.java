package com.sni.bokaticowork.features.payment.mobilemoney;

import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxService;
import com.sni.bokaticowork.features.payment.dto.response.WalletResponse;
import com.sni.bokaticowork.features.payment.enums.PaymentIntentStatus;
import com.sni.bokaticowork.features.payment.enums.PaymentMethod;
import com.sni.bokaticowork.features.payment.enums.PaymentTransactionStatus;
import com.sni.bokaticowork.features.payment.enums.WalletEntryType;
import com.sni.bokaticowork.features.payment.model.PawapayDeposit;
import com.sni.bokaticowork.features.payment.model.PaymentIntent;
import com.sni.bokaticowork.features.payment.model.PaymentTransaction;
import com.sni.bokaticowork.features.payment.model.WalletAccount;
import com.sni.bokaticowork.features.payment.provider.pawaypay.dto.PawapayCallbackPayload;
import com.sni.bokaticowork.features.payment.repository.PawapayDepositRepository;
import com.sni.bokaticowork.features.payment.repository.PaymentIntentRepository;
import com.sni.bokaticowork.features.payment.repository.PaymentTransactionRepository;
import com.sni.bokaticowork.features.payment.service.interfaces.WalletService;
import com.sni.bokaticowork.features.payment.service.pawaypay.MobileMoneyAlertRecipients;
import com.sni.bokaticowork.features.payment.service.pawaypay.MobileMoneyClientNotifier;
import com.sni.bokaticowork.features.payment.service.pawaypay.PawapayCallbackProcessor;
import com.sni.bokaticowork.features.payment.service.pawaypay.PawapayDepositService;
import com.sni.bokaticowork.features.payment.service.support.PaymentAllocationService;
import com.sni.bokaticowork.features.payment.service.support.SelfServicePaymentNotifier;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Une confirmation de l'operateur ne vaut que pour ce qu'on lui a demande.
 *
 * <p>Le montant et la devise annonces n'etaient pas compares au depot · un rappel forge, ou une
 * anomalie chez l'operateur, soldait la facture quel que soit le montant recu. Et ce qui depassait
 * le solde d'une facture deja reglee disparaissait au lieu d'etre credite au client.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PawapayCallbackProcessorAmountTest {

    @Mock private PaymentTransactionRepository transactionRepository;
    @Mock private PaymentIntentRepository intentRepository;
    @Mock private PaymentAllocationService allocationService;
    @Mock private OutboxService outboxService;
    @Mock private SequenceGeneratorFacade sequenceGenerator;
    @Mock private PawapayDepositService depositService;
    @Mock private PawapayDepositRepository depositRepository;
    @Mock private MobileMoneyClientNotifier clientNotifier;
    @Mock private SelfServicePaymentNotifier selfServiceNotifier;
    @Mock private MobileMoneyAlertRecipients alertRecipients;
    @Mock private WalletService walletService;
    @InjectMocks private PawapayCallbackProcessor processor;

    private PaymentIntent intent;
    private PaymentTransaction transaction;

    @BeforeEach
    void setUp() {
        intent = PaymentIntent.builder()
                .intentNumber("INT-1")
                .customerType("MEMBER")
                .customerCode("MBR-1")
                .amount(new BigDecimal("5000.00"))
                .currency("XAF")
                .status(PaymentIntentStatus.PROCESSING)
                .sourceType("BILLING_DOCUMENT")
                .sourceCode("INV-1")
                .build();
        intent.setId(1L);
        transaction = PaymentTransaction.builder()
                .transactionNumber("TXN-1")
                .paymentIntent(intent)
                .paymentMethod(PaymentMethod.MOBILE_MONEY)
                .amount(new BigDecimal("5000.00"))
                .currency("XAF")
                .providerReference("dep-1")
                .status(PaymentTransactionStatus.PROCESSING)
                .build();
        transaction.setId(10L);
        when(transactionRepository.findByProviderReference("dep-1")).thenReturn(Optional.of(transaction));
        when(transactionRepository.sumSucceededAmountByPaymentIntentId(1L)).thenReturn(new BigDecimal("5000.00"));
        when(transactionRepository.sumProcessingAmountByPaymentIntentId(1L)).thenReturn(BigDecimal.ZERO);
        when(sequenceGenerator.next("receipt")).thenReturn("RCP-1");
        when(allocationService.allocateIfBillingDocument(any())).thenReturn(BigDecimal.ZERO);
        when(alertRecipients.recipients()).thenReturn(List.of("tresorerie@elleaose.com"));
        when(depositService.markAmountMismatch(eq("dep-1"), anyString()))
                .thenReturn(PawapayDeposit.builder().depositId("dep-1").amount(new BigDecimal("5000")).currency("XAF").build());
    }

    private PawapayCallbackPayload completed(String amount, String currency) {
        return new PawapayCallbackPayload("dep-1", "COMPLETED", amount, currency, "TXN-1", null, null,
                "prov-1", null, null, null, null, null, null, null, null);
    }

    @Test
    @DisplayName("Un montant different de celui demande ne solde rien · le depot passe a rapprocher")
    void amountMismatchIsHeld() {
        processor.process(completed("10", "XAF"));

        assertThat(transaction.getStatus()).isEqualTo(PaymentTransactionStatus.PROCESSING);
        verify(depositService).markAmountMismatch(eq("dep-1"), contains("montant annonce 10, attendu 5000"));
        verify(allocationService, never()).allocateIfBillingDocument(any());
        verify(depositService, never()).markCompleted(anyString(), any());
        verify(outboxService).publish(eq("MOBILE_MONEY_DEPOSIT_UNRESOLVED"), eq("PAYMENT"), anyString(), anyMap());
        verify(clientNotifier).depositPendingReview(any());
    }

    @Test
    @DisplayName("Une autre devise ne solde rien non plus")
    void currencyMismatchIsHeld() {
        processor.process(completed("5000", "XOF"));

        assertThat(transaction.getStatus()).isEqualTo(PaymentTransactionStatus.PROCESSING);
        verify(depositService).markAmountMismatch(eq("dep-1"), contains("devise annoncee XOF"));
    }

    @Test
    @DisplayName("Le meme montant, ecrit sans decimales, est encaisse")
    void matchingAmountIsSettled() {
        processor.process(completed("5000", "XAF"));

        assertThat(transaction.getStatus()).isEqualTo(PaymentTransactionStatus.SUCCEEDED);
        verify(depositService).markCompleted(eq("dep-1"), any());
        verify(depositService, never()).markAmountMismatch(anyString(), anyString());
    }

    @Test
    @DisplayName("Une confirmation sans montant (resolution manuelle) n'est pas bloquee")
    void missingAmountIsNotBlocking() {
        processor.process(completed(null, null));

        assertThat(transaction.getStatus()).isEqualTo(PaymentTransactionStatus.SUCCEEDED);
    }

    @Test
    @DisplayName("Ce qui depasse le solde de la facture est credite au portefeuille du client")
    void overpaymentIsCredited() {
        when(allocationService.allocateIfBillingDocument(any())).thenReturn(new BigDecimal("2000.00"));
        WalletResponse walletResponse = mock(WalletResponse.class);
        when(walletResponse.walletNumber()).thenReturn("WLT-1");
        when(walletService.getOrCreate("MEMBER", "MBR-1", "XAF")).thenReturn(walletResponse);
        WalletAccount account = mock(WalletAccount.class);
        when(walletService.serviceWallet("WLT-1")).thenReturn(account);

        processor.process(completed("5000", "XAF"));

        verify(walletService).credit(eq(account), eq(new BigDecimal("2000.00")), eq(WalletEntryType.OVERPAYMENT_CREDIT),
                eq("PAYMENT_TRANSACTION"), eq("TXN-1"), eq("OVERPAYMENT"), eq("SYSTEM"), eq("OVERPAYMENT:TXN-1"));
    }

    @Test
    @DisplayName("Un rechargement n'est pas un trop-percu · son montant entier est credite ailleurs")
    void topUpIsNotAnOverpayment() {
        intent.setSourceType("WALLET_TOPUP");
        when(allocationService.allocateIfBillingDocument(any())).thenReturn(new BigDecimal("5000.00"));

        processor.process(completed("5000", "XAF"));

        verify(walletService, never()).credit(any(), any(), any(), anyString(), anyString(), anyString(), anyString(), anyString());
    }
}
