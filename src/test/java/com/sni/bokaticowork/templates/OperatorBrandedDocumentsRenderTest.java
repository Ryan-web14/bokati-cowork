package com.sni.bokaticowork.templates;

import com.sni.bokaticowork.core.identity.OperatorIdentity;
import com.sni.bokaticowork.core.identity.OperatorIdentityService;
import com.sni.bokaticowork.core.utils.BrandLogo;
import com.sni.bokaticowork.core.utils.pdf.PdfRenderer;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import com.sni.bokaticowork.features.document.kyc.KycCaseStatus;
import com.sni.bokaticowork.features.document.kyc.KycRiskLevel;
import com.sni.bokaticowork.features.document.kyc.dto.response.KycCaseResponse;
import com.sni.bokaticowork.features.document.kyc.dto.response.KycTimelineEntryResponse;
import com.sni.bokaticowork.features.document.kyc.service.support.KycReportFormatter;
import com.sni.bokaticowork.features.inventory.admin.dto.InventoryMovementReportResponse;
import com.sni.bokaticowork.features.inventory.admin.service.support.InventoryMovementReportPdfRenderer;
import com.sni.bokaticowork.features.inventory.asset.enums.AssetAssignmentStatus;
import com.sni.bokaticowork.features.inventory.asset.model.Asset;
import com.sni.bokaticowork.features.inventory.asset.model.AssetAssignment;
import com.sni.bokaticowork.features.inventory.asset.service.support.AssetLoanSheetPdfRenderer;
import com.sni.bokaticowork.features.inventory.catalog.model.InventoryItem;
import com.sni.bokaticowork.features.payment.dto.response.WalletLedgerEntryResponse;
import com.sni.bokaticowork.features.payment.enums.WalletEntryDirection;
import com.sni.bokaticowork.features.payment.enums.WalletEntryType;
import com.sni.bokaticowork.features.visitor.model.Visitor;
import com.sni.bokaticowork.features.visitor.model.VisitorPass;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.FileTemplateResolver;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pieces passees a l'en-tete commun au lot D1 · portefeuille, domiciliation, inventaire,
 * reservation, badge visiteur, rapport KYC.
 *
 * <p>Chacune doit imprimer l'identite de la fiche exploitant, et plus aucune des marques
 * historiques (« Bokati Cowork », « Elle A Osé » en texte).</p>
 */
class OperatorBrandedDocumentsRenderTest {

    /** Identite distincte des valeurs par defaut · prouve que la fiche est bien lue. */
    private static final OperatorIdentity OPERATOR = new OperatorIdentity("ELLE A OSE", "SARL", "M0123456789",
            "CG-PNR-01-2017-A11-00472", "12 Avenue de l'Indépendance, Pointe-Noire", "Pointe-Noire",
            "06 111 2233", "accueil@exemple.cg", "Mme Hope MADHONE", "Gérante").withDefaults();

    private static final float MM = 72f / 25.4f;

    private final SpringTemplateEngine engine = engine();

    @Test
    @DisplayName("Relevé de portefeuille · en-tête commun, plus de Bokati Cowork")
    void walletStatement() {
        Context context = context();
        context.setVariable("walletNumber", "WAL-000001");
        context.setVariable("holderName", "Jean Mbemba");
        context.setVariable("periodLabel", "du 01/10/2026 au 31/10/2026");
        context.setVariable("currency", "XAF");
        context.setVariable("issuedAt", "12/11/2026 10:00");
        context.setVariable("openingBalance", "0");
        context.setVariable("totalCredits", "10 000");
        context.setVariable("totalDebits", "0");
        context.setVariable("closingBalance", "10 000");
        context.setVariable("lines", List.of());
        context.setVariable("lastHash", "abc");

        String html = engine.process("wallet/statement", context);

        assertBranded(html);
        assertThat(html).contains("Relevé de compte").contains("WAL-000001");
    }

