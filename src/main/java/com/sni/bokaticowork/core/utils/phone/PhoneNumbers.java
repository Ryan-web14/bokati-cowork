package com.sni.bokaticowork.core.utils.phone;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * La forme d'un numero de telephone, sans rien savoir des pays.
 *
 * <p>Un numero arrive sous toutes les formes que les gens tapent : avec un {@code +}, un {@code 00},
 * des espaces, des points, des tirets, des parentheses. Aucune de ces formes n'est une erreur · ce
 * sont des habitudes d'ecriture, et le systeme les lit toutes. Ce qui est garde est la forme
 * internationale, {@code +} suivi des chiffres, dite E.164 : une seule ecriture par numero, donc
 * une seule cle pour retrouver quelqu'un.</p>
 *
 * <p>La resolution du pays · quel indicatif ajouter a un numero qui n'en porte pas · est ailleurs,
 * dans {@link PhoneNumberService}, parce qu'elle a besoin de la table des pays. Ici il n'y a que la
 * forme.</p>
 */
public final class PhoneNumbers {

    /** E.164 : un plus, puis huit a quinze chiffres, le premier non nul. */
    private static final Pattern E164 = Pattern.compile("^\\+[1-9][0-9]{7,14}$");

    /** Ce qui peut apparaitre dans un numero tel qu'on le tape · tout le reste est une faute. */
    private static final Pattern TYPABLE = Pattern.compile("^[+0-9()\\s.\\-]{6,30}$");

    private PhoneNumbers() {
    }

    /**
     * Ne garde que le sens : un {@code +} de tete s'il y en avait un, et les chiffres.
     *
     * <p>{@code 00} en tete vaut {@code +} · c'est la sortie internationale de la plupart des pays,
     * et quelqu'un qui tape {@code 00242} veut dire {@code +242}.</p>
     */
    public static String clean(String raw) {
        if (raw == null) {
            return "";
        }
        String digitsAndPlus = raw.trim().replaceAll("[\\s().\\-]", "");
        if (digitsAndPlus.startsWith("00")) {
            digitsAndPlus = "+" + digitsAndPlus.substring(2);
        }
        // Un plus ailleurs qu'en tete est une faute de frappe, pas un sens.
        boolean international = digitsAndPlus.startsWith("+");
        String digits = digitsAndPlus.replaceAll("[^0-9]", "");
        return international ? "+" + digits : digits;
    }

    /** Le numero est-il d'une forme qu'on peut lire · avant meme de savoir a quel pays il appartient. */
    public static boolean looksValid(String raw) {
        if (raw == null || raw.isBlank() || !TYPABLE.matcher(raw.trim()).matches()) {
            return false;
        }
        String cleaned = clean(raw);
        if (cleaned.startsWith("+")) {
            return E164.matcher(cleaned).matches();
        }
        // Sans indicatif : un numero national, de six a quatorze chiffres. Le pays dira le reste.
        return cleaned.length() >= 6 && cleaned.length() <= 14;
    }

    /**
     * Deja en forme internationale · rien a deviner.
     *
     * <p>Avec un plus, ou sans : {@code 33 6 41 53 45 35} est un numero francais que son auteur a
     * tape sans le plus que son clavier ne proposait pas. Dix chiffres ou plus, pas de zero de
     * tete, un indicatif connu devant · c'est international, voir {@link DialCodes#leading}.</p>
     */
    public static boolean isInternational(String raw) {
        String cleaned = clean(raw);
        return cleaned.startsWith("+") || DialCodes.leading(cleaned).isPresent();
    }

    /**
     * Met en forme internationale avec l'indicatif donne.
     *
     * <p>Le zero de tete d'un numero national est en general le prefixe d'acces national, qui
     * n'existe pas en forme internationale · {@code 06 12 34 56 78} en France devient
     * {@code +33612345678}. Mais certains pays le gardent, et le Congo en est : {@code 06 123 45 67}
     * devient {@code +242061234567}, le zero fait partie du numero. D'ou {@code keepTrunkZero}, que
     * le service regle selon le pays.</p>
     *
     * @param dialCode      indicatif, avec ou sans {@code +}
     * @param keepTrunkZero le zero de tete fait partie du numero national
     */
    public static Optional<String> toE164(String raw, String dialCode, boolean keepTrunkZero) {
        String cleaned = clean(raw);
        if (cleaned.isEmpty()) {
            return Optional.empty();
        }
        String candidate;
        if (cleaned.startsWith("+")) {
            candidate = cleaned;
        } else if (DialCodes.leading(cleaned).isPresent()) {
            candidate = "+" + cleaned;
        } else {
            String code = dialCode == null ? "" : dialCode.replaceAll("[^0-9]", "");
            if (code.isEmpty()) {
                return Optional.empty();
            }
            String national = keepTrunkZero ? cleaned : cleaned.replaceFirst("^0+", "");
            candidate = "+" + code + national;
        }
        return E164.matcher(candidate).matches() ? Optional.of(candidate) : Optional.empty();
    }
}
