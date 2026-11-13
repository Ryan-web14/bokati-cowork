package com.sni.bokaticowork.features.document.template;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.document.template.dto.DocumentTemplateDtos;
import com.sni.bokaticowork.features.document.template.render.DocumentData;
import com.sni.bokaticowork.features.document.template.service.DocumentDataSources;
import com.sni.bokaticowork.features.document.template.service.DocumentFieldService;
import com.sni.bokaticowork.features.document.template.service.DocumentTemplatePrinter;
import com.sni.bokaticowork.features.document.template.service.DocumentTemplateService;
import com.sni.bokaticowork.features.payment.service.support.CashVoucherDocumentData;
import com.sni.bokaticowork.support.PostgresIntegrationTestBase;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Lot D3 sur le schema de production : champs ajoutes au bon de sortie. Un champ obligatoire non
 * saisi bloque l'impression ; les valeurs sont controlees par type ; un champ imprime vide devient
 * une ligne a remplir ; un champ qu'aucun bloc ne place sort dans « Informations complementaires ».
 */
@Import({DocumentTemplateService.class, DocumentTemplatePrinter.class, DocumentDataSources.class, DocumentFieldService.class,
        DocumentTemplateD2IT.Beans.class})
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=validate")
class DocumentFieldD3IT extends PostgresIntegrationTestBase {

    private static final String CODE = CashVoucherDocumentData.CASH_OUT;

    @Autowired private DocumentTemplateService templates;
    @Autowired private DocumentTemplatePrinter printer;
    @Autowired private DocumentFieldService fields;

    @Test
    void capturedAndBlankFieldsFollowTheTemplate() {
        DocumentTemplateDtos.Detail detail = templates.get(CODE);
        List<Map<String, Object>> blocks = new ArrayList<>(detail.published().blocks());
        // Le numero de piece d'identite est place dans un bloc ; les autres champs ne le sont pas.
        blocks.add(3, Map.of("type", "FIELDS", "columns", 2, "fields", List.of("champ.piece_identite", "champ.date_retour")));
        List<Map<String, Object>> added = List.of(
                Map.of("key", "piece_identite", "label", "Pièce d'identité", "type", "TEXT", "mode", "CAPTURED", "required", true),
                Map.of("key", "date_retour", "label", "Date de retour", "type", "DATE", "mode", "CAPTURED"),
                Map.of("key", "mode_remise", "label", "Remis par", "type", "LIST", "mode", "CAPTURED", "options", List.of("Main propre", "Coursier")),
                Map.of("key", "observation", "label", "Observation du responsable", "type", "TEXT", "mode", "PRINTED_BLANK"));
        templates.saveDraft(CODE, new DocumentTemplateDtos.Content(detail.published().pageFormat(), detail.published().copies(),
                detail.published().style(), blocks, added, "Champs ajoutés"));
        templates.publish(CODE);

        String source = "CSM-D3-" + System.nanoTime();
        assertThat(fields.get(CODE, source).missingRequired()).containsExactly("piece_identite");
        assertThatThrownBy(() -> printer.tryRender(CODE, data(source)))
                .isInstanceOf(DocumentTemplatePrinter.MissingFieldsException.class).hasMessageContaining("Pièce d'identité");

        Map<String, Object> wrong = new HashMap<>();
        wrong.put("date_retour", "15/11/2026");
        wrong.put("mode_remise", "Pigeon voyageur");
        wrong.put("observation", "à la main");
        wrong.put("inconnu", "x");
        assertThatThrownBy(() -> fields.save(CODE, source, wrong)).isInstanceOf(BadRequestException.class)
                .hasMessageContaining("« Date de retour » : valeur invalide pour le type DATE")
                .hasMessageContaining("hors de la liste")
                .hasMessageContaining("s'imprime vide")
                .hasMessageContaining("champ inconnu « inconnu »");

        DocumentTemplateDtos.PieceFields saved = fields.save(CODE, source,
                Map.of("piece_identite", "CNI 0042-PNR", "date_retour", "2026-11-15", "mode_remise", "Coursier"));
        assertThat(saved.missingRequired()).isEmpty();
        assertThat(saved.fields()).extracting(DocumentTemplateDtos.PieceField::value).containsExactly("CNI 0042-PNR", "2026-11-15", "Coursier", null);

        String text = text(printer.tryRender(CODE, data(source)).orElseThrow());
        assertThat(text).contains(flat("CNI 0042-PNR")).contains(flat("15/11/2026"))
                .contains(flat("Informations complémentaires")).contains(flat("Remis par")).contains(flat("Coursier"))
                .contains(flat("Observation du responsable"));

        // Une valeur vide efface la saisie : l'impression est de nouveau refusee.
        fields.save(CODE, source, Map.of("piece_identite", ""));
        assertThat(fields.get(CODE, source).missingRequired()).containsExactly("piece_identite");
        templates.reset(CODE);
    }

    private static DocumentData data(String source) {
        return CashVoucherDocumentData.sample(CODE).source(source);
    }

    private static String flat(String value) {
        return value.replaceAll("[\\s\\u00A0\\u202F]+", "").toUpperCase(Locale.ROOT);
    }

    private static String text(byte[] pdf) {
        try (PDDocument document = PDDocument.load(pdf)) {
            return flat(new PDFTextStripper().getText(document));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
