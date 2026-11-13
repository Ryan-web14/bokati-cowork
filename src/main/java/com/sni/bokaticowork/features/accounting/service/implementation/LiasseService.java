package com.sni.bokaticowork.features.accounting.service.implementation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.identity.OperatorIdentity;
import com.sni.bokaticowork.core.identity.OperatorIdentityService;
import com.sni.bokaticowork.features.accounting.enums.FiscalYearStatus;
import com.sni.bokaticowork.features.accounting.model.AccFiscalYear;
import com.sni.bokaticowork.features.accounting.model.AccGeneratedReport;
import com.sni.bokaticowork.features.accounting.model.AccLiasseSheet;
import com.sni.bokaticowork.features.accounting.repository.AccGeneratedReportRepository;
import com.sni.bokaticowork.features.accounting.repository.AccLiasseSheetRepository;
import com.sni.bokaticowork.features.accounting.service.support.AccountingSettings;
import com.sni.bokaticowork.features.fund.voucher.VoucherService;
import com.sni.bokaticowork.features.payment.service.support.CashActorResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Liasse fiscale pre-remplie (lot F15, plan des etats, section 5) : fiche d'identification,
 * dirigeants, effectifs, etats financiers, notes annexes et tableau de passage au resultat fiscal,
 * dans l'ordre des feuillets du modele versionne ({@code acc_liasse_sheet}). Exports PDF et Excel
 * (classeur XML lu par Excel, un onglet par feuillet) ; edition definitive figee comme les etats.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class LiasseService {

    private final JdbcTemplate jdbc;
    private final AccLiasseSheetRepository sheets;
    private final AccGeneratedReportRepository reports;
    private final FinancialStatementService statements;
    private final ClosingService closing;
    private final TaxResultService taxes;
    private final AccountingSettings settings;
    private final CashActorResolver actors;
    private final ObjectMapper objectMapper;
    private final ObjectProvider<OperatorIdentityService> operator;
    private final ObjectProvider<VoucherService> vouchers;

    /** Un feuillet : intitule, colonnes, lignes. */
    public record Sheet(String code, String title, String kind, List<String> columns, List<Boolean> numeric, List<List<Object>> rows) {
    }

    public record Liasse(String fiscalYear, Integer version, LocalDate from, LocalDate to, boolean provisional, List<Sheet> sheets, Instant generatedAt) {
    }

    public Liasse build(String yearCode) {
        statements.require(FinancialStatementService.READ, "ACCOUNTING_REPORT:READ");
        AccFiscalYear year = statements.year(yearCode);
        Integer version = sheets.findFirstByActiveTrueOrderByVersionDesc().map(AccLiasseSheet::getVersion).orElse(1);
        List<Sheet> result = new ArrayList<>();
        for (AccLiasseSheet sheet : sheets.findAllByVersionAndActiveTrueOrderByPositionAsc(version)) {
            result.add(switch (sheet.getKind()) {
                case "IDENTIFICATION" -> identification(sheet, year);
                case "MANAGERS" -> managers(sheet);
                case "WORKFORCE" -> workforce(sheet, year);
                case "STATEMENT" -> statement(sheet, year);
                case "NOTES" -> notes(sheet, year);
                case "TAX" -> tax(sheet, year);
                default -> new Sheet(sheet.getCode(), sheet.getTitle(), sheet.getKind(), List.of(), List.of(), List.of());
            });
        }
        return new Liasse(year.getCode(), version, year.getStartDate(), year.getEndDate(), year.getStatus() != FiscalYearStatus.CLOSED, result, Instant.now());
    }

    // ================================================================== feuillets

    private OperatorIdentity identity() {
        OperatorIdentityService service = operator.getIfAvailable();
        return service == null ? OperatorIdentity.defaults() : service.current();
    }

    private Sheet identification(AccLiasseSheet sheet, AccFiscalYear year) {
        OperatorIdentity identity = identity();
        long months = ChronoUnit.MONTHS.between(year.getStartDate(), year.getEndDate().plusDays(1));
        List<List<Object>> rows = new ArrayList<>();
        pair(rows, "Denomination sociale", identity.name());
        pair(rows, "Forme juridique", identity.legalForm());
        pair(rows, "Numero d'identification unique (NIU)", identity.niu());
        pair(rows, "Registre du commerce (RCCM)", identity.rccm());
        pair(rows, "Adresse", identity.address());
        pair(rows, "Ville", identity.city());
        pair(rows, "Telephone", identity.phone());
        pair(rows, "Courriel", identity.email());
        pair(rows, "Activite principale", settings.text("accounting.liasse.activity", "Location d'espaces de travail partages et services associes"));
        pair(rows, "Centre des impots", settings.text("accounting.liasse.tax-office", ""));
        pair(rows, "Regime fiscal", settings.text("accounting.liasse.tax-regime", "Reel normal"));
        pair(rows, "Exercice du", year.getStartDate().toString());
        pair(rows, "Exercice au", year.getEndDate().toString());
        pair(rows, "Duree (mois)", String.valueOf(months));
        pair(rows, "Exercice precedent clos le", statements.previousYear(year).map(item -> item.getEndDate().toString()).orElse("Premier exercice"));
        pair(rows, "Referentiel comptable", "SYSCOHADA revise, systeme normal");
        return new Sheet(sheet.getCode(), sheet.getTitle(), sheet.getKind(), List.of("Rubrique", "Valeur"), List.of(false, false), rows);
    }

    private Sheet managers(AccLiasseSheet sheet) {
        OperatorIdentity identity = identity();
        List<List<Object>> rows = new ArrayList<>();
        rows.add(List.of(nz(identity.representativeName()), nz(identity.representativeTitle()), "Dirigeant"));
        String auditor = settings.text("accounting.liasse.auditor", "");
        if (StringUtils.hasText(auditor)) {
            rows.add(List.of(auditor, "Commissaire aux comptes", "Controle"));
        }
        String accountant = settings.text("accounting.liasse.accountant", "");
        if (StringUtils.hasText(accountant)) {
            rows.add(List.of(accountant, "Expert-comptable", "Tenue et revision"));
        }
        return new Sheet(sheet.getCode(), sheet.getTitle(), sheet.getKind(), List.of("Nom", "Qualite", "Role"), List.of(false, false, false), rows);
    }

    private Sheet workforce(AccLiasseSheet sheet, AccFiscalYear year) {
        Long employees = jdbc.queryForObject("SELECT COUNT(*) FROM acc_payroll_employee WHERE active = TRUE", Long.class);
        BigDecimal payroll = statements.balances(year.getStartDate(), year.getEndDate()).entrySet().stream()
                .filter(entry -> entry.getKey().startsWith("66")).map(Map.Entry::getValue).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal external = statements.balances(year.getStartDate(), year.getEndDate()).entrySet().stream()
                .filter(entry -> entry.getKey().startsWith("637")).map(Map.Entry::getValue).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<List<Object>> rows = new ArrayList<>();
        rows.add(List.of("Effectif salarie (fiches actives de paie)", BigDecimal.valueOf(employees == null ? 0 : employees)));
        rows.add(List.of("Charges de personnel (comptes 66)", payroll));
        rows.add(List.of("Personnel exterieur (compte 637)", external));
        return new Sheet(sheet.getCode(), sheet.getTitle(), sheet.getKind(), List.of("Rubrique", "Valeur"), List.of(false, true), rows);
    }

    private Sheet statement(AccLiasseSheet sheet, AccFiscalYear year) {
        FinancialStatementService.Statement statement = closing.statement(sheet.getSource() == null ? sheet.getCode() : sheet.getSource(), year);
        FixedAssetReportService.Table table = statements.table(statement);
        return new Sheet(sheet.getCode(), sheet.getTitle(), sheet.getKind(), table.columns(), table.numeric(), table.rows());
    }

    private Sheet notes(AccLiasseSheet sheet, AccFiscalYear year) {
        List<List<Object>> rows = new ArrayList<>();
        for (ClosingService.NoteView note : closing.notes(year.getCode())) {
            String content = note.notApplicable() ? "Neant" : note.text() ? nz(note.body()) : note.accounts().size() + " compte(s)";
            rows.add(List.of(note.code(), note.title(), note.total() == null ? BigDecimal.ZERO : note.total(),
                    note.previousTotal() == null ? "" : note.previousTotal(), content));
        }
        return new Sheet(sheet.getCode(), sheet.getTitle(), sheet.getKind(), List.of("Note", "Intitule", "Montant N", "Montant N-1", "Contenu"),
                List.of(false, false, true, true, false), rows);
    }

    private Sheet tax(AccLiasseSheet sheet, AccFiscalYear year) {
        FixedAssetReportService.Table table = taxes.table(taxes.computeFor(year));
        return new Sheet(sheet.getCode(), sheet.getTitle(), sheet.getKind(), table.columns(), table.numeric(), table.rows());
    }

    private static void pair(List<List<Object>> rows, String label, String value) {
        rows.add(List.of(label, nz(value)));
    }

    private static String nz(String value) {
        return value == null ? "" : value;
    }

    // ================================================================== exports

    /** PDF : un bloc par feuillet. */
    public byte[] pdf(Liasse liasse) {
        VoucherService renderer = vouchers.getIfAvailable();
        if (renderer == null) {
            throw new BadRequestException("Le rendu PDF n'est pas disponible");
        }
        String title = "Liasse fiscale " + liasse.fiscalYear() + (liasse.provisional() ? " (PROVISOIRE)" : "");
        VoucherService.Issued issued = new VoucherService.Issued("LIASSE-" + liasse.fiscalYear(), "LIASSE", liasse.fiscalYear(), null, title,
                actors.resolve("SYSTEM"), Instant.now(), 0, null, null);
        List<VoucherService.Section> sections = new ArrayList<>();
        for (Sheet sheet : liasse.sheets()) {
            VoucherService.Section section = new VoucherService.Section(sheet.title()).columns(sheet.columns(), sheet.numeric());
            for (List<Object> row : sheet.rows()) {
                section.row(row.stream().map(cell -> cell instanceof BigDecimal amount ? VoucherService.money(amount) : cell).toArray());
            }
            sections.add(section);
        }
        return renderer.render(title, issued, sections, null, null, List.of());
    }

    /** Classeur Excel (SpreadsheetML 2003, ouvert par Excel et LibreOffice) : un onglet par feuillet. */
    public byte[] excel(Liasse liasse) {
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<?mso-application progid=\"Excel.Sheet\"?>\n")
                .append("<Workbook xmlns=\"urn:schemas-microsoft-com:office:spreadsheet\" xmlns:ss=\"urn:schemas-microsoft-com:office:spreadsheet\">\n")
                .append("<Styles><Style ss:ID=\"h\"><Font ss:Bold=\"1\"/></Style></Styles>\n");
        for (Sheet sheet : liasse.sheets()) {
            String name = sheet.code().replaceAll("[\\\\/?*\\[\\]:]", "_");
            xml.append("<Worksheet ss:Name=\"").append(escape(name.length() > 31 ? name.substring(0, 31) : name)).append("\"><Table>\n");
            xml.append("<Row><Cell ss:StyleID=\"h\"><Data ss:Type=\"String\">").append(escape(sheet.title())).append("</Data></Cell></Row>\n<Row>");
            for (String column : sheet.columns()) {
                xml.append("<Cell ss:StyleID=\"h\"><Data ss:Type=\"String\">").append(escape(column)).append("</Data></Cell>");
            }
            xml.append("</Row>\n");
            for (List<Object> row : sheet.rows()) {
                xml.append("<Row>");
                for (Object cell : row) {
                    if (cell instanceof BigDecimal amount) {
                        xml.append("<Cell><Data ss:Type=\"Number\">").append(amount.stripTrailingZeros().toPlainString()).append("</Data></Cell>");
                    } else {
                        xml.append("<Cell><Data ss:Type=\"String\">").append(escape(cell == null ? "" : cell.toString())).append("</Data></Cell>");
                    }
                }
                xml.append("</Row>\n");
            }
            xml.append("</Table></Worksheet>\n");
        }
        xml.append("</Workbook>\n");
        return xml.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    // ================================================================== edition definitive

    /** Liasse definitive : exercice clos, donnees et PDF figes avec leur empreinte. */
    @Transactional
    public ClosingService.Edition finalize(String yearCode) {
        statements.require(FinancialStatementService.FINALIZE);
        AccFiscalYear year = statements.year(yearCode);
        if (year.getStatus() != FiscalYearStatus.CLOSED) {
            throw new BadRequestException("La liasse definitive ne se genere que sur un exercice clos");
        }
        Liasse liasse = build(year.getCode());
        String data;
        try {
            data = objectMapper.writeValueAsString(liasse);
        } catch (Exception exception) {
            throw new BadRequestException("Liasse illisible : " + exception.getMessage());
        }
        byte[] pdf = vouchers.getIfAvailable() == null ? null : pdf(liasse);
        AccGeneratedReport saved = reports.save(AccGeneratedReport.builder()
                .number("EF-" + year.getCode() + "-LIASSE-" + (reports.countByFiscalYearCode(year.getCode()) + 1))
                .reportCode("LIASSE").fiscalYearCode(year.getCode()).periodFrom(year.getStartDate()).periodTo(year.getEndDate())
                .templateVersion(liasse.version()).dataJson(data).contentHash(sha256(data)).pdf(pdf)
                .generatedBy(actors.resolve("SYSTEM")).generatedAt(Instant.now()).build());
        return new ClosingService.Edition(saved.getNumber(), saved.getReportCode(), saved.getFiscalYearCode(), saved.getPeriodFrom(), saved.getPeriodTo(),
                saved.getTemplateVersion(), saved.getContentHash(), null, saved.getGeneratedBy(), saved.getGeneratedAt());
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    static String lower(String value) {
        return value == null ? null : value.toLowerCase(Locale.ROOT);
    }
}
