package com.sni.bokaticowork.features.document.template.render;

import com.sni.bokaticowork.core.communication.mailService.config.BrandingDialect;
import com.sni.bokaticowork.core.communication.mailService.support.EmailBranding;
import com.sni.bokaticowork.core.communication.mailService.support.EmailSubjectOverride;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.document.template.model.DocumentTemplate;
import com.sni.bokaticowork.features.document.template.model.DocumentTemplateVersion;
import com.sni.bokaticowork.features.document.template.repository.DocumentTemplateRepository;
import com.sni.bokaticowork.features.document.template.repository.DocumentTemplateVersionRepository;
import com.sni.bokaticowork.features.document.template.service.EmailTemplateOverrideResolver;
import com.sni.bokaticowork.features.document.template.service.EmailTemplateRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.FileTemplateResolver;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Lot E1 : un courriel publie remplace son fichier d'origine sans toucher au code d'envoi ; le texte
 * saisi n'est jamais interprete ; l'objet du modele remplace celui du code.
 */
class EmailTemplatesE1Test {

    private static final List<Map<String, Object>> BLOCKS = List.of(
            Map.of("type", "SUBJECT", "text", "Votre facture {{documentNumber}}"),
            Map.of("type", "GREETING", "text", "Bonjour {{customerName}},"),
            Map.of("type", "TEXT", "text", "Montant : {{totalAmount}}\n[[${T(java.lang.Runtime).getRuntime()}]] <b>gras</b> ${x}"),
            Map.of("type", "DETAILS", "rows", List.of(Map.of("label", "Référence", "value", "{{documentNumber}}"))),
            Map.of("type", "BUTTON", "label", "Voir", "url", "https://cowork.elleaose.cg/espace"),
            Map.of("type", "LEGAL"));

    @Test
    void validationNamesEachError() {
        List<Map<String, Object>> blocks = List.of(
                Map.of("type", "SUBJECT", "text", "A"), Map.of("type", "SUBJECT", "text", "B"),
                Map.of("type", "TEXT", "text", "{{document.class.classLoader}} {{inconnu}}"),
                Map.of("type", "BUTTON", "label", "x", "url", "javascript:alert(1)"),
                Map.of("type", "SCRIPT"));
        assertThatThrownBy(() -> EmailTemplates.validate(blocks, Map.of(), Set.of("document", "documentNumber"), List.of("code")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("un et un seul bloc SUBJECT")
                .hasMessageContaining("jeton interdit « document.class.classLoader »")
                .hasMessageContaining("jeton inconnu de ce courriel « inconnu »")
                .hasMessageContaining("lien attendu")
                .hasMessageContaining("type inconnu « SCRIPT »")
                .hasMessageContaining("jeton obligatoire absent « {{code}} »");
        EmailTemplates.validate(BLOCKS, Map.of(), Set.of("documentNumber", "customerName", "totalAmount"), List.of());
    }

    @Test
    void publishedEmailReplacesTheFileAndNeverInterpretsTypedText() throws Exception {
        DocumentTemplateRepository templates = mock(DocumentTemplateRepository.class);
        DocumentTemplateVersionRepository versions = mock(DocumentTemplateVersionRepository.class);
        when(templates.findAllByOrderByFamilyAscDomainAscLabelAsc()).thenReturn(List.of(DocumentTemplate.builder().code("EMAIL_BILLING_DOCUMENT")
                .family("EMAIL").domain("BILLING").label("x").fallbackTemplate("email/billing-document").active(true).build()));
        when(versions.findFirstByTemplateCodeAndStatus(anyString(), anyString())).thenReturn(Optional.of(DocumentTemplateVersion.builder()
                .templateCode("EMAIL_BILLING_DOCUMENT").versionNumber(1).status("PUBLISHED").blocks(BLOCKS).style(Map.of("tone", "")).build()));
        SpringTemplateEngine engine = new SpringTemplateEngine();
        @SuppressWarnings("unchecked") ObjectProvider<SpringTemplateEngine> engineProvider = mock(ObjectProvider.class);
        when(engineProvider.getIfAvailable()).thenReturn(engine);
        EmailTemplateRegistry registry = new EmailTemplateRegistry(templates, versions, engineProvider);
        @SuppressWarnings("unchecked") ObjectProvider<EmailTemplateRegistry> registryProvider = mock(ObjectProvider.class);
        when(registryProvider.getIfAvailable()).thenReturn(registry);
        FileTemplateResolver files = new FileTemplateResolver();
        files.setPrefix("src/main/resources/templates/");
        files.setSuffix(".html");
        files.setTemplateMode(TemplateMode.HTML);
        files.setCharacterEncoding("UTF-8");
        files.setOrder(10);
        engine.addTemplateResolver(new EmailTemplateOverrideResolver(registryProvider));
        engine.addTemplateResolver(files);
        engine.addDialect(new BrandingDialect(new EmailBranding("https://cowork.elleaose.cg")));

        Context context = new Context(Locale.FRENCH);
        context.setVariable("documentNumber", "FAC-2026-000042");
        context.setVariable("customerName", "Société Kintele");
        context.setVariable("totalAmount", "590 000 XAF");
        String html = engine.process("email/billing-document", context);
        Files.createDirectories(Path.of("target", "e1-preview"));
        Files.writeString(Path.of("target", "e1-preview", "billing-document.html"), html);

        assertThat(html).contains("Bonjour Société Kintele,").contains("Montant : 590 000 XAF").contains("FAC-2026-000042");
        // Le texte saisi reste du texte : ni expression evaluee, ni balise.
        assertThat(html).contains("[[${T(java.lang.Runtime).getRuntime()}]]".replace("[", "&#91;").replace("]", "&#93;").replace("$", "&#36;")
                .replace("{", "&#123;").replace("}", "&#125;")).doesNotContain("<b>gras</b>").doesNotContain("java.lang.Runtime@");
        assertThat(html).contains("href=\"https://cowork.elleaose.cg/espace\"");

        EmailSubjectOverride.Mail mail = EmailSubjectOverride.apply("Facture", html);
        assertThat(mail.subject()).isEqualTo("Votre facture FAC-2026-000042");
        assertThat(mail.content()).doesNotContain("x-email-subject");
        assertThat(EmailSubjectOverride.apply("Sans modèle", "<p>corps</p>").subject()).isEqualTo("Sans modèle");
    }

    @Test
    void variablesComeFromTheOriginalTemplate() throws Exception {
        Set<String> variables = EmailTemplates.variables(Files.readString(Path.of("src/main/resources/templates/email/payment-confirmation.html")));
        assertThat(variables).contains("customerName", "documentNumber", "paidAmount", "balanceDue");
        assertThat(EmailTemplates.sampleVariables(List.of("document.number", "document"))).containsKey("document");
    }
}
