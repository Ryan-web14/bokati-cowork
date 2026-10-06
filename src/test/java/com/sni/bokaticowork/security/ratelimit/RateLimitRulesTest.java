package com.sni.bokaticowork.security.ratelimit;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Chaque route retombe sur un plafond, et sur le bon.
 *
 * <p>La regle precedente ne reconnaissait que {@code ApiPath.V1} et rendait {@code null} pour
 * tout le reste · ce {@code null} signifiait « passe sans compter ». Les tests ci-dessous
 * verifient d'abord que plus rien ne rend {@code null}, ensuite que les routes les plus couteuses
 * tombent sur le plafond serre et non sur le generique.</p>
 */
class RateLimitRulesTest {

    private RateLimitRules rules;

    @BeforeEach
    void setUp() {
        rules = new RateLimitRules();
        Map.of(
                "loginMax", 5,
                "otpMax", 3,
                "callbackMax", 120, "checkinMax", 120,
                "publicWriteMax", 10,
                "verifyMax", 60,
                "uploadMax", 5,
                "adminMax", 600,
                "defaultMax", 300
        ).forEach((field, value) -> ReflectionTestUtils.setField(rules, field, value));
        Map.of(
                "loginWindow", 60L,
                "otpWindow", 600L,
                "callbackWindow", 60L, "checkinWindow", 60L,
                "publicWriteWindow", 600L,
                "verifyWindow", 600L,
                "uploadWindow", 600L,
                "adminWindow", 60L,
                "defaultWindow", 60L
        ).forEach((field, value) -> ReflectionTestUtils.setField(rules, field, value));
    }

    @ParameterizedTest
    @DisplayName("Aucune route ne passe sans plafond")
    @ValueSource(strings = {
            "/",
            "/images/logo.png",
            "/verify/doc/42",
            "/verify/doc/42/compare",
            "/public/quotes/sign",
            "/ws/info",
            "/actuator/health",
            "/swagger-ui.html",
            "/n-importe-quoi/qui-n-existe-pas",
            ApiPath.V1 + "/auth/login",
            ApiPath.V1 + "/customers",
            ApiPath.V1 + "/payments/mobile-money/pawapay/callback"
    })
    void everyRouteHasACeiling(String uri) {
        RateLimitRules.Rule rule = rules.ruleFor("GET", uri, "203.0.113.9");

        assertThat(rule).isNotNull();
        assertThat(rule.maxRequests()).isPositive();
        assertThat(rule.windowSeconds()).isPositive();
        assertThat(rule.key()).contains("203.0.113.9");
    }

    @ParameterizedTest
    @DisplayName("Chaque route tombe sur le plafond attendu")
    @CsvSource({
            "POST, " + ApiPath.V1 + "/auth/login, login:, 5",
            "POST, " + ApiPath.V1 + "/auth/ott/request, otp:, 3",
            "POST, " + ApiPath.V1 + "/auth/password-reset/form, otp:, 3",
            "POST, " + ApiPath.V1 + "/auth/unlock-account, otp:, 3",
            "POST, " + ApiPath.V1 + "/auth/email/verify/resend, otp:, 3",
            "POST, " + ApiPath.V1 + "/payments/mobile-money/pawapay/callback, callback:, 120",
            "POST, /verify/doc/42/compare, upload:, 5",
            "GET, /verify/doc/42, verify:, 60",
            "POST, /public/quotes/sign, public-write:, 10",
            "POST, " + ApiPath.V1 + "/public/bookings/check-in/scan, checkin:, 120",
            "POST, " + ApiPath.V1 + "/public/bookings/check-in/self, checkin:, 120",
            "POST, " + ApiPath.V1 + "/public/bookings/check-in/scanner-verify, otp:, 3",
            "POST, " + ApiPath.V1 + "/public/bookings/check-in/scanner-setup, otp:, 3",
            "GET, " + ApiPath.V1 + "/customers, admin:, 600",
            "GET, /images/logo.png, default:, 300"
    })
    void routesMapToTheirBucket(String method, String uri, String keyPrefix, int maxRequests) {
        RateLimitRules.Rule rule = rules.ruleFor(method, uri, "203.0.113.9");

        assertThat(rule.key()).startsWith(keyPrefix);
        assertThat(rule.maxRequests()).isEqualTo(maxRequests);
    }

