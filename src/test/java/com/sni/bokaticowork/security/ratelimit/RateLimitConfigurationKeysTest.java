package com.sni.bokaticowork.security.ratelimit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Les plafonds sont declares dans les deux profils, et avec les memes noms.
 *
 * <p>Chaque plafond est lu par un {@code @Value} avec une valeur de repli. Un nom mal indente ou
 * oublie dans un profil ne casse donc rien au demarrage · le repli prend la main, et le plafond
 * reel n'est plus celui que la configuration annonce. C'est le genre d'ecart qui ne se voit qu'en
 * production, d'ou ce test : il compare les deux fichiers entre eux et aux noms attendus.</p>
 */
class RateLimitConfigurationKeysTest {

    /** Les plafonds nommes · un oubli ici est un oubli dans {@link RateLimitRules}. */
    private static final Set<String> EXPECTED_BUCKETS =
            Set.of("login", "otp", "callback", "public-write", "verify", "upload", "admin", "default");

    @SuppressWarnings("unchecked")
    private Map<String, Object> rateLimitSection(String profile) {
        Path file = Path.of("src", "main", "resources", "application-" + profile + ".yml");
        assertThat(Files.exists(file)).as("le fichier de profil %s existe", profile).isTrue();
        try (InputStream in = Files.newInputStream(file)) {
            Map<String, Object> root = new Yaml().load(in);
            Map<String, Object> app = (Map<String, Object>) root.get("app");
            Map<String, Object> security = (Map<String, Object>) app.get("security");
            Map<String, Object> rateLimit = (Map<String, Object>) security.get("rate-limit");
            assertThat(rateLimit).as("la section app.security.rate-limit du profil %s", profile).isNotNull();
            return rateLimit;
        } catch (IOException ex) {
            throw new IllegalStateException("Lecture du profil " + profile + " impossible", ex);
        }
    }

    @ParameterizedTest
    @DisplayName("Chaque profil declare tous les plafonds nommes, avec leurs deux bornes")
    @ValueSource(strings = {"dev", "prod"})
    @SuppressWarnings("unchecked")
    void everyProfileDeclaresEveryBucket(String profile) {
        Map<String, Object> rateLimit = rateLimitSection(profile);

        assertThat(rateLimit).containsKeys("enabled", "trusted-proxy-count");
        assertThat(rateLimit.keySet()).containsAll(EXPECTED_BUCKETS);
        EXPECTED_BUCKETS.forEach(bucket -> {
            Map<String, Object> values = (Map<String, Object>) rateLimit.get(bucket);
            assertThat(values)
                    .as("le plafond %s du profil %s", bucket, profile)
                    .containsKeys("max-requests", "window-seconds");
        });
    }

    @Test
    @DisplayName("Les deux profils declarent exactement les memes noms")
    void bothProfilesDeclareTheSameNames() {
        assertThat(rateLimitSection("dev").keySet()).isEqualTo(rateLimitSection("prod").keySet());
    }

    @ParameterizedTest
    @DisplayName("Le nombre de relais de confiance est declare · c'est lui qui rend l'adresse fiable")
    @ValueSource(strings = {"dev", "prod"})
    void trustedProxyCountIsDeclared(String profile) {
        assertThat(String.valueOf(rateLimitSection(profile).get("trusted-proxy-count")))
                .contains("RATE_LIMIT_TRUSTED_PROXY_COUNT");
    }
}
