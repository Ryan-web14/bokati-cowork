package com.sni.bokaticowork.features.payment.dto.response;

import java.util.List;
import java.util.Map;

/**
 * Valeurs proposees pour un formulaire de caisse, pour que le frontend n'ait aucune regle a recoder.
 *
 * @param context   contexte demande ({@code REGISTER_CREATE}, {@code SESSION_OPEN})
 * @param values    valeur proposee par champ
 * @param origins   origine de chaque valeur : {@code DEDUCED}, {@code GENERATED}, {@code INHERITED}, {@code LIST}
 * @param choices   listes de choix par champ (code et libelle)
 * @param warnings  messages a afficher (hors horaires, poste inconnu...)
 */
public record CashFormDefaultsResponse(
        String context,
        Map<String, Object> values,
        Map<String, String> origins,
        Map<String, List<Choice>> choices,
        List<String> warnings
) {
    public record Choice(String code, String label) {
    }
}
