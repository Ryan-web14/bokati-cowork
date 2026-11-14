package com.sni.bokaticowork.features.document.template.render;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Modeles de courriels en blocs (lot E1). Un modele devient un gabarit Thymeleaf genere ici, avec le
 * cadre commun ({@code email/fragments/notice-layout}) : le texte saisi est toujours echappe
 * (crochets compris, pour qu'aucune expression ne puisse s'y glisser), et chaque jeton
 * {@code {{chemin}}} devient une lecture de variable sure ({@code ${a?.b}}), limitee aux variables
 * que le courriel d'origine utilise deja.
 *
 * <p>L'objet du courriel est porte par un element cache {@code x-email-subject} que l'envoi lit et
 * retire ({@code EmailSubjectOverride}).</p>
 */
public final class EmailTemplates {

    public static final Set<String> BLOCK_TYPES = Set.of("SUBJECT", "PREHEADER", "TITLE", "GREETING", "TEXT", "DETAILS", "AMOUNT", "BUTTON",
            "ATTACHMENT_NOTE", "SIGNATURE", "LEGAL");
    public static final String SUBJECT_ID = "x-email-subject";
    private static final Pattern TOKEN = Pattern.compile("\\{\\{\\s*([A-Za-z][A-Za-z0-9_]*(?:\\.[A-Za-z][A-Za-z0-9_]*){0,2})\\s*}}");
    private static final Pattern VARIABLE = Pattern.compile("\\$\\{\\s*([A-Za-z][A-Za-z0-9_]*(?:\\.[A-Za-z][A-Za-z0-9_]*){0,2})");
    private static final Pattern URL = Pattern.compile("^https://[^\\s\"'<>|${}\\[\\]\\\\]{1,500}$");
    /** Proprietes jamais lisibles par un jeton, meme si elles existaient sur un objet du courriel. */
    private static final Set<String> FORBIDDEN = Set.of("class", "classloader", "module", "declaringclass", "protectiondomain", "runtime",
            "beanfactory", "applicationcontext", "environment", "servletcontext", "session", "request", "response", "param", "execinfo");
    private static final Map<String, Set<String>> OPTIONS = Map.ofEntries(
            Map.entry("SUBJECT", Set.of("type", "text")), Map.entry("PREHEADER", Set.of("type", "text")),
            Map.entry("TITLE", Set.of("type", "text")), Map.entry("GREETING", Set.of("type", "text")),
            Map.entry("TEXT", Set.of("type", "text", "tone")), Map.entry("DETAILS", Set.of("type", "title", "rows")),
            Map.entry("AMOUNT", Set.of("type", "label", "value")), Map.entry("BUTTON", Set.of("type", "label", "url")),
            Map.entry("ATTACHMENT_NOTE", Set.of("type", "text")), Map.entry("SIGNATURE", Set.of("type", "text")),
            Map.entry("LEGAL", Set.of("type")));
    private static final Set<String> TONES = Set.of("", "warn", "alert");

    private EmailTemplates() {
    }

    // ------------------------------------------------------------------ controle

    /**
     * Controle d'un modele de courriel : un objet, des blocs connus, des jetons parmi {@code allowed}
     * (variables du courriel d'origine ; vide : pas de limite), les jetons obligatoires presents.
     */
    public static void validate(List<Map<String, Object>> blocks, Map<String, Object> style, Collection<String> allowed, Collection<String> required) {
        List<String> errors = new ArrayList<>();
        if (blocks == null || blocks.isEmpty()) {
            throw BadRequestException.coded("MODELE_INVALIDE", "Modèle invalide : aucun bloc");
        }
        if (blocks.size() > 30) {
            errors.add("trente blocs au plus");
        }
        String tone = style == null || style.get("tone") == null ? "" : style.get("tone").toString();
        if (!TONES.contains(tone)) {
            errors.add("ton inconnu « " + tone + " » (vide, warn ou alert)");
        }
        long subjects = blocks.stream().filter(block -> "SUBJECT".equals(type(block))).count();
        if (subjects != 1) {
            errors.add("un et un seul bloc SUBJECT (objet du courriel)");
        }
        Set<String> used = new LinkedHashSet<>();
        for (int index = 0; index < blocks.size(); index++) {
            Map<String, Object> block = blocks.get(index);
            String where = "bloc " + (index + 1);
            String type = type(block);
            if (!BLOCK_TYPES.contains(type)) {
                errors.add(where + " : type inconnu « " + block.get("type") + " »");
                continue;
            }
            block.keySet().stream().filter(option -> !OPTIONS.get(type).contains(option))
                    .forEach(option -> errors.add(where + " (" + type + ") : option inconnue « " + option + " »"));
            for (String text : texts(block)) {
                if (text.length() > 2000) {
                    errors.add(where + " : texte trop long (2000 caractères au plus)");
                }
                Matcher matcher = TOKEN.matcher(text);
                while (matcher.find()) {
                    used.add(matcher.group(1));
                }
                if (text.contains("{{") && text.replaceAll(TOKEN.pattern(), "").contains("{{")) {
                    errors.add(where + " : jeton mal formé dans « " + abbreviate(text) + " »");
                }
            }
            if ("TEXT".equals(type) && !TONES.contains(String.valueOf(block.getOrDefault("tone", "")))) {
                errors.add(where + " : ton inconnu");
            }
            if ("BUTTON".equals(type)) {
                String url = String.valueOf(block.getOrDefault("url", "")).trim();
                if (!(TOKEN.matcher(url).matches() || URL.matcher(url).matches())) {
                    errors.add(where + " : lien attendu sous la forme https://... ou {{variable}}");
                }
            }
            if ("DETAILS".equals(type)) {
                Object rows = block.get("rows");
                if (!(rows instanceof Collection<?> list) || list.isEmpty() || list.size() > 20) {
                    errors.add(where + " : de une à vingt lignes {label, value}");
                }
            }
        }
        for (String token : used) {
            for (String segment : token.split("\\.")) {
                if (FORBIDDEN.contains(segment.toLowerCase())) {
                    errors.add("jeton interdit « " + token + " »");
                }
            }
            if (allowed != null && !allowed.isEmpty() && !allowed.contains(token)) {
                errors.add("jeton inconnu de ce courriel « " + token + " »");
            }
        }
        if (required != null) {
            required.stream().filter(token -> !token.isBlank() && !used.contains(token))
                    .forEach(token -> errors.add("jeton obligatoire absent « {{" + token + "}} »"));
        }
        if (!errors.isEmpty()) {
            throw BadRequestException.coded("MODELE_INVALIDE", "Modèle de courriel invalide : " + String.join(" ; ", errors));
        }
    }

