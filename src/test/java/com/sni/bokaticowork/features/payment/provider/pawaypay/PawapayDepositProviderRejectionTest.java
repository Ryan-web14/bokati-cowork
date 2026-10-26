package com.sni.bokaticowork.features.payment.provider.pawaypay;

import com.sni.bokaticowork.features.payment.provider.MobileMoneyInitiationRequest;
import com.sni.bokaticowork.features.payment.provider.MobileMoneyInitiationResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Un refus de PawaPay (4xx) est un echec explique, pas une issue inconnue.
 *
 * <p>Une cle API invalide ou une signature refusee etaient rangees en UNKNOWN · le depot partait
 * en relances pendant 24 heures, et personne ne savait pourquoi rien ne se passait.</p>
 */
@ExtendWith(MockitoExtension.class)
class PawapayDepositProviderRejectionTest {

    @Mock private PawapayClient client;
    @InjectMocks private PawapayDepositProvider provider;

    private static MobileMoneyInitiationRequest request() {
        return new MobileMoneyInitiationRequest("dep-1", "PI-1", "TX-1", "MBR-1", "242061234567",
                BigDecimal.TEN, "XAF", null, "MTN_MOMO_COG", "PI-1", "Bokati", List.of());
    }

    @Test
    @DisplayName("Une signature ou une authentification refusee (401) est un echec explique")
    void unauthorizedIsAnExplainedFailure() {
        when(client.initiateDeposit(any())).thenThrow(HttpClientErrorException.create(HttpStatus.UNAUTHORIZED,
                "Unauthorized", HttpHeaders.EMPTY, "{\"failureMessage\":\"Invalid signature\"}".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8));

        MobileMoneyInitiationResponse response = provider.initiate(request());

        assertThat(response.status()).isEqualTo("FAILED");
        assertThat(response.message()).contains("signature");
        assertThat(response.failureReason().get("failureCode").asText()).isEqualTo("HTTP_401");
        assertThat(response.failureReason().get("failureMessage").asText()).contains("Invalid signature");
    }

    @Test
    @DisplayName("Une demande mal formee (400) est un echec, pas une attente")
    void badRequestIsAFailure() {
        when(client.initiateDeposit(any())).thenThrow(HttpClientErrorException.create(HttpStatus.BAD_REQUEST,
                "Bad Request", HttpHeaders.EMPTY, new byte[0], StandardCharsets.UTF_8));

        assertThat(provider.initiate(request()).status()).isEqualTo("FAILED");
    }

    @Test
    @DisplayName("Un operateur injoignable reste une issue inconnue · la demande est peut-etre partie")
    void unreachableStaysUnknown() {
        when(client.initiateDeposit(any())).thenThrow(new PawapayClient.ProviderUnreachableException("timeout",
                new ResourceAccessException("timeout")));

        assertThat(provider.initiate(request()).status()).isEqualTo("UNKNOWN");
    }
}