    @Test
    @DisplayName("Reçu de portefeuille · en-tête commun, format A5 conservé")
    void walletReceipt() throws Exception {
        Context context = context();
        context.setVariable("entry", new WalletLedgerEntryResponse("WLE-1", "WTX-000001", UUID.randomUUID(), "WAL-000001",
                WalletEntryDirection.CREDIT, BigDecimal.TEN, "XAF", BigDecimal.TEN, WalletEntryType.TOPUP,
                null, null, null, "admin", Instant.now()));
        context.setVariable("holderName", "Jean Mbemba");
        context.setVariable("date", "12/11/2026 10:00");
        context.setVariable("amount", "10");
        context.setVariable("balanceAfter", "10");
        context.setVariable("nature", "Rechargement");
        context.setVariable("directionLabel", "Montant reçu");

        String html = engine.process("wallet/transaction-receipt", context);

        assertBranded(html);
        assertPageSize(PdfRenderer.render(html), 148, 210);
    }

    @Test
    @DisplayName("Contrat de domiciliation · le domiciliataire est nommé avec son RCCM et son siège")
    void domiciliationContract() {
        Context context = domiciliationContext();
        context.setVariable("noticePeriodDays", 30);
        context.setVariable("commitmentMonths", 12);
        context.setVariable("billingCycle", "mensuelle");
        context.setVariable("fiscalEligible", true);
        context.setVariable("mailMode", "HOLD");

        String html = engine.process("domiciliation/contract", context);

        assertBranded(html);
        assertThat(html).contains("Le domiciliataire")
                .contains("ELLE A OSE (SARL)")
                .contains("CG-PNR-01-2017-A11-00472")
                .contains("Mme Hope MADHONE");
    }

    @Test
    @DisplayName("Attestation de domiciliation · le domiciliataire soussigné est identifié")
    void domiciliationCertificate() {
        Context context = domiciliationContext();
        context.setVariable("scope", "FISCAL");
        context.setVariable("scopeLabel", "Attestation de domiciliation fiscale");
        context.setVariable("validUntil", "31/10/2027");
        context.setVariable("registrationReference", null);
        context.setVariable("registrationDate", null);
        context.setVariable("registrationOffice", null);

        String html = engine.process("domiciliation/certificate", context);

        assertBranded(html);
        assertThat(html).contains("Le domiciliataire soussigné, <strong>ELLE A OSE (SARL)</strong>")
                .contains("RCCM sous le numéro <span>CG-PNR-01-2017-A11-00472</span>")
                .contains("Fait à <span>Pointe-Noire</span>")
                // L'expression d'origine (apostrophe en &#39;) ne s'analysait pas · l'attestation ne se generait pas.
                .contains("Valable jusqu’au 31/10/2027");
    }

    @Test
    @DisplayName("Domiciliation · le fuseau est celui de Brazzaville, plus celui de Lagos")
    void domiciliationZone() throws Exception {
        var field = com.sni.bokaticowork.features.domiciliation.service.DomiciliationDocuments.class
                .getDeclaredField("APP_ZONE");
        field.setAccessible(true);
        assertThat(field.get(null)).hasToString("Africa/Brazzaville");
    }

