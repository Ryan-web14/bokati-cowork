package com.sni.bokaticowork.features.payment.service.implementation;

import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.payment.dto.request.InitiateMobileMoneyDepositRequest;
import com.sni.bokaticowork.features.payment.dto.request.RefundPaymentRequest;
import com.sni.bokaticowork.features.payment.dto.request.WalletPaymentRequest;
import com.sni.bokaticowork.features.payment.dto.response.MobileMoneyDepositResponse;
import com.sni.bokaticowork.features.payment.enums.PaymentIntentStatus;
import com.sni.bokaticowork.features.payment.enums.PaymentMethod;
import com.sni.bokaticowork.features.payment.enums.PaymentTransactionStatus;
import com.sni.bokaticowork.features.payment.model.PawapayDeposit;
import com.sni.bokaticowork.features.payment.model.PaymentIntent;
import com.sni.bokaticowork.features.payment.model.PaymentTransaction;
import com.sni.bokaticowork.features.payment.model.WalletAccount;
import com.sni.bokaticowork.features.payment.repository.PaymentIntentRepository;
import com.sni.bokaticowork.features.payment.repository.PaymentTransactionRepository;
import com.sni.bokaticowork.features.payment.service.interfaces.WalletService;
import com.sni.bokaticowork.features.payment.service.pawaypay.PawapayDepositService;
import com.sni.bokaticowork.features.payment.service.support.PaymentAllocationService;
import com.sni.bokaticowork.features.payment.service.support.PaymentOriginResolver;
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
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Ecarts du paiement regles le 17/11/2026. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaymentGapsFixTest {

    @Mock private PaymentTransactionRepository transactionRepository;
    @Mock private PaymentIntentRepository intentRepository;
    @Mock private PaymentAllocationService allocationService;
    @Mock private SequenceGeneratorFacade sequenceGenerator;
    @Mock private com.sni.bokaticowork.features.payment.mapper.interfaces.PaymentMapper mapper;
    @Mock private com.sni.bokaticowork.core.outbox.service.interfaces.OutboxService outboxService;
    @Mock private PawapayDepositService pawapayDepositService;
    @Mock private WalletService walletService;
    @Mock private PaymentOriginResolver paymentOrigin;
    @org.mockito.Spy private com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
    @InjectMocks private PaymentServiceImpl service;

    private PaymentIntent intent;

    @BeforeEach
    void setUp() {
        intent = new PaymentIntent();
        intent.setId(1L);
        intent.setIntentNumber("PI-1");
        intent.setAmount(new BigDecimal("100"));
        intent.setCurrency("XAF");
        intent.setCustomerType("MEMBER");
        intent.setCustomerCode("MBR-1");
        intent.setStatus(PaymentIntentStatus.PROCESSING);
        intent.setExpiresAt(Instant.now().plusSeconds(3600));
        when(intentRepository.findByIntentNumber("PI-1")).thenReturn(Optional.of(intent));
        when(transactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(sequenceGenerator.next(anyString())).thenReturn("SEQ-0000042");
        when(transactionRepository.sumSucceededAmountByPaymentIntentId(anyLong())).thenReturn(BigDecimal.ZERO);
        when(transactionRepository.sumProcessingAmountByPaymentIntentId(anyLong())).thenReturn(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Double envoi mobile money : le depot en cours est rendu, meme si plus rien n'est payable")
    void anInFlightDepositIsReturnedBeforeThePayableCheck() {
        // Le depot en cours reserve tout le montant : le reste payable est nul.
        when(transactionRepository.sumProcessingAmountByPaymentIntentId(anyLong())).thenReturn(new BigDecimal("100"));
        PawapayDeposit pending = new PawapayDeposit();
        pending.setDepositId("dep-1");
        when(pawapayDepositService.inFlight(eq(intent), any())).thenReturn(Optional.of(pending));
        MobileMoneyDepositResponse expected = mock(MobileMoneyDepositResponse.class);
        when(pawapayDepositService.toResponse(pending)).thenReturn(expected);

        MobileMoneyDepositResponse response = service.initiateMobileMoneyDeposit("PI-1",
                new InitiateMobileMoneyDepositRequest("PI-1", "242061234567", null, null, null, null));

        assertThat(response).isSameAs(expected);
    }

    @Test
    @DisplayName("Remboursement partiel : l'intention passe PARTIALLY_REFUNDED")
    void aPartialRefundMarksTheIntent() {
        intent.setStatus(PaymentIntentStatus.SUCCEEDED);
        PaymentTransaction original = new PaymentTransaction();
        original.setTransactionNumber("TXN-1");
        original.setPaymentIntent(intent);
        original.setPaymentMethod(PaymentMethod.CASH);
        original.setAmount(new BigDecimal("100"));
        original.setCurrency("XAF");
        original.setStatus(PaymentTransactionStatus.SUCCEEDED);
        when(transactionRepository.findByTransactionNumber("TXN-1")).thenReturn(Optional.of(original));
        when(transactionRepository.findAllByOriginalTransactionNumber("TXN-1")).thenReturn(List.of());

        service.refund("TXN-1", new RefundPaymentRequest(new BigDecimal("30"), "Geste commercial", null));

        assertThat(intent.getStatus()).isEqualTo(PaymentIntentStatus.PARTIALLY_REFUNDED);
    }

    @Test
    @DisplayName("Second paiement partiel au portefeuille : une cle distincte, donc un vrai debit ; auteur du jeton")
    void aSecondPartialWalletPaymentDebitsAgain() {
        intent.setStatus(PaymentIntentStatus.PENDING);
        WalletAccount wallet = new WalletAccount();
        wallet.setWalletNumber("W-1");
        wallet.setOwnerType("MEMBER");
        wallet.setOwnerCode("MBR-1");
        wallet.setCurrency("XAF");
        when(walletService.serviceWallet("W-1")).thenReturn(wallet);
        PaymentTransaction first = new PaymentTransaction();
        first.setPaymentMethod(PaymentMethod.WALLET);
        first.setStatus(PaymentTransactionStatus.SUCCEEDED);
        when(transactionRepository.findAllByPaymentIntentIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(first));
        when(transactionRepository.sumSucceededAmountByPaymentIntentId(anyLong())).thenReturn(new BigDecimal("40"));
        when(paymentOrigin.currentActor()).thenReturn("caissier@elleaose.com");

        try {
            service.payWithWallet("PI-1", new WalletPaymentRequest("W-1", new BigDecimal("30"), "quelqu'un d'autre", null));
        } catch (RuntimeException ignored) {
            // la suite (rapprochement, notifications) n'est pas simulee
        }

        verify(walletService).debit(eq(wallet), eq(new BigDecimal("30.0000")), any(), eq("PAYMENT_INTENT"), eq("PI-1"), eq("PI-1"),
                eq("caissier@elleaose.com"), eq("WALLET_PAYMENT:PI-1:2"));
    }

    @Test
    @DisplayName("Double clic sur un paiement en especes : le paiement deja enregistre est rendu, rien n'est cree")
    void aDoubleClickOnCashReturnsTheFirstPayment() {
        service.setDuplicateGuard(new com.sni.bokaticowork.features.payment.service.support.PaymentDuplicateGuard());
        when(paymentOrigin.currentActor()).thenReturn("caissier@elleaose.com");
        PaymentTransaction first = new PaymentTransaction();
        first.setTransactionNumber("TXN-CASH-1");
        first.setPaymentMethod(PaymentMethod.CASH);
        first.setStatus(PaymentTransactionStatus.SUCCEEDED);
        first.setAmount(new BigDecimal("50.0000"));
        first.setReceivedBy("caissier@elleaose.com");
        first.setCreatedAt(Instant.now().minusSeconds(2));
        when(transactionRepository.findAllByPaymentIntentIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(first));
        var expected = mock(com.sni.bokaticowork.features.payment.dto.response.PaymentTransactionResponse.class);
        when(mapper.toTransactionResponse(first)).thenReturn(expected);
        // Le premier clic a tout paye : sans le controle, le second echouerait ou paierait deux fois.
        intent.setStatus(PaymentIntentStatus.SUCCEEDED);

        var response = service.registerCashPayment("PI-1",
                new com.sni.bokaticowork.features.payment.dto.request.RegisterCashPaymentRequest(null, "CS-1", new BigDecimal("50"), null, null, null));

        assertThat(response).isSameAs(expected);
        org.mockito.Mockito.verify(transactionRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    @DisplayName("Paiement identique mais plus ancien que la fenetre, ou d'un autre montant : un nouveau paiement")
    void onlyARecentIdenticalPaymentIsReplayed() {
        var guard = new com.sni.bokaticowork.features.payment.service.support.PaymentDuplicateGuard();
        PaymentTransaction old = new PaymentTransaction();
        old.setPaymentMethod(PaymentMethod.CASH);
        old.setStatus(PaymentTransactionStatus.SUCCEEDED);
        old.setAmount(new BigDecimal("50.0000"));
        old.setReceivedBy("caissier@elleaose.com");
        old.setCreatedAt(Instant.now().minusSeconds(120));

        assertThat(guard.recentTwin(List.of(old), PaymentMethod.CASH, new BigDecimal("50.0000"), "caissier@elleaose.com")).isEmpty();
        old.setCreatedAt(Instant.now().minusSeconds(5));
        assertThat(guard.recentTwin(List.of(old), PaymentMethod.CASH, new BigDecimal("40.0000"), "caissier@elleaose.com")).isEmpty();
        assertThat(guard.recentTwin(List.of(old), PaymentMethod.WALLET, new BigDecimal("50.0000"), "caissier@elleaose.com")).isEmpty();
        assertThat(guard.recentTwin(List.of(old), PaymentMethod.CASH, new BigDecimal("50.0000"), "caissier@elleaose.com")).contains(old);
    }

    @Test
    @DisplayName("Vue du portail : sans adresse de rappel ni echanges bruts avec PawaPay")
    void thePortalViewHidesProviderPayloads() {
        MobileMoneyDepositResponse full = new MobileMoneyDepositResponse("dep-1", "TXN-1", null, null, null, "MTN", BigDecimal.TEN, "XAF",
                "ACCEPTED", "WAITING_FOR_PAYER", null, null, null, null, null, null, "PI-1", "TXN-1", "MEMBER", "MBR-1",
                "https://api/callback", "{\"secret\":1}", "{\"raw\":1}", null, null, null, null, null, null);

        MobileMoneyDepositResponse portal = full.forPortal();

        assertThat(portal.callbackUrl()).isNull();
        assertThat(portal.requestPayloadJson()).isNull();
        assertThat(portal.providerResponseJson()).isNull();
        assertThat(portal.depositId()).isEqualTo("dep-1");
        assertThat(portal.phase()).isEqualTo("WAITING_FOR_PAYER");
    }
}
