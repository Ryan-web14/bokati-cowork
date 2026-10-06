package com.sni.bokaticowork.features.booking.service.support;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.booking.config.BookingCheckInProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;

/**
 * Le cookie qui autorise un poste de pointage · pose, lu, et borne.
 *
 * <p>Quatre choses manquaient au cookie precedent, en plus de son contenu (voir
 * {@link ScannerGrantCodec}) :</p>
 * <ul>
 *   <li>{@code path=/} · il accompagnait <b>toutes</b> les requetes du domaine, l'API comprise,
 *       alors qu'il ne sert qu'a trois routes de pointage ;</li>
 *   <li>pas de {@code Secure} · il partait en clair si le domaine etait jamais joignable en
 *       HTTP ;</li>
 *   <li>pas de {@code SameSite} · il accompagnait les requetes declenchees par un site tiers, et
 *       la protection CSRF est desactivee globalement. C'est ce seul attribut qui enleve au
 *       cookie son autorite ambiante ;</li>
 *   <li>un an de duree de vie · voir la duree de l'autorisation elle-meme.</li>
 * </ul>
 *
 * <p>L'ancien cookie est expire activement a chaque passage : il porte la cle en clair et
 * resterait un an dans les navigateurs des postes deja actives.</p>
 */
@Component
@RequiredArgsConstructor
public class ScannerTerminalGate {

    /** L'autorisation signee · remplace {@link #LEGACY_COOKIE_NAME}, qui portait la cle. */
    static final String COOKIE_NAME = "bokati_checkin_grant";
    /** L'ancien cookie · conserve uniquement pour etre efface. */
    static final String LEGACY_COOKIE_NAME = "bokati_scanner";
    /** Les seules routes qui en ont besoin · le cookie ne part nulle part ailleurs. */
    static final String COOKIE_PATH = ApiPath.V1 + "/public/bookings/check-in";

    private final BookingCheckInProperties properties;
    private final ScannerGrantCodec grantCodec;

    /** Ce poste est-il autorise a enregistrer des arrivees. */
    public boolean isAuthorizedTerminal(HttpServletRequest request) {
        if (request == null || !properties.isScannerKeyConfigured()) {
            return false;
        }
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return false;
        }
        Instant now = Instant.now();
        return Arrays.stream(cookies)
                .filter(cookie -> COOKIE_NAME.equals(cookie.getName()))
                .anyMatch(cookie -> grantCodec.isValid(cookie.getValue(), now));
    }

    /** Autorise ce poste, et efface au passage l'ancien cookie porteur de la cle. */
    public void authorizeTerminal(HttpServletResponse response) {
        clearLegacyCookie(response);
        String grant = grantCodec.issue(Instant.now());
        if (grant == null) {
            return;
        }
        response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from(COOKIE_NAME, grant)
                .httpOnly(true)
                .secure(properties.isScannerCookieSecure())
                // Strict · un formulaire heberge ailleurs n'emporte pas l'autorisation du poste.
                .sameSite("Strict")
                .path(COOKIE_PATH)
                .maxAge(properties.grantValidity())
                .build()
                .toString());
    }

    /**
     * Efface l'ancien cookie · il contient la cle d'administration en clair et vivrait un an.
     *
     * <p>Il faut reprendre son {@code path} d'origine, sans quoi le navigateur en efface un autre
     * et garde celui-la.</p>
     */
    private void clearLegacyCookie(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from(LEGACY_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(properties.isScannerCookieSecure())
                .sameSite("Strict")
                .path("/")
                .maxAge(Duration.ZERO)
                .build()
                .toString());
    }
}
