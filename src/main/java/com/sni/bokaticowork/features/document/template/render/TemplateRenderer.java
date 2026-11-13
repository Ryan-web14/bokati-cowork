package com.sni.bokaticowork.features.document.template.render;

import com.sni.bokaticowork.core.identity.OperatorIdentity;
import com.sni.bokaticowork.core.utils.pdf.PdfRenderer;
import com.sni.bokaticowork.features.billing.service.support.FrenchAmountWords;
import com.sni.bokaticowork.features.fund.voucher.VoucherService;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.sni.bokaticowork.features.document.template.render.TemplateContent.flag;
import static com.sni.bokaticowork.features.document.template.render.TemplateContent.integer;
import static com.sni.bokaticowork.features.document.template.render.TemplateContent.list;
import static com.sni.bokaticowork.features.document.template.render.TemplateContent.text;

/**
 * Rendu d'un modele en blocs (lot D2) : chaque bloc est resolu ici en une vue simple (textes deja
 * calcules), puis le gabarit fixe {@code document-template/render} l'imprime avec {@code th:text}.
 * Aucun texte du modele n'est interprete comme du HTML ou une expression : les jetons
 * {@code {{cle}}} sont remplaces par la valeur du champ, echappee a l'impression.
 *
 * <p>Classe sans etat, construite avec le moteur Thymeleaf de l'application.</p>
 */
public final class TemplateRenderer {

    private static final Pattern TOKEN = Pattern.compile("\\{\\{\\s*([a-z0-9_.]+)\\s*}}");
    private static final String DEFAULT_ACCENT = "#c9a84c";

    private final TemplateEngine engine;

    public TemplateRenderer(TemplateEngine engine) {
        this.engine = engine;
    }

    // ------------------------------------------------------------------ vues lues par le gabarit

    public record Cell(String label, String value) {
    }

    public record Col(String label, boolean numeric, String width) {
    }

    public record Box(String label, String name, String note, boolean stamp) {
    }

    /** Un bloc resolu : seuls les champs de son type sont remplis. */
    public record BlockView(String type, String title, boolean flag, int columns, List<List<Cell>> rows, List<Col> cols,
                            List<List<String>> lines, String text, List<List<Box>> boxes, int width, int height, String label,
                            String amount, String words, String wordsLabel) {
    }

    public record CopyView(String label, boolean first, boolean pageBreak) {
    }

    public record View(String pageFormat, String accent, List<CopyView> copies, List<BlockView> blocks, DocumentData data,
                       String generatedAt) {
    }

    // ------------------------------------------------------------------ rendu

    public byte[] pdf(TemplateContent content, DocumentData data, OperatorIdentity operator, String logo) {
        return PdfRenderer.render(html(content, data, operator, logo));
    }

    public String html(TemplateContent content, DocumentData data, OperatorIdentity operator, String logo) {
        if (!data.fields().containsKey("exploitant")) {
            data.field("exploitant", "Exploitant", operator == null ? OperatorIdentity.DEFAULT_NAME : operator.name());
        }
        Context context = new Context(Locale.FRENCH);
        context.setVariable("t", view(content, data));
        context.setVariable("operator", operator);
        context.setVariable("logo", logo);
        return engine.process("document-template/render", context);
    }

    View view(TemplateContent content, DocumentData data) {
        List<BlockView> blocks = new ArrayList<>();
        int contentWidth = switch (content.pageFormat()) {
            case "A5_TWO_UP" -> 714;
            case "A6" -> 365;
            default -> 726;
        };
        for (Map<String, Object> block : content.blocks()) {
            blocks.add(resolve(block, data, contentWidth));
        }
        List<Map<String, Object>> copySpecs = content.copies().isEmpty() ? List.of(Map.of("label", "")) : content.copies();
        boolean twoUp = "A5_TWO_UP".equals(content.pageFormat());
        List<CopyView> copies = new ArrayList<>();
        for (int index = 0; index < copySpecs.size(); index++) {
            // Deux exemplaires par feuille en A5 : saut de page avant le troisieme.
            boolean pageBreak = index > 0 && (!twoUp || index % 2 == 0);
            // En majuscules ici : OpenHtmlToPDF mesure le cadre avant d'appliquer text-transform.
            copies.add(new CopyView(text(copySpecs.get(index).get("label")).toUpperCase(Locale.FRENCH), index == 0, pageBreak));
        }
        Object accent = content.style().get("accent");
        return new View(content.pageFormat(), accent == null ? DEFAULT_ACCENT : accent.toString(), copies, blocks, data,
                VoucherService.date(java.time.Instant.now()));
    }

