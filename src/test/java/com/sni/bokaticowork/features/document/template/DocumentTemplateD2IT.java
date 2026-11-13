package com.sni.bokaticowork.features.document.template;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.identity.OperatorIdentity;
import com.sni.bokaticowork.core.identity.OperatorIdentityService;
import com.sni.bokaticowork.core.utils.BrandLogo;
import com.sni.bokaticowork.features.document.template.dto.DocumentTemplateDtos;
import com.sni.bokaticowork.features.document.template.model.DocumentTemplateVersion;
import com.sni.bokaticowork.features.document.template.render.TemplateContent;
import com.sni.bokaticowork.features.document.template.repository.DocumentTemplateVersionRepository;
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
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.FileTemplateResolver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Lot D2 sur le schema de production : les versions de depart des quatre pieces maquettees sont
 * publiees par la migration, valides, et se rendent avec les donnees d'exemple ; cycle brouillon,
 * publication, restauration et retour au gabarit d'origine.
 */
@Import({DocumentTemplateService.class, DocumentTemplatePrinter.class, DocumentDataSources.class, DocumentTemplateD2IT.Beans.class})
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=validate")
class DocumentTemplateD2IT extends PostgresIntegrationTestBase {

    @TestConfiguration
    static class Beans {
        @Bean
        BrandLogo brandLogo() {
            return new BrandLogo();
        }

        @Bean
        OperatorIdentityService operatorIdentityService() {
            OperatorIdentityService service = mock(OperatorIdentityService.class);
            when(service.current()).thenReturn(OperatorIdentity.defaults());
            return service;
        }

        @Bean
        SpringTemplateEngine springTemplateEngine() {
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

    @Autowired private DocumentTemplateService service;
    @Autowired private DocumentTemplatePrinter printer;
    @Autowired private DocumentDataSources dataSources;
    @Autowired private DocumentTemplateVersionRepository versions;

    @ParameterizedTest
    @ValueSource(strings = {"CASH_OUT_VOUCHER", "CASH_IN_VOUCHER", "FUND_REQUEST", "NEED_STATEMENT"})
    void startingVersionsArePublishedValidAndRender(String code) throws Exception {
        DocumentTemplateVersion published = versions.findFirstByTemplateCodeAndStatus(code, "PUBLISHED").orElseThrow();
        TemplateContent content = DocumentTemplatePrinter.content(published).validate();

        byte[] pdf = printer.render(content, dataSources.sample(code));
        java.nio.file.Path out = java.nio.file.Path.of("target", "d2-preview");
        java.nio.file.Files.createDirectories(out);
        java.nio.file.Files.write(out.resolve(code + ".pdf"), pdf);
        // Texte sans blancs ni casse : l'extraction coupe les lettres espacees et les retours a la ligne.
        String text = flat(text(pdf));

        assertThat(text).contains(flat("ELLE A OSE")).contains(flat("CACHET ELLE A OSE")).contains(flat("ENGINEERED BY SNI"));
        assertThat(text).contains(flat("francs CFA"));
        try (PDDocument document = PDDocument.load(pdf)) {
            if (code.startsWith("CASH")) {
                // Deux exemplaires A5 sur une seule feuille A4.
                assertThat(document.getNumberOfPages()).isEqualTo(1);
                assertThat(text).contains(flat("ORIGINAL · À CONSERVER PAR LA CAISSE")).contains(flat("DÉCOUPER ICI"));
            } else {
                assertThat(document.getNumberOfPages()).isGreaterThanOrEqualTo(1);
            }
        }
        if (code.equals("CASH_OUT_VOUCHER")) {
            assertThat(text).contains(flat("BON DE SORTIE")).contains(flat("118 000 XAF")).contains(flat("Papeterie du Centre")).contains(flat("A. Mabiala · 12/11/2026"));
        }
        if (code.equals("FUND_REQUEST")) {
            // Signataires des niveaux de validation reportes dans leurs cases.
            assertThat(text).contains(flat("P. Moussa · 10/11/2026 14:05")).contains(flat("R. Ibara · 11/11/2026 09:30")).contains(flat("Bon pour accord"));
        }
        if (code.equals("NEED_STATEMENT")) {
            assertThat(text).contains(flat("Vidéoprojecteur de remplacement")).contains(flat("580 500 XAF"));
        }
    }

    @Test
    void draftPublishRestoreAndReset() {
        String code = "JUSTIFICATION_SHEET";
        assertThat(service.get(code).published()).isNull();

        DocumentTemplateDtos.Content content = new DocumentTemplateDtos.Content("A4", List.of(Map.of("label", "Exemplaire comptable")),
                Map.of("accent", "#1f1430"), blocks("Fiche de justification"), "Premier essai");
        DocumentTemplateDtos.Version draft = service.saveDraft(code, content);
        assertThat(draft.status()).isEqualTo("DRAFT");
        assertThat(draft.versionNumber()).isEqualTo(1);
        assertThat(flat(text(service.preview(code, null, null)))).contains(flat("FICHE DE JUSTIFICATION")).contains(flat("Hôtel Atlantic"));

        DocumentTemplateDtos.Version first = service.publish(code);
        assertThat(first.status()).isEqualTo("PUBLISHED");
        assertThat(printer.tryRender(code, dataSources.sample(code))).isPresent();

        service.saveDraft(code, new DocumentTemplateDtos.Content("A4", List.of(), Map.of(), blocks("Fiche modifiée"), "Second essai"));
        DocumentTemplateDtos.Version second = service.publish(code);
        assertThat(second.versionNumber()).isEqualTo(2);
        assertThat(service.get(code).versions()).extracting(DocumentTemplateDtos.VersionSummary::status).containsExactly("PUBLISHED", "ARCHIVED");

        DocumentTemplateDtos.Version restored = service.restore(code, 1);
        assertThat(restored.status()).isEqualTo("DRAFT");
        assertThat(restored.versionNumber()).isEqualTo(3);
        assertThat(restored.blocks().get(1)).containsEntry("title", "Fiche de justification");

        service.reset(code);
        assertThat(service.get(code).published()).isNull();
        assertThat(printer.tryRender(code, dataSources.sample(code))).isEmpty();
    }

    @Test
    void invalidContentIsRefused() {
        List<Map<String, Object>> blocks = new ArrayList<>(blocks("Titre"));
        blocks.add(Map.of("type", "SCRIPT", "html", "<script>alert(1)</script>"));
        assertThatThrownBy(() -> service.saveDraft("DISBURSEMENT_VOUCHER",
                new DocumentTemplateDtos.Content("A4", List.of(), Map.of(), blocks, null)))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("type inconnu");
        assertThatThrownBy(() -> service.publish("DISBURSEMENT_VOUCHER")).isInstanceOf(BadRequestException.class);
    }

    private static List<Map<String, Object>> blocks(String title) {
        return List.of(
                Map.of("type", "HEADER", "qr", true),
                Map.of("type", "TITLE", "title", title),
                Map.of("type", "FIELDS", "columns", 2, "fields", List.of("demande", "beneficiaire", "objet")),
                Map.of("type", "LINES", "table", "depenses"),
                Map.of("type", "TOTALS", "label", "Total justifié"),
                Map.of("type", "SIGNATURES", "fromData", true),
                Map.of("type", "FOOTER"));
    }

    private static String flat(String value) {
        return value.replaceAll("[\\s\\u00A0\\u202F]+", "").toUpperCase(java.util.Locale.ROOT);
    }

    private static String text(byte[] pdf) {
        try (PDDocument document = PDDocument.load(pdf)) {
            return new PDFTextStripper().getText(document);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