    @Test
    @DisplayName("Confirmation de réservation · identité commune au lieu de « Elle A Osé »")
    void bookingConfirmation() {
        Context context = context();
        context.setVariable("generatedAt", "12/11/2026");
        context.setVariable("bookingNumber", "BKG-000001");
        context.setVariable("recipientName", "Jean");
        context.setVariable("resourceName", "Salle Atlantique");
        context.setVariable("resourceType", "Salle de réunion");
        context.setVariable("status", "CONFIRMED");
        context.setVariable("bookingDate", "jeudi 12 novembre 2026");
        context.setVariable("startTime", "09:00");
        context.setVariable("endTime", "10:00");
        context.setVariable("durationLabel", "1 h");
        context.setVariable("quantity", 1);
        context.setVariable("seatsLabel", "1 place");
        context.setVariable("address", "Pointe-Noire");
        context.setVariable("city", "Pointe-Noire");
        context.setVariable("priceTTC", "10 000 XAF");
        context.setVariable("priceHT", "8 000 XAF");
        context.setVariable("priceTVA", "1 500 XAF");
        context.setVariable("priceCentimeAdditionnel", "500 XAF");
        context.setVariable("tvaPct", "18 %");
        context.setVariable("centimeAdditionnelPct", "5 %");
        context.setVariable("paymentMode", "CASH");
        context.setVariable("verificationCode", "CHK-1");
        context.setVariable("qrDataUri", "data:image/png;base64,");

        String html = engine.process("booking/confirmation-pdf", context);

        assertBranded(html);
        assertThat(html).contains("Contact ELLE A OSE").contains("accueil@exemple.cg").contains("Confirmée");
    }

    @Test
    @DisplayName("Badge visiteur · identité commune et vraie page A6 paysage")
    void visitorBadge() throws Exception {
        Visitor visitor = new Visitor();
        visitor.setFullName("Awa Diallo");
        VisitorPass pass = new VisitorPass();
        pass.setPassNumber("VIS-000001");
        pass.setHostName("Jean Mbemba");
        Context context = context();
        context.setVariable("pass", pass);
        context.setVariable("visitor", visitor);
        context.setVariable("qrCode", null);
        context.setVariable("validFrom", "12/11/2026 09:00");
        context.setVariable("validUntil", "12/11/2026 18:00");

        String html = engine.process("visitor/visitor-badge", context);

        assertThat(html).contains("ELLE A OSE · Espace de coworking")
                .doesNotContain("Bokati").doesNotContain("Elle A Osé");
        // OpenHtmlToPDF ne connait pas le mot-cle A6 · il retombait sur A4.
        assertPageSize(PdfRenderer.render(html), 148, 105);
    }

    @Test
    @DisplayName("Rapport KYC · en-tête commun, textes accentués, libellés français")
    void kycReport() {
        Context context = context();
        context.setVariable("case", KycCaseResponse.builder()
                .code("KYC-000001").ownerName("Jean Mbemba").ownerType(DocumentOwnerType.CUSTOMER)
                .status(KycCaseStatus.UNDER_REVIEW).riskLevel(KycRiskLevel.VERY_HIGH).kycLevel(2)
                .submittedAt(Instant.parse("2026-11-12T08:00:00Z")).documents(List.of()).build());
        context.setVariable("timeline", List.of(KycTimelineEntryResponse.builder()
                .timestamp(Instant.parse("2026-11-12T08:00:00Z")).action("CASE_CREATED").actor("SYSTEM")
                .description("KYC case created").build()));
        context.setVariable("expiry", List.of());
        context.setVariable("notes", List.of());
        KycReportFormatter fmt = new KycReportFormatter();
        context.setVariable("fmt", fmt);
        context.setVariable("generatedAt", fmt.dateTime(Instant.parse("2026-11-12T08:00:00Z")));

        String html = engine.process("kyc/kyc-case-report", context);

        assertBranded(html);
        assertThat(html).contains("Généré le").contains("Titulaire")
                .contains("En cours d&#39;examen").contains("Très élevé").contains("Client")
                .contains("Dossier créé").contains("Système").contains("Dossier KYC créé")
                .contains("12/11/2026 09:00")
                .doesNotContain("Genere").doesNotContain("Proprietaire")
                .doesNotContain("UNDER_REVIEW").doesNotContain("VERY_HIGH").doesNotContain("CASE_CREATED");
    }

