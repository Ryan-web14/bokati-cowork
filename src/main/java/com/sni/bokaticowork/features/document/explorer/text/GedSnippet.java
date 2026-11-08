package com.sni.bokaticowork.features.document.explorer.text;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * L'extrait d'un resultat de recherche, les mots trouves entre {@code ⟦} et {@code ⟧}.
 *
 * <p>Le surlignage de PostgreSQL ({@code ts_headline}) compare les mots accentues du texte a la
 * requete sans accents et rate « Échéance » quand on cherche « echeance ». Ici la comparaison se
 * fait sans accents ni majuscules, l'extrait garde le texte d'origine. Un mot cherche de plus de
 * cinq lettres trouve aussi ses variantes de fin (facture, factures, facturé), comme la recherche.</p>
 *
 * <p>Le frontend decoupe sur ces deux marques et affiche des noeuds texte, jamais du HTML : le
 * contenu vient de fichiers deposes.</p>
 */
public final class GedSnippet {

    public static final char OPEN = '⟦';
    public static final char CLOSE = '⟧';
    private static final int BEFORE = 80;

    private GedSnippet() {
    }

    public static String build(String content, String query, int maxLength) {
        if (content == null || content.isBlank()) {
            return null;
        }
        String text = content.replace(OPEN, '[').replace(CLOSE, ']').replace('\n', ' ');
        StringBuilder normalized = new StringBuilder(text.length());
        List<Integer> origin = new ArrayList<>(text.length());
        for (int index = 0; index < text.length(); index++) {
            String folded = fold(String.valueOf(text.charAt(index)));
            for (int k = 0; k < folded.length(); k++) {
                normalized.append(folded.charAt(k));
                origin.add(index);
            }
        }

        List<int[]> matches = new ArrayList<>();
        for (String stem : stems(query)) {
            int from = 0;
            while ((from = normalized.indexOf(stem, from)) >= 0) {
                if (from == 0 || !Character.isLetterOrDigit(normalized.charAt(from - 1))) {
                    int end = from + stem.length();
                    while (end < normalized.length() && Character.isLetterOrDigit(normalized.charAt(end))) end++;
                    matches.add(new int[]{origin.get(from), origin.get(end - 1) + 1});
                }
                from += stem.length();
            }
        }
        matches.sort((a, b) -> Integer.compare(a[0], b[0]));

        int start = 0;
        if (!matches.isEmpty()) {
            start = Math.max(0, matches.getFirst()[0] - BEFORE);
            while (start > 0 && start < text.length() && !Character.isWhitespace(text.charAt(start - 1))) start++;
        }
        int end = Math.min(text.length(), start + maxLength);
        while (end < text.length() && end > start && !Character.isWhitespace(text.charAt(end))) end--;
        if (end <= start) end = Math.min(text.length(), start + maxLength);

        StringBuilder snippet = new StringBuilder();
        if (start > 0) snippet.append('…');
        int cursor = start;
        for (int[] match : matches) {
            if (match[0] < cursor || match[1] > end) continue;
            snippet.append(text, cursor, match[0]).append(OPEN).append(text, match[0], match[1]).append(CLOSE);
            cursor = match[1];
        }
        snippet.append(text, cursor, end);
        if (end < text.length()) snippet.append('…');
        return snippet.toString().strip();
    }

    /** Les mots de la requete, sans accents, sans les mots exclus ({@code -brouillon}) ni « or ». */
    static Set<String> stems(String query) {
        Set<String> stems = new LinkedHashSet<>();
        if (query == null) return stems;
        for (String raw : query.split("\\s+")) {
            if (raw.startsWith("-")) continue;
            for (String word : fold(raw).split("[^\\p{L}\\p{N}]+")) {
                if (word.length() < 2 || word.equals("or")) continue;
                stems.add(word.length() > 5 ? word.substring(0, word.length() - 2) : word);
            }
        }
        return stems;
    }

    static String fold(String value) {
        String decomposed = Normalizer.normalize(value, Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT);
    }
}
