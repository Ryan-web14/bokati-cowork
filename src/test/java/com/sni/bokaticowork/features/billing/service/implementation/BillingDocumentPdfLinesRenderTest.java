package com.sni.bokaticowork.features.billing.service.implementation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.core.communication.mailService.config.BrandingDialect;
import com.sni.bokaticowork.core.communication.mailService.support.EmailBranding;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentLineResponse;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentResponse;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.enums.BillingLineType;
import com.sni.bokaticowork.features.billing.service.support.BillingReceivables;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.FileTemplateResolver;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Le tableau des prestations se rend, et chaque colonne dit ce que son titre annonce.
 *
 * <p>Deux defauts corriges ensemble : la colonne « P.U. HT » affichait le prix saisi, donc le prix
 * total TTC de la prestation sur une ligne en prix TTC ; et une colonne « TVA » repetait le meme
 * taux sur chaque ligne alors que le recapitulatif le porte deja.</p>
 */
class BillingDocumentPdfLinesRenderTest {

    private final SpringTemplateEngine engine = engine();

    private BillingDocumentLineResponse line(int order, String label, String quantity,
                                             String unitPrice, String subtotal, boolean taxable) {
        return new BillingDocumentLineResponse(
                order, BillingLineType.SERVICE, null, "Espace de travail", label, null,
                new BigDecimal(quantity), "h", new BigDecimal(unitPrice),
                BigDecimal.ZERO, BigDecimal.ZERO, taxable, null,
                taxable ? new BigDecimal("18") : BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal(subtotal), new BigDecimal(subtotal), BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal(subtotal),
                null, null, null, null, false);
    }

    /**
     * Le bloc des prestations, separateurs de milliers normalises.
     *
     * <p>La locale francaise separe les milliers par une espace fine insecable · une assertion
     * ecrite avec une espace ordinaire ne trouverait jamais « 21 186 ».</p>
     */
    private String linesBlockOf(String html) {
        // Jusqu'au recapitulatif, pas jusqu'au total : les sommes du recapitulatif sont
        // legitimement TTC, et les inclure ferait echouer toute assertion sur le hors taxe.
        // Le marqueur est cherche apres le debut · il apparait aussi dans la feuille de style.
        int start = html.indexOf("Détail des prestations");
        int end = html.indexOf("summary-wrap", start);
        return html.substring(start, end)
                .replace(' ', ' ')
                .replace(' ', ' ');
    }

    private String render(List<BillingDocumentLineResponse> lines) {
        BigDecimal total = new BigDecimal("25000");
        BillingDocumentResponse document = new BillingDocumentResponse(
                "INV-2026-000042", BillingDocumentType.INVOICE, BillingDocumentStatus.SENT,
                "MEMBER", "MBR-1", "Joël Bikindou", "joel@example.com", null, null, true,
                "BILLABLE_ITEM", "BIL-1", null, null, null, false,
                "Facture", null, null, "XAF",
                total, BigDecimal.ZERO, total, new BigDecimal("3814"), BigDecimal.ZERO,
                new BigDecimal("3814"), total, BigDecimal.ZERO, total, BigDecimal.ZERO,
                LocalDate.of(2026, 9, 23), LocalDate.of(2026, 10, 23), null, null, null,
                null, null, null, null, null, "fr", null, null, null, null, null,
                lines, List.of(), List.of(), List.of(),
                null, null, null, null, List.of(),
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null,
                null, null, null,
                BillingReceivables.receivable(BillingDocumentType.INVOICE, BillingDocumentStatus.SENT),
                BillingReceivables.customerImpact(BillingDocumentType.INVOICE, BillingDocumentStatus.SENT, total, total));

        Context context = new Context(Locale.FRANCE);
        context.setVariable("document", document);
        context.setVariable("generatedAt", LocalDate.of(2026, 9, 23));
        context.setVariable("fmt", new BillingDocumentPdfServiceImpl.BillingDocumentTemplateFormatter(
                "XAF", new ObjectMapper(), Locale.FRANCE));
        context.setVariable("qrCode", null);
        context.setVariable("payments", List.of());
        context.setVariable("logo", null);
        return engine.process("billing/document", context);
    }

    @Test
    @DisplayName("Le prix unitaire affiché est hors taxe, pas le prix total de la prestation")
    void theUnitPriceColumnIsExcludingTax() {
        // Ligne saisie en prix TTC : 1 x 25 000 TTC, brut HT 21 186.
        String html = render(List.of(line(1, "Salle de réunion", "1", "25000", "21186.44", true)));

        String linesBlock = linesBlockOf(html);
        assertThat(html).contains("P.U. HT");
        assertThat(linesBlock).contains("21 186");
        // 25 000 reste dans le recapitulatif · il ne doit plus figurer dans la colonne unitaire.
        assertThat(linesBlock).doesNotContain("25 000");
    }

    @Test
    @DisplayName("La colonne TVA par ligne a disparu · le récapitulatif la porte")
    void thePerLineVatColumnIsGone() {
        String html = render(List.of(line(1, "Salle de réunion", "2", "10000", "20000", true)));

        String linesBlock = linesBlockOf(html);
        assertThat(linesBlock).doesNotContain("18 %");
        assertThat(html).contains("TVA");
    }

    @Test
    @DisplayName("Une prestation exonérée se voit encore · elle le dit dans son libellé")
    void anExemptLineStaysVisible() {
        String html = render(List.of(line(1, "Prestation exonérée", "1", "10000", "10000", false)));

        assertThat(html).contains("Exonéré");
    }

    @Test
    @DisplayName("Chaque ligne se vérifie de gauche à droite · quantité fois prix unitaire égale le brut")
    void everyLineAddsUp() {
        String html = render(List.of(line(1, "Bureau partagé", "3", "11800", "30000", true)));

        // 30 000 / 3 = 10 000 HT l'unite, et le brut affiche reste 30 000.
        assertThat(linesBlockOf(html)).contains("10 000").contains("30 000");
    }

    private SpringTemplateEngine engine() {
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
}
