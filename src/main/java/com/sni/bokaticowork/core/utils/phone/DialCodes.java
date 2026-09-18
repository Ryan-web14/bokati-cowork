package com.sni.bokaticowork.core.utils.phone;

import java.util.Optional;
import java.util.Set;

/**
 * Les indicatifs telephoniques attribues dans le monde, et la reconnaissance d'un indicatif tape
 * sans son plus.
 *
 * <p>Quelqu'un tape {@code 33 6 41 53 45 35} pour un numero francais, ou {@code 242 06 123 45 67}
 * pour un numero congolais, sans le plus que son clavier ne propose pas toujours. Ce n'est pas une
 * faute : le nombre de chiffres et le prefixe suffisent a comprendre. Un numero national n'atteint
 * jamais dix chiffres dans les pays ou l'etablissement opere, et il commence presque toujours par
 * un zero · un numero de dix chiffres ou plus sans zero de tete qui commence par un indicatif connu
 * est un numero international.</p>
 *
 * <p>La liste est celle de l'UIT, figee ici plutot que lue en base : la table des pays peut etre
 * incomplete sur un environnement, l'attribution des indicatifs, elle, ne change pas.</p>
 */
public final class DialCodes {

    private static final Set<String> CODES = Set.of(
            // Zone 1 · Amerique du Nord et Caraibes (plan de numerotation nord-americain)
            "1",
            // Zone 2 · Afrique
            "20", "211", "212", "213", "216", "218", "220", "221", "222", "223", "224", "225", "226", "227",
            "228", "229", "230", "231", "232", "233", "234", "235", "236", "237", "238", "239", "240", "241",
            "242", "243", "244", "245", "246", "247", "248", "249", "250", "251", "252", "253", "254", "255",
            "256", "257", "258", "260", "261", "262", "263", "264", "265", "266", "267", "268", "269", "27",
            "290", "291", "297", "298", "299",
            // Zone 3 et 4 · Europe
            "30", "31", "32", "33", "34", "350", "351", "352", "353", "354", "355", "356", "357", "358", "359",
            "36", "370", "371", "372", "373", "374", "375", "376", "377", "378", "379", "380", "381", "382",
            "383", "385", "386", "387", "389", "39", "40", "41", "420", "421", "423", "43", "44", "45", "46",
            "47", "48", "49",
            // Zone 5 · Amerique du Sud et centrale
            "500", "501", "502", "503", "504", "505", "506", "507", "508", "509", "51", "52", "53", "54",
            "55", "56", "57", "58", "590", "591", "592", "593", "594", "595", "596", "597", "598", "599",
            // Zone 6 · Oceanie et Asie du Sud-Est
            "60", "61", "62", "63", "64", "65", "66", "670", "672", "673", "674", "675", "676", "677", "678",
            "679", "680", "681", "682", "683", "685", "686", "687", "688", "689", "690", "691", "692",
            // Zone 7 · Russie et Kazakhstan
            "7",
            // Zone 8 · Asie de l'Est et services
            "81", "82", "84", "850", "852", "853", "855", "856", "86", "880", "886",
            // Zone 9 · Asie du Sud, Moyen-Orient
            "90", "91", "92", "93", "94", "95", "960", "961", "962", "963", "964", "965", "966", "967",
            "968", "970", "971", "972", "973", "974", "975", "976", "977", "98", "992", "993", "994", "995",
            "996", "998"
    );

    /** En dessous, un numero sans plus ni zero de tete est national · jamais international. */
    private static final int MIN_INTERNATIONAL_DIGITS = 10;

    private DialCodes() {
    }

    /**
     * L'indicatif d'un numero qui porte deja son plus · le plus long connu en tete, sans condition
     * de longueur : ici il n'y a rien a deviner, seulement a decouper.
     *
     * @param digits chiffres seuls, apres le plus
     */
    public static Optional<String> ofInternational(String digits) {
        if (digits == null || digits.isEmpty()) {
            return Optional.empty();
        }
        for (int length = 3; length >= 1; length--) {
            if (digits.length() > length && CODES.contains(digits.substring(0, length))) {
                return Optional.of(digits.substring(0, length));
            }
        }
        return Optional.empty();
    }

    public static boolean isKnown(String dialCode) {
        return dialCode != null && CODES.contains(dialCode.replaceAll("[^0-9]", ""));
    }

    /**
     * L'indicatif que ces chiffres portent en tete, s'ils en portent un.
     *
     * <p>Le plus long d'abord : {@code 242…} est le Congo avant d'etre {@code 24} qui n'existe pas,
     * et {@code 1…} n'est retenu que s'il ne reste pas moins que ce qu'un numero exige. Un numero
     * qui commence par zero n'est jamais lu comme international · le zero est un prefixe d'acces ou
     * une partie du numero national, jamais un indicatif.</p>
     *
     * @param digits chiffres seuls, sans plus
     */
    public static Optional<String> leading(String digits) {
        if (digits == null || digits.length() < MIN_INTERNATIONAL_DIGITS || digits.startsWith("0")) {
            return Optional.empty();
        }
        for (int length = 3; length >= 1; length--) {
            if (digits.length() <= length) {
                continue;
            }
            String candidate = digits.substring(0, length);
            if (CODES.contains(candidate) && digits.length() - length >= 6) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }
}