    // ------------------------------------------------------------------ generation

    /** Gabarit Thymeleaf du courriel ; suppose le contenu controle par {@link #validate}. */
    public static String generate(List<Map<String, Object>> blocks, Map<String, Object> style) {
        String tone = style == null || style.get("tone") == null ? "" : style.get("tone").toString();
        if (!TONES.contains(tone)) {
            tone = "";
        }
        StringBuilder body = new StringBuilder();
        String subject = "";
        String preheader = "";
        for (Map<String, Object> block : blocks) {
            switch (type(block)) {
                case "SUBJECT" -> subject = inline(text(block, "text"));
                case "PREHEADER" -> preheader = inline(text(block, "text"));
                case "TITLE" -> body.append("<div class=\"em-title\">").append(inline(text(block, "text"))).append("</div>\n");
                case "GREETING" -> body.append("<div class=\"em-intro\">").append(inline(text(block, "text"))).append("</div>\n");
                case "TEXT" -> {
                    String blockTone = String.valueOf(block.getOrDefault("tone", ""));
                    String css = blockTone.isEmpty() ? "em-intro" : "em-note " + (TONES.contains(blockTone) ? blockTone : "");
                    body.append("<div class=\"").append(css.trim()).append("\">").append(paragraphs(text(block, "text"))).append("</div>\n");
                }
                case "DETAILS" -> {
                    String title = text(block, "title");
                    if (!title.isBlank()) {
                        body.append("<div class=\"section-lbl\">").append(inline(title)).append("</div>\n");
                    }
                    body.append("<table role=\"presentation\" class=\"rows\">\n");
                    for (Object item : (Collection<?>) block.getOrDefault("rows", List.of())) {
                        if (item instanceof Map<?, ?> row) {
                            body.append("<tr><td class=\"r-key\">").append(inline(String.valueOf(row.get("label") == null ? "" : row.get("label"))))
                                    .append("</td><td class=\"r-val\">").append(inline(String.valueOf(row.get("value") == null ? "" : row.get("value"))))
                                    .append("</td></tr>\n");
                        }
                    }
                    body.append("</table>\n");
                }
                case "AMOUNT" -> body.append("<div class=\"section-lbl\">").append(inline(text(block, "label"))).append("</div><div class=\"amount\">")
                        .append(inline(text(block, "value"))).append("</div>\n");
                case "BUTTON" -> {
                    String url = text(block, "url").trim();
                    Matcher token = TOKEN.matcher(url);
                    String href = token.matches() ? "th:href=\"${" + safe(token.group(1)) + "}\"" : "href=\"" + escape(url) + "\"";
                    body.append("<a class=\"cta\" ").append(href).append(">").append(inline(text(block, "label"))).append("</a>\n");
                }
                case "ATTACHMENT_NOTE" -> body.append("<div class=\"em-note\">").append(inline(text(block, "text"))).append("</div>\n");
                case "SIGNATURE" -> body.append("<div class=\"em-intro\" style=\"margin-top:22px\">").append(paragraphs(text(block, "text")))
                        .append("</div>\n");
                case "LEGAL" -> body.append("<div th:replace=\"~{email/fragments/notice-layout :: legal}\"></div>\n");
                default -> {
                }
            }
        }
        return "<!DOCTYPE html>\n<html lang=\"fr\" xmlns:th=\"http://www.thymeleaf.org\">\n"
                + "<head th:replace=\"~{email/fragments/notice-layout :: head('Message')}\"></head>\n<body>\n"
                + "<div id=\"" + SUBJECT_ID + "\" style=\"display:none\">" + subject + "</div>\n"
                + "<div style=\"display:none; font-size:1px; color:#ffffff; line-height:1px; max-height:0; max-width:0; opacity:0; overflow:hidden;\">"
                + preheader + "</div>\n"
                + "<div class=\"wrap\"><div class=\"sheet\">\n"
                + "<div th:replace=\"~{email/fragments/notice-layout :: header('" + tone + "')}\"></div>\n"
                + "<div class=\"em-body\">\n" + body + "</div>\n"
                + "<div th:replace=\"~{email/fragments/notice-layout :: footer}\"></div>\n"
                + "</div></div>\n</body>\n</html>\n";
    }

