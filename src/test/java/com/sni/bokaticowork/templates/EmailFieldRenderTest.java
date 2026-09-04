package com.sni.bokaticowork.templates;

import org.junit.jupiter.api.Test;
import com.sni.bokaticowork.core.communication.mailService.config.BrandingDialect;
import com.sni.bokaticowork.core.communication.mailService.support.EmailBranding;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.FileTemplateResolver;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Un courriel peut partir sans erreur tout en ayant perdu la moitie de ce qu'il devait dire.
 *
 * <p>Le service posait la variable, le gabarit ne la citait nulle part : le rendu reussissait, le
 * destinataire recevait un message ampute. Le remboursement partait sans son motif ni sa date de
 * traitement, la mise a jour d'assistance sans le statut du ticket, la tache sans sa description,
 * le prospect sans son origine. Rien dans les journaux, rien dans les tests.
 *
 * <p>Ce test rend les quatre gabarits concernes et exige que chaque valeur ressorte dans la sortie.
 */
class EmailFieldRenderTest {

    private final SpringTemplateEngine engine = engine();

    @Test
    void refundShouldCarryItsReasonAndProcessingDate() {
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("status", "Traité");
        model.put("reason", "Annulation à l'initiative du client");
        model.put("processedAt", "4 septembre 2026");
        model.put("sourceType", "Facture");
        model.put("sourceCode", "INV-CUS-20260904-00000016");

        String html = render("refund-confirmation", model);

        assertThat(html).contains("Annulation à l'initiative du client");
        assertThat(html).contains("4 septembre 2026");
        // La reference d'origine ne disait pas de quel document elle relevait.
        assertThat(html).contains("Facture INV-CUS-20260904-00000016");
    }

    @Test
    void supportShouldSayWhereTheTicketStands() {
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("ticketNumber", "TCK-000412");
        model.put("ticketStatus", "En cours de traitement");
        model.put("createdAt", "2 septembre 2026");

        String html = render("support-event", model);

        assertThat(html).contains("En cours de traitement");
        assertThat(html).contains("2 septembre 2026");
    }

    @Test
    void taskShouldCarryItsDescription() {
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("title", "Relancer le dossier de domiciliation");
        model.put("description", "Vérifier les pièces manquantes avant la signature.");

        String html = render("task-event", model);

        assertThat(html).contains("Vérifier les pièces manquantes avant la signature.");
    }

    @Test
    void leadShouldCarryItsOrigin() {
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("interest", "Bureau privatif");
        model.put("source", "Salon de l'entrepreneuriat");

        String html = render("crm-event", model);

        assertThat(html).contains("Salon de l'entrepreneuriat");
    }

    private String render(String template, Map<String, Object> model) {
        Context context = new Context(Locale.FRANCE);
        model.forEach(context::setVariable);
        // Thymeleaf echappe l'apostrophe en &#39; ; on la remet pour comparer au texte metier.
        return engine.process("email/" + template, context).replace("&#39;", "'");
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
        // Les en-tetes citent #branding.logoUrl() ; sans le dialecte, le gabarit ne s'analyse meme pas.
        engine.addDialect(new BrandingDialect(new EmailBranding("https://cowork.elleaose.cg")));
        return engine;
    }
}
