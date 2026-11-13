package com.sni.bokaticowork.features.accounting.service.implementation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.accounting.dto.AccountingRequests;
import com.sni.bokaticowork.features.accounting.dto.AccountingResponses;
import com.sni.bokaticowork.features.accounting.enums.EntryOrigin;
import com.sni.bokaticowork.features.accounting.enums.EntryStatus;
import com.sni.bokaticowork.features.accounting.enums.FiscalYearStatus;
import com.sni.bokaticowork.features.accounting.enums.LetteringKind;
import com.sni.bokaticowork.features.accounting.enums.LetteringStatus;
import com.sni.bokaticowork.features.accounting.enums.PeriodStatus;
import com.sni.bokaticowork.features.accounting.model.AccAccount;
import com.sni.bokaticowork.features.accounting.model.AccClosingStep;
import com.sni.bokaticowork.features.accounting.model.AccEntry;
import com.sni.bokaticowork.features.accounting.model.AccEntryLine;
import com.sni.bokaticowork.features.accounting.model.AccFiscalYear;
import com.sni.bokaticowork.features.accounting.model.AccGeneratedReport;
import com.sni.bokaticowork.features.accounting.model.AccLettering;
import com.sni.bokaticowork.features.accounting.model.AccLetteringLine;
import com.sni.bokaticowork.features.accounting.model.AccNoteText;
import com.sni.bokaticowork.features.accounting.model.AccPeriod;
import com.sni.bokaticowork.features.accounting.repository.AccAccountRepository;
import com.sni.bokaticowork.features.accounting.repository.AccClosingStepRepository;
import com.sni.bokaticowork.features.accounting.repository.AccEntryLineRepository;
import com.sni.bokaticowork.features.accounting.repository.AccFiscalYearRepository;
import com.sni.bokaticowork.features.accounting.repository.AccGeneratedReportRepository;
import com.sni.bokaticowork.features.accounting.repository.AccLetteringLineRepository;
import com.sni.bokaticowork.features.accounting.repository.AccLetteringRepository;
import com.sni.bokaticowork.features.accounting.repository.AccNoteTextRepository;
import com.sni.bokaticowork.features.accounting.repository.AccPeriodRepository;
import com.sni.bokaticowork.features.accounting.service.support.AccountingSettings;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Cloture d'exercice (lot F12) : liste de cloture, a-nouveaux, editions definitives, notes annexes.
 *
 * <p>La liste de cloture reunit les controles automatiques (balance, pieces en attente, comptes
 * d'attente, numerotation, rapprochements, caisses, immobilisations, provisions, constatations
 * d'avance, classement des comptes, bilan) et les etapes cochees a la main. L'exercice ne se clot
 * que lorsque chaque etape obligatoire est faite, ou levee par le responsable financier avec un
 * motif.</p>
 *
 * <p>A-nouveaux (plan, section 4.7) : dans le journal d'ouverture de l'exercice suivant, au premier
 * jour. Les comptes lettrables sont repris ligne a ligne (reste non lettre au 31 decembre, avec sa
 * reference et son echeance) ; la ligne de l'exercice clos est soldee par un lettrage de reprise,
 * et un lettrage deja fait avec une ligne de l'exercice suivant passe sur la ligne d'a-nouveau. Les
 * autres comptes reprennent leur solde par tiers. Le resultat va au compte 131 (benefice) ou 139
 * (perte). Les engagements hors bilan (comptes 90) sont repris dans le journal hors bilan ;
 * l'analytique (91 a 99) repart de zero.</p>
 */
@Service
@Transactional
@RequiredArgsConstructor
public class ClosingService {

    private final JdbcTemplate jdbc;
    private final FinancialStatementService statements;
    private final AccountingPeriodService periods;
    private final AccountingEntryService entries;
    private final AccountingLetteringService lettering;
    private final AccFiscalYearRepository fiscalYears;
    private final AccPeriodRepository periodRepository;
    private final AccAccountRepository accountRepository;
    private final AccEntryLineRepository lineRepository;
    private final AccLetteringRepository letteringRepository;
    private final AccLetteringLineRepository letteringLineRepository;
    private final AccClosingStepRepository steps;
    private final AccGeneratedReportRepository reports;
    private final AccNoteTextRepository notes;
    private final AccountingSettings settings;
    private final CashActorResolver actors;
    private final ObjectMapper objectMapper;
    private final ObjectProvider<AccountingReportService> accountingReports;
    private final ObjectProvider<BankService> banks;
    private final ObjectProvider<FixedAssetReportService> fixedAssets;
    private final ObjectProvider<RiskProvisionService> provisions;
    private final ObjectProvider<DeferralService> deferrals;
    private final BalanceJustificationService justifications;

    // ================================================================== liste de cloture

    /** Definition d'une etape : automatique (controle) ou a cocher ; obligatoire ou non. */
    public record StepDefinition(String code, String label, boolean automatic, boolean mandatory) {
    }

    public static final List<StepDefinition> STEPS = List.of(
            new StepDefinition("TRIAL_BALANCE", "Balance generale equilibree au dernier jour de l'exercice", true, true),
            new StepDefinition("PENDING_ENTRIES", "Aucune piece en attente d'equilibrage ni en brouillard dans l'exercice", true, true),
            new StepDefinition("UNBALANCED", "Compte d'attente d'equilibrage solde, aucune piece acceptee desequilibree non regularisee", true, true),
            new StepDefinition("NUMBERING", "Numerotation continue de chaque journal, chaine d'empreintes intacte", true, true),
            new StepDefinition("SUSPENSE_ACCOUNTS", "Comptes d'attente soldes (471, 585 par defaut)", true, true),
            new StepDefinition("BANK_RECONCILIATION", "Rapprochement bancaire valide au dernier jour pour chaque compte suivi", true, true),
            new StepDefinition("CASH_REVIEW", "Ecarts de caisse regularises : aucune session en revue d'ecart", true, true),
            new StepDefinition("FIXED_ASSETS", "Immobilisations : dotations passees, fichier egal aux comptes 2x et 28x", true, true),
            new StepDefinition("PROVISIONS", "Provisions et creances douteuses : revue validee, comptes justifies", true, true),
            new StepDefinition("DEFERRALS", "Constatations d'avance : traitements comptabilises, 477 et 476 justifies", true, true),
            new StepDefinition("COLLECTIVES", "Comptes collectifs justifies : solde egal a la somme des tiers, pieces ouvertes egales aux soldes", true, true),
            new StepDefinition("PRIOR_RESULT", "Resultat de l'exercice precedent affecte (comptes 13 soldes)", true, true),
            new StepDefinition("CLASS_NINE", "Balances analytique et hors bilan (classe 9) equilibrees", true, true),
            new StepDefinition("UNCLASSIFIED", "Chaque compte mouvemente est classe dans un poste des etats", true, true),
            new StepDefinition("STATEMENTS", "Bilan equilibre et resultat du bilan egal au resultat du compte de resultat", true, true),
            new StepDefinition("STAFF_ADVANCES", "Avances au personnel (421) revues", false, true),
            new StepDefinition("CASH_COUNT", "Proces-verbal de caisse au dernier jour etabli pour chaque caisse", false, true),
            new StepDefinition("STOCK_COUNT", "Inventaire des stocks rapproche des comptes 3", false, false),
            new StepDefinition("CHEQUES", "Inventaire des cheques a encaisser au dernier jour", false, false),
            new StepDefinition("NOTES", "Notes annexes redigees validees", true, false));

    public record StepView(String code, String label, boolean automatic, boolean mandatory, String status, String detail, String checkedBy,
                           Instant checkedAt, String comment) {
    }

    public record Checklist(String fiscalYear, String status, LocalDate start, LocalDate end, List<StepView> steps, boolean ready,
                            List<String> blocking, BigDecimal netResult, String openingEntryNumber) {
    }

    /** Liste de cloture : les controles automatiques sont recalcules a chaque lecture. */
    public Checklist checklist(String yearCode) {
        statements.require(FinancialStatementService.READ);
        AccFiscalYear year = statements.year(yearCode);
        if (year.getStatus() != FiscalYearStatus.CLOSED) {
            refresh(year);
        }
        return view(year);
    }

    /** Coche (ou decoche) une etape manuelle. */
    public Checklist tick(String yearCode, String stepCode, boolean done, String comment) {
        statements.require(FinancialStatementService.MANAGE);
        AccFiscalYear year = openYear(yearCode);
        StepDefinition definition = definition(stepCode);
        if (definition.automatic()) {
            throw new BadRequestException("L'etape " + definition.code() + " est un controle automatique : corrigez la cause, ou levez-la par derogation");
        }
        AccClosingStep step = step(year, definition);
        step.setStatus(done ? "DONE" : "TODO");
        step.setCheckedBy(done ? actors.resolve("SYSTEM") : null);
        step.setCheckedAt(done ? Instant.now() : null);
        step.setComment(trim(comment));
        steps.save(step);
        return view(year);
    }

    /** Leve une etape (derogation motivee du responsable financier). */
    public Checklist waive(String yearCode, String stepCode, String reason) {
        statements.require(FinancialStatementService.FINALIZE);
        AccFiscalYear year = openYear(yearCode);
        if (!StringUtils.hasText(reason)) {
            throw new BadRequestException("Indiquez le motif de la derogation");
        }
        StepDefinition definition = definition(stepCode);
        AccClosingStep step = step(year, definition);
        step.setStatus("WAIVED");
        step.setCheckedBy(actors.resolve("SYSTEM"));
        step.setCheckedAt(Instant.now());
        step.setComment(reason.trim());
        steps.save(step);
        return view(year);
    }

    private void refresh(AccFiscalYear year) {
        Map<String, Supplier<List<String>>> controls = new LinkedHashMap<>();
        LocalDate end = year.getEndDate();
        controls.put("TRIAL_BALANCE", () -> {
            BigDecimal gap = BigDecimal.ZERO;
            for (BigDecimal value : statements.balances(statements.scopeStart(end), end).entrySet().stream()
                    .filter(entry -> !entry.getKey().startsWith("9")).map(Map.Entry::getValue).toList()) {
                gap = gap.add(value);
            }
            return gap.signum() == 0 ? List.of() : List.of("Ecart de balance : " + plain(gap));
        });
        controls.put("PENDING_ENTRIES", () -> {
            Long count = jdbc.queryForObject("SELECT COUNT(*) FROM acc_entry WHERE status IN ('PENDING_BALANCE', 'DRAFT') AND entry_date BETWEEN ? AND ?",
                    Long.class, year.getStartDate(), end);
            return count == null || count == 0 ? List.of() : List.of(count + " piece(s) en attente ou en brouillard");
        });
        controls.put("UNBALANCED", () -> {
            List<String> issues = new ArrayList<>();
            BigDecimal suspense = balanceOf(year, settings.suspenseAccount());
            if (suspense.signum() != 0) {
                issues.add("Compte d'attente d'equilibrage " + settings.suspenseAccount() + " : solde " + plain(suspense));
            }
            Long accepted = jdbc.queryForObject("SELECT COUNT(*) FROM acc_entry WHERE accepted_unbalanced = TRUE AND regularized_by IS NULL "
                    + "AND status IN ('VALIDATED', 'REVERSED') AND entry_date BETWEEN ? AND ?", Long.class, year.getStartDate(), end);
            if (accepted != null && accepted > 0) {
                issues.add(accepted + " piece(s) acceptee(s) desequilibree(s) non regularisee(s)");
            }
            return issues;
        });
        controls.put("NUMBERING", () -> {
            AccountingReportService service = accountingReports.getIfAvailable();
            if (service == null) {
                return List.of();
            }
            List<String> issues = new ArrayList<>();
            for (String journal : jdbc.queryForList("SELECT DISTINCT journal_code FROM acc_entry WHERE fiscal_year_code = ? AND entry_number IS NOT NULL",
                    String.class, year.getCode())) {
                AccountingResponses.NumberingCheck check = service.checkContinuity(journal, year.getCode());
                if (!Boolean.TRUE.equals(check.ok())) {
                    issues.add("Journal " + journal + " : numerotation ou chaine d'empreintes en defaut");
                }
            }
            return issues;
        });
        controls.put("SUSPENSE_ACCOUNTS", () -> {
            List<String> issues = new ArrayList<>();
            for (String prefix : AccountingPeriodService.split(settings.text("accounting.closing.suspense-accounts", "471,585"))) {
                BigDecimal balance = balanceOf(year, prefix);
                if (balance.signum() != 0) {
                    issues.add("Comptes " + prefix + " : solde " + plain(balance));
                }
            }
            return issues;
        });
        controls.put("BANK_RECONCILIATION", () -> {
            BankService service = banks.getIfAvailable();
            List<String> missing = service == null ? List.of() : service.missingReconciliations(end);
            return missing.isEmpty() ? List.of() : List.of("Rapprochement non valide au " + end + " : " + String.join(", ", missing));
        });
        controls.put("CASH_REVIEW", () -> {
            Long count = jdbc.queryForObject("SELECT COUNT(*) FROM cash_session WHERE status = 'CLOSING_REVIEW' AND (closed_at IS NULL OR closed_at < ?)",
                    Long.class, java.sql.Timestamp.from(end.plusDays(1).atStartOfDay(PostingEngine.AccountingClock.ZONE).toInstant()));
            return count == null || count == 0 ? List.of() : List.of(count + " session(s) de caisse en revue d'ecart");
        });
        controls.put("FIXED_ASSETS", () -> findings(fixedAssets.getIfAvailable() == null ? List.of()
                : fixedAssets.getIfAvailable().closingCheck(end.getYear()).stream().map(item -> new String[]{item.severity(), item.message()}).toList()));
        controls.put("PROVISIONS", () -> findings(provisions.getIfAvailable() == null ? List.of()
                : provisions.getIfAvailable().closingCheck(end.getYear()).stream().map(item -> new String[]{item.severity(), item.message()}).toList()));
        controls.put("DEFERRALS", () -> findings(deferrals.getIfAvailable() == null ? List.of()
                : deferrals.getIfAvailable().closingCheck(end.getYear()).stream().map(item -> new String[]{item.severity(), item.message()}).toList()));
        controls.put("COLLECTIVES", () -> justifications.collectiveProblems(end));
        controls.put("PRIOR_RESULT", () -> {
            if (statements.previousYear(year).isEmpty()) {
                return List.of();
            }
            BigDecimal balance = balanceOf(year, "13");
            return balance.signum() == 0 ? List.of() : List.of("Comptes 13 : solde " + plain(balance) + ", le resultat precedent n'est pas affecte");
        });
        controls.put("CLASS_NINE", () -> {
            BigDecimal analytic = BigDecimal.ZERO;
            BigDecimal offBalance = BigDecimal.ZERO;
            for (Map.Entry<String, BigDecimal> entry : statements.balances(year.getStartDate(), end).entrySet()) {
                if (entry.getKey().startsWith("90")) {
                    offBalance = offBalance.add(entry.getValue());
                } else if (entry.getKey().startsWith("9")) {
                    analytic = analytic.add(entry.getValue());
                }
            }
            List<String> issues = new ArrayList<>();
            if (analytic.signum() != 0) {
                issues.add("Balance analytique desequilibree de " + plain(analytic));
            }
            if (offBalance.signum() != 0) {
                issues.add("Balance hors bilan desequilibree de " + plain(offBalance));
            }
            return issues;
        });
        FinancialStatementService.Statement balance = statements.balanceSheetAt(end);
        FinancialStatementService.Statement income = statements.incomeStatementFor(year.getStartDate(), end);
        controls.put("UNCLASSIFIED", () -> {
            List<String> issues = new ArrayList<>();
            balance.unclassified().forEach(item -> issues.add("Compte " + item.accountNumber() + " sans poste au bilan"));
            income.unclassified().forEach(item -> issues.add("Compte " + item.accountNumber() + " sans poste au compte de resultat"));
            return issues;
        });
        controls.put("STATEMENTS", () -> {
            List<String> issues = new ArrayList<>();
            balance.checks().stream().filter(check -> !check.ok()).forEach(check -> issues.add(check.label() + " : ecart " + plain(check.difference())));
            income.checks().stream().filter(check -> !check.ok()).forEach(check -> issues.add(check.label() + " : ecart " + plain(check.difference())));
            return issues;
        });
        controls.put("NOTES", () -> noteCatalogue().stream().filter(note -> note.text())
                .filter(note -> notes.findByFiscalYearCodeAndNoteCode(year.getCode(), note.code()).map(item -> !"VALIDATED".equals(item.getStatus())).orElse(true))
                .map(note -> "Note " + note.code() + " non validee").toList());
        controls.forEach((code, control) -> {
            AccClosingStep step = step(year, definition(code));
            if ("WAIVED".equals(step.getStatus())) {
                return;
            }
            List<String> issues;
            try {
                issues = control.get();
            } catch (RuntimeException exception) {
                issues = List.of("Controle impossible : " + exception.getMessage());
            }
            step.setStatus(issues.isEmpty() ? "DONE" : "FAILED");
            step.setDetail(issues.isEmpty() ? null : String.join(" ; ", issues));
            step.setCheckedBy("SYSTEM");
            step.setCheckedAt(Instant.now());
            steps.save(step);
        });
    }

    /** Les constats bloquants d'un controle de lot ; les avertissements ne bloquent pas. */
    private static List<String> findings(List<String[]> findings) {
        return findings.stream().filter(item -> "BLOCKING".equals(item[0])).map(item -> item[1]).toList();
    }

    private Checklist view(AccFiscalYear year) {
        Map<String, AccClosingStep> saved = steps.findAllByFiscalYearCode(year.getCode()).stream()
                .collect(Collectors.toMap(AccClosingStep::getStepCode, item -> item));
        List<StepView> result = new ArrayList<>();
        List<String> blocking = new ArrayList<>();
        for (StepDefinition definition : STEPS) {
            AccClosingStep step = saved.get(definition.code());
            String status = step == null ? "TODO" : step.getStatus();
            result.add(new StepView(definition.code(), definition.label(), definition.automatic(), definition.mandatory(), status,
                    step == null ? null : step.getDetail(), step == null ? null : step.getCheckedBy(), step == null ? null : step.getCheckedAt(),
                    step == null ? null : step.getComment()));
            if (definition.mandatory() && !"DONE".equals(status) && !"WAIVED".equals(status)) {
                blocking.add(definition.code());
            }
        }
        return new Checklist(year.getCode(), year.getStatus().name(), year.getStartDate(), year.getEndDate(), result,
                blocking.isEmpty() && year.getStatus() != FiscalYearStatus.CLOSED, blocking, year.getNetResult(), year.getOpeningEntryNumber());
    }

    private AccClosingStep step(AccFiscalYear year, StepDefinition definition) {
        return steps.findByFiscalYearCodeAndStepCode(year.getCode(), definition.code())
                .orElseGet(() -> AccClosingStep.builder().fiscalYearCode(year.getCode()).stepCode(definition.code()).status("TODO").build());
    }

    private static StepDefinition definition(String code) {
        String value = code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
        return STEPS.stream().filter(item -> item.code().equals(value)).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Etape de cloture inconnue : " + code));
    }

    private BigDecimal balanceOf(AccFiscalYear year, String prefix) {
        if (!StringUtils.hasText(prefix)) {
            return BigDecimal.ZERO;
        }
        return statements.balances(statements.scopeStart(year.getEndDate()), year.getEndDate()).entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(prefix.trim())).map(Map.Entry::getValue).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ================================================================== a-nouveaux

    /** Une ligne d'a-nouveau ; {@code sourceLineId} : ligne lettrable de l'exercice clos qu'elle prolonge. */
    public record OpeningLine(String accountNumber, String thirdPartyCode, String label, BigDecimal debit, BigDecimal credit, String reference,
                              LocalDate dueDate, String currency, BigDecimal foreignAmount, Long sourceLineId) {
    }

    public record OpeningPreview(String fiscalYear, String nextFiscalYear, LocalDate entryDate, String journal, BigDecimal netResult,
                                 List<OpeningLine> lines, List<OpeningLine> offBalanceLines, BigDecimal totalDebit, BigDecimal totalCredit) {
    }

    public record CloseRequest(String derogation) {
    }

    public record CloseResult(String fiscalYear, BigDecimal netResult, String openingEntryNumber, String offBalanceEntryNumber, int lines,
                              int carriedLines, String nextFiscalYear) {
    }

    /** Simulation des a-nouveaux, sans ecriture. */
    @Transactional(readOnly = true)
    public OpeningPreview previewOpening(String yearCode) {
        statements.require(FinancialStatementService.READ);
        AccFiscalYear year = statements.year(yearCode);
        return opening(year, nextYearCode(year));
    }

    /** Cloture de l'exercice : liste complete, a-nouveaux, periodes closes, exercice clos. */
    public CloseResult close(String yearCode, CloseRequest request) {
        statements.require(FinancialStatementService.FINALIZE);
        AccFiscalYear year = openYear(yearCode);
        for (AccFiscalYear earlier : fiscalYears.findAllByOrderByStartDateAsc()) {
            if (earlier.getEndDate().isBefore(year.getStartDate()) && earlier.getStatus() != FiscalYearStatus.CLOSED) {
                throw new BadRequestException("Closez d'abord l'exercice " + earlier.getCode());
            }
        }
        refresh(year);
        Checklist checklist = view(year);
        if (!checklist.blocking().isEmpty()) {
            throw new BadRequestException("Liste de cloture incomplete : " + String.join(", ", checklist.blocking())
                    + ". Corrigez, cochez ou levez chaque etape obligatoire.");
        }
        String actor = actors.resolve("SYSTEM");
        year.setStatus(FiscalYearStatus.CLOSING);
        fiscalYears.save(year);
        AccFiscalYear next = ensureNextYear(year);
        OpeningPreview preview = opening(year, next.getCode());
        String journal = preview.journal();
        AccEntry opening = null;
        if (!preview.lines().isEmpty()) {
            opening = entries.post(new AccountingEntryService.AutoPost(journal, next.getStartDate(), null,
                    "A-nouveaux de l'exercice " + year.getCode(), EntryOrigin.OPENING, "FISCAL_YEAR", year.getCode(),
                    preview.lines().stream().map(ClosingService::request).toList(), actor, true, null));
            if (opening.getStatus() != EntryStatus.VALIDATED) {
                throw new BadRequestException("La piece d'a-nouveaux n'a pas pu etre validee : " + opening.getPendingDetail());
            }
            carryLettering(year, opening, preview.lines(), actor);
        }
        AccEntry offBalance = null;
        if (!preview.offBalanceLines().isEmpty()) {
            offBalance = entries.post(new AccountingEntryService.AutoPost(settings.text("accounting.closing.off-balance-journal", "HB"),
                    next.getStartDate(), null, "Reprise des engagements hors bilan de l'exercice " + year.getCode(), EntryOrigin.OPENING,
                    "FISCAL_YEAR", year.getCode(), preview.offBalanceLines().stream().map(ClosingService::request).toList(), actor, true, null));
        }
        for (AccPeriod period : periodRepository.findAllByOrderByStartDateAsc()) {
            if (Objects.equals(period.getFiscalYearId(), year.getId()) && period.getStatus() != PeriodStatus.CLOSED) {
                periods.close(period.getCode());
            }
        }
        year.setStatus(FiscalYearStatus.CLOSED);
        year.setClosedAt(Instant.now());
        year.setClosedBy(actor);
        year.setNetResult(preview.netResult());
        year.setOpeningEntryNumber(opening == null ? null : opening.getEntryNumber());
        year.setClosingDerogation(request == null ? null : trim(request.derogation()));
        fiscalYears.save(year);
        int carried = (int) preview.lines().stream().filter(line -> line.sourceLineId() != null).count();
        return new CloseResult(year.getCode(), preview.netResult(), year.getOpeningEntryNumber(), offBalance == null ? null : offBalance.getEntryNumber(),
                preview.lines().size(), carried, next.getCode());
    }

    private OpeningPreview opening(AccFiscalYear year, String nextCode) {
        LocalDate end = year.getEndDate();
        LocalDate scope = statements.scopeStart(end);
        Map<String, AccAccount> chart = accountRepository.findAllByOrderByNumberAsc().stream()
                .collect(Collectors.toMap(AccAccount::getNumber, item -> item, (a, b) -> a));
        // Soldes par compte et par tiers.
        Map<String, Map<String, BigDecimal>> byThirdParty = new TreeMap<>();
        jdbc.query("SELECT l.account_number, COALESCE(l.third_party_code, ''), COALESCE(SUM(l.debit), 0) - COALESCE(SUM(l.credit), 0) "
                        + "FROM acc_entry_line l JOIN acc_entry e ON e.id = l.entry_id WHERE e.status IN ('VALIDATED', 'REVERSED') "
                        + "AND e.entry_date BETWEEN ? AND ? GROUP BY l.account_number, COALESCE(l.third_party_code, '')",
                rs -> {
                    byThirdParty.computeIfAbsent(rs.getString(1), key -> new TreeMap<>()).put(rs.getString(2), rs.getBigDecimal(3));
                }, scope, end);
        List<OpeningLine> lines = new ArrayList<>();
        List<OpeningLine> offBalance = new ArrayList<>();
        for (Map.Entry<String, Map<String, BigDecimal>> account : byThirdParty.entrySet()) {
            String number = account.getKey();
            if (number.startsWith("90")) {
                account.getValue().forEach((thirdParty, balance) -> addBalance(offBalance, number, thirdParty, balance, "Reprise hors bilan"));
                continue;
            }
            if (!FinancialStatementService.isBalanceSheet(number)) {
                continue;
            }
            AccAccount definition = chart.get(number);
            if (definition != null && Boolean.TRUE.equals(definition.getReconcilable())) {
                lines.addAll(detailed(number, account.getValue(), scope, end));
            } else {
                account.getValue().forEach((thirdParty, balance) -> addBalance(lines, number, thirdParty, balance, "A-nouveau"));
            }
        }
        BigDecimal result = statements.incomeResult(year.getStartDate(), end);
        if (result.signum() > 0) {
            lines.add(new OpeningLine(settings.text("accounting.closing.profit-account", "131"), null, "Resultat de l'exercice " + year.getCode(),
                    BigDecimal.ZERO, result, null, null, null, null, null));
        } else if (result.signum() < 0) {
            lines.add(new OpeningLine(settings.text("accounting.closing.loss-account", "139"), null, "Resultat de l'exercice " + year.getCode(),
                    result.negate(), BigDecimal.ZERO, null, null, null, null, null));
        }
        BigDecimal debit = lines.stream().map(OpeningLine::debit).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal credit = lines.stream().map(OpeningLine::credit).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new OpeningPreview(year.getCode(), nextCode, end.plusDays(1), settings.text("accounting.closing.opening-journal", "AN"), result,
                lines, offBalance, debit, credit);
    }

    private static void addBalance(List<OpeningLine> lines, String account, String thirdParty, BigDecimal balance, String label) {
        if (balance.signum() == 0) {
            return;
        }
        lines.add(new OpeningLine(account, thirdParty.isEmpty() ? null : thirdParty, label, balance.signum() > 0 ? balance : BigDecimal.ZERO,
                balance.signum() < 0 ? balance.negate() : BigDecimal.ZERO, null, null, null, null, null));
    }

    /** Lignes non lettrees au dernier jour, reprises une a une ; un reste d'arrondi complete le solde du tiers. */
    private List<OpeningLine> detailed(String account, Map<String, BigDecimal> balances, LocalDate scope, LocalDate end) {
        Map<Long, BigDecimal> inner = new HashMap<>();
        jdbc.query("SELECT ll.entry_line_id, ll.matched_amount, EXISTS (SELECT 1 FROM acc_lettering_line o JOIN acc_entry_line ol ON ol.id = o.entry_line_id "
                        + "JOIN acc_entry oe ON oe.id = ol.entry_id WHERE o.lettering_id = lt.id AND oe.entry_date > ?) "
                        + "FROM acc_lettering_line ll JOIN acc_lettering lt ON lt.id = ll.lettering_id JOIN acc_entry_line el ON el.id = ll.entry_line_id "
                        + "JOIN acc_entry e ON e.id = el.entry_id WHERE lt.status <> 'UNLETTERED' AND el.account_number = ? "
                        + "AND e.status IN ('VALIDATED', 'REVERSED') AND e.entry_date BETWEEN ? AND ?",
                rs -> {
                    if (!rs.getBoolean(3)) {
                        inner.merge(rs.getLong(1), rs.getBigDecimal(2), BigDecimal::add);
                    }
                }, end, account, scope, end);
        List<OpeningLine> lines = new ArrayList<>();
        Map<String, BigDecimal> carried = new HashMap<>();
        jdbc.query("SELECT l.id, COALESCE(l.third_party_code, ''), l.label, l.debit, l.credit, l.reference, l.due_date, l.currency, l.foreign_amount, "
                        + "e.entry_number FROM acc_entry_line l JOIN acc_entry e ON e.id = l.entry_id WHERE l.account_number = ? "
                        + "AND e.status IN ('VALIDATED', 'REVERSED') AND e.entry_date BETWEEN ? AND ? ORDER BY e.entry_date, e.id, l.line_no",
                rs -> {
                    BigDecimal debit = rs.getBigDecimal(4);
                    BigDecimal credit = rs.getBigDecimal(5);
                    boolean isDebit = debit.signum() > 0;
                    BigDecimal amount = isDebit ? debit : credit;
                    BigDecimal rest = amount.subtract(inner.getOrDefault(rs.getLong(1), BigDecimal.ZERO));
                    if (rest.signum() <= 0) {
                        return;
                    }
                    BigDecimal foreign = rs.getBigDecimal(9);
                    if (foreign != null && amount.signum() != 0) {
                        foreign = foreign.multiply(rest).divide(amount, 4, java.math.RoundingMode.HALF_UP);
                    }
                    String thirdParty = rs.getString(2);
                    String label = rs.getString(3);
                    lines.add(new OpeningLine(account, thirdParty.isEmpty() ? null : thirdParty,
                            "A-nouveau " + rs.getString(10) + (StringUtils.hasText(label) ? " " + label : ""),
                            isDebit ? rest : BigDecimal.ZERO, isDebit ? BigDecimal.ZERO : rest, rs.getString(6),
                            rs.getDate(7) == null ? null : rs.getDate(7).toLocalDate(), rs.getString(8), foreign, rs.getLong(1)));
                    carried.merge(thirdParty, isDebit ? rest : rest.negate(), BigDecimal::add);
                }, account, scope, end);
        balances.forEach((thirdParty, balance) -> {
            BigDecimal gap = balance.subtract(carried.getOrDefault(thirdParty, BigDecimal.ZERO));
            addBalance(lines, account, thirdParty, gap, "A-nouveau, ecart de lettrage");
        });
        return lines;
    }

    /**
     * Suite du lettrage : chaque ligne reprise est soldee par un lettrage de reprise ; un lettrage
     * deja fait avec une ligne de l'exercice suivant passe sur la ligne d'a-nouveau.
     */
    private void carryLettering(AccFiscalYear year, AccEntry opening, List<OpeningLine> requested, String actor) {
        List<AccEntryLine> created = lineRepository.findByEntryIdOrderByLineNoAsc(opening.getId());
        Map<String, List<Long[]>> groups = new LinkedHashMap<>();
        Map<Long, Long> successor = new HashMap<>();
        Map<Long, BigDecimal> carriedAmount = new HashMap<>();
        for (int index = 0; index < requested.size() && index < created.size(); index++) {
            OpeningLine line = requested.get(index);
            if (line.sourceLineId() == null) {
                continue;
            }
            AccEntryLine target = created.get(index);
            target.setCarriedFromLineId(line.sourceLineId());
            lineRepository.save(target);
            successor.put(line.sourceLineId(), target.getId());
            carriedAmount.put(line.sourceLineId(), line.debit().signum() > 0 ? line.debit() : line.credit());
            groups.computeIfAbsent(line.accountNumber() + "|" + Objects.toString(line.thirdPartyCode(), ""), key -> new ArrayList<>())
                    .add(new Long[]{line.sourceLineId()});
        }
        if (successor.isEmpty()) {
            return;
        }
        // Lettrages croises (avec une ligne posterieure au dernier jour) : la ligne close cede sa place.
        jdbc.query("SELECT ll.id, ll.entry_line_id FROM acc_lettering_line ll JOIN acc_lettering lt ON lt.id = ll.lettering_id "
                        + "WHERE lt.status <> 'UNLETTERED' AND ll.entry_line_id IN (" + successor.keySet().stream().map(String::valueOf).collect(Collectors.joining(",")) + ") "
                        + "AND EXISTS (SELECT 1 FROM acc_lettering_line o JOIN acc_entry_line ol ON ol.id = o.entry_line_id "
                        + "JOIN acc_entry oe ON oe.id = ol.entry_id WHERE o.lettering_id = lt.id AND oe.entry_date > ?)",
                rs -> {
                    AccLetteringLine moved = letteringLineRepository.findById(rs.getLong(1)).orElseThrow();
                    moved.setEntryLineId(successor.get(rs.getLong(2)));
                    letteringLineRepository.save(moved);
                }, year.getEndDate());
        for (Map.Entry<String, List<Long[]>> group : groups.entrySet()) {
            String[] key = group.getKey().split("\\|", -1);
            String thirdParty = key[1].isEmpty() ? null : key[1];
            BigDecimal debit = BigDecimal.ZERO;
            BigDecimal credit = BigDecimal.ZERO;
            for (Long[] source : group.getValue()) {
                AccEntryLine original = lineRepository.findById(source[0]).orElseThrow();
                if (original.getDebit().signum() > 0) {
                    debit = debit.add(carriedAmount.get(source[0]));
                } else {
                    credit = credit.add(carriedAmount.get(source[0]));
                }
            }
            AccLettering saved = letteringRepository.save(AccLettering.builder()
                    .code(lettering.nextCode(key[0], thirdParty))
                    .accountNumber(key[0])
                    .thirdPartyCode(thirdParty)
                    .kind(LetteringKind.CARRY_FORWARD)
                    .status(LetteringStatus.COMPLETE)
                    .totalDebit(debit)
                    .totalCredit(credit)
                    .difference(BigDecimal.ZERO)
                    .differenceEntryNumber(opening.getEntryNumber())
                    .letteredBy(actor)
                    .build());
            for (Long[] source : group.getValue()) {
                letteringLineRepository.save(AccLetteringLine.builder().letteringId(saved.getId()).entryLineId(source[0])
                        .matchedAmount(carriedAmount.get(source[0])).build());
            }
        }
    }

    private static AccountingRequests.EntryLine request(OpeningLine line) {
        return new AccountingRequests.EntryLine(line.accountNumber(), line.thirdPartyCode(), line.label(),
                line.debit().signum() > 0 ? line.debit() : null, line.credit().signum() > 0 ? line.credit() : null, line.currency(),
                line.foreignAmount(), null, null, null, line.dueDate(), line.reference());
    }

    private AccFiscalYear ensureNextYear(AccFiscalYear year) {
        LocalDate start = year.getEndDate().plusDays(1);
        Optional<AccFiscalYear> existing = fiscalYears.findAllByOrderByStartDateAsc().stream()
                .filter(item -> !start.isBefore(item.getStartDate()) && !start.isAfter(item.getEndDate())).findFirst();
        if (existing.isPresent()) {
            if (existing.get().getStatus() == FiscalYearStatus.CLOSED) {
                throw new BadRequestException("L'exercice suivant " + existing.get().getCode() + " est deja clos");
            }
            return existing.get();
        }
        periods.createFiscalYear(new AccountingRequests.CreateFiscalYear(nextYearCode(year), start, start.plusYears(1).minusDays(1)));
        return fiscalYears.findByCode(nextYearCode(year)).orElseThrow();
    }

    private String nextYearCode(AccFiscalYear year) {
        LocalDate start = year.getEndDate().plusDays(1);
        return fiscalYears.findAllByOrderByStartDateAsc().stream()
                .filter(item -> !start.isBefore(item.getStartDate()) && !start.isAfter(item.getEndDate()))
                .map(AccFiscalYear::getCode).findFirst().orElse(String.valueOf(start.getYear()));
    }

    private AccFiscalYear openYear(String code) {
        AccFiscalYear year = statements.year(code);
        if (year.getStatus() == FiscalYearStatus.CLOSED) {
            throw new BadRequestException("L'exercice " + year.getCode() + " est clos");
        }
        return year;
    }

    // ================================================================== editions definitives

    public static final List<String> FINAL_REPORTS = List.of(FinancialStatementService.BALANCE_SHEET, FinancialStatementService.INCOME_STATEMENT,
            FinancialStatementService.CASH_FLOW);

    public record EditionRequest(String report, String derogation) {
    }

    public record Edition(String number, String report, String fiscalYear, LocalDate from, LocalDate to, Integer templateVersion,
                          String contentHash, String derogation, String generatedBy, Instant generatedAt) {
    }

    /**
     * Edition definitive : seulement sur un exercice clos, controles de coherence au vert ou
     * derogation motivee. Les donnees et le PDF sont figes avec leur empreinte SHA-256.
     */
    public Edition finalize(String yearCode, EditionRequest request) {
        statements.require(FinancialStatementService.FINALIZE);
        AccFiscalYear year = statements.year(yearCode);
        if (year.getStatus() != FiscalYearStatus.CLOSED) {
            throw new BadRequestException("Un etat definitif ne se genere que sur un exercice clos");
        }
        String code = request == null || request.report() == null ? "" : request.report().trim().toUpperCase(Locale.ROOT);
        if (!FINAL_REPORTS.contains(code)) {
            throw new BadRequestException("Etat inconnu : " + code + " (" + String.join(", ", FINAL_REPORTS) + ")");
        }
        FinancialStatementService.Statement statement = statement(code, year);
        String derogation = trim(request.derogation());
        if (!statement.consistent() && derogation == null) {
            throw new BadRequestException("Controles de coherence en echec : corrigez, ou indiquez le motif de la derogation");
        }
        String data;
        try {
            data = objectMapper.writeValueAsString(statement);
        } catch (Exception exception) {
            throw new BadRequestException("Etat illisible : " + exception.getMessage());
        }
        FixedAssetReportService renderer = fixedAssets.getIfAvailable();
        byte[] pdf = renderer == null ? null : renderer.pdf(statements.table(statement));
        long rank = reports.countByFiscalYearCode(year.getCode()) + 1;
        AccGeneratedReport saved = reports.save(AccGeneratedReport.builder()
                .number("EF-" + year.getCode() + "-" + code + "-" + rank)
                .reportCode(code)
                .fiscalYearCode(year.getCode())
                .periodFrom(statement.from())
                .periodTo(statement.to())
                .templateVersion(statement.version())
                .dataJson(data)
                .contentHash(sha256(data))
                .pdf(pdf)
                .derogation(derogation)
                .generatedBy(actors.resolve("SYSTEM"))
                .generatedAt(Instant.now())
                .build());
        return edition(saved);
    }

    @Transactional(readOnly = true)
    public List<Edition> editions(String yearCode) {
        statements.require(FinancialStatementService.READ);
        return reports.findAllByFiscalYearCodeOrderByGeneratedAtDesc(statements.year(yearCode).getCode()).stream().map(ClosingService::edition).toList();
    }

    /** PDF fige d'une edition definitive, reimprime a l'identique. */
    @Transactional(readOnly = true)
    public byte[] editionPdf(String number) {
        statements.require(FinancialStatementService.READ);
        AccGeneratedReport report = reports.findByNumber(number).orElseThrow(() -> new ResourceNotFoundException("Edition introuvable : " + number));
        if (report.getPdf() == null) {
            throw new BadRequestException("Cette edition n'a pas de PDF");
        }
        return report.getPdf();
    }

    /** Verifie qu'une edition n'a pas ete alteree : empreinte recalculee sur les donnees figees. */
    @Transactional(readOnly = true)
    public boolean verify(String number) {
        statements.require(FinancialStatementService.READ);
        AccGeneratedReport report = reports.findByNumber(number).orElseThrow(() -> new ResourceNotFoundException("Edition introuvable : " + number));
        return sha256(report.getDataJson()).equals(report.getContentHash());
    }

    FinancialStatementService.Statement statement(String code, AccFiscalYear year) {
        return switch (code) {
            case FinancialStatementService.BALANCE_SHEET -> statements.balanceSheetAt(year.getEndDate());
            case FinancialStatementService.INCOME_STATEMENT -> statements.incomeStatementFor(year.getStartDate(), year.getEndDate());
            default -> statements.cashFlowFor(year);
        };
    }

    private static Edition edition(AccGeneratedReport report) {
        return new Edition(report.getNumber(), report.getReportCode(), report.getFiscalYearCode(), report.getPeriodFrom(), report.getPeriodTo(),
                report.getTemplateVersion(), report.getContentHash(), report.getDerogation(), report.getGeneratedBy(), report.getGeneratedAt());
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    // ================================================================== notes annexes

    /**
     * Note du SYSCOHADA revise. {@code prefixes} : comptes repris automatiquement (note chiffree) ;
     * {@code source} : etat d'un autre lot qui la detaille ; {@code text} : note redigee.
     */
    public record NoteDefinition(String code, String title, List<String> prefixes, String source, boolean text) {
    }

    private static NoteDefinition figures(String code, String title, String... prefixes) {
        return new NoteDefinition(code, title, List.of(prefixes), null, false);
    }

    public static List<NoteDefinition> noteCatalogue() {
        return List.of(
                new NoteDefinition("1", "Dettes garanties par des suretes reelles", List.of(), null, true),
                new NoteDefinition("2", "Informations obligatoires : regles et methodes comptables", List.of(), null, true),
                new NoteDefinition("3A", "Immobilisations brutes", List.of("2"), "/accounting/fixed-assets/reports/note-3a", false),
                new NoteDefinition("3B", "Biens pris en location-acquisition", List.of("17"), "/accounting/fixed-assets/reports/note-3b", false),
                new NoteDefinition("3C", "Immobilisations : amortissements", List.of("28"), "/accounting/fixed-assets/reports/note-3c", false),
                new NoteDefinition("3D", "Immobilisations : plus-values et moins-values de cession", List.of("81", "82"), "/accounting/fixed-assets/reports/note-3d", false),
                new NoteDefinition("3E", "Informations sur les reevaluations", List.of("106"), null, false),
                figures("4", "Immobilisations financieres", "26", "27"),
                figures("5", "Actif circulant et dettes circulantes HAO", "48"),
                figures("6", "Stocks et encours", "3"),
                figures("7", "Clients", "41"),
                figures("8", "Autres creances", "42", "43", "44", "45", "46", "47"),
                figures("9", "Titres de placement", "50"),
                figures("10", "Valeurs a encaisser", "51"),
                figures("11", "Disponibilites", "52", "53", "54", "55", "57", "58"),
                figures("12", "Ecarts de conversion et transferts de charges", "478", "479", "78"),
                figures("13", "Capital : valeur nominale des actions ou parts", "10"),
                figures("14", "Primes et reserves", "105", "11", "12"),
                figures("15A", "Subventions et provisions reglementees", "14", "15"),
                figures("16A", "Dettes financieres et ressources assimilees", "16", "17", "18", "19"),
                new NoteDefinition("16B", "Engagements de retraite et avantages assimiles", List.of("196"), "/accounting/provisions/reports", false),
                new NoteDefinition("16C", "Actifs et passifs eventuels", List.of(), null, true),
                figures("17", "Fournisseurs d'exploitation", "40"),
                figures("18", "Dettes fiscales et sociales", "42", "43", "44"),
                figures("19", "Autres dettes et provisions pour risques a court terme", "185", "45", "46", "47", "499", "599"),
                figures("20", "Banques, credit d'escompte et de tresorerie", "56"),
                figures("21", "Chiffre d'affaires et autres produits", "70", "71", "72", "73", "75"),
                figures("22", "Achats", "60"),
                figures("23", "Transports", "61"),
                figures("24", "Services exterieurs", "62", "63"),
                figures("25", "Impots et taxes", "64"),
                figures("26", "Autres charges", "65"),
                figures("27A", "Charges de personnel", "66"),
                new NoteDefinition("27B", "Effectifs, masse salariale et personnel exterieur", List.of(), null, true),
                new NoteDefinition("28", "Provisions et depreciations inscrites au bilan", List.of("19", "29", "39", "49", "59", "68", "69", "79"),
                        "/accounting/provisions/reports", false),
                figures("29", "Charges et revenus financiers", "67", "77"),
                figures("30", "Autres charges et produits HAO", "83", "84", "85", "86", "87", "88", "89"),
                new NoteDefinition("31", "Repartition du resultat et elements des cinq derniers exercices", List.of("13"), null, false),
                new NoteDefinition("35", "Informations sociales, environnementales et societales", List.of(), null, true),
                new NoteDefinition("EP", "Evenements posterieurs a la cloture", List.of(), null, true),
                new NoteDefinition("PL", "Parties liees", List.of(), null, true),
                new NoteDefinition("HB", "Engagements hors bilan", List.of("90"), null, true));
    }

    public record NoteAccount(String accountNumber, String label, BigDecimal amount, BigDecimal previous) {
    }

    public record NoteView(String code, String title, boolean text, String source, boolean notApplicable, String body, String status,
                           String validatedBy, Instant validatedAt, boolean reusedFromPreviousYear, List<NoteAccount> accounts, BigDecimal total,
                           BigDecimal previousTotal) {
    }

    public record NoteRequest(String body, Boolean notApplicable) {
    }

    /** Notes d'un exercice : montants repris des comptes, textes saisis (repris de N-1 a defaut). */
    @Transactional(readOnly = true)
    public List<NoteView> notes(String yearCode) {
        statements.require(FinancialStatementService.READ);
        AccFiscalYear year = statements.year(yearCode);
        Optional<AccFiscalYear> previous = statements.previousYear(year);
        Map<String, BigDecimal> current = noteBalances(year);
        Map<String, BigDecimal> before = previous.map(this::noteBalances).orElse(Map.of());
        Map<String, AccAccount> chart = accountRepository.findAllByOrderByNumberAsc().stream()
                .collect(Collectors.toMap(AccAccount::getNumber, item -> item, (a, b) -> a));
        List<NoteView> result = new ArrayList<>();
        for (NoteDefinition note : noteCatalogue()) {
            List<NoteAccount> lines = new ArrayList<>();
            java.util.Set<String> numbers = new java.util.TreeSet<>();
            numbers.addAll(current.keySet());
            numbers.addAll(before.keySet());
            for (String number : numbers) {
                if (note.prefixes().stream().noneMatch(number::startsWith)) {
                    continue;
                }
                BigDecimal amount = current.getOrDefault(number, BigDecimal.ZERO);
                BigDecimal last = before.getOrDefault(number, BigDecimal.ZERO);
                if (amount.signum() != 0 || last.signum() != 0) {
                    lines.add(new NoteAccount(number, chart.containsKey(number) ? chart.get(number).getLabel() : null, amount, last));
                }
            }
            Optional<AccNoteText> saved = notes.findByFiscalYearCodeAndNoteCode(year.getCode(), note.code());
            Optional<AccNoteText> reused = saved.isPresent() || previous.isEmpty() ? Optional.empty()
                    : notes.findByFiscalYearCodeAndNoteCode(previous.get().getCode(), note.code());
            AccNoteText text = saved.or(() -> reused).orElse(null);
            boolean empty = lines.isEmpty() && (text == null || !StringUtils.hasText(text.getBody()));
            result.add(new NoteView(note.code(), note.title(), note.text(), note.source(),
                    text != null ? Boolean.TRUE.equals(text.getNotApplicable()) : empty,
                    text == null ? null : text.getBody(), saved.map(AccNoteText::getStatus).orElse(null),
                    saved.map(AccNoteText::getValidatedBy).orElse(null), saved.map(AccNoteText::getValidatedAt).orElse(null),
                    saved.isEmpty() && reused.isPresent(), lines,
                    lines.stream().map(NoteAccount::amount).reduce(BigDecimal.ZERO, BigDecimal::add),
                    previous.isEmpty() ? null : lines.stream().map(NoteAccount::previous).reduce(BigDecimal.ZERO, BigDecimal::add)));
        }
        return result;
    }

    public NoteView saveNote(String yearCode, String noteCode, NoteRequest request) {
        statements.require(FinancialStatementService.MANAGE);
        AccFiscalYear year = openYear(yearCode);
        NoteDefinition note = note(noteCode);
        AccNoteText text = notes.findByFiscalYearCodeAndNoteCode(year.getCode(), note.code())
                .orElseGet(() -> AccNoteText.builder().fiscalYearCode(year.getCode()).noteCode(note.code()).build());
        if ("VALIDATED".equals(text.getStatus())) {
            throw new BadRequestException("La note " + note.code() + " est validee : rouvrez-la avant de la modifier");
        }
        text.setBody(request == null ? null : request.body());
        text.setNotApplicable(request != null && Boolean.TRUE.equals(request.notApplicable()));
        text.setAuthor(actors.resolve("SYSTEM"));
        text.setUpdatedAt(Instant.now());
        notes.save(text);
        return notes(year.getCode()).stream().filter(item -> item.code().equals(note.code())).findFirst().orElseThrow();
    }

    /** Valide une note (ou la rouvre, {@code validate} faux). */
    public NoteView validateNote(String yearCode, String noteCode, boolean validate) {
        statements.require(FinancialStatementService.FINALIZE, FinancialStatementService.MANAGE);
        AccFiscalYear year = openYear(yearCode);
        NoteDefinition note = note(noteCode);
        AccNoteText text = notes.findByFiscalYearCodeAndNoteCode(year.getCode(), note.code())
                .orElseThrow(() -> new BadRequestException("La note " + note.code() + " n'est pas redigee"));
        text.setStatus(validate ? "VALIDATED" : "DRAFT");
        text.setValidatedBy(validate ? actors.resolve("SYSTEM") : null);
        text.setValidatedAt(validate ? Instant.now() : null);
        notes.save(text);
        return notes(year.getCode()).stream().filter(item -> item.code().equals(note.code())).findFirst().orElseThrow();
    }

    private Map<String, BigDecimal> noteBalances(AccFiscalYear year) {
        Map<String, BigDecimal> result = new TreeMap<>();
        statements.balances(statements.scopeStart(year.getEndDate()), year.getEndDate()).forEach((account, amount) -> {
            if (FinancialStatementService.isBalanceSheet(account) || account.startsWith("90")) {
                result.put(account, amount);
            }
        });
        statements.balances(year.getStartDate(), year.getEndDate()).forEach((account, amount) -> {
            if (FinancialStatementService.isIncome(account)) {
                result.put(account, amount);
            }
        });
        return result;
    }

    private static NoteDefinition note(String code) {
        String value = code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
        return noteCatalogue().stream().filter(item -> item.code().equals(value)).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Note annexe inconnue : " + code));
    }

    // ================================================================== outils

    private static String plain(BigDecimal value) {
        return value == null ? "0" : value.stripTrailingZeros().toPlainString();
    }

    private static String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