    /** Variables lues par un gabarit Thymeleaf d'origine : ce sont les jetons permis (3 niveaux au plus). */
    public static Set<String> variables(String templateSource) {
        Set<String> found = new java.util.TreeSet<>();
        if (templateSource == null) {
            return found;
        }
        Matcher matcher = VARIABLE.matcher(templateSource);
        while (matcher.find()) {
            String path = matcher.group(1);
            String[] parts = path.split("\\.");
            StringBuilder prefix = new StringBuilder();
            for (String part : parts) {
                prefix.append(prefix.isEmpty() ? "" : ".").append(part);
                found.add(prefix.toString());
            }
        }
        return found;
    }

    /** Valeurs d'exemple « ‹chemin› » pour l'apercu, en cartes imbriquees. */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> sampleVariables(Collection<String> paths) {
        Map<String, Object> root = new LinkedHashMap<>();
        List<String> sorted = new ArrayList<>(paths);
        sorted.sort((a, b) -> Integer.compare(b.split("\\.").length, a.split("\\.").length));
        for (String path : sorted) {
            String[] parts = path.split("\\.");
            Map<String, Object> current = root;
            for (int index = 0; index < parts.length - 1; index++) {
                Object next = current.get(parts[index]);
                if (!(next instanceof Map)) {
                    next = new LinkedHashMap<String, Object>();
                    current.put(parts[index], next);
                }
                current = (Map<String, Object>) next;
            }
            current.putIfAbsent(parts[parts.length - 1], "‹" + path + "›");
        }
        return root;
    }

    // ------------------------------------------------------------------ outils

    private static String type(Map<String, Object> block) {
        return block.get("type") == null ? "" : block.get("type").toString().toUpperCase();
    }

    private static String text(Map<String, Object> block, String key) {
        Object value = block.get(key);
        return value == null ? "" : value.toString();
    }

    private static List<String> texts(Map<String, Object> block) {
        List<String> texts = new ArrayList<>();
        for (String key : List.of("text", "title", "label", "value", "url")) {
            if (block.get(key) != null) {
                texts.add(block.get(key).toString());
            }
        }
        if (block.get("rows") instanceof Collection<?> rows) {
            for (Object row : rows) {
                if (row instanceof Map<?, ?> map) {
                    texts.add(String.valueOf(map.get("label") == null ? "" : map.get("label")));
                    texts.add(String.valueOf(map.get("value") == null ? "" : map.get("value")));
                }
            }
        }
        return texts;
    }

    /** Texte echappe, jetons remplaces par une lecture de variable ; retours a la ligne conserves. */
    static String inline(String text) {
        StringBuilder out = new StringBuilder();
        Matcher matcher = TOKEN.matcher(text);
        int last = 0;
        while (matcher.find()) {
            out.append(escape(text.substring(last, matcher.start())));
            out.append("<th:block th:text=\"${").append(safe(matcher.group(1))).append("}\"></th:block>");
            last = matcher.end();
        }
        out.append(escape(text.substring(last)));
        return out.toString().replace("\n", "<br/>");
    }

    private static String paragraphs(String text) {
        return inline(text);
    }

    /** {@code a.b.c} devient {@code a?.b?.c} : une variable absente donne un texte vide. */
    private static String safe(String path) {
        return path.replace(".", "?.");
    }

    /** Echappement HTML, crochets et accolades compris (aucune expression en ligne possible). */
    static String escape(String text) {
        StringBuilder out = new StringBuilder(text.length());
        for (char c : text.toCharArray()) {
            switch (c) {
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '&' -> out.append("&amp;");
                case '"' -> out.append("&quot;");
                case '\'' -> out.append("&#39;");
                case '[' -> out.append("&#91;");
                case ']' -> out.append("&#93;");
                case '$' -> out.append("&#36;");
                case '{' -> out.append("&#123;");
                case '}' -> out.append("&#125;");
                case '|' -> out.append("&#124;");
                default -> out.append(c);
            }
        }
        return out.toString();
    }

    private static String abbreviate(String text) {
        return text.length() <= 40 ? text : text.substring(0, 40) + "…";
    }
}
