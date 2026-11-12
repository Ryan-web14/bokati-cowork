package com.sni.bokaticowork.templates;

import com.sni.bokaticowork.core.generator.qrcode.QrCodeRenderer;
import com.sni.bokaticowork.core.identity.OperatorIdentity;
import com.sni.bokaticowork.core.identity.OperatorIdentityService;
import com.sni.bokaticowork.core.utils.pdf.PdfRenderer;
import com.sni.bokaticowork.features.fund.voucher.VoucherService;
import com.sni.bokaticowork.features.payment.dto.response.CashMovementResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashRequestResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashSessionStatisticsResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashSessionSummaryResponse;
import com.sni.bokaticowork.features.payment.service.implementation.CashDocumentPdfServiceImpl;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.FileTemplateResolver;

import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Lot D1 · en-tete commun des pieces de caisse, des pieces de fonds et des rapports.
 *
 * <p>L'identite imprimee vient de la fiche exploitant (OperatorIdentity) ; la marque Bokati Cowork
 * ne doit plus apparaitre ; le rapport de cloture imprime enfin son numero de session et sa
 * caisse ; un tableau d'emprunt ou un brouillon ne porte plus de QR vers une verification qui
 * repondait 404.</p>
 */
class FinanceDocumentsBrandRenderTest {

    private static final OperatorIdentity OPERATOR = new OperatorIdentity("ELLE A OSE", "SARL", "M000111222333A",
            "CG-PNR-01-2017-A11-00472", "12 avenue de l'Essai, Pointe-Noire", "Pointe-Noire", "06 000 00 00",
            "contact@essai.cg", "Mme Hope MADHONE", "Gérante");

    @Test
    @DisplayName("Rapport de clôture : numéro de session et caisse imprimés, identité de l'exploitant, plus de Bokati Cowork")
    void closingReportPrintsSessionAndRegister() {
        CashSessionSummaryResponse summary = record(CashSessionSummaryResponse.class,
                Map.of("sessionNumber", "CSS-202611-000042", "registerCode", "CSR-00007"));
        Context context = context();
        context.setVariable("summary", summary);
        context.setVariable("statistics", record(CashSessionStatisticsResponse.class, Map.of()));
        context.setVariable("movements", List.of(record(CashMovementResponse.class, Map.of("movementNumber", "CSM-202611-000001"))));
        context.setVariable("reviewedBy", "superviseur");

        String html = engine().process("cash/session-closing-report", context);

        assertThat(html).contains("CSS-202611-000042").contains("Caisse CSR-00007").contains("CSM-202611-000001");
        assertBranded(html);
        assertThat(PdfRenderer.render(html)).isNotEmpty();
    }

    @Test
    @DisplayName("Pièce de mouvement de caisse : en-tête commun, plus de Bokati Cowork")
    void movementDocumentIsBranded() {
        Context context = context();
        context.setVariable("movement", record(CashMovementResponse.class, Map.of("movementNumber", "CSM-202611-000009")));
        context.setVariable("docTypeLabel", "Bon de sortie de caisse");
        context.setVariable("duplicate", false);
        context.setVariable("reversedBy", null);

        String html = engine().process("cash/movement-document", context);

        assertThat(html).contains("CSM-202611-000009").contains("Bon de sortie de caisse");
        assertBranded(html);
        assertThat(PdfRenderer.render(html)).isNotEmpty();
    }

    @Test
    @DisplayName("Demande de caisse : en-tête commun, plus de Bokati Cowork")
    void requestDocumentIsBranded() {
        Context context = context();
        context.setVariable("request", record(CashRequestResponse.class, Map.of("requestNumber", "CRQ-202611-000003")));
        context.setVariable("requestTypeLabel", "Demande de remise de fonds");
        context.setVariable("statusLabel", "Approuvée");

        String html = engine().process("cash/request-document", context);

        assertThat(html).contains("CRQ-202611-000003");
        assertBranded(html);
        assertThat(PdfRenderer.render(html)).isNotEmpty();
    }

    @Test
    @DisplayName("Sans fiche exploitant au contexte, l'en-tête retombe sur l'identité historique")
    void letterheadFallsBackWithoutOperator() {
        Context context = context();
        context.setVariable("operator", null);
        context.setVariable("request", record(CashRequestResponse.class, Map.of("requestNumber", "CRQ-1")));
        context.setVariable("requestTypeLabel", "Demande");
        context.setVariable("statusLabel", "");

        String html = engine().process("cash/request-document", context);

        assertThat(html).contains("ELLE A OSE").contains("RCCM CG-PNR-01-2017-A11-00472").doesNotContain("Bokati");
    }

    @Test
    @DisplayName("Pièce de fonds enregistrée : en-tête commun, titre accentué, QR de vérification")
    void registeredVoucherKeepsItsVerification() throws Exception {
        VoucherService vouchers = voucherService();
        VoucherService.Issued issued = issued("DTV-202611-000001");
        Mockito.doReturn(Optional.of(issued)).when(vouchers).find("DTV-202611-000001");

        String text = text(vouchers.render("Déclaration de TVA 2026-10", issued, List.of(new VoucherService.Section("Déclaration")
                .pair("Période", "2026-10")), new BigDecimal("1000"), null, List.of()));

        // Le titre est en capitales (text-transform) dans le PDF.
        assertThat(text).contains("DÉCLARATION DE TVA 2026-10").contains("12 avenue de l'Essai")
                .contains("RCCM CG-PNR-01-2017-A11-00472").contains("/verify/voucher/DTV-202611-000001");
    }

