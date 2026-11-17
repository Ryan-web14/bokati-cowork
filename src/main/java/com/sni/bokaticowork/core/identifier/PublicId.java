package com.sni.bokaticowork.core.identifier;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Identifiant public lisible et reversible (17/11/2026).
 *
 * <p>Les identifiants generes depassent 2^53 : un navigateur les arrondit (7319298661696688123
 * devient 7319298661696688000) et renvoie un identifiant qui n'existe pas. Le serveur rend donc
 * chaque identifiant en texte : un prefixe lisible du type, un tiret, puis l'identifiant en base 32
 * de Crockford sur 13 caracteres, par exemple {@code VR-6BK2Q9F0ZD1HM}. Le prefixe n'est qu'une
 * aide a la lecture : le decodage ne lit que les 13 caracteres. L'ancien nombre reste accepte.</p>
 */
public final class PublicId {

    private static final char[] ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();
    private static final int LENGTH = 13;
    private static final Pattern CODE = Pattern.compile("^(?:[A-Za-z0-9]{1,8}-)?([0-9A-Za-z]{13})$");
    private static final Pattern DIGITS = Pattern.compile("^\\d{1,19}$");

    /** Au-dela, un nombre JSON perd sa precision dans un navigateur. */
    public static final long MAX_SAFE_INTEGER = 9_007_199_254_740_991L;

    private PublicId() {
    }

    public static String encode(String prefix, long value) {
        char[] out = new char[LENGTH];
        long v = value;
        for (int i = LENGTH - 1; i >= 0; i--) {
            out[i] = ALPHABET[(int) (v & 31)];
            v >>>= 5;
        }
        String body = new String(out);
        return prefix == null || prefix.isEmpty() ? body : prefix + "-" + body;
    }

    /** Code public ou ancien nombre vers l'identifiant ; {@link IllegalArgumentException} sinon. */
    public static long decode(String text) {
        if (text == null) {
            throw new IllegalArgumentException("Identifiant vide");
        }
        String value = text.trim();
        if (DIGITS.matcher(value).matches()) {
            return Long.parseLong(value);
        }
        Matcher matcher = CODE.matcher(value);
        if (!matcher.matches()) {
            // « NaN » ou « undefined » : le frontend a converti le code en nombre au lieu de le renvoyer tel quel.
            throw new IllegalArgumentException("Identifiant invalide : " + text
                    + " (renvoyer le code reçu tel quel, par exemple VR-6B4TZB9ZPRKZV, sans le convertir en nombre)");
        }
        String body = matcher.group(1).toUpperCase(Locale.ROOT);
        long result = 0;
        for (int i = 0; i < LENGTH; i++) {
            int digit = digit(body.charAt(i));
            if (digit < 0 || (i == 0 && digit > 15)) {
                throw new IllegalArgumentException("Identifiant invalide : " + text);
            }
            result = (result << 5) | digit;
        }
        return result;
    }

    /** Vrai pour un code public ou un nombre decimal (sans lever d'erreur). */
    public static boolean isDecodable(String text) {
        try {
            decode(text);
            return true;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private static int digit(char c) {
        switch (c) {
            case 'O':
                return 0;
            case 'I':
            case 'L':
                return 1;
            default:
                for (int i = 0; i < ALPHABET.length; i++) {
                    if (ALPHABET[i] == c) {
                        return i;
                    }
                }
                return -1;
        }
    }

    /**
     * Prefixe tire d'un nom (type ou propriete) : initiales des mots s'il y en a plusieurs
     * ({@code VatReturn} : VR, {@code fixedAssetId} : FA), sinon les trois premieres lettres
     * ({@code Account} : ACC). Prefixe {@code Acc} des entites comptables et suffixes de DTO ignores.
     */
    public static String prefixFor(String name) {
        if (name == null || name.isBlank()) {
            return "ID";
        }
        String base = name;
        if (base.endsWith("Ids")) {
            base = base.substring(0, base.length() - 3);
        } else if (base.endsWith("Id") && base.length() > 2) {
            base = base.substring(0, base.length() - 2);
        }
        base = base.replaceFirst("^Acc(?=[A-Z])", "")
                .replaceFirst("(Response|Dto|DTO|View)$", "");
        String[] words = base.split("(?<=[a-z0-9])(?=[A-Z])");
        StringBuilder prefix = new StringBuilder();
        if (words.length >= 2) {
            for (String word : words) {
                if (!word.isEmpty() && prefix.length() < 4) {
                    prefix.append(Character.toUpperCase(word.charAt(0)));
                }
            }
        } else {
            String word = words.length == 0 ? "" : words[0];
            prefix.append(word, 0, Math.min(3, word.length()));
        }
        String result = prefix.toString().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
        return result.isEmpty() ? "ID" : result;
    }
}
