package com.sni.bokaticowork.features.billing.service.support;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Montant en toutes lettres, pour la mention « arretee a la somme de ».
 *
 * <p>Une facture sans somme en lettres se conteste plus facilement : le chiffre seul se rature, la
 * lettre non. L'usage est constant sur les pieces comptables, et la mention manquait.
 *
 * <p>L'orthographe francaise des nombres compte plusieurs pieges qu'un formatage naif rate :
 * <ul>
 *   <li>{@code quatre-vingts} prend un s seul, {@code quatre-vingt-un} et
 *       {@code quatre-vingt mille} le perdent ;</li>
 *   <li>{@code deux cents} prend un s seul, {@code deux cent un} et {@code deux cent mille}
 *       le perdent ;</li>
 *   <li>{@code mille} est invariable · jamais {@code deux milles} ;</li>
 *   <li>{@code vingt et un} porte un « et », mais {@code quatre-vingt-un} n'en porte pas ;</li>
 *   <li>{@code soixante et onze} et {@code quatre-vingt-onze} se comptent sur la dizaine
 *       precedente, 60 et 80.</li>
 * </ul>
 *
 * <p>D'ou le drapeau {@code terminal} qui traverse la recursion : le s de quatre-vingts et de cents
 * ne survit que si <b>rien</b> ne suit, pas meme un nom d'echelle.
 */
public final class FrenchAmountWords {

    private static final String[] UNITS = {
            "zéro", "un", "deux", "trois", "quatre", "cinq", "six", "sept", "huit", "neuf",
            "dix", "onze", "douze", "treize", "quatorze", "quinze", "seize"
    };

    private static final String[] TENS = {
            "", "", "vingt", "trente", "quarante", "cinquante", "soixante",
            "soixante", "quatre-vingt", "quatre-vingt"
    };

    private FrenchAmountWords() {
    }

    /**
     * {@code 547 653} devient « cinq cent quarante-sept mille six cent cinquante-trois ».
     *
     * <p>Le montant est arrondi a l'unite : le franc CFA n'a pas de subdivision en circulation, et
     * imprimer des centimes dans la mention en lettres laisserait croire le contraire.
     */
    public static String of(BigDecimal amount) {
        if (amount == null) {
            return null;
        }
        BigDecimal rounded = amount.setScale(0, RoundingMode.HALF_UP);
        boolean negative = rounded.signum() < 0;
        String words = spell(rounded.abs().longValueExact(), true);
        return negative ? "moins " + words : words;
    }

    /** Somme complete, prete a etre imprimee apres « arretee a la somme de ». */
    public static String withCurrency(BigDecimal amount, String currency) {
        String words = of(amount);
        if (words == null) {
            return null;
        }
        String unit = "XAF".equalsIgnoreCase(currency) ? "francs CFA"
                : (currency == null ? "" : currency);
        return unit.isEmpty() ? words : words + " " + unit;
    }

    /**
     * @param terminal rien ne suit ce groupe · seule position ou quatre-vingts et cents gardent
     *                 leur s
     */
    private static String spell(long n, boolean terminal) {
        if (n < 0) {
            throw new IllegalArgumentException("Nombre negatif : " + n);
        }
        if (n < 17) {
            return UNITS[(int) n];
        }
        if (n < 20) {
            // dix-sept, dix-huit, dix-neuf · les seules dizaines composees sur dix.
            return "dix-" + UNITS[(int) n - 10];
        }
        if (n < 100) {
            return spellUnderHundred((int) n, terminal);
        }
        if (n < 1_000) {
            return spellHundreds((int) n, terminal);
        }
        if (n < 1_000_000) {
            return spellScale(n, 1_000L, "mille", true, terminal);
        }
        if (n < 1_000_000_000L) {
            return spellScale(n, 1_000_000L, "million", false, terminal);
        }
        return spellScale(n, 1_000_000_000L, "milliard", false, terminal);
    }

    private static String spellUnderHundred(int n, boolean terminal) {
        int tens = n / 10;
        int unit = n % 10;

        // 70 et 90 se comptent sur soixante et quatre-vingt : soixante-douze, quatre-vingt-treize.
        if (tens == 7 || tens == 9) {
            int remainder = n - (tens == 7 ? 60 : 80);
            // « soixante et onze » garde le « et » ; « quatre-vingt-onze » ne le prend pas.
            String joiner = (tens == 7 && remainder == 11) ? " et " : "-";
            return TENS[tens] + joiner + spell(remainder, true);
        }

        String base = TENS[tens];
        if (unit == 0) {
            return tens == 8 && terminal ? base + "s" : base;
        }
        if (unit == 1 && tens != 8) {
            return base + " et un";
        }
        return base + "-" + UNITS[unit];
    }

    private static String spellHundreds(int n, boolean terminal) {
        int hundreds = n / 100;
        int remainder = n % 100;
        String head = hundreds == 1 ? "cent" : UNITS[hundreds] + " cent";
        if (remainder == 0) {
            return hundreds > 1 && terminal ? head + "s" : head;
        }
        return head + " " + spell(remainder, terminal);
    }

    private static String spellScale(long n, long scale, String name,
                                     boolean invariable, boolean terminal) {
        long count = n / scale;
        long remainder = n % scale;
        String head;
        if (count == 1) {
            // « mille » ne prend jamais « un » devant : mille, non un mille.
            head = invariable ? name : "un " + name;
        } else {
            // Le nom d'echelle suit le compte · celui-ci n'est donc jamais en position terminale.
            head = spell(count, false) + " " + name + (invariable ? "" : "s");
        }
        return remainder == 0 ? head : head + " " + spell(remainder, terminal);
    }
}
