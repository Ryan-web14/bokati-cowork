package com.sni.bokaticowork.features.payment.service.support;

import com.sni.bokaticowork.features.payment.enums.PaymentChannel;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * D'ou vient l'encaissement qu'on est en train d'ecrire.
 *
 * <p>On ne le demande pas a l'appelant · un parametre de plus sur chaque methode de paiement
 * serait oublie quelque part, et c'est precisement la ou il serait oublie qu'on aurait voulu
 * prevenir quelqu'un. La reponse se lit dans la session en cours : un compte du realm
 * d'administration paie au nom de la maison, tout autre compte authentifie est un client qui paie
 * pour lui-meme, et l'absence de session est une automatisation.</p>
 */
@Component
public class PaymentOriginResolver {

    /**
     * Les roles de la maison · leur presence signifie qu'un agent opere.
     *
     * <p>Alignee sur {@code AdminApiAuthorizationManager} · un role ajoute la-bas sans l'etre ici
     * ferait passer un agent pour un client, donc enverrait une notification de trop. Le sens de
     * l'erreur est le bon : on previent a tort plutot que de se taire a tort.</p>
     */
    private static final Set<String> BACK_OFFICE_ROLES = Set.of(
            "ROLE_SUPER_ADMIN",
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

    public PaymentChannel current() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getAuthorities() == null
                || auth instanceof AnonymousAuthenticationToken) {
            // Aucune session · worker de relecture, rappel de l'operateur, reprise de l'outbox.
            // Une route publique porte une authentification anonyme : ce n'est pas un client.
            return PaymentChannel.SYSTEM;
        }
        for (GrantedAuthority authority : auth.getAuthorities()) {
            if (authority.getAuthority() != null && BACK_OFFICE_ROLES.contains(authority.getAuthority())) {
                return PaymentChannel.BACK_OFFICE;
            }
        }
        return PaymentChannel.SELF_SERVICE;
    }

    /** Qui opere, pour le journal · jamais affiche a un client. */
    public String currentActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return auth.getName();
    }
}
