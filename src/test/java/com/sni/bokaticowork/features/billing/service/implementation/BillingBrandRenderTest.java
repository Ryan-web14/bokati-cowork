package com.sni.bokaticowork.features.billing.service.implementation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.core.communication.mailService.config.BrandingDialect;
import com.sni.bokaticowork.core.communication.mailService.support.EmailBranding;
import com.sni.bokaticowork.core.identity.OperatorIdentity;
import com.sni.bokaticowork.core.utils.BrandLogo;
import com.sni.bokaticowork.core.utils.pdf.PdfRenderer;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentLineResponse;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentResponse;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.enums.BillingLineType;
import com.sni.bokaticowork.features.billing.service.support.BillingReceivables;
import com.sni.bokaticowork.features.payment.dto.response.PaymentReceiptResponse;
import com.sni.bokaticowork.features.payment.enums.PaymentMethod;
import com.sni.bokaticowork.features.payment.enums.PaymentTransactionStatus;
import com.sni.bokaticowork.features.payment.service.implementation.PaymentReceiptServiceImpl;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.FileTemplateResolver;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * En-tete commun des pieces de facturation (fragments/document-brand) · l'identite imprimee vient
 * de la fiche exploitant, plus des valeurs ecrites en dur.
 */
class BillingBrandRenderTest {

    private static final OperatorIdentity OPERATOR = new OperatorIdentity(
            "SOCIETE TEMOIN", "SARL", "M0000000001X", "CG-BZV-99-2026-B12-00001",
            "7 rue du Test, Mpita, Pointe-Noire", "Pointe-Noire", "06 999 99 99", "contact@temoin.cg",
            "Mme Test", "Gerante");

    @Test
    @DisplayName("Facture · l'identite de la fiche exploitant s'imprime en tete et sur le visa")
    void invoicePrintsOperatorIdentity() throws Exception {
        String html = renderDocument(BillingDocumentType.INVOICE, OPERATOR);

        assertThat(html).contains("7 rue du Test, Mpita, Pointe-Noire")
                .contains("RCCM CG-BZV-99-2026-B12-00001")
                .contains("contact@temoin.cg · 06 999 99 99")
                .contains("Visa SOCIETE TEMOIN")
                .doesNotContain("84 Boulevard du Général")
                .doesNotContain("Émetteur (Vendeur)");
        try (PDDocument pdf = PDDocument.load(PdfRenderer.render(html))) {
            assertThat(pdf.getNumberOfPages()).isEqualTo(1);
        }
    }

    @Test
    @DisplayName("Facture sans fiche · les valeurs historiques s'impriment")
    void invoiceWithoutOperatorFallsBack() {
        String html = renderDocument(BillingDocumentType.INVOICE, null);

        assertThat(html).contains("84 Boulevard du Général Charles de Gaulle")
                .contains("RCCM CG-PNR-01-2017-A11-00472")
                .contains("Visa ELLE A OSE");
    }

    @Test
    @DisplayName("Facture rectificative · titrée comme telle, plus « FACTURE » seul")
    void correctiveInvoiceHasItsOwnTitle() {
        String html = renderDocument(BillingDocumentType.CORRECTIVE_INVOICE, OPERATOR);

        assertThat(html).contains("FACTURE RECTIFICATIVE");
    }

    @Test
    @DisplayName("Avoir · en-tete et bandeau legal tirés de la fiche exploitant")
    void creditNotePrintsOperatorIdentity() {
        Context context = context();
        context.setVariable("document", document(BillingDocumentType.CREDIT_NOTE));
        context.setVariable("operator", OPERATOR);
        context.setVariable("origin", null);
        String html = engine().process("billing/avoir-note-credit", context);

        assertThat(html).contains("7 rue du Test, Mpita, Pointe-Noire")
                .contains("SOCIETE TEMOIN · 7 rue du Test, Mpita, Pointe-Noire · RCCM CG-BZV-99-2026-B12-00001")
                .doesNotContain("84 Boulevard du Général");
    }

