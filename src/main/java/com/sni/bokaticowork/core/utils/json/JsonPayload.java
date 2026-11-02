package com.sni.bokaticowork.core.utils.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Un petit contenu JSON produit par la plateforme · journal, evenement, metadonnees.
 *
 * <p>Ces contenus etaient assembles a la main par concatenation : une valeur portant un guillemet,
 * une barre oblique inverse ou un saut de ligne produisait un JSON invalide, et une colonne
 * {@code jsonb} le refusait au moment d'ecrire, ce qui annulait l'operation entiere (une annulation
 * d'abonnement l'a montre). Ici, chaque valeur est echappee par Jackson, dans l'ordre donne.</p>
 *
 * <p>Les dates sont ecrites en ISO 8601, les montants en notation decimale ordinaire.</p>
 */
public final class JsonPayload {

    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN)
            .build();

    private JsonPayload() {
    }

    /**
     * Un objet JSON a partir de paires cle, valeur · {@code of("code", code, "fee", fee)}.
     *
     * @throws IllegalArgumentException si le nombre d'arguments est impair ou qu'une cle n'est pas une chaine
     */
    public static String of(Object... keyValues) {
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("Paires cle, valeur attendues");
        }
        Map<String, Object> map = new LinkedHashMap<>();
        for (int index = 0; index < keyValues.length; index += 2) {
            if (!(keyValues[index] instanceof String key)) {
                throw new IllegalArgumentException("Cle JSON attendue en position " + index);
            }
            map.put(key, keyValues[index + 1]);
        }
        return of(map);
    }

    public static String of(Map<String, ?> map) {
        try {
            return MAPPER.writeValueAsString(map);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Contenu non serialisable en JSON", ex);
        }
    }

    /** Le texte est-il un JSON valide · pour refuser au plus tot un contenu mal forme. */
    public static boolean isValid(String json) {
        if (json == null) {
            return false;
        }
        try {
            MAPPER.readTree(json);
            return true;
        } catch (JsonProcessingException ex) {
            return false;
        }
    }
}
