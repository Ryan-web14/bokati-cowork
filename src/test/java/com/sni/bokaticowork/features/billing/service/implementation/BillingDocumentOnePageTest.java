package com.sni.bokaticowork.features.billing.service.implementation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.sni.bokaticowork.core.communication.mailService.support.EmailBranding;
import com.sni.bokaticowork.core.utils.BrandLogo;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentLineResponse;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentResponse;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.enums.BillingLineType;
import com.sni.bokaticowork.features.billing.service.support.BillingReceivables;
import com.sni.bokaticowork.core.communication.mailService.config.BrandingDialect;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.FileTemplateResolver;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Une facture d'une ligne, avec logo, QR, conditions, instructions et coordonnees bancaires, tient
 * sur une seule page, bas de page et case de visa compris.
 */
class BillingDocumentOnePageTest {

    @Test
    @DisplayName("Une facture d'une ligne tient sur une page, visa ELLE A OSE compris")
    void oneLineInvoiceFitsOnOnePage() throws Exception {
        String html = render();
        assertThat(html).contains("Visa ELLE A OSE");
        try (PDDocument pdf = PDDocument.load(pdf(html))) {
            assertThat(pdf.getNumberOfPages()).isEqualTo(1);
        }
    }

    private String render() throws Exception {
        BigDecimal total = new BigDecimal("25000");
        BillingDocumentLineResponse line = new BillingDocumentLineResponse(
                1, BillingLineType.SERVICE, null, "Espace de travail", "Salle de reunion Kintele, demi-journee", null,
                BigDecimal.ONE, "h", new BigDecimal("25000"), BigDecimal.ZERO, BigDecimal.ZERO, true, null,
                new BigDecimal("18"), BigDecimal.ZERO, new BigDecimal("21186.44"), new BigDecimal("21186.44"), BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("21186.44"), null, null, null, null, false);
        BillingDocumentResponse document = new BillingDocumentResponse(
                "INV-2026-000042", BillingDocumentType.INVOICE, BillingDocumentStatus.SENT,
                "MEMBER", "MBR-1", "Joel Bikindou", "joel@example.com", "+242 06 000 00 00",
                "{\"line1\":\"12 avenue de l'Independance\",\"city\":\"Pointe-Noire\",\"country\":\"Congo\"}", true,
                "BILLABLE_ITEM", "BIL-1", null, null, null, false,
                "Facture", "Location de salle", "Paiement a reception de facture.", "XAF",
                total, BigDecimal.ZERO, total, new BigDecimal("3814"), BigDecimal.ZERO,
                new BigDecimal("3814"), total, BigDecimal.ZERO, total, BigDecimal.ZERO,
                LocalDate.of(2026, 9, 23), LocalDate.of(2026, 10, 23), null, null, null,
                "REF-CLIENT", null, null, null, null, "fr", null, null,
                "Paiement par virement, mobile money (06 000 00 00) ou especes a l'accueil.",
                "{\"bank\":\"Banque Postale du Congo\",\"iban\":\"CG39 3001 1000 0123 4567 8901 234\",\"holder\":\"ELLE A OSE\"}", null,
                List.of(line), List.of(), List.of(), List.of(),
                null, null, null, null, List.of(),
                true, "FAC-2026-000042", LocalDate.of(2026, 9, 23), null, "ELLE A OSE", "M2026000012345A", "+242 06 111 11 11",
                "facturation@elleaose.com", "P2026000099999B", null,
                null, null, null, null,
                null, null, null,
                BillingReceivables.receivable(BillingDocumentType.INVOICE, BillingDocumentStatus.SENT),
                BillingReceivables.customerImpact(BillingDocumentType.INVOICE, BillingDocumentStatus.SENT, total, total),
                null, null, null);

        FileTemplateResolver resolver = new FileTemplateResolver();
        resolver.setPrefix("src/main/resources/templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        engine.addDialect(new BrandingDialect(new EmailBranding("https://cowork.elleaose.cg")));

        Context context = new Context(Locale.FRANCE);
        context.setVariable("document", document);
        context.setVariable("generatedAt", LocalDate.of(2026, 9, 23));
        context.setVariable("fmt", new BillingDocumentPdfServiceImpl.BillingDocumentTemplateFormatter("XAF", new ObjectMapper(), Locale.FRANCE));
        context.setVariable("qrCode", square(130));
        context.setVariable("payments", List.of());
        context.setVariable("logo", new BrandLogo().base64());
        context.setVariable("usageAnnex", null);
        return engine.process("billing/document", context);
    }

    private static String square(int size) throws Exception {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", out);
            return Base64.getEncoder().encodeToString(out.toByteArray());
        }
    }

    @Test
    @DisplayName("La case du cachet recoit un cachet rond de 5,7 cm de diametre")
    void theStampCellFitsARoundStampOf57mm() throws Exception {
        String template;
        try (var in = getClass().getResourceAsStream("/templates/billing/document.html")) {
            template = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
        java.util.regex.Matcher height = java.util.regex.Pattern.compile("\\.visa-box td\\.stamp \\{ height: ([0-9.]+)cm;").matcher(template);
        java.util.regex.Matcher width = java.util.regex.Pattern.compile("\\.visa-box \\{ width: ([0-9.]+)cm;").matcher(template);
        assertThat(height.find()).isTrue();
        assertThat(width.find()).isTrue();
        assertThat(Double.parseDouble(height.group(1))).isGreaterThanOrEqualTo(5.9);
        assertThat(Double.parseDouble(width.group(1))).isGreaterThanOrEqualTo(5.9);
    }

    private static byte[] pdf(String html) throws Exception {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            org.jsoup.nodes.Document jsoup = org.jsoup.Jsoup.parse(html);
            jsoup.outputSettings().syntax(org.jsoup.nodes.Document.OutputSettings.Syntax.xml)
                    .escapeMode(org.jsoup.nodes.Entities.EscapeMode.xhtml).charset(java.nio.charset.StandardCharsets.UTF_8).prettyPrint(false);
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withW3cDocument(new org.jsoup.helper.W3CDom().fromJsoup(jsoup), null);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        }
    }
}
