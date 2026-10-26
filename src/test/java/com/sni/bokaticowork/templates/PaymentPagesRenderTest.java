package com.sni.bokaticowork.templates;

import com.sni.bokaticowork.features.payment.dto.response.MobileMoneyDepositResponse;
import com.sni.bokaticowork.features.payment.service.pawaypay.PublicPaymentPageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.FileTemplateResolver;
import org.thymeleaf.web.servlet.IServletWebExchange;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Les pages de paiement d'un lien se rendent, et echappent ce qui vient du client.
 */
class PaymentPagesRenderTest {

    private final SpringTemplateEngine engine = engine();

    private static PublicPaymentPageService.Page page(String customerName) {
        return new PublicPaymentPageService.Page("PI-1", new BigDecimal("15000"), "XAF", customerName,
                List.of(new PublicPaymentPageService.Country("COG", "Congo-Brazzaville", "+242", List.of(
                                new PublicPaymentPageService.Operator("MTN_MOMO_COG", "MTN Mobile Money Congo"),
                                new PublicPaymentPageService.Operator("AIRTEL_COG", "Airtel Money Congo"))),
                        new PublicPaymentPageService.Country("GAB", "Gabon", "+241", List.of(
                                new PublicPaymentPageService.Operator("AIRTEL_GAB", "Airtel Money Gabon")))),
                null);
    }

    @Test
    @DisplayName("Le formulaire montre le montant, les pays, et poste vers le lien")
    void formRenders() {
        String html = render("payment/pay-form", model("page", page("Joel"), "action", "/public/pay/abc123",
                "selectedCountry", "COG", "phone", null, "error", null, "needOperator", false));

        assertThat(html).contains("method=\"post\"").contains("action=\"/public/pay/abc123\"");
        assertThat(html).contains("15 000 XAF").contains("Congo-Brazzaville (+242)").contains("Gabon (+241)");
        assertThat(html).doesNotContain("id=\"operator\"");
    }

    @Test
    @DisplayName("Quand l'operateur n'est pas devine, la page le demande, pour le pays choisi seulement")
    void formAsksForTheOperator() {
        String html = render("payment/pay-form", model("page", page("Joel"), "action", "/public/pay/abc123",
                "selectedCountry", "COG", "phone", "061234567", "error", "Choisissez-le.", "needOperator", true));

        assertThat(html).contains("id=\"operator\"").contains("MTN Mobile Money Congo").contains("Airtel Money Congo");
        assertThat(html).doesNotContain("Airtel Money Gabon</option>");
        assertThat(html).contains("value=\"061234567\"");
    }

    @Test
    @DisplayName("Le nom du client et les messages sont echappes")
    void contentIsEscaped() {
        String html = render("payment/pay-form", model("page", page("<script>x</script>"), "action", "/public/pay/abc",
                "selectedCountry", "COG", "phone", "\"><img>", "error", "<b>non</b>", "needOperator", false));

        assertThat(html).doesNotContain("<script>x</script>").doesNotContain("<b>non</b>").doesNotContain("\"><img>");
    }

    @Test
    @DisplayName("La page de suivi porte l'adresse d'etat et le lien pour reessayer")
    void statusRenders() {
        MobileMoneyDepositResponse deposit = mock(MobileMoneyDepositResponse.class);
        when(deposit.phase()).thenReturn("WAITING_FOR_PAYER");
        when(deposit.amount()).thenReturn(new BigDecimal("10"));
        when(deposit.currency()).thenReturn("XAF");
        when(deposit.depositId()).thenReturn("dep-123");

        String html = render("payment/pay-status", model("deposit", deposit,
                "stateUrl", "/public/pay/abc/suivi/dep-123/etat", "retryUrl", "/public/pay/abc"));

        assertThat(html).contains("data-url=\"/public/pay/abc/suivi/dep-123/etat\"").contains("data-phase=\"WAITING_FOR_PAYER\"");
        assertThat(html).contains("href=\"/public/pay/abc\"").contains("dep-123").contains("10 XAF");
    }

    @Test
    @DisplayName("Un lien ferme dit pourquoi")
    void closedRenders() {
        assertThat(render("payment/pay-closed", model("reason", "Ce paiement est déjà réglé. Merci.")))
                .contains("Ce paiement est déjà réglé. Merci.");
    }

    private static Map<String, Object> model(Object... pairs) {
        Map<String, Object> model = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            model.put((String) pairs[i], pairs[i + 1]);
        }
        return model;
    }

    private String render(String template, Map<String, Object> model) {
        MockServletContext servletContext = new MockServletContext();
        JakartaServletWebApplication application = JakartaServletWebApplication.buildApplication(servletContext);
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
