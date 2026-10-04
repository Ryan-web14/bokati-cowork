package com.sni.bokaticowork.core.configuration;

import com.sni.bokaticowork.security.authorization.SecurityRoles;
import org.springframework.security.core.Authentication;

import java.util.List;

/**
 * Qui a le droit d'ecouter quoi.
 *
 * <p>L'abonnement n'exigeait qu'une seule chose · etre authentifie. Aucun controle de role, aucun
 * controle par destination. N'importe quel membre du portail client s'abonnait donc a
 * {@code /topic/admin/alerts} avec son propre jeton et recevait en direct le flux de paiement
 * nominatif de toute la maison · « untel a regle tant depuis son espace », les alertes de caisse,
 * les evenements de securite des portefeuilles.</p>
 *
 * <p>Les files {@code /user/**} sont, elles, isolees par construction : Spring les resout par
 * principal, un abonne ne recoit que ce qui lui est adresse. Ce sont les diffusions
 * {@code /topic/**} qui demandent une regle, parce qu'elles vont a tout le monde.</p>
 */
public final class WebSocketDestinations {

    /** Les diffusions internes · elles portent des donnees de clients et de caisse. */
    private static final List<String> ADMIN_ONLY_PREFIXES = List.of(
            "/topic/admin/",
            "/topic/inventory/",
            "/topic/support/"
    );

    private WebSocketDestinations() {
    }

    /** La destination est-elle reservee au personnel ? */
    public static boolean isAdminOnly(String destination) {
        if (destination == null) {
            return false;
        }
        return ADMIN_ONLY_PREFIXES.stream().anyMatch(destination::startsWith);
    }

    /**
     * L'abonnement est-il permis ?
     *
     * <p>Tout abonnement demande une identite · c'etait deja le cas pour {@code /topic/}, ce ne
     * l'etait pas pour {@code /queue/}. Les destinations internes demandent en plus un role de la
     * maison.</p>
     */
    public static boolean permits(Authentication user, String destination) {
        if (user == null || !user.isAuthenticated()) {
            return false;
        }
        if (isAdminOnly(destination)) {
            return SecurityRoles.hasAdminRealmRole(user);
        }
        return true;
    }
}
