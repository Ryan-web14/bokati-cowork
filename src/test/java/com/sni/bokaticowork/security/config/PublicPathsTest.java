package com.sni.bokaticowork.security.config;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Une seule liste de chemins publics, et elle dit la meme chose aux deux cotes.
 *
 * <p>Il y en avait deux · les {@code permitAll} de la configuration et la liste du filtre JWT.
 * Elles divergeaient deja : trois chemins ouverts par la premiere manquaient a la seconde, et ne
 * fonctionnaient que par le repli sur l'authentification automatique en administrateur. En
 * supprimant ce repli, il fallait d'abord reunir les deux listes.</p>
 */
class PublicPathsTest {

    @ParameterizedTest
    @DisplayName("Les chemins d'ouverture de session sont publics")
    @ValueSource(strings = {
            ApiPath.V1 + "/auth/login",
            ApiPath.V1 + "/auth/refresh",
            ApiPath.V1 + "/auth/register",
            ApiPath.V1 + "/auth/ott/request",
            ApiPath.V1 + "/auth/password-reset/form",
            ApiPath.V1 + "/auth/unlock-account",
            ApiPath.V1 + "/auth/email/verify/resend"
    })
    void authenticationPathsArePublic(String uri) {
        assertThat(PublicPaths.matches(uri)).isTrue();
    }

    @ParameterizedTest
    @DisplayName("Les rappels de l'opérateur et les pages à jeton sont publics")
    @ValueSource(strings = {
            ApiPath.V1 + "/payments/mobile-money/pawapay/callback",
            ApiPath.V1 + "/payments/mobile-money/pawaypay/refund-callback",
            ApiPath.V1 + "/payments/mobile-money/pawapay/return",
            ApiPath.V1 + "/payments/mobile-money/providers",
            "/verify/doc/INV-1",
            "/verify/receipt/REC-1",
            ApiPath.V1 + "/shares/abcdef",
            ApiPath.V1 + "/public/crm/leads",
            "/public/quotes/sign"
    })
    void tokenGatedPathsArePublic(String uri) {
        assertThat(PublicPaths.matches(uri)).isTrue();
    }

    @ParameterizedTest
    @DisplayName("Les trois chemins qui manquaient au filtre sont désormais couverts")
    @ValueSource(strings = {
            ApiPath.V1 + "/admin/provisioning/bootstrap-admin",
            ApiPath.V1 + "/public/events/EVT-1",
            ApiPath.V1 + "/countries"
    })
    void thePathsMissingFromTheFilterAreNowCovered(String uri) {
        assertThat(PublicPaths.matches(uri)).isTrue();
    }

    @ParameterizedTest
    @DisplayName("Rien d'autre n'est public · surtout pas l'administration")
    @ValueSource(strings = {
            ApiPath.V1 + "/admin/provisioning/staff",
            ApiPath.V1 + "/admin/users",
            ApiPath.V1 + "/admin/settings",
            ApiPath.V1 + "/billing/documents",
            ApiPath.V1 + "/payments/cash-declarations",
            ApiPath.V1 + "/payments/mobile-money/deposits/dep-1",
            ApiPath.V1 + "/client/billing/invoices",
            ApiPath.V1 + "/documents/DOC-1/download",
            ApiPath.V1 + "/auth/me",
            "/actuator/metrics",
            "/actuator/env"
    })
    void nothingElseIsPublic(String uri) {
        assertThat(PublicPaths.matches(uri)).isFalse();
    }

    @Test
    @DisplayName("Un chemin absent ne passe pas, et un chemin nul non plus")
    void unknownAndNullAreRefused() {
        assertThat(PublicPaths.matches((String) null)).isFalse();
        assertThat(PublicPaths.matches("")).isFalse();
        assertThat(PublicPaths.matches("/n/importe/quoi")).isFalse();
    }

    @Test
    @DisplayName("Les motifs passés à la configuration sont exactement la liste")
    void thePatternsHandedToTheConfigurationAreTheList() {
        assertThat(PublicPaths.patterns()).hasSameSizeAs(PublicPaths.ALL);
        assertThat(PublicPaths.patterns()).containsExactlyElementsOf(PublicPaths.ALL);
    }

    @Test
    @DisplayName("Aucun motif n'ouvre l'administration par mégarde")
    void noPatternOpensTheAdministration() {
        // Un « /** » mal place sur /admin ou /client ouvrirait tout · le test le verrait.
        assertThat(PublicPaths.ALL)
                .noneMatch(pattern -> pattern.equals(ApiPath.V1 + "/**"))
                .noneMatch(pattern -> pattern.equals("/**"))
                .noneMatch(pattern -> pattern.startsWith(ApiPath.V1 + "/admin/**"));
    }
}
