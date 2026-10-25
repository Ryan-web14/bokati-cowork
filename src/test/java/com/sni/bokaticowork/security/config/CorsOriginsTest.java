package com.sni.bokaticowork.security.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * La liste des origines se lit sans surprise, et la production refuse l'etoile.
 */
class CorsOriginsTest {

    @Test
    @DisplayName("La liste se decoupe aux virgules, sans espaces ni entrees vides")
    void parsesCommaSeparatedList() {
        assertThat(CorsOrigins.parse(" https://elleaose.com, https://*.elleaose.com ,,"))
                .containsExactly("https://elleaose.com", "https://*.elleaose.com");
    }

    @Test
    @DisplayName("Une valeur absente ou vide n'ouvre aucune origine")
    void emptyValueOpensNothing() {
        assertThat(CorsOrigins.parse(null)).isEmpty();
        assertThat(CorsOrigins.parse("  ")).isEmpty();
    }

    @Test
    @DisplayName("L'etoile seule est reconnue, un motif a etoile ne l'est pas")
    void detectsWildcardOnlyWhenAlone() {
        assertThat(CorsOrigins.allowsAny(CorsOrigins.parse("*"))).isTrue();
        assertThat(CorsOrigins.allowsAny(CorsOrigins.parse("https://*.elleaose.com"))).isFalse();
    }

    @Test
    @DisplayName("Le garde-fou signale l'etoile et la liste vide, et laisse passer une liste fermee")
    void guardRejectsOpenOrEmptyOrigins() {
        assertThat(corsFaults("*")).hasSize(1).first().asString().contains("vaut *");
        assertThat(corsFaults("")).hasSize(1).first().asString().contains("est vide");
        assertThat(corsFaults("https://elleaose.com,https://*.elleaose.com")).isEmpty();
    }

    private java.util.List<String> corsFaults(String origins) {
        SecurityConfigurationGuard guard = new SecurityConfigurationGuard(new MockEnvironment());
        ReflectionTestUtils.setField(guard, "jwtSecret", "x".repeat(64));
        ReflectionTestUtils.setField(guard, "accessTokenExpirationMs", 900_000L);
        ReflectionTestUtils.setField(guard, "fiscalSigningKey", "cle-fiscale");
        ReflectionTestUtils.setField(guard, "corsAllowedOrigins", origins);
        return guard.faults().stream().filter(fault -> fault.contains("cors")).toList();
    }
}