    @Test
    @DisplayName("Tableau d'emprunt (pseudo-numéro non enregistré) : plus de QR ni de lien de vérification")
    void unregisteredVoucherHasNoVerification() throws Exception {
        VoucherService vouchers = voucherService();
        VoucherService.Issued issued = issued("EMP-0001-V2");
        Mockito.doReturn(Optional.empty()).when(vouchers).find("EMP-0001-V2");

        String text = text(vouchers.render("Tableau d'amortissement EMP-0001", issued, List.of(), null, null, List.of()));

        assertThat(text).contains("TABLEAU D'AMORTISSEMENT EMP-0001").contains("12 avenue de l'Essai")
                .doesNotContain("Vérifier").doesNotContain("/verify/voucher");
    }

    @ParameterizedTest
    @DisplayName("Rapports : en-tête commun, plus d'adresse ni de RCCM en dur, plus de Bokati Cowork")
    @ValueSource(strings = {"activity-report", "debt-recovery-report", "financial-report", "member-profile-report",
            "occupancy-report", "resource-utilization-report", "stock-valuation-report", "subscription-report"})
    void reportsUseTheCommonLetterhead(String name) throws Exception {
        String source = Files.readString(Path.of("src/main/resources/templates/reporting/" + name + ".html"), StandardCharsets.UTF_8);
        assertThat(source).contains("fragments/document-brand :: styles").contains("fragments/document-brand :: letterhead")
                .contains("fragments/document-brand :: footer")
                .doesNotContain("Bokati").doesNotContain("CG-PNR-01-2017").doesNotContain("Boulevard du Général");
    }

    @Test
    @DisplayName("Pièces de fonds et de caisse : aucune marque Bokati Cowork, aucune variable session fantôme")
    void voucherAndCashTemplatesCarryNoHardcodedBrand() throws Exception {
        for (String name : List.of("fund/voucher", "cash/movement-document", "cash/request-document", "cash/session-closing-report")) {
            String source = Files.readString(Path.of("src/main/resources/templates/" + name + ".html"), StandardCharsets.UTF_8);
            assertThat(source).as(name).contains("fragments/document-brand :: letterhead").doesNotContain("Bokati")
                    .doesNotContain("${session.");
        }
    }

    // ------------------------------------------------------------------ outils

    private static void assertBranded(String html) {
        assertThat(html).doesNotContain("Bokati").contains("12 avenue de l").contains("RCCM CG-PNR-01-2017-A11-00472")
                .contains("contact@essai.cg").contains("dbr-rule");
    }

    private static VoucherService voucherService() {
        VoucherService service = Mockito.spy(new VoucherService(null, engine(), new QrCodeRenderer()));
        ReflectionTestUtils.setField(service, "verifyBaseUrl", "https://verif.test");
        OperatorIdentityService identity = Mockito.mock(OperatorIdentityService.class);
        Mockito.when(identity.current()).thenReturn(OPERATOR);
        ReflectionTestUtils.setField(service, "operatorIdentityService", identity);
        return service;
    }

    private static VoucherService.Issued issued(String number) {
        return new VoucherService.Issued(number, number.substring(0, 3), "SRC", BigDecimal.TEN, null, "comptable",
                Instant.parse("2026-11-01T10:00:00Z"), 0, null, null);
    }

    private static String text(byte[] pdf) throws Exception {
        try (PDDocument document = PDDocument.load(pdf)) {
            return new PDFTextStripper().getText(document).replace(' ', ' ').replace(' ', ' ');
        }
    }

    private static Context context() {
        Context context = new Context(Locale.FRANCE);
        context.setVariable("operator", OPERATOR);
        context.setVariable("logo", null);
        context.setVariable("generatedAt", Instant.parse("2026-11-12T09:00:00Z"));
        context.setVariable("fmt", new CashDocumentPdfServiceImpl.CashDocumentTemplateFormatter(Locale.FRANCE));
        return context;
    }

    private static SpringTemplateEngine engine() {
        FileTemplateResolver resolver = new FileTemplateResolver();
        resolver.setPrefix("src/main/resources/templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }

    /** Construit un record avec des valeurs neutres, puis les valeurs donnees. */
    static <T> T record(Class<T> type, Map<String, Object> values) {
        try {
            RecordComponent[] components = type.getRecordComponents();
            Class<?>[] types = new Class<?>[components.length];
            Object[] args = new Object[components.length];
            for (int i = 0; i < components.length; i++) {
                types[i] = components[i].getType();
                String name = components[i].getName();
                args[i] = values.containsKey(name) ? values.get(name) : neutral(types[i], name);
            }
            Constructor<T> constructor = type.getDeclaredConstructor(types);
            constructor.setAccessible(true);
            return constructor.newInstance(args);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static Object neutral(Class<?> type, String name) {
        if (type == String.class) return name;
        if (type == BigDecimal.class) return BigDecimal.ONE;
        if (type == Instant.class) return Instant.parse("2026-11-12T09:00:00Z");
        if (type == LocalDate.class) return LocalDate.of(2026, 11, 12);
        if (type == LocalDateTime.class) return LocalDateTime.of(2026, 11, 12, 9, 0);
        if (type == List.class) return List.of();
        if (type == Set.class) return Set.of();
        if (type == Map.class) return Map.of();
        if (type == Long.class || type == long.class) return 1L;
        if (type == Integer.class || type == int.class) return 1;
        if (type == Double.class || type == double.class) return 1d;
        if (type == Boolean.class || type == boolean.class) return false;
        if (type.isEnum()) return type.getEnumConstants()[0];
        return null;
    }
}
