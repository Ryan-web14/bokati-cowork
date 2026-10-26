package com.sni.bokaticowork.security.config;

import java.util.Arrays;
import java.util.List;

/**
 * Les origines autorisees a appeler l'API depuis un navigateur.
 *
 * <p>La configuration CORS acceptait toutes les origines, pour l'API comme pour le WebSocket ·
 * n'importe quelle page ouverte dans le navigateur d'un client ou d'un employe pouvait appeler
 * l'API en son nom. La liste vient desormais de {@code app.security.cors.allowed-origins}, lue de la
 * meme facon par les deux configurations.</p>
 *
 * <p>CORS ne protege que des navigateurs · un script en ligne de commande n'envoie pas d'origine
 * et n'est pas concerne. C'est une defense contre les pages tierces, pas contre les robots.</p>
 */
public final class CorsOrigins {

    /** L'etoile seule · toutes les origines. */
    public static final String ANY = "*";

    private CorsOrigins() {
    }

    /** Une liste separee par des virgules, sans les vides · une valeur vide n'ouvre rien. */
    public static List<String> parse(String raw) {
        if (raw == null) {
            return List.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();
    }

    /** La liste ouvre-t-elle toutes les origines · refuse en production. */
    public static boolean allowsAny(List<String> origins) {
        return origins.contains(ANY);
    }
}
