package com.sni.bokaticowork.templates;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.FileTemplateResolver;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Les courriels mobile money se rendent avec les variables que le code leur donne.
 */
class MobileMoneyEmailsRenderTest {

    private final SpringTemplateEngine engine = engine();

    @Test
    @DisplayName("Rapport du matin : les ecarts listes, le numero tel qu'on l'a masque")
    void dailyReconciliationListsDiscrepancies() {
        Map<String, Object> model = new HashMap<>();
        model.put("day", "2026-10-27");
        model.put("checked", 12);
        model.put("truncated", false);
        model.put("autoSettledCount", 1);
        model.put("remainingCount", 1);
        model.put("moreLines", 0);
        model.put("lines", List.of(Map.of("depositId", "dep-1", "intentNumber", "INT-1", "amount", "5000",
                "currency", "XAF", "phoneNumber", "+24206*****67", "severity", "CRITICAL",
                "message", "Montant chez PawaPay 10, chez nous 5000.", "action", "Vérifier le montant")));

        String html = render("email/mobile-money-daily-reconciliation", model);

        assertThat(html).contains("Rapprochement mobile money du 2026-10-27").contains("dep-1")
                .contains("+24206*****67").contains("Écart critique").contains("Vérifier le montant");
    }

    @Test
    @DisplayName("Ecart de montant a la confirmation : l'alerte le dit, pas « sans reponse »")
    void unresolvedAlertExplainsAnAmountMismatch() {
        Map<String, Object> model = new HashMap<>();
        model.put("anomaly", "L'opérateur a confirmé ce paiement avec un écart : montant annonce 10, attendu 5000.");
        model.put("depositId", "dep-1");
        model.put("amount", "5000");
        model.put("currency", "XAF");

        String html = render("email/mobile-money-deposit-unresolved", model);

        assertThat(html).contains("montant annonce 10, attendu 5000").doesNotContain("sans que l'opérateur ait tranché");
    }

    private String render(String template, Map<String, Object> model) {
        Context context = new Context(Locale.FRANCE);
        model.forEach(context::setVariable);
        return engine.process(template, context);
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
        engine.addDialect(new com.sni.bokaticowork.core.communication.mailService.config.BrandingDialect(
                new com.sni.bokaticowork.core.communication.mailService.support.EmailBranding("https://cowork.elleaose.cg")));
        return engine;
    }
}
