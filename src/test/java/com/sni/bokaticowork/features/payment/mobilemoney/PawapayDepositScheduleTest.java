package com.sni.bokaticowork.features.payment.mobilemoney;

import com.sni.bokaticowork.features.payment.provider.pawaypay.PawapayProperties;
import com.sni.bokaticowork.features.payment.provider.pawaypay.PawapaySignatureVerifier;
import com.sni.bokaticowork.features.payment.service.pawaypay.PawapayDepositService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Le calendrier de relecture, et le verrou qui manquait sur la signature.
 */
class PawapayDepositScheduleTest {

    @Test
    @DisplayName("On relit souvent au debut, rarement ensuite · un client absent ne se rappelle pas toutes les minutes")
    void backoffTightensThenLoosens() {
        long[] expectedMinutes = {1, 2, 3, 5, 10, 15, 30};
        for (int attempt = 1; attempt <= expectedMinutes.length; attempt++) {
            long seconds = ReflectionTestUtils.invokeMethod(PawapayDepositService.class, "backoffSeconds", attempt);
            assertThat(seconds).isEqualTo(expectedMinutes[attempt - 1] * 60);
        }
        // Au-dela, une fois par heure · l'operateur n'a plus rien a dire avant longtemps.
        for (int attempt = 8; attempt <= 40; attempt++) {
            long seconds = ReflectionTestUtils.invokeMethod(PawapayDepositService.class, "backoffSeconds", attempt);
            assertThat(seconds).isEqualTo(3600L);
        }
    }

    @Test
    @DisplayName("Sans secret configure, aucune signature n'est reputee valide")
    void signatureIsNeverTrustedWithoutASecret() {
        PawapayProperties properties = new PawapayProperties();
        properties.setCallbackSecret("");
        PawapaySignatureVerifier verifier = new PawapaySignatureVerifier(properties);

        assertThat(verifier.isEnabled()).isFalse();
        assertThat(verifier.verify("{\"depositId\":\"dep-1\"}", "sha256=nimporte-quoi")).isFalse();
        assertThat(verifier.verify("{\"depositId\":\"dep-1\"}", null)).isFalse();
    }

    @Test
    @DisplayName("Avec un secret, seule la bonne empreinte passe")
    void signatureIsCheckedAgainstTheSecret() {
        PawapayProperties properties = new PawapayProperties();
        properties.setCallbackSecret("s3cr3t");
        PawapaySignatureVerifier verifier = new PawapaySignatureVerifier(properties);

        String body = "{\"depositId\":\"dep-1\"}";
        String expected = ReflectionTestUtils.invokeMethod(verifier, "hmacSha256", "s3cr3t", body);

        assertThat(verifier.verify(body, expected)).isTrue();
        assertThat(verifier.verify(body, "sha256=" + expected)).isTrue();
        assertThat(verifier.verify(body, "sha256=00000000")).isFalse();
        assertThat(verifier.verify("{\"depositId\":\"dep-2\"}", expected)).isFalse();
    }
}
