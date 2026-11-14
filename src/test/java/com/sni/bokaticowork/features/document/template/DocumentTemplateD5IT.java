package com.sni.bokaticowork.features.document.template;

import com.sni.bokaticowork.features.document.template.dto.DocumentTemplateDtos;
import com.sni.bokaticowork.features.document.template.service.DocumentDataSources;
import com.sni.bokaticowork.features.document.template.service.DocumentTemplatePrinter;
import com.sni.bokaticowork.features.document.template.service.DocumentTemplateService;
import com.sni.bokaticowork.support.PostgresIntegrationTestBase;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Lot D5 sur le schema de production : brouillons de depart des pieces clients (non publies, rendus
 * avec les donnees d'exemple) ; une piece de facturation figee garde sa version de modele.
 */
@Import({DocumentTemplateService.class, DocumentTemplatePrinter.class, DocumentDataSources.class, DocumentTemplateD2IT.Beans.class})
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=validate")
class DocumentTemplateD5IT extends PostgresIntegrationTestBase {

    @Autowired private DocumentTemplateService service;
    @Autowired private DocumentTemplatePrinter printer;
    @Autowired private DocumentDataSources dataSources;

    @ParameterizedTest
    @ValueSource(strings = {"INVOICE", "CREDIT_NOTE", "PAYMENT_RECEIPT", "WALLET_STATEMENT", "WALLET_RECEIPT", "BOOKING_CONFIRMATION"})
    void startingDraftRendersWithoutBeingPublished(String code) throws Exception {
        DocumentTemplateDtos.Detail detail = service.get(code);
        assertThat(detail.published()).isNull();
        assertThat(printer.tryRender(code, dataSources.sample(code))).isEmpty();

        byte[] pdf = service.preview(code, null, null);
        Path out = Path.of("target", "d5-preview");
        Files.createDirectories(out);
        Files.write(out.resolve(code + ".pdf"), pdf);
        String text = flat(text(pdf));
        assertThat(text).contains(flat(dataSources.sample(code).number())).contains(flat("ENGINEERED BY SNI"));
    }

    @Test
    void aPinnedPieceKeepsItsVersion() {
        String code = "INVOICE";
        DocumentTemplateDtos.Version first = service.publish(code);
        assertThat(printer.tryRenderPinned(code, dataSources.sample(code), null)).get()
                .extracting(DocumentTemplatePrinter.Printed::version).isEqualTo(first.versionNumber());

        List<Map<String, Object>> blocks = new ArrayList<>(first.blocks());
        blocks.add(1, Map.of("type", "TEXT", "text", "Nouvelle mention"));
        service.saveDraft(code, new DocumentTemplateDtos.Content("A4", List.of(), Map.of(), blocks, "v2"));
        DocumentTemplateDtos.Version second = service.publish(code);

        // Piece figee sur la premiere version : elle sort encore avec elle, archivee ou non.
        DocumentTemplatePrinter.Printed pinned = printer.tryRenderPinned(code, dataSources.sample(code), first.versionNumber()).orElseThrow();
        assertThat(pinned.version()).isEqualTo(first.versionNumber());
        assertThat(flat(text(pinned.pdf()))).doesNotContain(flat("Nouvelle mention"));
        assertThat(printer.tryRenderPinned(code, dataSources.sample(code), null).orElseThrow().version()).isEqualTo(second.versionNumber());
        // 0 : piece produite par le gabarit d'origine, elle y reste.
        assertThat(printer.tryRenderPinned(code, dataSources.sample(code), 0)).isEmpty();
    }

    private static String flat(String value) {
        return value.replaceAll("[\s\u00A0\u202F]+", "").toUpperCase(Locale.ROOT);
    }

    private static String text(byte[] pdf) {
        try (PDDocument document = PDDocument.load(pdf)) {
            return new PDFTextStripper().getText(document);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
