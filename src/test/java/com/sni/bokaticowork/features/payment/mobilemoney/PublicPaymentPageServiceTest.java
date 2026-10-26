package com.sni.bokaticowork.features.payment.mobilemoney;

import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.payment.dto.request.InitiateMobileMoneyDepositRequest;
import com.sni.bokaticowork.features.payment.dto.response.MobileMoneyDepositResponse;
import com.sni.bokaticowork.features.payment.dto.response.PaymentIntentResponse;
import com.sni.bokaticowork.features.payment.enums.PaymentIntentStatus;
import com.sni.bokaticowork.features.payment.mapper.interfaces.PaymentMapper;
import com.sni.bokaticowork.features.payment.model.PawapayDeposit;
import com.sni.bokaticowork.features.payment.model.PaymentIntent;
import com.sni.bokaticowork.features.payment.provider.pawaypay.CongoCorrespondent;
import com.sni.bokaticowork.features.payment.provider.pawaypay.PawapayClient;
import com.sni.bokaticowork.features.payment.repository.PawapayDepositRepository;
import com.sni.bokaticowork.features.payment.repository.PaymentIntentRepository;
import com.sni.bokaticowork.features.payment.service.interfaces.PaymentService;
import com.sni.bokaticowork.features.payment.service.pawaypay.MobileMoneyInitiationGuard;
import com.sni.bokaticowork.features.payment.service.pawaypay.MobileMoneySupervisionService;
import com.sni.bokaticowork.features.payment.service.pawaypay.PawapayDepositService;
import com.sni.bokaticowork.features.payment.service.pawaypay.PublicPaymentPageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * La page de paiement d'un lien · le bon pays, le bon operateur, et rien d'un autre lien.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PublicPaymentPageServiceTest {

    private static final String TOKEN = "abc123";

    @Mock private PaymentIntentRepository intentRepository;
    @Mock private PaymentService paymentService;
    @Mock private PawapayDepositRepository depositRepository;
    @Mock private PawapayDepositService depositService;
    @Mock private MobileMoneySupervisionService supervision;
    @Mock private PaymentMapper mapper;
    @Mock private ObjectProvider<PawapayClient> clientProvider;
    @Mock private PawapayClient client;

    private PublicPaymentPageService service;
    private PaymentIntent intent;

    @BeforeEach
    void setUp() {
        MobileMoneyInitiationGuard guard = new MobileMoneyInitiationGuard();
        ReflectionTestUtils.setField(guard, "enabledProviders",
                "MTN_MOMO_COG,AIRTEL_COG,AIRTEL_GAB,MTN_MOMO_CMR,MTN_MOMO_CIV,ORANGE_CIV,FREE_SEN,ORANGE_SEN,AIRTEL_COD,ORANGE_COD,VODACOM_MPESA_COD");
        service = new PublicPaymentPageService(intentRepository, paymentService, depositRepository, depositService,
                supervision, guard, mapper, clientProvider);

        intent = new PaymentIntent();
        intent.setIntentNumber("PI-1");
        intent.setCurrency("XAF");
        intent.setStatus(PaymentIntentStatus.PENDING);
        intent.setPaymentLinkToken(TOKEN);
        intent.setPaymentLinkExpiresAt(Instant.now().plusSeconds(3600));
        when(intentRepository.findByPaymentLinkToken(TOKEN)).thenReturn(Optional.of(intent));
        PaymentIntentResponse view = mock(PaymentIntentResponse.class);
        when(view.remainingAmount()).thenReturn(new BigDecimal("10"));
        when(mapper.toIntentResponse(any())).thenReturn(view);
        when(clientProvider.getIfAvailable()).thenReturn(client);
        MobileMoneyDepositResponse started = mock(MobileMoneyDepositResponse.class);
        when(started.depositId()).thenReturn("dep-1");
        when(paymentService.initiateMobileMoneyDeposit(anyString(), any())).thenReturn(started);
    }

    @Test
    @DisplayName("Un paiement en XAF ne propose que les pays qui encaissent en XAF, operateurs actives seulement")
    void countriesFollowTheCurrency() {
        List<PublicPaymentPageService.Country> countries = service.open(TOKEN).countries();

        assertThat(countries).extracting(PublicPaymentPageService.Country::code).containsExactly("COG", "GAB", "CMR");
        assertThat(countries.getFirst().operators()).extracting(PublicPaymentPageService.Operator::code)
                .containsExactlyInAnyOrder("MTN_MOMO_COG", "AIRTEL_COG");
    }

    @Test
    @DisplayName("Un pays a un seul operateur actif n'a pas besoin de prediction")
    void singleOperatorCountrySkipsPrediction() {
        service.start(TOKEN, "GAB", "074123456", null);

        ArgumentCaptor<InitiateMobileMoneyDepositRequest> request = ArgumentCaptor.forClass(InitiateMobileMoneyDepositRequest.class);
        verify(paymentService).initiateMobileMoneyDeposit(eq("PI-1"), request.capture());
        assertThat(request.getValue().correspondent()).isEqualTo(CongoCorrespondent.AIRTEL_GAB);
        assertThat(request.getValue().amount()).isEqualByComparingTo("10");
        verify(client, never()).predictProvider(anyString());
    }

    @Test
    @DisplayName("L'operateur predit par PawaPay est utilise, le numero part en international")
    void predictedOperatorIsUsed() {
        when(client.predictProvider("+242061234567"))
                .thenReturn(new PawapayClient.ProviderPrediction("COG", "MTN_MOMO_COG", "242061234567"));

        assertThat(service.start(TOKEN, "COG", "06 123 45 67", null)).isEqualTo("dep-1");

        ArgumentCaptor<InitiateMobileMoneyDepositRequest> request = ArgumentCaptor.forClass(InitiateMobileMoneyDepositRequest.class);
        verify(paymentService).initiateMobileMoneyDeposit(eq("PI-1"), request.capture());
        assertThat(request.getValue().correspondent()).isEqualTo(CongoCorrespondent.MTN_MOMO_COG);
    }

    @Test
    @DisplayName("Un numero non reconnu, ou reconnu hors du pays choisi, fait demander l'operateur")
    void unknownOperatorIsAsked() {
        when(client.predictProvider(anyString())).thenReturn(null);
        assertThatThrownBy(() -> service.start(TOKEN, "COG", "061234567", null))
                .isInstanceOf(PublicPaymentPageService.OperatorNeededException.class);

        when(client.predictProvider(anyString()))
                .thenReturn(new PawapayClient.ProviderPrediction("COD", "VODACOM_MPESA_COD", "243812345678"));
        assertThatThrownBy(() -> service.start(TOKEN, "COG", "061234567", null))
                .isInstanceOf(PublicPaymentPageService.OperatorNeededException.class);
        verify(paymentService, never()).initiateMobileMoneyDeposit(anyString(), any());
    }

    @Test
    @DisplayName("L'operateur choisi par le payeur doit appartenir au pays choisi")
    void chosenOperatorMustBelongToTheCountry() {
        service.start(TOKEN, "COG", "051234567", "AIRTEL_COG");
        verify(paymentService).initiateMobileMoneyDeposit(eq("PI-1"), any());

        assertThatThrownBy(() -> service.start(TOKEN, "COG", "051234567", "AIRTEL_GAB"))
                .hasMessageContaining("pas disponible");
    }

    @Test
    @DisplayName("Un lien expire, deja paye ou annule ne lance aucun paiement")
    void closedLinksDoNotPay() {
        intent.setPaymentLinkExpiresAt(Instant.now().minusSeconds(1));
        assertThatThrownBy(() -> service.start(TOKEN, "GAB", "074123456", null))
                .isInstanceOf(PublicPaymentPageService.LinkClosedException.class).hasMessageContaining("expiré");

        intent.setPaymentLinkExpiresAt(Instant.now().plusSeconds(3600));
        intent.setStatus(PaymentIntentStatus.SUCCEEDED);
        assertThatThrownBy(() -> service.open(TOKEN))
                .isInstanceOf(PublicPaymentPageService.LinkClosedException.class).hasMessageContaining("déjà réglé");
        verify(paymentService, never()).initiateMobileMoneyDeposit(anyString(), any());
    }

    @Test
    @DisplayName("Le suivi d'un depot d'une autre intention repond introuvable")
    void statusIsScopedToTheLink() {
        PawapayDeposit other = new PawapayDeposit();
        other.setDepositId("dep-autre");
        other.setIntentNumber("PI-AUTRE");
        when(depositRepository.findByDepositId("dep-autre")).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> service.status(TOKEN, "dep-autre")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Le suivi relit l'operateur au plus toutes les 20 secondes")
    void statusRecheckIsThrottled() {
        PawapayDeposit deposit = new PawapayDeposit();
        deposit.setDepositId("dep-1");
        deposit.setIntentNumber("PI-1");
        deposit.setStatus("PROCESSING");
        deposit.setLastStatusCheckedAt(Instant.now().minusSeconds(5));
        when(depositRepository.findByDepositId("dep-1")).thenReturn(Optional.of(deposit));

        service.status(TOKEN, "dep-1");
        verify(supervision, never()).recheck(anyString());
        verify(depositService).get("dep-1");

        deposit.setLastStatusCheckedAt(Instant.now().minusSeconds(60));
        service.status(TOKEN, "dep-1");
        verify(supervision).recheck("dep-1");
    }

    @Test
    @DisplayName("Un paiement deja en cours sur le lien est retrouve a l'ouverture")
    void depositInProgressIsFound() {
        PawapayDeposit pending = new PawapayDeposit();
        pending.setDepositId("dep-en-cours");
        pending.setStatus("PROCESSING");
        when(depositRepository.findAllByIntentNumberOrderByCreatedAtDesc("PI-1")).thenReturn(List.of(pending));

        assertThat(service.open(TOKEN).depositInProgress()).isEqualTo("dep-en-cours");
    }
}
