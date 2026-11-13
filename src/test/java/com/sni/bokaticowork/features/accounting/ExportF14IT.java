package com.sni.bokaticowork.features.accounting;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.core.settings.platform.PlatformSettings;
import com.sni.bokaticowork.features.accounting.dto.AccountingRequests;
import com.sni.bokaticowork.features.accounting.enums.UnbalancedPolicy;
import com.sni.bokaticowork.features.accounting.repository.AccFiscalYearRepository;
import com.sni.bokaticowork.features.accounting.service.implementation.AccountingArchiveService;
import com.sni.bokaticowork.features.accounting.service.implementation.AccountingChartService;
import com.sni.bokaticowork.features.accounting.service.implementation.AccountingEntryService;
import com.sni.bokaticowork.features.accounting.service.implementation.AccountingExportService;
import com.sni.bokaticowork.features.accounting.service.implementation.AccountingImportService;
import com.sni.bokaticowork.features.accounting.service.implementation.AccountingLetteringService;
import com.sni.bokaticowork.features.accounting.service.implementation.AccountingPeriodService;
import com.sni.bokaticowork.features.accounting.service.implementation.AccountingReportService;
import com.sni.bokaticowork.features.accounting.service.implementation.AnalyticAllocator;
import com.sni.bokaticowork.features.accounting.service.implementation.BalanceJustificationService;
import com.sni.bokaticowork.features.accounting.service.implementation.ClosingService;
import com.sni.bokaticowork.features.accounting.service.implementation.FinancialStatementService;
import com.sni.bokaticowork.features.accounting.service.support.AccountingNotifier;
import com.sni.bokaticowork.features.accounting.service.support.AccountingSettings;
import com.sni.bokaticowork.features.accounting.service.support.EntryNumbering;
import com.sni.bokaticowork.features.payment.service.support.CashActorResolver;
import com.sni.bokaticowork.support.PostgresIntegrationTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Lot F14 sur le schema de production, sur un exercice 2023 isole : reprise des soldes d'ouverture
 * et d'ecritures, exports Sage 100, Sage X3, Odoo et CSV, correspondance d'un journal, balance
 * exportee puis comparee sans ecart (critere du plan), archive annuelle et son sommaire.
 */
