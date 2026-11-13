package com.sni.bokaticowork.features.document.template.render;

import com.sni.bokaticowork.features.fund.voucher.VoucherService;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Donnees d'une piece a imprimer, independantes de sa mise en page (chantier des modeles de
 * documents, lot D2). Les blocs d'un modele designent les champs, tableaux et signataires par leur
 * cle ; une cle absente s'imprime vide, sans faire echouer le rendu.
 *
 * <p>Les cles sont des noms en minuscules sans accents ({@code date_souhaitee}). Pour les pieces
 * de fonds et de comptabilite, elles derivent des libelles des sections de {@link VoucherService}
 * ({@link #slug(String)}) : la meme piece donne donc toujours les memes cles.</p>
 */
public final class DocumentData {

    public record Field(String label, String value) {
    }

    public record Column(String key, String label, boolean numeric) {
    }

    public record Table(String title, List<Column> columns, List<List<String>> rows) {
    }

    public record Signer(String role, String name, String date) {
    }

    private final String code;
    private String title;
    private String number;
    private String currency = "XAF";
    private BigDecimal amount;
    private boolean duplicate;
    private String duplicateLabel;
    private String cancelledBy;
    private String verifyUrl;
    private String qrDataUri;
    private String issuedAt;
    private String issuedBy;
    private final Map<String, Field> fields = new LinkedHashMap<>();
    private final Map<String, Table> tables = new LinkedHashMap<>();
    private final Map<String, Signer> signers = new LinkedHashMap<>();

    public DocumentData(String code) {
        this.code = code;
    }

    /** Donnees d'une piece de fonds ou de comptabilite, a partir de la vue de {@link VoucherService}. */
    public static DocumentData fromVoucher(String code, VoucherService.View view, BigDecimal amount) {
        DocumentData data = new DocumentData(code).title(view.title()).number(view.number()).amount(amount, "XAF");
        data.duplicate = view.duplicate();
        data.duplicateLabel = view.duplicate() ? "DUPLICATA, réimpression du " + view.printedAtLabel() : null;
        data.cancelledBy = view.cancelledBy();
        data.verifyUrl = view.verifyUrl();
        data.qrDataUri = view.qrDataUri();
        data.issuedAt = view.issuedAtLabel();
        data.issuedBy = view.issuedBy();
        for (VoucherService.Section section : view.sections()) {
            String sectionKey = slug(section.getTitle());
            for (String[] pair : section.getPairs()) {
                String key = slug(pair[0]);
                if (data.fields.containsKey(key)) {
                    key = sectionKey + "_" + key;
                }
                data.fields.putIfAbsent(key, new Field(pair[0], pair[1]));
            }
            if (section.getColumns() != null && !section.getColumns().isEmpty()) {
                List<Column> columns = new ArrayList<>();
                for (int index = 0; index < section.getColumns().size(); index++) {
                    String label = section.getColumns().get(index);
                    boolean numeric = index < section.getNumeric().size() && Boolean.TRUE.equals(section.getNumeric().get(index));
                    columns.add(new Column(slug(label), label, numeric));
                }
                data.tables.put(sectionKey, new Table(section.getTitle(), columns, section.getRows()));
            }
        }
        if (view.commitment() != null) {
            data.field("engagement", "Engagement", view.commitment());
        }
        for (VoucherService.Signature signature : view.signatures()) {
            String role = signature.role();
            int colon = role.indexOf(" : ");
            String key = slug(colon > 0 ? role.substring(0, colon) : role);
            data.signers.putIfAbsent(key, new Signer(role, signature.signedBy(), signature.signedAt()));
        }
        return data;
    }

    // ------------------------------------------------------------------ construction

    public DocumentData title(String title) {
        this.title = title;
        return this;
    }

    public DocumentData number(String number) {
        this.number = number;
        return this;
    }

    public DocumentData amount(BigDecimal amount, String currency) {
        this.amount = amount;
        if (currency != null && !currency.isBlank()) {
            this.currency = currency;
        }
        return this;
    }

    public DocumentData duplicate(boolean duplicate, String label) {
        this.duplicate = duplicate;
        this.duplicateLabel = duplicate ? (label == null ? "DUPLICATA" : label) : null;
        return this;
    }

    public DocumentData cancelledBy(String cancelledBy) {
        this.cancelledBy = cancelledBy;
        return this;
    }

    public DocumentData verify(String url, String qrDataUri) {
        this.verifyUrl = url;
        this.qrDataUri = qrDataUri;
        return this;
    }

    public DocumentData issued(String at, String by) {
        this.issuedAt = at;
        this.issuedBy = by;
        return this;
    }

    /** Champ ; une valeur vide ou nulle est gardee (le modele imprime alors une case vide). */
    public DocumentData field(String key, String label, Object value) {
        fields.put(key, new Field(label, value == null ? "" : value.toString()));
        return this;
    }

    public DocumentData table(String key, Table table) {
        tables.put(key, table);
        return this;
    }

    public DocumentData signer(String key, String role, String name, String date) {
        signers.put(key, new Signer(role, name, date));
        return this;
    }

    // ------------------------------------------------------------------ lecture

    public String code() {
        return code;
    }

    public String title() {
        return title;
    }

    public String number() {
        return number;
    }

    public String currency() {
        return currency;
    }

    public BigDecimal amount() {
        return amount;
    }

    public boolean duplicate() {
        return duplicate;
    }

    public String duplicateLabel() {
        return duplicateLabel;
    }

    public String cancelledBy() {
        return cancelledBy;
    }

    public String verifyUrl() {
        return verifyUrl;
    }

    public String qrDataUri() {
        return qrDataUri;
    }

    public String issuedAt() {
        return issuedAt;
    }

    public String issuedBy() {
        return issuedBy;
    }

    public Map<String, Field> fields() {
        return Collections.unmodifiableMap(fields);
    }

    public Map<String, Table> tables() {
        return Collections.unmodifiableMap(tables);
    }

    public Map<String, Signer> signers() {
        return Collections.unmodifiableMap(signers);
    }

    /**
     * Valeur d'une cle pour les jetons {@code {{cle}}} : un champ, ou une donnee commune de la
     * piece ({@code numero}, {@code titre}, {@code montant}, {@code date_emission}, {@code emis_par}).
     */
    public String value(String key) {
        if (key == null) {
            return "";
        }
        Field field = fields.get(key);
        if (field != null) {
            return field.value() == null ? "" : field.value();
        }
        return switch (key) {
            case "numero" -> nullToEmpty(number);
            case "titre" -> nullToEmpty(title);
            case "montant" -> amount == null ? "" : VoucherService.money(amount) + " " + currency;
            case "date_emission" -> nullToEmpty(issuedAt);
            case "emis_par" -> nullToEmpty(issuedBy);
            default -> "";
        };
    }

    /** Libelle d'une cle de champ, ou la cle elle-meme. */
    public String label(String key) {
        Field field = fields.get(key);
        return field == null ? key : field.label();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    /** Cle d'un libelle : minuscules, sans accents, mots separes par un souligne. */
    public static String slug(String label) {
        if (label == null) {
            return "";
        }
        String plain = Normalizer.normalize(label, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return plain.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
    }
}
