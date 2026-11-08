package com.sni.bokaticowork.security.config;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

import java.util.List;

/**
 * La seule liste des chemins joignables sans jeton.
 *
 * <p>Il y en avait deux, et elles disaient la meme chose : les {@code permitAll} de
 * {@code SecurityConfig} decidaient de l'autorisation, et {@code JWTFilter.isPublicApiRequest}
 * decidait du traitement du jeton. Elles divergeaient deja · trois chemins ouverts par la premiere
 * manquaient a la seconde, et ces chemins ne fonctionnaient que grace au repli sur
 * l'authentification automatique en administrateur. En supprimant ce repli, il fallait d'abord
 * reunir les deux listes, sinon ces routes se fermaient sans qu'on l'ait voulu.</p>
 *
 * <p>Une liste unique, et un test qui compare ce que la configuration ouvre a ce que le filtre
 * laisse passer · la prochaine ouverture ne pourra plus etre oubliee d'un cote.</p>
 */
public final class PublicPaths {

    private static final PathMatcher MATCHER = new AntPathMatcher();

    /** Ouverture et fin de session · le point d'entree, par nature non authentifie. */
    public static final List<String> AUTHENTICATION = List.of(
            ApiPath.V1 + "/auth/login",
            ApiPath.V1 + "/auth/refresh",
            ApiPath.V1 + "/auth/register",
            ApiPath.V1 + "/auth/ott/**",
            ApiPath.V1 + "/auth/password-reset/**",
            ApiPath.V1 + "/auth/unlock-account",
            ApiPath.V1 + "/auth/unlock-account/confirm",
            ApiPath.V1 + "/auth/email/verify/resend"
    );

    /**
     * Rappels de l'operateur de paiement · leur securite est dans la passerelle, pas ici.
     *
     * <p>Signature verifiee, ou statut relu chez l'operateur avant toute ecriture · voir
     * {@code docs/backend/mobile-money.md}.</p>
     */
    public static final List<String> PAYMENT_CALLBACKS = List.of(
            ApiPath.V1 + "/payments/mobile-money/pawapay/callback",
            ApiPath.V1 + "/payments/mobile-money/pawaypay/callback",
            ApiPath.V1 + "/payments/mobile-money/pawapay/refund-callback",
            ApiPath.V1 + "/payments/mobile-money/pawaypay/refund-callback",
            ApiPath.V1 + "/payments/mobile-money/pawapay/return",
            ApiPath.V1 + "/payments/mobile-money/pawaypay/return",
            ApiPath.V1 + "/payments/mobile-money/providers"
    );

    /**
     * Pages et ressources destinees a des tiers sans compte.
     *
     * <p>Chacune porte son propre controle · un jeton de partage, un jeton de reservation, une
     * signature fiscale. L'absence d'authentification n'y est pas l'absence de controle.</p>
     */
    public static final List<String> TOKEN_GATED = List.of(
            "/verify/**",
            ApiPath.V1 + "/shares/**",
            ApiPath.V1 + "/public/**",
            "/public/**"
    );

    /** Catalogue et referentiels · consultables par construction. */
    public static final List<String> CATALOGUE = List.of(
            ApiPath.V1 + "/client/catalog/plans",
            ApiPath.V1 + "/client/catalog/plans/**"
    );

    /**
     * Referentiels ouverts en lecture seulement · le GET est public, les autres verbes non.
     *
     * <p>La liste des pays sert a l'inscription, donc sans compte. Elle figurait dans
     * {@link #CATALOGUE}, ouverte a tous les verbes : creer, modifier ou supprimer un pays se
     * faisait sans jeton. L'ecriture repasse par {@code AdminApiAuthorizationManager}.</p>
     */
    public static final List<String> PUBLIC_READS = List.of(
            ApiPath.V1 + "/countries"
    );

    /** Infrastructure · sonde de vie et ressources de marque des courriels. */
    public static final List<String> INFRASTRUCTURE = List.of(
            "/actuator/health",
            "/actuator/info",
            "/images/**",
            "/ws/**"
    );

    /**
     * L'amorcage du premier administrateur.
     *
     * <p>Reste joignable sans compte · par definition, il n'y a encore personne. Sa fermeture est
     * dans le service : un secret d'amorcage exige, et refus des qu'un administrateur existe.</p>
     */
    public static final List<String> BOOTSTRAP = List.of(
            ApiPath.V1 + "/admin/provisioning/bootstrap-admin"
    );

    /** Tout, dans l'ordre de lecture · c'est cette liste que les deux cotes consomment. */
    public static final List<String> ALL = java.util.stream.Stream.of(
                    AUTHENTICATION, PAYMENT_CALLBACKS, TOKEN_GATED, CATALOGUE, INFRASTRUCTURE, BOOTSTRAP)
            .flatMap(List::stream)
            .toList();

    private PublicPaths() {
    }

    /** Le chemin est-il public pour tous les verbes · ne tient pas compte de {@link #PUBLIC_READS}. */
    public static boolean matches(String uri) {
        return matchesAny(ALL, uri);
    }

    /** Le chemin est-il public pour ce verbe · meme reponse pour la configuration et pour le filtre. */
    public static boolean matches(String method, String uri) {
        if (matches(uri)) {
            return true;
        }
        return "GET".equalsIgnoreCase(method) && matchesAny(PUBLIC_READS, uri);
    }

    public static boolean matches(HttpServletRequest request) {
        return request != null && matches(request.getMethod(), request.getRequestURI());
    }

    private static boolean matchesAny(List<String> patterns, String uri) {
        if (uri == null) {
            return false;
        }
        for (String pattern : patterns) {
            if (MATCHER.match(pattern, uri)) {
                return true;
            }
        }
        return false;
    }

    /** Les motifs ouverts a tous les verbes, pour les passer tels quels a la configuration de securite. */
    public static String[] patterns() {
        return ALL.toArray(String[]::new);
    }

    /** Les motifs ouverts au seul GET, pour la configuration de securite. */
    public static String[] readPatterns() {
        return PUBLIC_READS.toArray(String[]::new);
    }
}
