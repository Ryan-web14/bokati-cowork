package com.sni.bokaticowork.features.document.template;

import com.sni.bokaticowork.features.document.template.dto.DocumentTemplateDtos;
import com.sni.bokaticowork.features.document.template.service.DocumentDataSources;
import com.sni.bokaticowork.features.document.template.service.DocumentTemplatePrinter;
import com.sni.bokaticowork.features.document.template.service.DocumentTemplateService;
import com.sni.bokaticowork.support.PostgresIntegrationTestBase;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Lot D4 sur le schema de production : chaque piece ajoutee a un brouillon de depart, non publie
 * (la piece sort encore de son gabarit d'origine), valide et rendu avec les donnees d'exemple.
 */
@Import({DocumentTemplateService.class, DocumentTemplatePrinter.class, DocumentDataSources.class, DocumentTemplateD2IT.Beans.class})
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=validate")
class DocumentTemplateD4IT extends PostgresIntegrationTestBase {

    @Autowired private DocumentTemplateService service;
    @Autowired private DocumentTemplatePrinter printer;
    @Autowired private DocumentDataSources dataSources;

    @ParameterizedTest
    @ValueSource(strings = {"DISBURSEMENT_VOUCHER", "JUSTIFICATION_SHEET", "PURCHASE_ORDER", "VAT_RETURN", "BANK_RECONCILIATION", "LOAN_SCHEDULE",
            "BANK_DEPOSIT_SLIP", "SUPPLIER_STATEMENT", "PARTNER_STATEMENT", "CASH_CLOSING_REPORT"})
    void startingDraftRendersWithoutBeingPublished(String code) throws Exception {
        DocumentTemplateDtos.Detail detail = service.get(code);
        assertThat(detail.published()).isNull();
        assertThat(detail.draft()).isNotNull();
        assertThat(printer.tryRender(code, dataSources.sample(code))).isEmpty();

        byte[] pdf = service.preview(code, null, null);
        Path out = Path.of("target", "d4-preview");
        Files.createDirectories(out);
        Files.write(out.resolve(code + ".pdf"), pdf);

        String text = flat(text(pdf));
        assertThat(text).contains(flat(dataSources.sample(code).number()));
        assertThat(text).contains(flat("ENGINEERED BY SNI"));
        // Le bloc SECTIONS imprime chaque section de la piece.
        boolean sectionsBlock = detail.draft().blocks().stream().anyMatch(block -> "SECTIONS".equals(block.get("type")));
        if (sectionsBlock) {
            dataSources.sample(code).sections().forEach(section -> assertThat(text).contains(flat(section.title())));
        }
    }

    /** La demande de caisse est publiee par migration (V355, prod V349) : elle sort de son modele. */
    @org.junit.jupiter.api.Test
    void cashRequestIsPublishedByMigration() {
        DocumentTemplateDtos.Detail detail = service.get("CASH_REQUEST");
        assertThat(detail.published()).isNotNull();
        assertThat(detail.draft()).isNull();
        assertThat(printer.tryRender("CASH_REQUEST", dataSources.sample("CASH_REQUEST"))).isPresent();
    }

    private static String flat(String value) {
        return value.replaceAll("[\\s\\u00A0\\u202F]+", "").toUpperCase(Locale.ROOT);
    }

    private static String text(byte[] pdf) {
        try (PDDocument document = PDDocument.load(pdf)) {
            return new PDFTextStripper().getText(document);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
