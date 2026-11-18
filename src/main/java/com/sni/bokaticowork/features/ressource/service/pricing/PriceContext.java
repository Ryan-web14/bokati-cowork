package com.sni.bokaticowork.features.ressource.service.pricing;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Lot R1 : qui reserve et comment, pour les conditions des regles tarifaires.
 *
 * @param segments  segments du client : PUBLIC (client hors membre), MEMBER, SUBSCRIBER (abonnement
 *                  en cours), COMPANY (entreprise ou client entreprise), PARTNER
 * @param planCode  code du plan de l'abonnement en cours, s'il y en a un
 * @param channel   PORTAL, DESK (accueil) ou TEAM (back-office) ; nul si inconnu
 * @param now       instant de la reservation (delai avant le debut, dernier moment)
 */
public record PriceContext(Set<String> segments, String planCode, String channel, LocalDateTime now) {

    public static PriceContext anonymous() {
        return new PriceContext(Set.of("PUBLIC"), null, null, LocalDateTime.now(java.time.ZoneId.of("Africa/Brazzaville")));
    }

    public PriceContext withNow(LocalDateTime instant) {
        return new PriceContext(segments, planCode, channel, instant);
    }
}
