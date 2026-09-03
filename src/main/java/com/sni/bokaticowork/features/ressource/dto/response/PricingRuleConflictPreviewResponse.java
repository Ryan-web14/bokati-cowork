package com.sni.bokaticowork.features.ressource.dto.response;

import java.util.List;

/**
 * Ce qu'une regle tarifaire viendrait concurrencer, ressource par ressource.
 *
 * <p>Poser une regle sur vingt ressources en un appel rend trivial ce qui etait fastidieux · y
 * compris l'erreur. Cette lecture, qui n'ecrit rien, montre avant de valider les regles deja en
 * place dont la fenetre chevauche celle proposee.
 *
 * <p>Un chevauchement n'est pas une faute : c'est le mecanisme meme des grilles tarifaires, que
 * {@code priority} arbitre. Ce qui merite un regard, c'est le doublon strict · une regle identique
 * a une regle active · qui n'apporte rien et brouille l'arbitrage.
 */
public record PricingRuleConflictPreviewResponse(int resourcesInspected,
                                                 int resourcesWithOverlap,
                                                 int strictDuplicates,
                                                 List<ResourcePreview> results) {

    /**
     * @param overlapping regles actives dont la fenetre croise celle proposee
     * @param duplicate   une regle active est strictement identique a celle proposee
     */
    public record ResourcePreview(String resourceCode,
                                  boolean duplicate,
                                  List<CompetingRule> overlapping) {}

    /**
     * @param wins la regle existante l'emporterait sur celle proposee · sa priorite est
     *             superieure, ou egale avec un identifiant plus recent
     */
    public record CompetingRule(Long id,
                                String label,
                                Integer price,
                                Integer priority,
                                Integer dayOfWeek,
                                String startsAt,
                                String endsAt,
                                boolean wins) {}
}