    @Test
    @DisplayName("Reçu · logo encre, identité de la fiche, « vérifiable » et plus « scellé »")
    void receiptIsVerifiableNotSealed() throws Exception {
        String logo = new BrandLogo().base64();
        Context context = new Context(Locale.FRANCE);
        context.setVariable("receipt", receipt());
        context.setVariable("generatedAt", LocalDate.of(2026, 11, 12));
        context.setVariable("fmt", new PaymentReceiptServiceImpl.PaymentReceiptTemplateFormatter("XAF", new ObjectMapper(), Locale.FRANCE));
        context.setVariable("qrCode", new com.sni.bokaticowork.core.generator.qrcode.QrCodeRenderer()
                .toBase64("https://exemple.test/verify/receipt/RCP-1", 350));
        context.setVariable("logo", logo);
        context.setVariable("operator", OPERATOR);
        String html = engine().process("payment/receipt", context);

        assertThat(html).doesNotContain("scellé")
                .contains("Document vérifiable")
                .contains("RCCM CG-BZV-99-2026-B12-00001")
                .contains(logo.substring(0, 40))
                .doesNotContain("84 Boulevard du Général");
        assertThat(PdfRenderer.render(html)).isNotEmpty();
    }

    private String renderDocument(BillingDocumentType type, OperatorIdentity operator) {
        Context context = context();
        context.setVariable("document", document(type));
        context.setVariable("qrCode", null);
        context.setVariable("payments", List.of());
        context.setVariable("usageAnnex", null);
        context.setVariable("operator", operator);
        return engine().process("billing/document", context);
    }

    private static Context context() {
        Context context = new Context(Locale.FRANCE);
        context.setVariable("generatedAt", LocalDate.of(2026, 9, 23));
        context.setVariable("fmt", new BillingDocumentPdfServiceImpl.BillingDocumentTemplateFormatter("XAF", new ObjectMapper(), Locale.FRANCE));
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
        engine.addDialect(new BrandingDialect(new EmailBranding("https://cowork.elleaose.cg")));
        return engine;
    }

    private static PaymentReceiptResponse receipt() {
        BigDecimal amount = new BigDecimal("25000");
        return new PaymentReceiptResponse(
                "RCP-1", Instant.parse("2026-11-12T10:00:00Z"), "TX-1", null,
                PaymentMethod.CASH, PaymentTransactionStatus.SUCCEEDED, null, null, null, null, null,
                "Paiement", "BILLING_DOCUMENT", "INV-1", "MEMBER", "MBR-1", "Joel Bikindou",
                "joel@example.com", null, null, amount, amount, BigDecimal.ZERO, "XAF",
                Instant.parse("2026-11-12T10:00:00Z"), "Caisse", List.of(), "Espèces",
                null, null, null, null);
    }

    private static BillingDocumentResponse document(BillingDocumentType type) {
        BigDecimal total = new BigDecimal("25000");
        BillingDocumentLineResponse line = new BillingDocumentLineResponse(
                1, BillingLineType.SERVICE, null, "Espace de travail", "Salle de reunion, demi-journee", null,
                BigDecimal.ONE, "h", new BigDecimal("25000"), BigDecimal.ZERO, BigDecimal.ZERO, true, null,
                new BigDecimal("18"), BigDecimal.ZERO, new BigDecimal("21186.44"), new BigDecimal("21186.44"), BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("21186.44"), null, null, null, null, false);
        return new BillingDocumentResponse(
                "INV-2026-000042", type, BillingDocumentStatus.SENT,
                "MEMBER", "MBR-1", "Joel Bikindou", "joel@example.com", "+242 06 000 00 00",
                null, true,
                "BILLABLE_ITEM", "BIL-1", null, null, null, false,
                "Facture", "Location de salle", "Paiement a reception de facture.", "XAF",
                total, BigDecimal.ZERO, total, new BigDecimal("3814"), BigDecimal.ZERO,
                new BigDecimal("3814"), total, BigDecimal.ZERO, total, BigDecimal.ZERO,
                LocalDate.of(2026, 9, 23), LocalDate.of(2026, 10, 23), null, null, null,
                "REF-CLIENT", null, null, null, null, "fr", null, null,
                null, null, null,
                List.of(line), List.of(), List.of(), List.of(),
                null, null, null, null, List.of(),
                true, "FAC-2026-000042", LocalDate.of(2026, 9, 23), null, "SOCIETE TEMOIN", "M0000000001X", "06 999 99 99",
                "contact@temoin.cg", null, null,
                null, null, null, null,
                null, null, null,
                BillingReceivables.receivable(BillingDocumentType.INVOICE, BillingDocumentStatus.SENT),
                BillingReceivables.customerImpact(BillingDocumentType.INVOICE, BillingDocumentStatus.SENT, total, total),
                null, null, null);
    }
}