    @Test
    @DisplayName("Le depot de fichier de /verify est plus serre que la lecture de /verify")
    void uploadIsTighterThanRead() {
        RateLimitRules.Rule upload = rules.ruleFor("POST", "/verify/doc/42/compare", "203.0.113.9");
        RateLimitRules.Rule read = rules.ruleFor("GET", "/verify/doc/42", "203.0.113.9");

        assertThat(upload.maxRequests()).isLessThan(read.maxRequests());
    }

    @Test
    @DisplayName("Un accueil charge ne se bloque pas lui-meme · tous ses postes ont la meme adresse")
    void checkInIsNotCountedAsAnAnonymousWrite() {
        // Le pointage est sous /public et part en POST · il tombait donc dans public-write, a dix
        // par dix minutes, pour l'ensemble des postes du lieu reunis.
        RateLimitRules.Rule checkIn =
                rules.ruleFor("POST", ApiPath.V1 + "/public/bookings/check-in/scan", "203.0.113.9");
        RateLimitRules.Rule anonymousWrite =
                rules.ruleFor("POST", ApiPath.V1 + "/public/crm/leads", "203.0.113.9");

        assertThat(checkIn.key()).startsWith("checkin:");
        assertThat(checkIn.maxRequests()).isGreaterThan(anonymousWrite.maxRequests());
    }

    @Test
    @DisplayName("La cle d'un poste est plus serree que le pointage lui-meme")
    void theTerminalKeyIsTighterThanTheCheckInItself() {
        RateLimitRules.Rule key =
                rules.ruleFor("POST", ApiPath.V1 + "/public/bookings/check-in/scanner-verify", "203.0.113.9");
        RateLimitRules.Rule checkIn =
                rules.ruleFor("POST", ApiPath.V1 + "/public/bookings/check-in/scan", "203.0.113.9");

        assertThat(key.maxRequests()).isLessThan(checkIn.maxRequests());
    }

    @Test
    @DisplayName("Une lecture publique n'est pas comptee comme une ecriture publique")
    void publicReadIsNotAPublicWrite() {
        assertThat(rules.ruleFor("GET", ApiPath.V1 + "/public/catalogue", "203.0.113.9").key())
                .doesNotStartWith("public-write:");
    }

    @Test
    @DisplayName("La casse du chemin et du verbe ne change pas le plafond")
    void matchingIsCaseInsensitive() {
        assertThat(rules.ruleFor("post", ApiPath.V1.toUpperCase() + "/AUTH/LOGIN", "203.0.113.9").key())
                .startsWith("login:");
    }

    @Test
    @DisplayName("Un chemin ou un verbe absent retombe sur le filet, sans erreur")
    void nullInputsFallBackToDefault() {
        assertThat(rules.ruleFor(null, null, "203.0.113.9").key()).startsWith("default:");
    }

    @Test
    @DisplayName("Le code a usage unique est compte par chemin · un plafond ne consomme pas l'autre")
    void otpIsCountedPerPath() {
        String unlock = rules.ruleFor("POST", ApiPath.V1 + "/auth/unlock-account", "203.0.113.9").key();
        String resend = rules.ruleFor("POST", ApiPath.V1 + "/auth/email/verify/resend", "203.0.113.9").key();

        assertThat(unlock).isNotEqualTo(resend);
    }

    @Test
    @DisplayName("Deux adresses ne partagent jamais un compteur")
    void countersAreScopedToTheCaller() {
        assertThat(rules.ruleFor("POST", ApiPath.V1 + "/auth/login", "203.0.113.9").key())
                .isNotEqualTo(rules.ruleFor("POST", ApiPath.V1 + "/auth/login", "198.51.100.4").key());
    }
}
