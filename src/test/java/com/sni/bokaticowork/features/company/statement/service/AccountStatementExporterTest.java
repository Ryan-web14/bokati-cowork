package com.sni.bokaticowork.features.company.statement.service;

import com.sni.bokaticowork.core.utils.BrandLogo;
import com.sni.bokaticowork.features.company.statement.dto.StatementDtos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * L'etat de compte se rend en PDF avec le vrai modele, et en CSV.
 */
class AccountStatementExporterTest {

    private static SpringTemplateEngine engine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("/templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }

    private static StatementDtos.Statement statement() {
        LocalDate from = LocalDate.of(2026, 1, 1), to = LocalDate.of(2026, 10, 30);
        List<StatementDtos.Line> lines = List.of(
                new StatementDtos.Line("INV-1", "INVOICE", "OVERDUE", LocalDate.of(2025, 12, 1), LocalDate.of(2025, 12, 31), "XAF",
                        StatementDtos.AccountType.COMPANY, "CUS-COM-1", "SARL Mboté", null, null, null,
                        new BigDecimal("118900"), BigDecimal.ZERO, new BigDecimal("118900"), new BigDecimal("118900"), 303, false),
                new StatementDtos.Line("INV-2", "INVOICE", "SENT", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 11, 1), "XAF",
                        StatementDtos.AccountType.MEMBER, "MBR-1", "Jean Mbemba", "AT_ISSUE", "CUS-COM-1", "SARL Mboté",
                        new BigDecimal("50000"), new BigDecimal("10000"), new BigDecimal("40000"), new BigDecimal("40000"), 0, true));
        return new StatementDtos.Statement(StatementDtos.Subject.COMPANY, "CUS-COM-1", "SARL Mboté", "compta@mbote.cg",
                from, to, to, new BigDecimal("500000"), 30, true, "Impayés de septembre",
                AccountStatementService.totals(lines), AccountStatementService.accounts(lines), lines);
    }

    @Test
    @DisplayName("PDF : le modele se rend, organisation bloquee et lignes hors periode comprises")
    void rendersPdf() {
        AccountStatementExporter exporter = new AccountStatementExporter(engine(), Locale.FRANCE, new BrandLogo());

        byte[] pdf = exporter.pdf(statement());

        assertThat(new String(pdf, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
        String out = System.getenv("STATEMENT_PDF_OUT");
        if (out != null) {
            try {
                java.nio.file.Files.write(java.nio.file.Path.of(out), pdf);
            } catch (java.io.IOException ignored) {
                // Copie de controle visuel seulement.
            }
        }
    }

    @Test
    @DisplayName("CSV : une ligne par document, montants bruts")
    void rendersCsv() {
        AccountStatementExporter exporter = new AccountStatementExporter(engine(), Locale.FRANCE, new BrandLogo());

        String csv = exporter.csv(statement());

        assertThat(csv.lines()).hasSize(3);
        assertThat(csv).contains("\"INV-2\"").contains(";40000;").contains("\"AT_ISSUE\"");
    }

    @Test
    @DisplayName("Montants a la francaise, sans decimales en XAF")
    void formatsAmounts() {
        AccountStatementExporter.Formatter fmt = new AccountStatementExporter.Formatter(Locale.FRANCE);
        assertThat(fmt.amount(new BigDecimal("118900.4"), "XAF")).isEqualTo("118 900 XAF");
        assertThat(fmt.date(LocalDate.of(2026, 10, 30))).isEqualTo("30/10/2026");
    }
}
