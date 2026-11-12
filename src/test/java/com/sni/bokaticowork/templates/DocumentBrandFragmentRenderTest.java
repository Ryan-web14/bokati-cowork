package com.sni.bokaticowork.templates;

import com.sni.bokaticowork.core.identity.OperatorIdentity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.FileTemplateResolver;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/** Fragments communs d'en-tete et de pied des pieces PDF (fragments/document-brand.html). */
class DocumentBrandFragmentRenderTest {

    private static final String PAGE = """
            <html xmlns:th="http://www.thymeleaf.org"><head>
            <th:block th:replace="~{fragments/document-brand :: styles}"></th:block></head><body>
            <div th:replace="~{fragments/document-brand :: titled('Bon de caisse', 'CSM-000001')}"></div>
            <div th:replace="~{fragments/document-brand :: letterhead(~{::#droite})}"><div id="droite">BLOC DROIT</div></div>
            <div th:replace="~{fragments/document-brand :: footer('Réf. CSM-000001')}"></div>
            </body></html>
            """;

    @Test
    @DisplayName("L'en-tete imprime l'identite de la fiche exploitant")
    void theLetterheadPrintsTheOperatorIdentity() {
        Context context = new Context(Locale.FRANCE);
        context.setVariable("operator", new OperatorIdentity("ELLE A OSE", "SARL", "M0123",
                "CG-BZV-01-2020-B12-00001", "12 Avenue de l'Indépendance, Brazzaville", "Brazzaville",
                "06 000 0000", "contact@exemple.cg", null, null).withDefaults());

        String html = render(context);

        assertThat(html).contains("ELLE A OSE")
                .contains("12 Avenue de l&#39;Indépendance, Brazzaville")
                .contains("RCCM CG-BZV-01-2020-B12-00001 · NIU M0123")
                .contains("contact@exemple.cg · 06 000 0000")
                .contains("Bon de caisse").contains("CSM-000001")
                .contains("BLOC DROIT")
                .contains("Réf. CSM-000001")
                .doesNotContain("Bokati")
                .doesNotContain("84 Boulevard");
    }

    @Test
    @DisplayName("Sans identite fournie, les valeurs historiques s'impriment")
    void withoutIdentityTheHistoricValuesArePrinted() {
        String html = render(new Context(Locale.FRANCE));

        assertThat(html).contains("ELLE A OSE")
                .contains("84 Boulevard du Général Charles de Gaulle")
                .contains("RCCM CG-PNR-01-2017-A11-00472")
                .contains("coworkspace@elleaose.com");
    }

    @Test
    @DisplayName("Avec un logo, l'image remplace le nom en toutes lettres")
    void theLogoReplacesTheName() {
        Context context = new Context(Locale.FRANCE);
        context.setVariable("logo", "iVBORw0KGgo=");
        context.setVariable("operator", OperatorIdentity.defaults());

        String html = render(context);

        assertThat(html).contains("data:image/png;base64,iVBORw0KGgo=")
                .doesNotContain("class=\"dbr-name\"");
    }

    private static String render(Context context) {
        FileTemplateResolver files = new FileTemplateResolver();
        files.setPrefix("src/main/resources/templates/");
        files.setSuffix(".html");
        files.setTemplateMode(TemplateMode.HTML);
        files.setCharacterEncoding(StandardCharsets.UTF_8.name());
        files.setCheckExistence(true);
        files.setOrder(1);
        StringTemplateResolver strings = new StringTemplateResolver();
        strings.setTemplateMode(TemplateMode.HTML);
        strings.setOrder(2);
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.addTemplateResolver(strings);
        engine.addTemplateResolver(files);
        return engine.process(PAGE, context);
    }
}
