package com.sni.bokaticowork.features.document.template;

import com.sni.bokaticowork.core.communication.mailService.config.BrandingDialect;
import com.sni.bokaticowork.core.communication.mailService.support.EmailBranding;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.identity.OperatorIdentity;
import com.sni.bokaticowork.core.identity.OperatorIdentityService;
import com.sni.bokaticowork.core.utils.BrandLogo;
import com.sni.bokaticowork.features.document.template.dto.DocumentTemplateDtos;
import com.sni.bokaticowork.features.document.template.service.DocumentDataSources;
import com.sni.bokaticowork.features.document.template.service.DocumentTemplatePrinter;
import com.sni.bokaticowork.features.document.template.service.DocumentTemplateService;
import com.sni.bokaticowork.features.document.template.service.EmailTemplateOverrideResolver;
import com.sni.bokaticowork.features.document.template.service.EmailTemplateRegistry;
import com.sni.bokaticowork.support.PostgresIntegrationTestBase;
import org.junit.jupiter.api.Test;
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
 * Lot E1 sur le schema de production : brouillons de depart des deux premiers courriels, jetons
 * limites aux variables du courriel d'origine, publication et apercu HTML.
 */
@Import({DocumentTemplateService.class, DocumentTemplatePrinter.class, DocumentDataSources.class, EmailTemplateRegistry.class,
        EmailTemplateOverrideResolver.class, DocumentTemplateE1IT.Beans.class})
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=validate")
class DocumentTemplateE1IT extends PostgresIntegrationTestBase {

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
        SpringTemplateEngine springTemplateEngine(EmailTemplateOverrideResolver override) {
            FileTemplateResolver resolver = new FileTemplateResolver();
            resolver.setPrefix("src/main/resources/templates/");
            resolver.setSuffix(".html");
            resolver.setTemplateMode(TemplateMode.HTML);
            resolver.setCharacterEncoding("UTF-8");
            resolver.setOrder(10);
            SpringTemplateEngine engine = new SpringTemplateEngine();
            engine.addTemplateResolver(override);
            engine.addTemplateResolver(resolver);
            engine.addDialect(new BrandingDialect(new EmailBranding("https://cowork.elleaose.cg")));
            return engine;
        }
    }

    @Autowired private DocumentTemplateService service;
    @Autowired private EmailTemplateRegistry registry;

    @Test
    void emailDraftPublishesAndPreviews() {
        String code = "EMAIL_PAYMENT_CONFIRMATION";
        assertThat(service.catalog(code).fields()).extracting(DocumentTemplateDtos.FieldEntry::key)
                .contains("customerName", "documentNumber", "paidAmount");

        String preview = service.previewEmail(code, null);
        assertThat(preview).contains("Paiement reçu").contains("‹paidAmount›").contains("x-email-subject");

        assertThat(registry.lookup("payment-confirmation")).isEmpty();
        service.publish(code);
        assertThat(registry.lookup("payment-confirmation")).isPresent();
        assertThat(registry.lookup("email/payment-confirmation")).isPresent();

        List<Map<String, Object>> blocks = new ArrayList<>(service.get(code).published().blocks());
        blocks.add(Map.of("type", "TEXT", "text", "{{walletBalance}}"));
        assertThatThrownBy(() -> service.saveDraft(code, new DocumentTemplateDtos.Content("A4", List.of(), Map.of(), blocks, null)))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("jeton inconnu de ce courriel « walletBalance »");

        service.reset(code);
        assertThat(registry.lookup("payment-confirmation")).isEmpty();
    }
}