@Import({AccountingChartService.class, AccountingPeriodService.class, AccountingEntryService.class, AnalyticAllocator.class,
        AccountingLetteringService.class, EntryNumbering.class, CashActorResolver.class, AccountingReportService.class,
        FinancialStatementService.class, ClosingService.class, BalanceJustificationService.class, AccountingExportService.class,
        AccountingImportService.class, AccountingArchiveService.class, ExportF14IT.Beans.class})
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=validate")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ExportF14IT extends PostgresIntegrationTestBase {

    private static final AtomicInteger SEQ = new AtomicInteger(1);
    private static final String[] RIGHTS = {"ACCOUNTING_EXPORT_RUN", "ACCOUNTING_EXPORT_MAPPING", "ACCOUNTING_IMPORT_RUN", "ACCOUNTING_ARCHIVE_RUN",
            "ACCOUNTING_CLOSING_READ", "ACCOUNTING_REPORT_READ"};

    @TestConfiguration
    static class Beans {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper().findAndRegisterModules();
        }
    }

    @Autowired private AccountingExportService exports;
    @Autowired private AccountingImportService imports;
    @Autowired private AccountingArchiveService archives;
    @Autowired private AccountingPeriodService periods;
    @Autowired private AccFiscalYearRepository fiscalYears;
    @MockitoBean private AccountingSettings settings;
    @MockitoBean private AccountingNotifier notifier;
    @MockitoBean private SequenceGeneratorFacade sequences;
    @MockitoBean private PlatformSettings platformSettings;

    @BeforeEach
    void setUp() {
        com.sni.bokaticowork.core.settings.platform.PlatformSettingsStubs.fallbacks(platformSettings);
        when(sequences.next(anyString())).thenAnswer(invocation -> "BR-F14-" + SEQ.getAndIncrement() + "-" + UUID.randomUUID().toString().substring(0, 6));
        when(settings.enabled()).thenReturn(true);
        when(settings.familyEnabled(anyString())).thenReturn(true);
        when(settings.startDate()).thenReturn(LocalDate.of(2023, 1, 1));
        when(settings.unbalancedPolicy()).thenReturn(UnbalancedPolicy.HOLD);
        when(settings.strictChronology()).thenReturn(false);
        when(settings.numberingWidth()).thenReturn(6);
        when(settings.text(anyString(), anyString())).thenAnswer(invocation -> invocation.getArgument(1));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("comptable@elleaose.com", null,
                Arrays.stream(RIGHTS).map(SimpleGrantedAuthority::new).toList()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Exercice 2023 : reprise, exports Sage 100, Sage X3, Odoo et CSV, balance reimportee sans ecart, archive")
    void exportsAndReprise() throws Exception {
        if (fiscalYears.findByCode("2023").isEmpty()) {
            periods.createFiscalYear(new AccountingRequests.CreateFiscalYear("2023", LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31)));
        }
        // Soldes d'ouverture : la creance client reprise avec sa reference et son echeance.
        AccountingImportService.ImportResult opening = imports.importOpening("2023", csv(
                "compte;tiers;reference;echeance;debit;credit;libelle",
                "521;;;;5 000 000;;Banque",
                "411;CUS-F14;FAC-ANCIENNE;15/01/2023;1000000;;Facture ancienne",
                "101;;;;;4000000;Capital",
                "401;FRN-F14;FF-ANCIENNE;;;2000000,00;Fournisseur"));
        assertThat(opening.errors()).isEmpty();
        assertThat(opening.validated()).isEqualTo(1);

        // Ecritures : deux pieces valides et une en erreur (compte inconnu) : elle reste au brouillard, signalee, sans arreter le fichier.
        AccountingImportService.ImportResult imported = imports.importEntries(csv(
                "journal;date;piece;compte;tiers;libelle;debit;credit;reference;echeance",
                "OD;2023-02-01;P1;622;;Loyer de fevrier;300000;;;",
                "OD;2023-02-01;P1;521;;Loyer de fevrier;;300000;;",
                "OD;2023-03-01;P2;411;CUS-F14;Facture mars;590000;;FAC-MARS;2023-03-31",
                "OD;2023-03-01;P2;706;;Facture mars;;500000;;",
                "OD;2023-03-01;P2;4431;;Facture mars;;90000;;",
                "OD;2023-04-01;P3;999999;;Erreur;1000;;;",
                "OD;2023-04-01;P3;521;;Erreur;;1000;;"), true);
        assertThat(imported.entries()).isEqualTo(3);
        assertThat(imported.validated()).isEqualTo(2);
        assertThat(imported.errors()).singleElement().satisfies(error -> assertThat(error).contains("P3"));

        exports.saveMapping(new AccountingExportService.MappingRequest("SAGE100", "JOURNAL", "OD", "ODV"));
        LocalDate from = LocalDate.of(2023, 1, 1);
        LocalDate to = LocalDate.of(2023, 12, 31);

        // Sage 100 : 9 lignes, comptes sur 8 chiffres, journal converti, debits = credits.
        AccountingExportService.ExportFile sage = exports.export(new AccountingExportService.ExportRequest("SAGE100", "ENTRIES", from, to, null));
        List<String[]> sageLines = new String(sage.content(), Charset.forName("windows-1252")).lines().map(line -> line.split(";", -1)).toList();
        assertThat(sageLines).hasSize(9).allSatisfy(fields -> assertThat(fields).hasSize(10));
        assertThat(sageLines).anyMatch(fields -> fields[0].equals("ODV") && fields[3].equals("62200000"));
        assertThat(sum(sageLines, 6)).isEqualByComparingTo(sum(sageLines, 7));
        assertThat(sage.batch().getEntryCount()).isEqualTo(3);

        // Odoo : en-tete, puis la premiere ligne de chaque piece porte son identifiant.
        String odoo = new String(exports.export(new AccountingExportService.ExportRequest("ODOO", "ENTRIES", from, to, null)).content(), StandardCharsets.UTF_8);
        List<String> odooLines = odoo.lines().toList();
        assertThat(odooLines.get(0)).startsWith("id,journal_id,date,ref,line_ids/account_id");
        assertThat(odooLines).hasSize(10);
        assertThat(odooLines.stream().filter(line -> line.startsWith("\"bokati_")).count()).isEqualTo(3);

        // Sage X3 : 3 en-tetes G, 9 lignes D.
        List<String> x3 = new String(exports.export(new AccountingExportService.ExportRequest("SAGEX3", "ENTRIES", from, to, null)).content(),
                StandardCharsets.UTF_8).lines().toList();
        assertThat(x3.stream().filter(line -> line.startsWith("G;")).count()).isEqualTo(3);
        assertThat(x3.stream().filter(line -> line.startsWith("D;")).count()).isEqualTo(9);

        // CSV generique : colonne du marqueur de piece desequilibree.
        String generic = new String(exports.export(new AccountingExportService.ExportRequest("CSV", "ENTRIES", from, to, null)).content(), StandardCharsets.UTF_8);
        assertThat(generic.lines().findFirst().orElseThrow()).endsWith("Desequilibree");

        // Critere du plan : la balance exportee se reimporte sans ecart.
        AccountingExportService.ExportFile balance = exports.export(new AccountingExportService.ExportRequest("SAGE100", "BALANCE", from, to, null));
        List<String[]> balanceLines = new String(balance.content(), Charset.forName("windows-1252")).lines().map(line -> line.split(";", -1)).toList();
        assertThat(sum(balanceLines, 6)).isEqualByComparingTo(sum(balanceLines, 7));
        Map<String, BigDecimal[]> byAccount = new TreeMap<>();
        for (String[] fields : balanceLines) {
            BigDecimal[] totals = byAccount.computeIfAbsent(fields[3], key -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            totals[0] = totals[0].add(amount(fields[6]));
            totals[1] = totals[1].add(amount(fields[7]));
        }
        List<String> rows = new ArrayList<>(List.of("compte;debit;credit"));
        byAccount.forEach((account, totals) -> rows.add(account + ";" + totals[0].toPlainString().replace('.', ',') + ";" + totals[1].toPlainString().replace('.', ',')));
        AccountingImportService.Comparison comparison = imports.compare(to, "SAGE100", csv(rows.toArray(String[]::new)));
        assertThat(comparison.gaps()).isEmpty();
        assertThat(comparison.identical()).isTrue();
        // Un ecart sur un compte est detecte.
        rows.set(1, rows.get(1).replaceFirst(";", ";1"));
        assertThat(imports.compare(to, "SAGE100", csv(rows.toArray(String[]::new))).identical()).isFalse();
        assertThat(exports.batches()).hasSizeGreaterThanOrEqualTo(5);

        // Archive annuelle : sommaire avec l'empreinte de chaque fichier.
        AccountingArchiveService.Archive archive = archives.archive("2023");
        Map<String, byte[]> files = new HashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(archive.content()))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                files.put(entry.getName(), zip.readAllBytes());
            }
        }
        assertThat(files).containsKeys("manifest.json", "ecritures.csv", "balance.csv", "etats/bilan.json", "lettrages.csv", "pieces-justificatives.csv");
        String manifest = new String(files.get("manifest.json"), StandardCharsets.UTF_8);
        assertThat(manifest).contains("\"exercice\" : \"2023\"").contains("ecritures.csv").contains("sha256");
        assertThat(archive.record().getContentHash()).hasSize(64);
        assertThat(archives.archives("2023")).isNotEmpty();
    }

    private static byte[] csv(String... lines) {
        return String.join("\r\n", lines).getBytes(StandardCharsets.UTF_8);
    }

    private static BigDecimal amount(String value) {
        return value == null || value.isBlank() ? BigDecimal.ZERO : new BigDecimal(value.replace(',', '.'));
    }

    private static BigDecimal sum(List<String[]> lines, int column) {
        return lines.stream().map(fields -> amount(fields[column])).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
