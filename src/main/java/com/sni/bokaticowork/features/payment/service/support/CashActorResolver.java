package com.sni.bokaticowork.features.payment.service.support;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Qui agit sur une caisse.
 *
 * <p>L'acteur vient du jeton, jamais du corps de la requete : avant, {@code openedBy},
 * {@code closedBy}, {@code reviewedBy} ou {@code createdBy} etaient saisis par l'appelant, et
 * n'importe quel utilisateur habilite pouvait agir au nom d'un autre. La valeur envoyee n'est plus
 * lue que hors contexte authentifie (traitements internes, workers), ou le serveur est seul a
 * agir.</p>
 *
 * <p>L'identifiant retenu est le nom du principal (l'adresse de courriel), le meme que
 * {@code PaymentOriginResolver#currentActor} : le notificateur de caisse sait retrouver
 * l'utilisateur a partir de lui.</p>
 */
@Component
public class CashActorResolver {

    /** L'utilisateur connecte, ou {@code null} hors contexte authentifie. */
    public String current() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken
                || !StringUtils.hasText(auth.getName())) {
            return null;
        }
        return auth.getName().trim();
    }

    /**
     * L'utilisateur connecte ; a defaut (appel interne), la valeur fournie.
     *
     * @throws BadRequestException si ni l'un ni l'autre n'est connu
     */
    public String resolve(String fallback) {
        String current = current();
        if (current != null) {
            return current;
        }
        if (StringUtils.hasText(fallback)) {
            return fallback.trim();
        }
        throw new BadRequestException("Impossible d'identifier l'auteur de l'operation de caisse");
    }

    /** L'utilisateur connecte a-t-il cette autorite (permission {@code MODULE:ACTION} ou role) ? */
    public boolean hasAuthority(String authority) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || authority == null) {
            return false;
        }
        String underscored = authority.replace(':', '_');
        for (GrantedAuthority granted : auth.getAuthorities()) {
            String value = granted.getAuthority();
            if (authority.equals(value) || underscored.equals(value)) {
                return true;
            }
        }
        return false;
    }

    /** Deux acteurs sont-ils la meme personne (comparaison sans casse ni espaces) ? */
    public static boolean samePerson(String first, String second) {
        return StringUtils.hasText(first) && StringUtils.hasText(second)
                && first.trim().equalsIgnoreCase(second.trim());
    }
}
