package com.sni.bokaticowork.features.payment.mobilemoney;

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
}
