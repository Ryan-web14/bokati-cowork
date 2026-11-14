package com.sni.bokaticowork.features.fund.voucher;

import org.springframework.beans.factory.annotation.Autowired;
import com.sni.bokaticowork.core.settings.platform.PlatformSettings;
import com.sni.bokaticowork.core.identity.OperatorIdentityService;
import com.sni.bokaticowork.core.utils.BrandLogo;
import com.sni.bokaticowork.core.utils.pdf.PdfRenderer;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.generator.qrcode.QrCodeRenderer;
import com.sni.bokaticowork.features.billing.service.support.FrenchAmountWords;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Pieces comptables emises par le logiciel (lot F7, plan {@code pieces-comptables.md}) : numero
 * sans trou par type et par mois ({@code PREFIXE-AAAAMM-000001}), attribue a la premiere emission
 * dans la transaction en cours ; reimpressions tracees et marquees DUPLICATA ; QR de verification.
 */
@Service
@RequiredArgsConstructor
public class VoucherService {

    static final ZoneId ZONE = ZoneId.of("Africa/Brazzaville");
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZONE);

    private final DataSource dataSource;
    private final SpringTemplateEngine templateEngine;
    private final QrCodeRenderer qrCodeRenderer;

    @Value("${fund.voucher.issuer-name:ELLE A OSE}")
    private String issuerName;
    @Value("${fund.voucher.issuer-details:}")
    private String issuerDetails;
    @Value("${app.verify-base-url:https://api.elleaose.com}")
    private String verifyBaseUrl;

    private static final String VOUCHER_ISSUER_NAME_KEY = "fund.voucher.issuer-name";
    private static final String VOUCHER_ISSUER_DETAILS_KEY = "fund.voucher.issuer-details";
    private static final String APP_VERIFY_BASE_URL_KEY = "app.verify-base-url";

    private PlatformSettings platformSettings;
    private OperatorIdentityService operatorIdentityService;
    private BrandLogo brandLogo;
    private com.sni.bokaticowork.features.document.template.service.DocumentTemplatePrinter templatePrinter;

    @Autowired(required = false)
    void setTemplatePrinter(com.sni.bokaticowork.features.document.template.service.DocumentTemplatePrinter templatePrinter) {
        this.templatePrinter = templatePrinter;
    }

    /** Facultatifs · injectes par mutateur pour ne pas toucher au constructeur des appelants. */
    @Autowired(required = false)
    void setOperatorIdentityService(OperatorIdentityService operatorIdentityService) {
        this.operatorIdentityService = operatorIdentityService;
    }

    @Autowired(required = false)
    void setBrandLogo(BrandLogo brandLogo) {
        this.brandLogo = brandLogo;
    }

    @Autowired(required = false)
    void setPlatformSettings(PlatformSettings platformSettings) {
        this.platformSettings = platformSettings;
    }

    private String issuerName() {
        return platformSettings == null ? issuerName : platformSettings.text(VOUCHER_ISSUER_NAME_KEY, issuerName);
    }

    private String issuerDetails() {
        return platformSettings == null ? issuerDetails : platformSettings.text(VOUCHER_ISSUER_DETAILS_KEY, issuerDetails);
    }

    private String verifyBaseUrl() {
        String value = platformSettings == null ? null : platformSettings.text(APP_VERIFY_BASE_URL_KEY, verifyBaseUrl);
        if (value == null || value.isBlank()) {
            value = verifyBaseUrl;
        }
        return value == null || value.isBlank() ? "https://api.elleaose.com" : value;
    }

    /** Piece enregistree. */
    public record Issued(String number, String type, String sourceCode, BigDecimal amount, String label, String issuedBy, Instant issuedAt,
                         int printCount, Instant lastPrintedAt, String cancelledBy) {
    }

    /**
     * Numero de la piece du fait (cree a la premiere emission, sans trou), et trace de l'impression.
     * Doit etre appele dans une transaction : un echec annule aussi le numero.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Issued issueAndPrint(String type, String sourceType, String sourceCode, BigDecimal amount, String label, String actor) {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        List<Map<String, Object>> existing = jdbc.queryForList("SELECT * FROM acc_voucher WHERE voucher_type = ? AND source_code = ?", type, sourceCode);
        if (existing.isEmpty()) {
            String period = Instant.now().atZone(ZONE).format(DateTimeFormatter.ofPattern("yyyyMM"));
            jdbc.update("INSERT INTO acc_voucher_counter (voucher_type, period, last_number) VALUES (?, ?, 0) ON CONFLICT DO NOTHING", type, period);
            Integer last = jdbc.queryForObject("SELECT last_number FROM acc_voucher_counter WHERE voucher_type = ? AND period = ? FOR UPDATE",
                    Integer.class, type, period);
            int next = (last == null ? 0 : last) + 1;
            jdbc.update("UPDATE acc_voucher_counter SET last_number = ? WHERE voucher_type = ? AND period = ?", next, type, period);
            String number = type + "-" + period + "-" + String.format("%06d", next);
            jdbc.update("INSERT INTO acc_voucher (id, voucher_type, number, source_type, source_code, amount, label, issued_by) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE, type, number, sourceType, sourceCode, amount, label == null ? null : label.length() > 500 ? label.substring(0, 500) : label, actor);
            existing = jdbc.queryForList("SELECT * FROM acc_voucher WHERE voucher_type = ? AND source_code = ?", type, sourceCode);
        }
        Map<String, Object> row = existing.get(0);
        Issued before = toIssued(row);
        jdbc.update("UPDATE acc_voucher SET print_count = print_count + 1, last_printed_at = now(), last_printed_by = ? WHERE number = ?",
                actor, before.number());
        return before;
    }

    /** Lien d'annulation : l'originale portera « ANNULEE PAR » a sa reimpression. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void markCancelled(String type, String sourceCode, String cancellingNumber) {
        new JdbcTemplate(dataSource).update("UPDATE acc_voucher SET cancelled_by_number = ? WHERE voucher_type = ? AND source_code = ?",
                cancellingNumber, type, sourceCode);
    }

    public Optional<Issued> find(String number) {
        List<Map<String, Object>> rows = new JdbcTemplate(dataSource).queryForList("SELECT * FROM acc_voucher WHERE number = ?", number);
        return rows.isEmpty() ? Optional.empty() : Optional.of(toIssued(rows.get(0)));
    }

    private static Issued toIssued(Map<String, Object> row) {
        return new Issued((String) row.get("number"), (String) row.get("voucher_type"), (String) row.get("source_code"),
                (BigDecimal) row.get("amount"), (String) row.get("label"), (String) row.get("issued_by"), instant(row.get("issued_at")),
                ((Number) row.get("print_count")).intValue(), instant(row.get("last_printed_at")), (String) row.get("cancelled_by_number"));
    }

    private static Instant instant(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof java.sql.Timestamp timestamp) {
            return timestamp.toInstant();
        }
        if (value instanceof java.time.OffsetDateTime offset) {
            return offset.toInstant();
        }
        return Instant.parse(value.toString());
    }

    // ------------------------------------------------------------------ rendu

    /** Section d'une piece : paires libelle-valeur, ou tableau. */
    public static final class Section {
        private final String title;
        private final List<String[]> pairs = new ArrayList<>();
        private List<String> columns = List.of();
        private List<Boolean> numeric = List.of();
        private final List<List<String>> rows = new ArrayList<>();

        public Section(String title) {
            this.title = title;
        }

        public Section pair(String label, Object value) {
            if (value instanceof java.time.LocalDate day) {
                value = day.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            }
            if (value != null && !value.toString().isBlank()) {
                pairs.add(new String[]{label, value.toString()});
            }
            return this;
        }

        public Section columns(List<String> columns, List<Boolean> numeric) {
            this.columns = columns;
            this.numeric = numeric;
            return this;
        }

        public Section row(Object... cells) {
            rows.add(Arrays.stream(cells).map(cell -> cell == null ? "" : cell.toString()).toList());
            return this;
        }

        public String getTitle() {
            return title;
        }

        public List<String[]> getPairs() {
            return pairs;
        }

        /** Deux paires par ligne. */
        public List<List<String[]>> getPairRows() {
            List<List<String[]>> result = new ArrayList<>();
            for (int index = 0; index < pairs.size(); index += 2) {
                result.add(pairs.subList(index, Math.min(index + 2, pairs.size())));
            }
            return result;
        }

        public List<String> getColumns() {
            return columns;
        }

        public List<Boolean> getNumeric() {
            return numeric;
        }

        public List<List<String>> getRows() {
            return rows;
        }
    }

    public record Signature(String role, String signedBy, String signedAt) {
    }

    /** Modele d'une piece. */
    public record View(String title, String number, String issuerName, String issuerDetails, boolean duplicate, String printedAtLabel,
                       String cancelledBy, List<Section> sections, String amountLabel, String amountWords, String commitment,
                       List<Signature> signatures, String issuedAtLabel, String issuedBy, String generatedAtLabel, String generationId,
                       String verifyUrl, String qrDataUri) {
    }

    /** Modele de document de chaque type de piece (chantier des modeles de documents). */
    public static final Map<String, String> TEMPLATE_BY_TYPE = Map.ofEntries(
            Map.entry("EDB", "NEED_STATEMENT"), Map.entry("DDF", "FUND_REQUEST"), Map.entry("BDF", "DISBURSEMENT_VOUCHER"),
            Map.entry("JUS", "JUSTIFICATION_SHEET"), Map.entry("BC", "PURCHASE_ORDER"), Map.entry("DTV", "VAT_RETURN"),
            Map.entry("RAP", "BANK_RECONCILIATION"), Map.entry("EMP", "LOAN_SCHEDULE"), Map.entry("BRB", "BANK_DEPOSIT_SLIP"),
            Map.entry("RLF", "SUPPLIER_STATEMENT"), Map.entry("RLP", "PARTNER_STATEMENT"));

    /**
     * Capture des donnees d'une piece pour l'apercu d'un modele (lot D4) : pendant {@link #capturing},
     * {@link #render} rend des octets vides et garde les donnees de la piece. L'appelant execute
     * l'impression dans une transaction annulee : numero et trace d'impression ne sont pas gardes.
     */
    private static final ThreadLocal<com.sni.bokaticowork.features.document.template.render.DocumentData[]> CAPTURE = new ThreadLocal<>();

    public static com.sni.bokaticowork.features.document.template.render.DocumentData capturing(Runnable print) {
        com.sni.bokaticowork.features.document.template.render.DocumentData[] holder =
                new com.sni.bokaticowork.features.document.template.render.DocumentData[1];
        CAPTURE.set(holder);
        try {
            print.run();
        } finally {
            CAPTURE.remove();
        }
        if (holder[0] == null) {
            throw new BadRequestException("La pièce n'a pas été produite");
        }
        return holder[0];
    }

    /**
     * Rend la piece : avec la version publiee de son modele s'il en existe une, sinon (ou si ce
     * rendu echoue) avec le gabarit d'origine {@code fund/voucher}.
     */
    public byte[] render(String title, Issued issued, List<Section> sections, BigDecimal amount, String commitment, List<Signature> signatures) {
        View view = view(title, issued, sections, amount, commitment, signatures);
        String templateCode = issued == null ? null : TEMPLATE_BY_TYPE.get(issued.type());
        com.sni.bokaticowork.features.document.template.render.DocumentData[] capture = CAPTURE.get();
        if (capture != null) {
            capture[0] = com.sni.bokaticowork.features.document.template.render.DocumentData
                    .fromVoucher(templateCode == null ? "CAPTURE" : templateCode, view, amount).source(issued.sourceCode());
            return new byte[0];
        }
        if (templatePrinter != null && templateCode != null) {
            Optional<byte[]> printed = templatePrinter.tryRender(templateCode,
                    com.sni.bokaticowork.features.document.template.render.DocumentData.fromVoucher(templateCode, view, amount).source(issued.sourceCode()));
            if (printed.isPresent()) {
                return printed.get();
            }
        }
        Context context = new Context(Locale.FRENCH);
        context.setVariable("v", view);
        context.setVariable("operator", operatorIdentityService == null ? null : operatorIdentityService.current());
        context.setVariable("logo", brandLogo == null ? null : brandLogo.base64());
        return pdf(templateEngine.process("fund/voucher", context));
    }

    /** Vue de la piece, sans rendu : sert au gabarit d'origine, aux modeles et a leur apercu. */
    public View view(String title, Issued issued, List<Section> sections, BigDecimal amount, String commitment, List<Signature> signatures) {
        // Seule une piece enregistree (acc_voucher) se verifie · un brouillon ou un tableau d'emprunt
        // porte un pseudo-numero que /verify/voucher ne connait pas : son QR menait a une erreur 404.
        String verifyUrl = isRegistered(issued) ? verifyBaseUrl().replaceAll("/+$", "") + "/verify/voucher/" + issued.number() : null;
        return new View(title, issued.number(), issuerName(), issuerDetails(), issued.printCount() > 0,
                STAMP.format(Instant.now()), issued.cancelledBy(), sections,
                amount == null ? null : money(amount) + " XAF",
                amount == null ? null : FrenchAmountWords.withCurrency(amount, "XAF"), commitment, signatures,
                issued.issuedAt() == null ? "" : STAMP.format(issued.issuedAt()), issued.issuedBy(), STAMP.format(Instant.now()),
                UUID.randomUUID().toString().substring(0, 8), verifyUrl, verifyUrl == null ? null : qrCodeRenderer.toDataUri(verifyUrl, 160));
    }

    /**
     * Piece deja enregistree pour ce fait, ou piece provisoire « APERÇU » : rien n'est ecrit, ni
     * numero ni impression (apercu d'un modele avec une piece reelle).
     */
    public Issued peek(String type, String sourceCode, BigDecimal amount, String label, String actor) {
        List<Map<String, Object>> existing = new JdbcTemplate(dataSource)
                .queryForList("SELECT * FROM acc_voucher WHERE voucher_type = ? AND source_code = ?", type, sourceCode);
        if (!existing.isEmpty()) {
            Issued issued = toIssued(existing.get(0));
            return new Issued(issued.number(), issued.type(), issued.sourceCode(), issued.amount(), issued.label(), issued.issuedBy(),
                    issued.issuedAt(), 0, issued.lastPrintedAt(), issued.cancelledBy());
        }
        return new Issued("APERÇU", type, sourceCode, amount, label, actor, Instant.now(), 0, null, null);
    }

    /** Vrai si le numero figure au registre des pieces, donc si /verify/voucher le connait. */
    private boolean isRegistered(Issued issued) {
        if (issued == null || issued.number() == null) {
            return false;
        }
        try {
            return find(issued.number()).isPresent();
        } catch (RuntimeException ex) {
            return false;
        }
    }

    public static String money(BigDecimal amount) {
        if (amount == null) {
            return "";
        }
        java.text.NumberFormat format = java.text.NumberFormat.getIntegerInstance(Locale.FRENCH);
        return format.format(amount.setScale(0, java.math.RoundingMode.HALF_UP)).replace(' ', ' ').replace(' ', ' ');
    }

    public static String date(Instant instant) {
        return instant == null ? "" : STAMP.format(instant);
    }

    private static byte[] pdf(String html) {
        try {
            return PdfRenderer.render(html);
        } catch (Exception exception) {
            throw new BadRequestException("Impossible de produire la piece PDF : " + exception.getMessage(), exception);
        }
    }
}
