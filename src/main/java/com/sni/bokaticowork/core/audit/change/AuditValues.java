package com.sni.bokaticowork.core.audit.change;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Mise en forme des valeurs gardees dans l'avant / apres : textes courts, references en
 * {@code Type:code}, donnees sensibles masquees (jamais ecrites, meme modifiees).
 */
public final class AuditValues {

    public static final String MASK = "***";
    static final int MAX_TEXT = 300;

    /** Mots qui, dans le nom d'un champ, le rendent sensible : mots de passe, secrets, jetons, codes, cles, cartes. */
    private static final Set<String> SENSITIVE_WORDS = Set.of(
            "password", "passwd", "pwd", "secret", "token", "otp", "pin", "cvv", "cvc", "pan", "hash", "salt",
            "mfa", "totp", "credential", "credentials", "apikey", "privatekey", "cardnumber");

    private static final Pattern WORD_BOUNDARY = Pattern.compile("(?<=[a-z0-9])(?=[A-Z])|[_.\\s\\-]+");

    /** Champs techniques sans interet pour l'exploitant. */
    private static final Set<String> IGNORED = Set.of(
            "version", "updatedat", "updatedate", "lastmodifiedat", "lastmodifieddate", "modifiedat", "lastupdated");

    private AuditValues() {
    }

    public static boolean isSensitive(String property) {
        String[] words = WORD_BOUNDARY.split(property);
        String joined = String.join("", words).toLowerCase(Locale.ROOT);
        if (joined.contains("apikey") || joined.contains("privatekey") || joined.contains("cardnumber") || joined.contains("password")) {
            return true;
        }
        for (String word : words) {
            if (SENSITIVE_WORDS.contains(word.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    /** Chaine de requete d'une adresse avec les valeurs sensibles masquees ({@code token=***}). */
    public static String maskQuery(String query) {
        if (query == null || query.isEmpty()) {
            return query;
        }
        StringBuilder masked = new StringBuilder();
        for (String pair : query.split("&")) {
            if (masked.length() > 0) {
                masked.append('&');
            }
            int equals = pair.indexOf('=');
            String name = equals < 0 ? pair : pair.substring(0, equals);
            String decoded;
            try {
                decoded = java.net.URLDecoder.decode(name, java.nio.charset.StandardCharsets.UTF_8);
            } catch (IllegalArgumentException malformed) {
                decoded = name;
            }
            masked.append(equals < 0 || !isSensitive(decoded) ? pair : name + "=" + MASK);
        }
        return shorten(masked.toString());
    }

    public static boolean isIgnored(String property) {
        return IGNORED.contains(property.toLowerCase(Locale.ROOT));
    }

    public static Object render(String property, Object value) {
        if (value == null) {
            return null;
        }
        if (isSensitive(property)) {
            return MASK;
        }
        if (AuditedEntities.isEntity(value)) {
            return AuditedEntities.label(value);
        }
        if (value instanceof Enum<?> e) {
            return e.name();
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value;
        }
        if (value instanceof byte[] bytes) {
            return "(binaire, " + bytes.length + " octets)";
        }
        if (value instanceof Collection<?> || value instanceof Map<?, ?>) {
            return shorten(String.valueOf(value));
        }
        return shorten(String.valueOf(value));
    }

    static String shorten(String text) {
        return text.length() <= MAX_TEXT ? text : text.substring(0, MAX_TEXT) + "...";
    }
}
