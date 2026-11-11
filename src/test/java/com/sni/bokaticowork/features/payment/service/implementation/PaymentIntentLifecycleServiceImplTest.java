package com.sni.bokaticowork.features.payment.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.payment.dto.request.CancelPaymentIntentRequest;
import com.sni.bokaticowork.features.payment.dto.request.RelaunchPaymentIntentRequest;
import com.sni.bokaticowork.features.payment.enums.PaymentIntentStatus;
import com.sni.bokaticowork.features.payment.mapper.interfaces.PaymentMapper;
import com.sni.bokaticowork.features.payment.model.PaymentIntent;
import com.sni.bokaticowork.features.payment.repository.PaymentIntentRepository;
import com.sni.bokaticowork.features.payment.repository.PaymentTransactionRepository;
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
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaymentIntentLifecycleServiceImplTest {

    @Mock private PaymentIntentRepository intentRepository;
    @Mock private PaymentTransactionRepository transactionRepository;
    @Mock private BillingDocumentRepository billingDocumentRepository;
    @Mock private PaymentMapper mapper;
    @Mock private PaymentOriginResolver paymentOrigin;

    @InjectMocks private PaymentIntentLifecycleServiceImpl service;

    private PaymentIntent intent;

    @BeforeEach
    void setUp() {
        Instant created = Instant.now().minus(Duration.ofDays(3));
        intent = PaymentIntent.builder()
                .id(1L)
                .intentNumber("INT-1")
                .customerType("MEMBER")
                .customerCode("MBR-1")
                .amount(new BigDecimal("50000"))
                .currency("XAF")
                .status(PaymentIntentStatus.PENDING)
                .sourceType("BILLING_DOCUMENT")
                .sourceCode("FAC-1")
                .createdAt(created)
                .expiresAt(created.plus(Duration.ofDays(2)))
                .build();
        when(intentRepository.findByIntentNumber("INT-1")).thenReturn(Optional.of(intent));
        when(intentRepository.findLatestReusable(anyString(), anyString(), anyString(), anyString())).thenReturn(Optional.empty());
        when(transactionRepository.sumSucceededAmountByPaymentIntentId(1L)).thenReturn(BigDecimal.ZERO);
        when(transactionRepository.sumProcessingAmountByPaymentIntentId(1L)).thenReturn(BigDecimal.ZERO);
        when(paymentOrigin.currentActor()).thenReturn("caisse@sni-cg.com");
        when(billingDocumentRepository.findByDocumentNumber("FAC-1")).thenReturn(Optional.of(invoice("50000")));
    }

    private static BillingDocument invoice(String balance) {
        BillingDocument document = new BillingDocument();
        document.setDocumentNumber("FAC-1");
        document.setStatus(BillingDocumentStatus.ISSUED);
        document.setBalanceDue(new BigDecimal(balance));
        return document;
    }

    // --- Annulation -------------------------------------------------------------------------

    @Test
    @DisplayName("Annuler une intention en attente : CANCELLED, auteur, motif, lien de paiement ferme")
    void cancelAPendingIntent() {
        intent.setExpiresAt(Instant.now().plus(Duration.ofHours(1)));
        intent.setPaymentLinkToken("tok");
        intent.setPaymentLinkExpiresAt(Instant.now().plus(Duration.ofDays(1)));

        service.cancel("INT-1", new CancelPaymentIntentRequest("  Client parti  "));

        assertThat(intent.getStatus()).isEqualTo(PaymentIntentStatus.CANCELLED);
        assertThat(intent.getCancelledBy()).isEqualTo("caisse@sni-cg.com");
        assertThat(intent.getCancellationReason()).isEqualTo("Client parti");
        assertThat(intent.getPaymentLinkExpiresAt()).isBeforeOrEqualTo(Instant.now());
        verify(intentRepository).save(intent);
    }

    @Test
    @DisplayName("Annuler pendant un paiement mobile money en cours : refuse")
    void cancelWhileMobileMoneyIsProcessingIsRefused() {
        intent.setStatus(PaymentIntentStatus.PROCESSING);
        intent.setExpiresAt(Instant.now().plus(Duration.ofHours(1)));
        when(transactionRepository.sumProcessingAmountByPaymentIntentId(1L)).thenReturn(new BigDecimal("50000"));

        assertThatThrownBy(() -> service.cancel("INT-1", null)).isInstanceOf(ConflictException.class);
        assertThat(intent.getStatus()).isEqualTo(PaymentIntentStatus.PROCESSING);
    }

    @Test
    @DisplayName("Annuler une intention reglee : refuse ; deja annulee : rendue telle quelle")
    void cancelOnlyOpenIntents() {
        intent.setStatus(PaymentIntentStatus.SUCCEEDED);
        assertThatThrownBy(() -> service.cancel("INT-1", null)).isInstanceOf(BadRequestException.class);

        intent.setStatus(PaymentIntentStatus.CANCELLED);
        service.cancel("INT-1", null);
        verify(intentRepository, never()).save(any());
    }

    // --- Relance ----------------------------------------------------------------------------

    @Test
    @DisplayName("Relancer une intention expiree : PENDING, meme numero, la duree d'origine repart de maintenant")
    void relaunchAnExpiredIntent() {
        intent.setStatus(PaymentIntentStatus.EXPIRED);
        intent.setExpiryAlertSentAt(Instant.now().minus(Duration.ofDays(1)));

        service.relaunch("INT-1", null);

        assertThat(intent.getStatus()).isEqualTo(PaymentIntentStatus.PENDING);
        assertThat(intent.getExpiresAt()).isCloseTo(Instant.now().plus(Duration.ofDays(2)), within(Duration.ofMinutes(1)));
        assertThat(intent.getExpiryAlertSentAt()).isNull();
        assertThat(intent.getRelaunchCount()).isEqualTo(1);
        assertThat(intent.getLastRelaunchedBy()).isEqualTo("caisse@sni-cg.com");
        assertThat(intent.getAmount()).isEqualByComparingTo("50000");
    }

    @Test
    @DisplayName("Relancer une intention annulee avec une echeance choisie ; l'annulation est effacee")
    void relaunchACancelledIntentWithAChosenExpiry() {
        intent.setStatus(PaymentIntentStatus.CANCELLED);
        intent.setCancelledBy("x");
        intent.setCancellationReason("motif");
        Instant wanted = Instant.now().plus(Duration.ofDays(7));

        service.relaunch("INT-1", new RelaunchPaymentIntentRequest(wanted));

        assertThat(intent.getExpiresAt()).isEqualTo(wanted);
        assertThat(intent.getCancelledBy()).isNull();
        assertThat(intent.getCancellationReason()).isNull();
    }

    @Test
    @DisplayName("Echeance dans le passe : refusee")
    void pastExpiryIsRefused() {
        intent.setStatus(PaymentIntentStatus.EXPIRED);
        assertThatThrownBy(() -> service.relaunch("INT-1", new RelaunchPaymentIntentRequest(Instant.now().minusSeconds(60))))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Facture reglee en partie entre-temps : le montant est ramene au reste du")
    void relaunchShrinksToTheOutstandingBalance() {
        intent.setStatus(PaymentIntentStatus.EXPIRED);
        when(transactionRepository.sumSucceededAmountByPaymentIntentId(1L)).thenReturn(new BigDecimal("10000"));
        when(billingDocumentRepository.findByDocumentNumber("FAC-1")).thenReturn(Optional.of(invoice("15000")));

        service.relaunch("INT-1", null);

        // 10 000 deja payes sur l'intention + 15 000 encore dus
        assertThat(intent.getAmount()).isEqualByComparingTo("25000");
    }

    @Test
    @DisplayName("Facture deja soldee : rien a relancer")
    void relaunchWhenTheInvoiceIsPaidIsRefused() {
        intent.setStatus(PaymentIntentStatus.EXPIRED);
        when(billingDocumentRepository.findByDocumentNumber("FAC-1")).thenReturn(Optional.of(invoice("0")));

        assertThatThrownBy(() -> service.relaunch("INT-1", null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("déjà réglées");
    }

    @Test
    @DisplayName("Une autre intention est deja en cours pour la meme facture : refuse")
    void relaunchWhenAnotherIntentIsOpenIsRefused() {
        intent.setStatus(PaymentIntentStatus.EXPIRED);
        PaymentIntent other = PaymentIntent.builder().intentNumber("INT-2").expiresAt(Instant.now().plusSeconds(600)).build();
        when(intentRepository.findLatestReusable("MEMBER", "MBR-1", "BILLING_DOCUMENT", "FAC-1")).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> service.relaunch("INT-1", null))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("INT-2");
    }

    @Test
    @DisplayName("Une intention en cours et dans les temps ne se relance pas ; echeance depassee avant le worker : oui")
    void onlyClosedOrLapsedIntentsCanBeRelaunched() {
        intent.setExpiresAt(Instant.now().plus(Duration.ofHours(1)));
        assertThatThrownBy(() -> service.relaunch("INT-1", null)).isInstanceOf(BadRequestException.class);

        intent.setExpiresAt(Instant.now().minusSeconds(30));
        service.relaunch("INT-1", null);
        assertThat(intent.getStatus()).isEqualTo(PaymentIntentStatus.PENDING);
        assertThat(intent.getExpiresAt()).isAfter(Instant.now());
    }

    @Test
    @DisplayName("Intention deja entierement payee : pas de relance")
    void fullyPaidIntentIsNotRelaunched() {
        intent.setStatus(PaymentIntentStatus.CANCELLED);
        when(transactionRepository.sumSucceededAmountByPaymentIntentId(1L)).thenReturn(new BigDecimal("50000"));

        assertThatThrownBy(() -> service.relaunch("INT-1", null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("entièrement payée");
    }
}