    private BlockView resolve(Map<String, Object> block, DocumentData data, int contentWidth) {
        String type = text(block.get("type")).toUpperCase();
        String title = tokens(text(block.get("title")), data);
        return switch (type) {
            case "HEADER" -> simple(type, null, flag(block.get("qr"), true) && data.qrDataUri() != null);
            case "TITLE" -> new BlockView(type, title.isBlank() ? data.title() : title, flag(block.get("copyLabel"), true), 0, List.of(), List.of(),
                    List.of(), null, List.of(), 0, 0, null, null, null, null);
            case "FIELDS" -> fields(block, title, data);
            case "LINES" -> lines(block, title, data);
            case "TOTALS" -> totals(block, data);
            case "TEXT" -> new BlockView(type, title, flag(block.get("boxed"), false), 0, List.of(), List.of(), List.of(),
                    tokens(text(block.get("text")), data), List.of(), 0, 0, null, null, null, null);
            case "SIGNATURES" -> signatures(block, data, contentWidth);
            case "FOOTER" -> new BlockView(type, null, false, 0, List.of(), List.of(), List.of(), tokens(text(block.get("text")), data),
                    List.of(), 0, 0, null, null, null, null);
            default -> simple(type, title, false);
        };
    }

    private static BlockView simple(String type, String title, boolean flag) {
        return new BlockView(type, title, flag, 0, List.of(), List.of(), List.of(), null, List.of(), 0, 0, null, null, null, null);
    }

    private BlockView fields(Map<String, Object> block, String title, DocumentData data) {
        int columns = Math.max(1, Math.min(4, integer(block.get("columns"), 3)));
        List<Cell> cells = new ArrayList<>();
        for (Object item : list(block.get("fields"))) {
            String key;
            String label = null;
            if (item instanceof Map<?, ?> map) {
                key = text(map.get("key"));
                label = map.get("label") == null ? null : text(map.get("label"));
            } else {
                key = text(item);
            }
            cells.add(new Cell(label == null || label.isBlank() ? data.label(key) : label, data.value(key)));
        }
        List<List<Cell>> rows = new ArrayList<>();
        for (int index = 0; index < cells.size(); index += columns) {
            List<Cell> row = new ArrayList<>(cells.subList(index, Math.min(index + columns, cells.size())));
            while (row.size() < columns) {
                row.add(new Cell("", ""));
            }
            rows.add(row);
        }
        return new BlockView("FIELDS", title, false, columns, rows, List.of(), List.of(), null, List.of(), 0, 0, null, null, null, null);
    }

