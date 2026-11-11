package com.sni.bokaticowork.core.utils.validation;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Listes d'adresses de courriel saisies dans un seul champ, separees par des virgules.
 *
 * <p>Une entreprise peut vouloir que ses factures partent a plusieurs adresses (comptabilite,
 * direction). Le champ garde la liste telle quelle, normalisee ; l'envoi la decoupe en autant de
 * destinataires. Le point-virgule est accepte aussi, parce que c'est le separateur d'Outlook.</p>
 */
public final class EmailAddresses {

    /** Longueur de la colonne de stockage. */
    public static final int MAX_LENGTH = 500;

    private EmailAddresses() {
    }

    /** Les adresses du champ, sans espaces ni doublons, dans l'ordre de saisie. Vide si rien. */
    public static List<String> split(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        Set<String> seen = new LinkedHashSet<>();
        List<String> result = new ArrayList<>();
        for (String part : value.split("[,;]")) {
            String email = part.trim();
            if (!email.isEmpty() && seen.add(email.toLowerCase(Locale.ROOT))) {
                result.add(email);
            }
        }
        return result;
    }

    /** Vrai si le champ contient au moins une adresse et que toutes sont valides. */
    public static boolean isValidList(String value) {
        if (value == null || value.length() > MAX_LENGTH) {
            return false;
        }
        List<String> emails = split(value);
        return !emails.isEmpty() && emails.stream().allMatch(ValidationUtils::validateEmail);
    }

    /** La liste en minuscules, separee par « , », sans doublons · nul si vide. */
    public static String normalize(String value) {
        List<String> emails = split(value);
        if (emails.isEmpty()) {
            return null;
        }
        return String.join(", ", emails.stream().map(e -> e.toLowerCase(Locale.ROOT)).toList());
    }

    /** La premiere adresse de la liste · pour les usages qui n'en acceptent qu'une. Nul si vide. */
    public static String first(String value) {
        List<String> emails = split(value);
        return emails.isEmpty() ? null : emails.get(0);
    }
}