    @Test
    @DisplayName("Fiche de prêt d'actif · en-tête commun et date lisible")
    void assetLoanSheet() throws Exception {
        InventoryItem item = new InventoryItem();
        item.setName("Vidéoprojecteur");
        Asset asset = new Asset();
        asset.setAssetCode("AST-1");
        asset.setItem(item);
        AssetAssignment assignment = new AssetAssignment();
        assignment.setAssigneeCode("MBR-1");
        assignment.setStatus(AssetAssignmentStatus.ACTIVE);

        byte[] pdf = new AssetLoanSheetPdfRenderer(Locale.FRANCE, identityService(), new BrandLogo())
                .render(asset, assignment);

        String text = text(pdf);
        assertThat(text).contains("12 Avenue de l'Indépendance").contains("FICHE DE PRÊT")
                .doesNotContain("Bokati");
        assertThat(text).containsPattern("Générée le \\d{2}/\\d{2}/\\d{4} à \\d{2}:\\d{2}");
    }

    @Test
    @DisplayName("Rapport de mouvements de stock · en-tête commun, plus de « Elle A Ose »")
    void inventoryMovementReport() throws Exception {
        InventoryMovementReportResponse report = InventoryMovementReportResponse.builder()
                .summary(InventoryMovementReportResponse.Summary.builder()
                        .totalInQuantity(BigDecimal.ZERO).totalOutQuantity(BigDecimal.ZERO)
                        .netQuantity(BigDecimal.ZERO).build())
                .lines(List.of()).items(List.of()).locations(List.of()).recentMovements(List.of())
                .build();

        byte[] pdf = new InventoryMovementReportPdfRenderer(Locale.FRANCE, identityService(), new BrandLogo())
                .render(report);

        String text = text(pdf);
        assertThat(text).contains("12 Avenue de l'Indépendance").contains("Généré le")
                .doesNotContain("Elle A Ose").doesNotContain("Genere");
    }

    // -----------------------------------------------------------------------------------------

    private static void assertBranded(String html) {
        assertThat(html).contains("12 Avenue de l&#39;Indépendance, Pointe-Noire")
                .contains("RCCM CG-PNR-01-2017-A11-00472")
                .contains("accueil@exemple.cg")
                .contains("dbr-letterhead")
                .doesNotContain("Bokati")
                .doesNotContain("Elle A Osé")
                .doesNotContain("Elle A Ose");
    }

    private static void assertPageSize(byte[] pdf, float widthMm, float heightMm) throws Exception {
        try (PDDocument document = PDDocument.load(pdf)) {
            PDRectangle box = document.getPage(0).getMediaBox();
            assertThat(box.getWidth()).isCloseTo(widthMm * MM, within(2f));
            assertThat(box.getHeight()).isCloseTo(heightMm * MM, within(2f));
        }
    }

    private static String text(byte[] pdf) throws Exception {
        try (PDDocument document = PDDocument.load(pdf)) {
            return new PDFTextStripper().getText(document);
        }
    }

    private static OperatorIdentityService identityService() {
        OperatorIdentityService service = mock(OperatorIdentityService.class);
        when(service.current()).thenReturn(OPERATOR);
        return service;
    }

    private Context domiciliationContext() {
        Context context = context();
        context.setVariable("contractNumber", "DOM-2026-00001");
        context.setVariable("legalName", "SARL Mbote");
        context.setVariable("legalForm", "SARL");
        context.setVariable("registrationNumber", "CG-PNR-01-2020-B14-00123");
        context.setVariable("taxNumber", "M999");
        context.setVariable("legalRepresentativeName", "Jean Mbemba");
        context.setVariable("addressLine", "84 Boulevard, Pointe-Noire");
        context.setVariable("suiteNumber", null);
        context.setVariable("startDate", "01/11/2026");
        context.setVariable("endDate", "");
        context.setVariable("issuedAt", "12/11/2026");
        return context;
    }

    private static Context context() {
        Context context = new Context(Locale.FRANCE);
        context.setVariable("operator", OPERATOR);
        context.setVariable("logo", new BrandLogo().base64());
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
}
