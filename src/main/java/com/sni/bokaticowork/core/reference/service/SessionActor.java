package com.sni.bokaticowork.core.reference.service;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * « Qui a fait » lu dans la session (socle des listes de choix, lot L0, regle « acteur »).
 *
 * <p>Les champs {@code createdBy}, {@code changedBy}, {@code actor}... envoyes par l'ecran ne sont
 * plus crus : seul un traitement sans session (tache planifiee, rappel d'un prestataire) donne
 * lui-meme son nom. Meme regle que la caisse ({@code CashActorResolver}) et les reservations (lot B0).</p>
 */
@Component
public class SessionActor {

    public static final String SYSTEM = "SYSTEM";

    /** L'utilisateur connecte (son adresse de courriel), ou {@code null} hors session. */
    public String current() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken || !StringUtils.hasText(auth.getName())) {
            return null;
        }
        return auth.getName().trim();
    }

    /** L'utilisateur connecte ; hors session, la valeur fournie, sinon {@code SYSTEM}. */
    public String resolve(String fallback) {
        String current = current();
        if (current != null) {
            return current;
        }
        return StringUtils.hasText(fallback) ? fallback.trim() : SYSTEM;
    }
}
