package com.sni.bokaticowork.features.payment.service.support;

import com.sni.bokaticowork.features.payment.enums.PaymentChannel;
import com.sni.bokaticowork.security.authorization.SecurityRoles;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;


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
     * Les roles de la maison · leur presence signifie qu un agent opere.
     *
     * <p>La liste vit dans {@link SecurityRoles} · elle etait recopiee ici, et une copie
     * oubliee aurait fait passer un agent pour un client, donc envoye une notification de
     * trop. Le sens de l erreur etait le bon, mais mieux vaut ne pas avoir a y compter.</p>
     */

    public PaymentChannel current() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getAuthorities() == null
                || auth instanceof AnonymousAuthenticationToken) {
            // Aucune session · worker de relecture, rappel de l'operateur, reprise de l'outbox.
            // Une route publique porte une authentification anonyme : ce n'est pas un client.
            return PaymentChannel.SYSTEM;
        }
        return SecurityRoles.hasAdminRealmRole(auth.getAuthorities())
                ? PaymentChannel.BACK_OFFICE
                : PaymentChannel.SELF_SERVICE;
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
