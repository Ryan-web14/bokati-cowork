package com.sni.bokaticowork.templates;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.FileTemplateResolver;
import org.thymeleaf.web.servlet.IServletWebExchange;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Les pages du poste de pointage se rendent, et leurs formulaires visent les bonnes routes.
 *
 * <p>Les deux formulaires visaient {@code /v1/public/bookings/...} alors que le prefixe de l'API
 * est {@code /sni/api/v1} · ils tombaient donc en 404, et aucun poste ne pouvait etre active.
 * L'URL vient desormais du controleur, qui la construit a partir de {@link ApiPath}, et ce test
 * la verifie dans le HTML rendu plutot que dans le modele seul.</p>
 */
class CheckInPagesRenderTest {

    private static final String VERIFY_URL = ApiPath.V1 + "/public/bookings/check-in/scanner-verify";
    private static final String SETUP_URL = ApiPath.V1 + "/public/bookings/check-in/scanner-setup";
    private static final String CONFIRM_URL = ApiPath.V1 + "/public/bookings/check-in/scan";

    private final SpringTemplateEngine engine = engine();

    @Test
    @DisplayName("La page de confirmation envoie le pointage en POST vers la bonne route")
    void confirmationPagePostsToTheRealRoute() {
        String html = render("booking/checkin-confirm",
                model("checkInToken", "jeton-de-pointage", "confirmUrl", CONFIRM_URL));

        assertThat(html).contains("method=\"post\"");
        assertThat(html).contains("action=\"" + CONFIRM_URL + "\"");
        assertThat(html).contains("jeton-de-pointage");
        // Le lien ouvert ne pointe pas · la page le dit a l'agent de l'accueil.
        assertThat(html).contains("Enregistrer l");
    }

    @Test
    @DisplayName("La demande de code vise la route de verification, et non un chemin inexistant")
    void adminCodePageTargetsTheVerifyRoute() {
        String html = render("booking/checkin-admin-code",
                model("checkInToken", "jeton", "scannerConfigured", true, "verifyUrl", VERIFY_URL));

        assertThat(html).contains("action=\"" + VERIFY_URL + "\"");
        assertThat(html).doesNotContain("\"/v1/public/bookings");
    }

    @Test
    @DisplayName("La page d'activation vise la route d'activation")
    void setupPageTargetsTheSetupRoute() {
        String html = render("booking/checkin-scanner-setup",
                model("scannerConfigured", true, "setupUrl", SETUP_URL));

        assertThat(html).contains("action=\"" + SETUP_URL + "\"");
        assertThat(html).doesNotContain("\"/v1/public/bookings");
    }

    @Test
    @DisplayName("Un poste active apprend quand son autorisation expire")
    void setupSuccessAnnouncesTheExpiry() {
        String html = render("booking/checkin-scanner-setup",
                model("scannerConfigured", true, "setupSuccess", true, "setupUrl", SETUP_URL,
                        "grantValidityDays", 30L));

        assertThat(html).contains("30").contains("expire");
    }

    @Test
    @DisplayName("Sans cle configuree, la page d'activation ne propose pas de formulaire")
    void withoutAKeyNoSetupFormIsOffered() {
        String html = render("booking/checkin-scanner-setup",
                model("scannerConfigured", false, "setupUrl", SETUP_URL));

        assertThat(html).doesNotContain("method=\"post\"");
    }

    private static Map<String, Object> model(Object... kv) {
        Map<String, Object> model = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            model.put(String.valueOf(kv[i]), kv[i + 1]);
        }
        return model;
    }

    /** Un contexte web est necessaire · les expressions {@code @{...}} sont relatives au contexte. */
    private String render(String template, Map<String, Object> model) {
        MockServletContext servletContext = new MockServletContext();
        JakartaServletWebApplication application =
                JakartaServletWebApplication.buildApplication(servletContext);
        IServletWebExchange exchange = application.buildExchange(
                new MockHttpServletRequest(servletContext), new MockHttpServletResponse());
        WebContext context = new WebContext(exchange, Locale.FRANCE);
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
        return engine;
    }
}
