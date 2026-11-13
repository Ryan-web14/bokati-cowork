package com.sni.bokaticowork.features.document.template.render;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Contenu d'une version de modele : format, exemplaires, style et blocs (lot D2). Les blocs restent
 * des objets JSON (cles et valeurs simples) : chacun est rendu par un fragment fixe, jamais par du
 * HTML fourni par l'utilisateur. {@link #validate()} refuse un type ou une option inconnus, et
 * borne les tailles pour qu'un modele ne puisse pas produire une piece demesuree.
 */
public record TemplateContent(String pageFormat, List<Map<String, Object>> copies, Map<String, Object> style,
                              List<Map<String, Object>> blocks) {

    public static final Set<String> PAGE_FORMATS = Set.of("A4", "A5_TWO_UP", "A6");
    public static final Set<String> BLOCK_TYPES = Set.of("HEADER", "TITLE", "FIELDS", "LINES", "TOTALS", "TEXT", "SIGNATURES", "FOOTER");
    private static final Pattern COLOR = Pattern.compile("^#[0-9a-fA-F]{6}$");
    private static final Pattern KEY = Pattern.compile("^[a-z0-9_.]{1,80}$");
    private static final int MAX_BLOCKS = 40;
    private static final int MAX_TEXT = 2000;

    private static final Map<String, Set<String>> OPTIONS = Map.of(
            "HEADER", Set.of("type", "qr"),
            "TITLE", Set.of("type", "title", "copyLabel"),
            "FIELDS", Set.of("type", "title", "columns", "fields"),
            "LINES", Set.of("type", "title", "table", "columns"),
            "TOTALS", Set.of("type", "label", "words", "wordsLabel"),
            "TEXT", Set.of("type", "title", "text", "boxed"),
            "SIGNATURES", Set.of("type", "boxes", "fromData", "width", "height"),
            "FOOTER", Set.of("type", "text"));

    public TemplateContent {
        pageFormat = pageFormat == null || pageFormat.isBlank() ? "A4" : pageFormat.trim().toUpperCase();
        copies = copies == null ? List.of() : copies;
        style = style == null ? Map.of() : style;
        blocks = blocks == null ? List.of() : blocks;
    }

    /** Controle complet ; leve {@code MODELE_INVALIDE} avec la liste des erreurs. */
    public TemplateContent validate() {
        List<String> errors = new ArrayList<>();
        if (!PAGE_FORMATS.contains(pageFormat)) {
            errors.add("Format inconnu : " + pageFormat + " (A4, A5_TWO_UP ou A6)");
        }
        if (copies.size() > 4) {
            errors.add("Quatre exemplaires au plus");
        }
        for (Map<String, Object> copy : copies) {
            if (text(copy.get("label")).length() > 120) {
                errors.add("Mention d'exemplaire trop longue (120 caractères au plus)");
            }
        }
        Object accent = style.get("accent");
        if (accent != null && !COLOR.matcher(accent.toString()).matches()) {
            errors.add("Couleur d'accent invalide : " + accent + " (forme #RRGGBB)");
        }
        if (blocks.isEmpty()) {
            errors.add("Le modèle n'a aucun bloc");
        }
        if (blocks.size() > MAX_BLOCKS) {
            errors.add("Quarante blocs au plus");
        }
        for (int index = 0; index < blocks.size(); index++) {
            validateBlock(index + 1, blocks.get(index), errors);
        }
        if (!errors.isEmpty()) {
            throw BadRequestException.coded("MODELE_INVALIDE", "Modèle invalide : " + String.join(" ; ", errors));
        }
        return this;
    }

    private static void validateBlock(int position, Map<String, Object> block, List<String> errors) {
        String type = text(block.get("type")).toUpperCase();
        String where = "bloc " + position;
        if (!BLOCK_TYPES.contains(type)) {
            errors.add(where + " : type inconnu « " + text(block.get("type")) + " »");
            return;
        }
        for (String option : block.keySet()) {
            if (!OPTIONS.get(type).contains(option)) {
                errors.add(where + " (" + type + ") : option inconnue « " + option + " »");
            }
        }
        for (String option : List.of("title", "label", "wordsLabel")) {
            if (text(block.get(option)).length() > 150) {
                errors.add(where + " : « " + option + " » trop long (150 caractères au plus)");
            }
        }
        if (text(block.get("text")).length() > MAX_TEXT) {
            errors.add(where + " : texte trop long (" + MAX_TEXT + " caractères au plus)");
        }
        switch (type) {
            case "FIELDS" -> {
                int columns = integer(block.get("columns"), 3);
                if (columns < 1 || columns > 4) {
                    errors.add(where + " : de une à quatre colonnes");
                }
                List<?> fields = list(block.get("fields"));
                if (fields.isEmpty()) {
                    errors.add(where + " : aucun champ");
                }
                fields.forEach(item -> checkKey(where, item instanceof Map<?, ?> map ? map.get("key") : item, errors));
            }
            case "LINES" -> {
                checkKey(where, block.get("table"), errors);
                list(block.get("columns")).forEach(item -> checkKey(where, item instanceof Map<?, ?> map ? map.get("key") : item, errors));
            }
            case "SIGNATURES" -> {
                int width = integer(block.get("width"), 160);
                int height = integer(block.get("height"), 164);
                if (width < 80 || width > 260 || height < 60 || height > 260) {
                    errors.add(where + " : cases de 80 à 260 px de large et de 60 à 260 px de haut");
                }
                List<?> boxes = list(block.get("boxes"));
                if (boxes.size() > 6) {
                    errors.add(where + " : six cases au plus");
                }
                if (boxes.isEmpty() && !Boolean.TRUE.equals(block.get("fromData"))) {
                    errors.add(where + " : aucune case de signature");
                }
                for (Object box : boxes) {
                    if (!(box instanceof Map<?, ?> map) || text(map.get("label")).isBlank()) {
                        errors.add(where + " : chaque case a un libellé");
                    } else if (map.get("signer") != null) {
                        checkKey(where, map.get("signer"), errors);
                    }
                }
            }
            default -> {
            }
        }
    }

    private static void checkKey(String where, Object key, List<String> errors) {
        if (key == null || !KEY.matcher(key.toString()).matches()) {
            errors.add(where + " : clé invalide « " + key + " » (minuscules, chiffres, souligné)");
        }
    }

    // ------------------------------------------------------------------ lecture tolerante

    static String text(Object value) {
        return value == null ? "" : value.toString();
    }

    static int integer(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return value == null ? fallback : Integer.parseInt(value.toString().trim());
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    static boolean flag(Object value, boolean fallback) {
        if (value == null) {
            return fallback;
        }
        return value instanceof Boolean bool ? bool : Boolean.parseBoolean(value.toString());
    }

    static List<?> list(Object value) {
        return value instanceof Collection<?> collection ? new ArrayList<>(collection) : List.of();
    }

    /** Copie modifiable d'un bloc, cles conservees dans leur ordre. */
    public static Map<String, Object> block(Object... keyValues) {
        Map<String, Object> block = new LinkedHashMap<>();
        for (int index = 0; index + 1 < keyValues.length; index += 2) {
            block.put(keyValues[index].toString(), keyValues[index + 1]);
        }
        return block;
    }
}
