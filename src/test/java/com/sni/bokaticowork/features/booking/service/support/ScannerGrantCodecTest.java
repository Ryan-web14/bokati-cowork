package com.sni.bokaticowork.features.booking.service.support;

import com.sni.bokaticowork.features.booking.config.BookingCheckInProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * L'autorisation d'un poste · une preuve, pas un secret.
 */
class ScannerGrantCodecTest {

    private static final String KEY = "cle-de-poste-partagee-0123456789";
    private static final Instant NOW = Instant.parse("2026-10-04T10:00:00Z");

    private BookingCheckInProperties propertiesWith(String key, int validityDays) {
        BookingCheckInProperties properties = new BookingCheckInProperties();
        properties.setScannerKey(key);
        properties.setScannerGrantValidityDays(validityDays);
        return properties;
    }

    private ScannerGrantCodec codecWith(String key) {
        return new ScannerGrantCodec(propertiesWith(key, 30));
    }

    @Test
    @DisplayName("Une autorisation emise se verifie")
    void issuedGrantVerifies() {
        ScannerGrantCodec codec = codecWith(KEY);

        assertThat(codec.isValid(codec.issue(NOW), NOW)).isTrue();
    }

    @Test
    @DisplayName("L'autorisation ne contient pas la cle · c'est tout l'objet du correctif")
    void grantDoesNotCarryTheKey() {
        // Le cookie contenait la cle d'administration en clair, un an, sur tout le domaine.
        // Lire le cookie d'un poste ne doit plus permettre d'en activer un autre.
        String grant = codecWith(KEY).issue(NOW);

        assertThat(grant).doesNotContain(KEY);
    }

    @Test
    @DisplayName("La cle elle-meme ne vaut pas autorisation")
    void theRawKeyIsNotAGrant() {
        // Un cookie pose par l'ancienne version contient la cle · il ne doit plus autoriser.
        assertThat(codecWith(KEY).isValid(KEY, NOW)).isFalse();
    }

    @Test
    @DisplayName("Une autorisation expiree est refusee")
    void expiredGrantIsRefused() {
        ScannerGrantCodec codec = new ScannerGrantCodec(propertiesWith(KEY, 30));
        String grant = codec.issue(NOW);

        assertThat(codec.isValid(grant, NOW.plus(Duration.ofDays(29)))).isTrue();
        assertThat(codec.isValid(grant, NOW.plus(Duration.ofDays(31)))).isFalse();
    }

    @Test
    @DisplayName("Changer la cle revoque toutes les autorisations d'un coup")
    void rotatingTheKeyRevokesEveryGrant() {
        String grant = codecWith(KEY).issue(NOW);

        assertThat(codecWith("une-toute-autre-cle-9876543210").isValid(grant, NOW)).isFalse();
    }

    @Test
    @DisplayName("Repousser la date d'expiration invalide la signature")
    void extendingTheExpiryBreaksTheSignature() {
        ScannerGrantCodec codec = codecWith(KEY);
        String grant = codec.issue(NOW);
        String[] parts = grant.split("\\.");
        String forged = parts[0] + "." + (Long.parseLong(parts[1]) + 86_400_000L) + "." + parts[2];

        assertThat(codec.isValid(forged, NOW)).isFalse();
    }

    @Test
    @DisplayName("Une signature retouchee est refusee")
    void tamperedSignatureIsRefused() {
        ScannerGrantCodec codec = codecWith(KEY);
        String grant = codec.issue(NOW);

        assertThat(codec.isValid(grant.substring(0, grant.length() - 1) + "Z", NOW)).isFalse();
    }

    @ParameterizedTest
    @DisplayName("Les valeurs malformees sont refusees sans erreur")
    @ValueSource(strings = {
            "   ",
            "n-importe-quoi",
            "s1.1790000000",
            "s1.1790000000.sig.supplement",
            "s0.1790000000.c2ln",
            "s1.pas-une-date.c2ln"
    })
    void malformedValuesAreRefused(String value) {
        assertThat(codecWith(KEY).isValid(value, NOW)).isFalse();
    }

    @ParameterizedTest
    @DisplayName("Une valeur absente est refusee")
    @NullAndEmptySource
    void absentValueIsRefused(String value) {
        assertThat(codecWith(KEY).isValid(value, NOW)).isFalse();
    }

    @Test
    @DisplayName("Sans cle configuree, rien n'est emis et rien n'est accepte")
    void withoutAKeyNothingIsIssuedNorAccepted() {
        ScannerGrantCodec withKey = codecWith(KEY);
        String grant = withKey.issue(NOW);
        ScannerGrantCodec withoutKey = codecWith("");

        assertThat(withoutKey.issue(NOW)).isNull();
        assertThat(withoutKey.isValid(grant, NOW)).isFalse();
    }

    @Test
    @DisplayName("Une validite nulle ou negative retombe sur un jour, jamais sur zero")
    void validityNeverCollapsesToZero() {
        BookingCheckInProperties properties = propertiesWith(KEY, 0);
        ScannerGrantCodec codec = new ScannerGrantCodec(properties);

        assertThat(properties.grantValidity()).isEqualTo(Duration.ofDays(1));
        assertThat(codec.isValid(codec.issue(NOW), NOW)).isTrue();
    }
}
