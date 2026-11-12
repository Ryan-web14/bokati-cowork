package com.sni.bokaticowork.features.accounting.service.support;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lecture des releves OFX (1.x SGML et 2.x XML) et SWIFT MT940 (lot F10). Ces formats portent
 * eux-memes leurs dates, montants signes, references et soldes : ils ne demandent pas de modele.
 */
public final class BankFileParsers {

    private BankFileParsers() {
    }

    /** Ligne lue : date d'operation, date de valeur, libelle, reference, montant signe (entree positive). */
    public record Line(LocalDate date, LocalDate valueDate, String label, String reference, BigDecimal amount) {
    }

    /** Releve lu ; les soldes et la periode sont nuls quand le fichier ne les donne pas. */
    public record Parsed(String format, List<Line> lines, BigDecimal openingBalance, BigDecimal closingBalance, LocalDate from, LocalDate to) {
    }

    /** {@code OFX}, {@code MT940} ou {@code null} (fichier a lire par un modele CSV). */
    public static String detect(byte[] content) {
        String head = new String(content, 0, Math.min(content.length, 4096), StandardCharsets.ISO_8859_1).toUpperCase();
        if (head.contains("OFXHEADER") || head.contains("<OFX>")) {
            return "OFX";
        }
        if (head.contains(":20:") && (head.contains(":60F:") || head.contains(":60M:") || head.contains(":61:"))) {
            return "MT940";
        }
        return null;
    }

    public static Parsed parse(byte[] content) {
        String format = detect(content);
        if ("OFX".equals(format)) {
            return ofx(new String(content, StandardCharsets.ISO_8859_1));
        }
        if ("MT940".equals(format)) {
            return mt940(new String(content, StandardCharsets.ISO_8859_1));
        }
        throw new BadRequestException("Fichier ni OFX ni MT940");
    }

    // ------------------------------------------------------------------ OFX

