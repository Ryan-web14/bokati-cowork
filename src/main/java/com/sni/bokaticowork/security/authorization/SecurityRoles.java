package com.sni.bokaticowork.security.authorization;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.Set;

/**
 * Les roles de la maison · la liste, une seule fois.
 *
 * <p>Elle existait en trois exemplaires · le gestionnaire d'autorisation de l'API
 * d'administration, le resolveur d'origine de paiement, et bientot l'autorisation WebSocket.
 * Trois copies d'une meme regle finissent par diverger, et c'est la copie oubliee qui devient la
 * faille : un role ajoute ici et pas la, et un agent passe pour un client, ou l'inverse.</p>
 */
public final class SecurityRoles {

    public static final String SUPER_ADMIN = "ROLE_SUPER_ADMIN";

    /**
     * Appartenir a l'un de ces roles, c'est operer au nom de la maison.
     *
     * <p>Un compte qui n'en porte aucun est un client · il accede a son espace, pas a
     * l'administration.</p>
     */
    public static final Set<String> ADMIN_REALM = Set.of(
            SUPER_ADMIN,
            "ROLE_ADMIN",
            "ROLE_MANAGER",
            "ROLE_FINANCE",
            "ROLE_CASHIER",
            "ROLE_STAFF",
            "ROLE_SUPPORT",
            "ROLE_KYC_REVIEWER",
            "ROLE_AUDITOR",
            "ROLE_VIEWER",
            "ROLE_OPERATIONS_AGENT"
    );

    private SecurityRoles() {
    }

    public static boolean hasAdminRealmRole(Collection<? extends GrantedAuthority> authorities) {
        if (authorities == null) {
            return false;
        }
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(ADMIN_REALM::contains);
    }

    public static boolean hasAdminRealmRole(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && hasAdminRealmRole(authentication.getAuthorities());
    }

    public static boolean has(Collection<? extends GrantedAuthority> authorities, String authority) {
        if (authorities == null) {
            return false;
        }
        return authorities.stream().anyMatch(granted -> authority.equals(granted.getAuthority()));
    }
}