    private BlockView lines(Map<String, Object> block, String title, DocumentData data) {
        DocumentData.Table table = data.tables().get(text(block.get("table")));
        if (table == null) {
            return simple("LINES", title, false);
        }
        List<Integer> picked = new ArrayList<>();
        List<Col> cols = new ArrayList<>();
        List<?> wanted = list(block.get("columns"));
        if (wanted.isEmpty()) {
            for (int index = 0; index < table.columns().size(); index++) {
                picked.add(index);
                cols.add(new Col(table.columns().get(index).label(), table.columns().get(index).numeric(), null));
            }
        } else {
            for (Object item : wanted) {
                String key = item instanceof Map<?, ?> map ? text(map.get("key")) : text(item);
                for (int index = 0; index < table.columns().size(); index++) {
                    DocumentData.Column column = table.columns().get(index);
                    if (column.key().equals(key)) {
                        String label = item instanceof Map<?, ?> map && map.get("label") != null ? text(map.get("label")) : column.label();
                        String width = item instanceof Map<?, ?> map && map.get("width") != null ? integer(map.get("width"), 0) + "px" : null;
                        picked.add(index);
                        cols.add(new Col(label, column.numeric(), "0px".equals(width) ? null : width));
                    }
                }
            }
        }
        List<List<String>> rows = new ArrayList<>();
        for (List<String> row : table.rows()) {
            List<String> cells = new ArrayList<>();
            for (Integer index : picked) {
                cells.add(index < row.size() ? row.get(index) : "");
            }
            rows.add(cells);
        }
        return new BlockView("LINES", title.isBlank() ? table.title() : title, false, 0, List.of(), cols, rows, null, List.of(), 0, 0,
                null, null, null, null);
    }

    private BlockView totals(Map<String, Object> block, DocumentData data) {
        if (data.amount() == null) {
            return simple("TOTALS", null, false);
        }
        String label = text(block.get("label"));
        String wordsLabel = text(block.get("wordsLabel"));
        boolean words = flag(block.get("words"), true);
        return new BlockView("TOTALS", null, words, 0, List.of(), List.of(), List.of(), null, List.of(), 0, 0,
                label.isBlank() ? "Montant" : label, VoucherService.money(data.amount()) + " " + data.currency(),
                words ? FrenchAmountWords.withCurrency(data.amount(), data.currency()) : null,
                wordsLabel.isBlank() ? "Montant en lettres" : wordsLabel);
    }

    private BlockView signatures(Map<String, Object> block, DocumentData data, int contentWidth) {
        List<Box> boxes = new ArrayList<>();
        if (flag(block.get("fromData"), false)) {
            data.signers().values().forEach(signer -> boxes.add(new Box(signer.role(), signerLine(signer), null, false)));
        }
        for (Object item : list(block.get("boxes"))) {
            if (!(item instanceof Map<?, ?> map)) {
                continue;
            }
            DocumentData.Signer signer = map.get("signer") == null ? null : data.signers().get(text(map.get("signer")));
            String note = map.get("note") == null ? null : tokens(text(map.get("note")), data);
            boxes.add(new Box(tokens(text(map.get("label")), data), signer == null ? null : signerLine(signer), note,
                    flag(map.get("stamp"), false)));
        }
        int width = Math.max(80, Math.min(260, integer(block.get("width"), 160)));
        int height = Math.max(60, Math.min(260, integer(block.get("height"), 164)));
        // Autant de cases par rangee que la largeur utile en admet (8 px entre deux cases), comme
        // sur les maquettes ; les suivantes passent a la rangee suivante, centree elle aussi.
        int perRow = Math.max(1, (contentWidth + 8) / (width + 8));
        List<List<Box>> rows = new ArrayList<>();
        for (int index = 0; index < boxes.size(); index += perRow) {
            rows.add(new ArrayList<>(boxes.subList(index, Math.min(index + perRow, boxes.size()))));
        }
        return new BlockView("SIGNATURES", null, false, 0, List.of(), List.of(), List.of(), null, rows, width, height,
                null, null, null, null);
    }

    private static String signerLine(DocumentData.Signer signer) {
        if (signer.name() == null || signer.name().isBlank()) {
            return null;
        }
        return signer.date() == null || signer.date().isBlank() ? signer.name() : signer.name() + " · " + signer.date();
    }

    /** Remplace les jetons {@code {{cle}}} par la valeur de la piece ; un jeton inconnu devient vide. */
    static String tokens(String text, DocumentData data) {
        if (text == null || text.isEmpty()) {
            return text == null ? "" : text;
        }
        Matcher matcher = TOKEN.matcher(text);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(out, Matcher.quoteReplacement(data.value(matcher.group(1))));
        }
        matcher.appendTail(out);
        return out.toString();
    }
}