    private static final Pattern TRANSACTION = Pattern.compile("<STMTTRN>(.*?)(</STMTTRN>|(?=<STMTTRN>)|(?=</BANKTRANLIST>))",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    static Parsed ofx(String text) {
        List<Line> lines = new ArrayList<>();
        Matcher matcher = TRANSACTION.matcher(text);
        while (matcher.find()) {
            String block = matcher.group(1);
            String amount = tag(block, "TRNAMT");
            String posted = tag(block, "DTPOSTED");
            if (amount == null || posted == null) {
                continue;
            }
            String name = tag(block, "NAME");
            String memo = tag(block, "MEMO");
            String label = (name == null ? "" : name) + (memo == null || memo.equals(name) ? "" : (name == null ? "" : " ") + memo);
            String reference = tag(block, "CHECKNUM") != null ? tag(block, "CHECKNUM") : tag(block, "REFNUM") != null ? tag(block, "REFNUM") : tag(block, "FITID");
            String user = tag(block, "DTUSER");
            lines.add(new Line(ofxDate(posted), user == null ? null : ofxDate(user), label.isBlank() ? "(sans libelle)" : label.trim(), reference,
                    new BigDecimal(amount.replace(",", "."))));
        }
        if (lines.isEmpty()) {
            throw new BadRequestException("Aucune operation (STMTTRN) dans le fichier OFX");
        }
        BigDecimal closing = null;
        int ledger = text.toUpperCase().indexOf("<LEDGERBAL>");
        if (ledger >= 0) {
            String amount = tag(text.substring(ledger), "BALAMT");
            closing = amount == null ? null : new BigDecimal(amount.replace(",", "."));
        }
        String start = tag(text, "DTSTART");
        String end = tag(text, "DTEND");
        BigDecimal sum = lines.stream().map(Line::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Parsed("OFX", lines, closing == null ? null : closing.subtract(sum), closing,
                start == null ? null : ofxDate(start), end == null ? null : ofxDate(end));
    }

    /** Valeur d'une balise OFX, fermee (XML) ou non (SGML). */
    private static String tag(String block, String name) {
        Matcher matcher = Pattern.compile("<" + name + ">\\s*([^<\\r\\n]*)", Pattern.CASE_INSENSITIVE).matcher(block);
        if (!matcher.find()) {
            return null;
        }
        String value = matcher.group(1).trim();
        return value.isEmpty() ? null : value.replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">");
    }

    private static LocalDate ofxDate(String value) {
        String digits = value.replaceAll("[^0-9].*$", "");
        if (digits.length() < 8) {
            throw new BadRequestException("Date OFX illisible : " + value);
        }
        return LocalDate.of(Integer.parseInt(digits.substring(0, 4)), Integer.parseInt(digits.substring(4, 6)), Integer.parseInt(digits.substring(6, 8)));
    }

    // ------------------------------------------------------------------ MT940

    /** :61:AAMMJJ[MMJJ]C|D|RC|RD[lettre devise]montant,dec Nxxx reference[//ref banque] */
    private static final Pattern STATEMENT_LINE = Pattern.compile(
            "^(\\d{6})(\\d{4})?(RC|RD|C|D)([A-Z])?(\\d+,\\d*)[A-Z]([A-Z0-9]{3})([^/\\r\\n]*)(//([^\\r\\n]*))?");
    private static final Pattern BALANCE = Pattern.compile("^(C|D)(\\d{6})([A-Z]{3})(\\d+,\\d*)");

    static Parsed mt940(String text) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = null;
        for (String raw : text.replace("\r", "").split("\n")) {
            if (raw.startsWith(":")) {
                if (current != null) {
                    fields.add(current.toString());
                }
                current = new StringBuilder(raw);
            } else if (current != null && !raw.startsWith("-}") && !raw.equals("-")) {
                current.append('\n').append(raw);
            }
        }
        if (current != null) {
            fields.add(current.toString());
        }
        List<Line> lines = new ArrayList<>();
        BigDecimal opening = null;
        BigDecimal closing = null;
        LocalDate from = null;
        LocalDate to = null;
        for (int index = 0; index < fields.size(); index++) {
            String field = fields.get(index);
            if ((field.startsWith(":60F:") || field.startsWith(":60M:")) && opening == null) {
                Matcher m = BALANCE.matcher(field.substring(5).trim());
                if (m.find()) {
                    opening = signed(m.group(1), m.group(4));
                    from = swiftDate(m.group(2));
                }
            } else if (field.startsWith(":62F:") || field.startsWith(":62M:")) {
                Matcher m = BALANCE.matcher(field.substring(5).trim());
                if (m.find()) {
                    closing = signed(m.group(1), m.group(4));
                    to = swiftDate(m.group(2));
                }
            } else if (field.startsWith(":61:")) {
                String body = field.substring(4).split("\n")[0].trim();
                Matcher m = STATEMENT_LINE.matcher(body);
                if (!m.find()) {
                    throw new BadRequestException("Ligne :61: illisible : " + body);
                }
                LocalDate valueDate = swiftDate(m.group(1));
                LocalDate entryDate = valueDate;
                if (m.group(2) != null) {
                    entryDate = LocalDate.of(valueDate.getYear(), Integer.parseInt(m.group(2).substring(0, 2)), Integer.parseInt(m.group(2).substring(2, 4)));
                    if (entryDate.isAfter(valueDate.plusMonths(6))) {
                        entryDate = entryDate.minusYears(1);
                    } else if (entryDate.isBefore(valueDate.minusMonths(6))) {
                        entryDate = entryDate.plusYears(1);
                    }
                }
                String mark = m.group(3);
                BigDecimal amount = new BigDecimal(m.group(5).replace(",", ".").replaceAll("\\.$", ""));
                boolean credit = mark.equals("C") || mark.equals("RD");
                String reference = m.group(7) == null ? null : m.group(7).trim();
                if ("NONREF".equalsIgnoreCase(reference)) {
                    reference = m.group(9) == null ? null : m.group(9).trim();
                }
                String label = "";
                if (index + 1 < fields.size() && fields.get(index + 1).startsWith(":86:")) {
                    label = fields.get(index + 1).substring(4).replace("\n", " ").replaceAll("\\?\\d{2}", " ").replaceAll("\\s+", " ").trim();
                }
                lines.add(new Line(entryDate, valueDate, label.isEmpty() ? (reference == null ? "(sans libelle)" : reference) : label,
                        reference == null || reference.isBlank() ? null : reference, credit ? amount : amount.negate()));
            }
        }
        if (lines.isEmpty()) {
            throw new BadRequestException("Aucune operation (:61:) dans le fichier MT940");
        }
        return new Parsed("MT940", lines, opening, closing, from, to);
    }

    private static BigDecimal signed(String mark, String amount) {
        BigDecimal value = new BigDecimal(amount.replace(",", ".").replaceAll("\\.$", ""));
        return "D".equals(mark) ? value.negate() : value;
    }

    private static LocalDate swiftDate(String yymmdd) {
        return LocalDate.of(2000 + Integer.parseInt(yymmdd.substring(0, 2)), Integer.parseInt(yymmdd.substring(2, 4)), Integer.parseInt(yymmdd.substring(4, 6)));
    }
}
