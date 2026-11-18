package com.sni.bokaticowork.features.ressource.service.pricing;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Lot R1 : qui reserve et comment, pour les conditions des regles tarifaires.
 *
 * @param segments  segments du client : PUBLIC (client hors membre), MEMBER, SUBSCRIBER (abonnement
 *                  en cours), COMPANY (entreprise ou client entreprise), PARTNER
 * @param planCode  code du plan de l'abonnement en cours, s'il y en a un
 * @param channel   PORTAL, DESK (accueil) ou TEAM (back-office) ; nul si inconnu
 * @param now       instant de la reservation (delai avant le debut, dernier moment)
 * @param owners    lot B10 : identites du client, « TYPE:CODE » (le titulaire, et pour un membre
 *                  les organisations auxquelles il est rattache) ; vide pour un visiteur
 */
public record PriceContext(Set<String> segments, String planCode, String channel, LocalDateTime now, Set<String> owners) {

    public PriceContext(Set<String> segments, String planCode, String channel, LocalDateTime now) {
        this(segments, planCode, channel, now, Set.of());
    }

    public PriceContext {
        owners = owners == null ? Set.of() : Set.copyOf(owners);
    }

    public static PriceContext anonymous() {
        return new PriceContext(Set.of("PUBLIC"), null, null, LocalDateTime.now(java.time.ZoneId.of("Africa/Brazzaville")));
    }

    public PriceContext withNow(LocalDateTime instant) {
        return new PriceContext(segments, planCode, channel, instant, owners);
    }

    public PriceContext withChannel(String value) {
        return new PriceContext(segments, planCode, value, now, owners);
    }

    /**
     * Lot B10 : cles d'audience du client · ses identites, ses segments (« SEGMENT:MEMBER ») et
     * son plan (« PLAN:CODE »). Une regle ou une ressource reservee a une audience s'applique si
     * l'une de ses cles y figure.
     */
    public Set<String> audienceKeys() {
        Set<String> keys = new LinkedHashSet<>(owners);
        if (segments != null) {
            segments.forEach(segment -> keys.add("SEGMENT:" + segment));
        }
        if (planCode != null && !planCode.isBlank()) {
            keys.add("PLAN:" + planCode);
        }
        return keys;
    }
}
