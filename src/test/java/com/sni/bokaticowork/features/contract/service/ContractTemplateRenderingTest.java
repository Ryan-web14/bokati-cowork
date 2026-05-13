package com.sni.bokaticowork.features.contract.service;

import com.sni.bokaticowork.features.contract.dto.request.GenerateContractRequest;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContractTemplateRenderingTest {

    private final SpringTemplateEngine templateEngine = templateEngine();

    @Test
    void shouldRenderSubscriptionPassContractTemplateWithSystemSignature() {
        String html = assertDoesNotThrow(() -> templateEngine.process(
                "contracts/subscription-pass-non-refundable",
                context("subscription-pass-non-refundable")
        ));

        assertTrue(html.contains("Signé automatiquement par le système"));
    }

    @Test
    void shouldRenderOtherContractTemplates() {
        assertDoesNotThrow(() -> templateEngine.process("contracts/membership-agreement", context("membership-agreement")));
        assertDoesNotThrow(() -> templateEngine.process("contracts/business-service-agreement", context("business-service-agreement")));
        assertDoesNotThrow(() -> templateEngine.process("contracts/contrat-domiciliation", context("contrat-domiciliation")));
    }

    private Context context(String templateCode) {
        GenerateContractRequest request = new GenerateContractRequest();
        request.setTemplateCode(templateCode);
        request.setOwnerType(DocumentOwnerType.MEMBER);
        request.setOwnerCode("MBR-0001");
        request.setTitle("Contrat test");
        request.setDescription("Description test");
        request.setEffectiveDate(LocalDate.now());
        request.setStartDate(LocalDate.now());
        request.setEndDate(LocalDate.now().plusMonths(1));
        request.setSignatoryName("Client Test");
        request.setSignatoryRole("Souscripteur");

        Context context = new Context();
        context.setVariable("request", request);
        context.setVariable("generatedAt", LocalDate.now());
        context.setVariable("ownerName", "Client Test");
        context.setVariable("ownerCode", "MBR-0001");
        context.setVariable("ownerEmail", "client@example.com");
        context.setVariable("ownerPhone", "+237000000000");
        context.setVariable("business", null);
        context.setVariable("clauses", List.of("Clause test"));
        context.setVariable("variables", Map.of(
                "operatorName", "ELLE A OSE",
                "operatorSignatureName", "ELLE A OSE",
                "operatorSignatureRole", "Signature automatique du système",
                "operatorSignedAt", LocalDate.now().toString()
        ));
        return context;
    }

    private SpringTemplateEngine templateEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resolver.setCacheable(false);

        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }
}
