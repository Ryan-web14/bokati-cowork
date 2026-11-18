package com.sni.bokaticowork.core.utils.time;

import org.junit.jupiter.api.Test;

import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

/** Lot B8 (ecart Bn-26) : l'heure de l'application ne depend pas du fuseau du serveur. */
class AppTimeTest {

    @Test
    void theApplicationZoneIsBrazzavilleByDefaultAndToleratesTypos() {
        assertThat(AppTime.parse(null)).isEqualTo(ZoneId.of("Africa/Brazzaville"));
        assertThat(AppTime.parse("Africa/Lagos")).isEqualTo(ZoneId.of("Africa/Lagos"));
        assertThat(AppTime.parse("Afrique/Nulle-part")).isEqualTo(AppTime.DEFAULT_ZONE);
    }

    @Test
    void nowIsTakenInTheApplicationZone() {
        java.time.LocalDateTime expected = java.time.LocalDateTime.now(AppTime.zone());
        assertThat(java.time.Duration.between(expected, AppTime.now()).abs().toMinutes()).isZero();
    }
}
