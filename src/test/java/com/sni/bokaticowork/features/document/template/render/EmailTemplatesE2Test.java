package com.sni.bokaticowork.features.document.template.render;

import com.sni.bokaticowork.core.communication.mailService.config.BrandingDialect;
import com.sni.bokaticowork.core.communication.mailService.support.EmailBranding;
import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.FileTemplateResolver;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Lot E2 : chaque courriel inscrit a son gabarit, ses jetons obligatoires font partie des variables
 * permises, et les courriels autrefois ecrits en Java se rendent depuis leur gabarit.
 */
class EmailTemplatesE2Test {

    private static final Path TEMPLATES = Path.of("src/main/resources/templates");
    private static final Pattern ROW = Pattern.compile("\\('(EMAIL_[A-Z_]+)', '[^']*(?:''[^']*)*', '[A-Z_]+', '(email/[a-z_-]+)',\\s*'(?:[^']|'')*', (NULL|'[A-Za-z]+')\\)");

    private static Map<String, String[]> registered() throws Exception {
        String sql = Files.readString(Path.of("src/main/resources/db/migration/V356__modeles_documents_e2_courriels.sql"));
        Map<String, String[]> rows = new LinkedHashMap<>();
        Matcher matcher = Pattern.compile("\\(\\d+, " + ROW.pattern().substring(2)).matcher(sql);
        while (matcher.find()) {
            String required = "NULL".equals(matcher.group(3)) ? null : matcher.group(3).replace("'", "");
            rows.put(matcher.group(1), new String[]{matcher.group(2), required});
        }
        return rows;
    }

    @Test
    void everyRegisteredEmailHasItsTemplateAndItsRequiredTokens() throws Exception {
        Map<String, String[]> rows = registered();
        assertThat(rows).hasSize(64);
        String prod = Files.readString(Path.of("src/main/resources/db/migration-prod/V350__modeles_documents_e2_courriels.sql"));
        assertThat(prod).isEqualTo(Files.readString(Path.of("src/main/resources/db/migration/V356__modeles_documents_e2_courriels.sql")));
        for (Map.Entry<String, String[]> row : rows.entrySet()) {
            Path file = TEMPLATES.resolve(row.getValue()[0] + ".html");
            assertThat(file).as(row.getKey()).exists();
            if (row.getValue()[1] != null) {
                assertThat(EmailTemplates.variables(Files.readString(file))).as(row.getKey()).contains(row.getValue()[1]);
            }
        }
        assertThat(rows.get("EMAIL_OTT_LOGIN")[1]).isEqualTo("token");
        assertThat(rows.get("EMAIL_PASSWORD_RESET")[1]).isEqualTo("resetLink");
        assertThat(rows.get("EMAIL_PAYMENT_LINK")[1]).isEqualTo("checkoutUrl");
    }

    @Test
    void emailsOnceWrittenInJavaRenderFromTheirTemplateAndEscapeValues() {
        SpringTemplateEngine engine = new SpringTemplateEngine();
        FileTemplateResolver files = new FileTemplateResolver();
        files.setPrefix(TEMPLATES + "/");
        files.setSuffix(".html");
        files.setTemplateMode(TemplateMode.HTML);
        files.setCharacterEncoding("UTF-8");
        engine.addTemplateResolver(files);
        engine.addDialect(new BrandingDialect(new EmailBranding("https://cowork.elleaose.cg")));

        Context link = new Context(Locale.FRENCH);
        link.setVariable("recipientName", "Awa <b>Ngoma</b>");
        link.setVariable("amount", "150000");
        link.setVariable("currency", "XAF");
        link.setVariable("checkoutUrl", "https://pay.example/abc");
        link.setVariable("intentNumber", "PI-2026-000001");
        String html = engine.process("email/payment-link", link);
        assertThat(html).contains("Awa &lt;b&gt;Ngoma&lt;/b&gt;").contains("href=\"https://pay.example/abc\"").contains("PI-2026-000001");

        Context expiry = new Context(Locale.FRENCH);
        expiry.setVariable("customerName", "Awa");
        expiry.setVariable("documentNumber", "FAC-2026-00012");
        expiry.setVariable("amount", "150000");
        expiry.setVariable("currency", "XAF");
        expiry.setVariable("expiresAt", "15/11/2026 10:00");
        assertThat(engine.process("email/payment-expiry-reminder", expiry)).contains("FAC-2026-00012").contains("15/11/2026 10:00");

        Context quote = new Context(Locale.FRENCH);
        quote.setVariable("signerName", "Awa");
        quote.setVariable("quoteNumber", "DEV-2026-00087");
        quote.setVariable("signatureUrl", "https://app.example/sign/t");
        quote.setVariable("customMessage", "<script>x</script>");
        assertThat(engine.process("email/quote-signature-request", quote)).contains("href=\"https://app.example/sign/t\"")
                .contains("&lt;script&gt;").doesNotContain("<script>x");

        Context resolution = new Context(Locale.FRENCH);
        resolution.setVariable("action", "SUCCEEDED");
        resolution.setVariable("actor", "admin@bokati.com");
        resolution.setVariable("depositId", "DEP-1");
        assertThat(engine.process("email/mobile-money-resolution", resolution)).contains("SUCCEEDED").contains("DEP-1");
    }
}
