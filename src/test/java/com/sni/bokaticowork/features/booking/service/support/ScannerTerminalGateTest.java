package com.sni.bokaticowork.features.booking.service.support;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.booking.config.BookingCheckInProperties;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Le cookie du poste de pointage · ce qu'il porte, ou il part, et combien de temps.
 */
class ScannerTerminalGateTest {

    private static final String KEY = "cle-de-poste-partagee-0123456789";

    private BookingCheckInProperties properties;
    private ScannerGrantCodec codec;
    private ScannerTerminalGate gate;

    private void configure(String key, boolean secure) {
        properties = new BookingCheckInProperties();
        properties.setScannerKey(key);
        properties.setScannerCookieSecure(secure);
        codec = new ScannerGrantCodec(properties);
        gate = new ScannerTerminalGate(properties, codec);
    }

    private String grantCookieHeader(MockHttpServletResponse response) {
        return response.getHeaders(HttpHeaders.SET_COOKIE).stream()
                .filter(header -> header.startsWith(ScannerTerminalGate.COOKIE_NAME + "="))
                .findFirst()
                .orElseThrow(() -> new AssertionError("aucun cookie d'autorisation pose"));
    }

    @Test
    @DisplayName("Le cookie pose est HttpOnly, Secure, SameSite=Strict, et limite aux routes de pointage")
    void grantCookieCarriesEveryAttributeThatWasMissing() {
        configure(KEY, true);
        MockHttpServletResponse response = new MockHttpServletResponse();

        gate.authorizeTerminal(response);
        String header = grantCookieHeader(response);

        assertThat(header).contains("HttpOnly");
        // Absent avant · le cookie partait en clair si le domaine etait joignable en HTTP.
        assertThat(header).contains("Secure");
        // Absent avant · c'est ce seul attribut qui lui enleve son autorite ambiante.
        assertThat(header).contains("SameSite=Strict");
        // C'etait path=/ · il accompagnait toutes les requetes du domaine, API comprise.
        assertThat(header).contains("Path=" + ApiPath.V1 + "/public/bookings/check-in");
        assertThat(header).doesNotContain("Path=/;");
    }

    @Test
    @DisplayName("Le cookie ne contient pas la cle")
    void grantCookieDoesNotCarryTheKey() {
        configure(KEY, true);
        MockHttpServletResponse response = new MockHttpServletResponse();

        gate.authorizeTerminal(response);

        assertThat(grantCookieHeader(response)).doesNotContain(KEY);
    }

    @Test
    @DisplayName("Le cookie expire · il durait un an")
    void grantCookieExpires() {
        configure(KEY, true);
        properties.setScannerGrantValidityDays(30);
        MockHttpServletResponse response = new MockHttpServletResponse();

        gate.authorizeTerminal(response);

        assertThat(grantCookieHeader(response)).contains("Max-Age=" + (30L * 24 * 3600));
    }

    @Test
    @DisplayName("L'ancien cookie, qui portait la cle, est efface a chaque autorisation")
    void legacyCookieIsCleared() {
        configure(KEY, true);
        MockHttpServletResponse response = new MockHttpServletResponse();

        gate.authorizeTerminal(response);

        List<String> headers = response.getHeaders(HttpHeaders.SET_COOKIE);
        assertThat(headers).anySatisfy(header -> {
            assertThat(header).startsWith(ScannerTerminalGate.LEGACY_COOKIE_NAME + "=;");
            assertThat(header).contains("Max-Age=0");
            // Le path d'origine, sinon le navigateur en efface un autre et garde celui-la.
            assertThat(header).contains("Path=/");
        });
    }

    @Test
    @DisplayName("Un poste autorise est reconnu")
    void authorizedTerminalIsRecognised() {
        configure(KEY, true);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(ScannerTerminalGate.COOKIE_NAME, codec.issue(Instant.now())));

        assertThat(gate.isAuthorizedTerminal(request)).isTrue();
    }

    @Test
    @DisplayName("Un cookie de l'ancienne version n'autorise plus rien")
    void legacyCookieNoLongerAuthorizes() {
        configure(KEY, true);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(ScannerTerminalGate.LEGACY_COOKIE_NAME, KEY));

        assertThat(gate.isAuthorizedTerminal(request)).isFalse();
    }

    @Test
    @DisplayName("La cle posee sous le nouveau nom n'autorise pas davantage")
    void rawKeyUnderTheNewNameDoesNotAuthorize() {
        configure(KEY, true);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(ScannerTerminalGate.COOKIE_NAME, KEY));

        assertThat(gate.isAuthorizedTerminal(request)).isFalse();
    }

    @Test
    @DisplayName("Sans cookie, sans requete, ou sans cle configuree, le poste n'est pas autorise")
    void absenceIsNeverAnAuthorization() {
        configure(KEY, true);
        assertThat(gate.isAuthorizedTerminal(new MockHttpServletRequest())).isFalse();
        assertThat(gate.isAuthorizedTerminal(null)).isFalse();

        String grant = codec.issue(Instant.now());
        configure("", true);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(ScannerTerminalGate.COOKIE_NAME, grant));
        assertThat(gate.isAuthorizedTerminal(request)).isFalse();
    }

    @Test
    @DisplayName("Sans cle configuree, aucun cookie d'autorisation n'est pose")
    void noKeyMeansNoGrantCookie() {
        configure("", true);
        MockHttpServletResponse response = new MockHttpServletResponse();

        gate.authorizeTerminal(response);

        assertThat(response.getHeaders(HttpHeaders.SET_COOKIE))
                .noneMatch(header -> header.startsWith(ScannerTerminalGate.COOKIE_NAME + "="));
    }

    @Test
    @DisplayName("En local, le cookie peut etre servi sans Secure · uniquement par configuration")
    void secureAttributeFollowsConfiguration() {
        configure(KEY, false);
        MockHttpServletResponse response = new MockHttpServletResponse();

        gate.authorizeTerminal(response);

        assertThat(grantCookieHeader(response)).doesNotContain("Secure");
    }

    @Test
    @DisplayName("Le defaut est Secure · il faut une decision explicite pour l'enlever")
    void secureIsTheDefault() {
        assertThat(new BookingCheckInProperties().isScannerCookieSecure()).isTrue();
    }
}
