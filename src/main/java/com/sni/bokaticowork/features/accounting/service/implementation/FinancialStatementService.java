package com.sni.bokaticowork.features.accounting.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ForbiddenException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.accounting.enums.FiscalYearStatus;
import com.sni.bokaticowork.features.accounting.model.AccAccount;
import com.sni.bokaticowork.features.accounting.model.AccFiscalYear;
import com.sni.bokaticowork.features.accounting.model.AccStatementLine;
import com.sni.bokaticowork.features.accounting.model.AccStatementMapping;
import com.sni.bokaticowork.features.accounting.model.AccStatementTemplate;
import com.sni.bokaticowork.features.accounting.repository.AccAccountRepository;
import com.sni.bokaticowork.features.accounting.repository.AccFiscalYearRepository;
import com.sni.bokaticowork.features.accounting.repository.AccStatementLineRepository;
import com.sni.bokaticowork.features.accounting.repository.AccStatementMappingRepository;
import com.sni.bokaticowork.features.accounting.repository.AccStatementTemplateRepository;
import com.sni.bokaticowork.features.payment.service.support.CashActorResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Etats financiers SYSCOHADA (lot F12, plan des etats, section 4) : bilan, compte de resultat avec
 * ses soldes de gestion, tableau des flux de tresorerie, comptes non classes et controles de
 * coherence. Les postes et la correspondance des comptes vers les postes sont des donnees
 * versionnees ({@code acc_statement_template}, {@code acc_statement_line}, {@code acc_statement_mapping}).
 *
 * <p>Perimetre des soldes : les comptes de bilan (classes 1 a 5) cumulent depuis le debut du
 * premier exercice qui suit le dernier exercice clos (ses a-nouveaux compris) ; les comptes de
 * gestion (classes 6 a 8) ne portent que sur l'exercice. Tant qu'un exercice anterieur n'est pas
 * clos, son resultat est reporte au poste du resultat (compte {@code 130}, resultat en instance).</p>
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FinancialStatementService {

    public static final String READ = "ACCOUNTING_CLOSING:READ";
    public static final String MANAGE = "ACCOUNTING_CLOSING:MANAGE";
    public static final String FINALIZE = "ACCOUNTING_CLOSING:FINALIZE";
    public static final String EDIT = "ACCOUNTING_STATEMENT:EDIT";
    public static final String BALANCE_SHEET = "BILAN";
    public static final String INCOME_STATEMENT = "COMPTE_RESULTAT";
    public static final String CASH_FLOW = "TFT";
    static final String PENDING_RESULT_ACCOUNT = "130";
    private static final List<String> POSTED = List.of("VALIDATED", "REVERSED");

    private final JdbcTemplate jdbc;
    private final AccFiscalYearRepository fiscalYears;
    private final AccStatementTemplateRepository templates;
    private final AccStatementLineRepository statementLines;
    private final AccStatementMappingRepository mappings;
    private final AccAccountRepository accounts;
    private final CashActorResolver actors;

    // ================================================================== reponses

    /**
     * Un poste. Pour l'actif : brut, amortissements et depreciations, net ; ailleurs seul le net est
     * servi. {@code previous} : net de l'exercice precedent (vide au premier exercice).
     */
    public record Row(String code, String label, int level, String type, String section, String noteRef, BigDecimal gross,
                      BigDecimal depreciation, BigDecimal net, BigDecimal previous, List<String> accounts) {
    }

    /** Compte mouvemente sans poste : a classer avant l'edition definitive. */
    public record Unclassified(String accountNumber, String label, BigDecimal balance) {
    }

    /** Controle de coherence (plan des etats, section 4.5). */
    public record Check(String code, String label, boolean ok, BigDecimal difference) {
    }

    public record Statement(String code, String title, Integer version, LocalDate from, LocalDate to, LocalDate previousTo,
                            boolean firstYear, boolean provisional, List<Row> rows, List<Unclassified> unclassified, List<Check> checks,
                            Instant generatedAt, String generatedBy) {

        public BigDecimal net(String lineCode) {
            return rows.stream().filter(row -> row.code().equals(lineCode)).map(Row::net).findFirst().orElse(BigDecimal.ZERO);
        }

        public boolean consistent() {
            return unclassified.isEmpty() && checks.stream().allMatch(Check::ok);
        }
    }

    public record MappingView(Long id, String lineCode, String accountPrefix, String balanceSide, String valueColumn) {
    }

    public record MappingRequest(String lineCode, String accountPrefix, String balanceSide, String valueColumn) {
    }

    // ================================================================== etats

    /** Bilan a une date (fin d'exercice par defaut), avec le net de la fin de l'exercice precedent. */
    public Statement balanceSheet(LocalDate date) {
        require(READ, "ACCOUNTING_REPORT:READ");
        return balanceSheetAt(date == null ? currentYearEnd() : date);
    }

    /** Compte de resultat d'une periode (exercice de la date de fin par defaut). */
    public Statement incomeStatement(LocalDate from, LocalDate to) {
        require(READ, "ACCOUNTING_REPORT:READ");
        LocalDate end = to == null ? currentYearEnd() : to;
        return incomeStatementFor(from == null ? yearOf(end).getStartDate() : from, end);
    }

    /** Tableau des flux de tresorerie d'un exercice. */
    public Statement cashFlow(String fiscalYearCode) {
        require(READ, "ACCOUNTING_REPORT:READ");
        AccFiscalYear year = StringUtils.hasText(fiscalYearCode) ? year(fiscalYearCode) : yearOf(LocalDate.now());
        return cashFlowFor(year);
    }

    Statement balanceSheetAt(LocalDate date) {
        Template template = template(BALANCE_SHEET);
        AccFiscalYear year = yearOf(date);
        Optional<AccFiscalYear> previous = previousYear(year);
        Computed current = computeBalanceSheet(template, date);
        Computed before = previous.map(item -> computeBalanceSheet(template, item.getEndDate())).orElse(null);
        List<Check> checks = new ArrayList<>();
        BigDecimal assets = current.net.getOrDefault("BZ", BigDecimal.ZERO);
        BigDecimal liabilities = current.net.getOrDefault("DZ", BigDecimal.ZERO);
        checks.add(new Check("ASSETS_EQUAL_LIABILITIES", "Total actif = total passif", assets.compareTo(liabilities) == 0, assets.subtract(liabilities)));
        // Le resultat porte au bilan (produits moins charges) egale le resultat net calcule poste par poste.
        BigDecimal xi = computeIncome(template(INCOME_STATEMENT), year.getStartDate(), date).net.getOrDefault("XI", BigDecimal.ZERO);
        checks.add(new Check("RESULT_MATCH", "Resultat du bilan = resultat net du compte de resultat", current.resultCarried.compareTo(xi) == 0,
                current.resultCarried.subtract(xi)));
        return statement(template, year.getStartDate(), date, previous.map(AccFiscalYear::getEndDate).orElse(null), current, before, checks);
    }

    Statement incomeStatementFor(LocalDate from, LocalDate to) {
        Template template = template(INCOME_STATEMENT);
        AccFiscalYear year = yearOf(to);
        Optional<AccFiscalYear> previous = previousYear(year);
        Computed current = computeIncome(template, from, to);
        Computed before = previous.map(item -> computeIncome(template, item.getStartDate(), item.getEndDate())).orElse(null);
        BigDecimal result = incomeResult(from, to);
        BigDecimal xi = current.net.getOrDefault("XI", BigDecimal.ZERO);
        List<Check> checks = List.of(new Check("NET_RESULT", "Resultat net = produits moins charges (classes 6 a 8)", xi.compareTo(result) == 0,
                xi.subtract(result)));
        return statement(template, from, to, previous.map(AccFiscalYear::getEndDate).orElse(null), current, before, checks);
    }

    Statement cashFlowFor(AccFiscalYear year) {
        Template template = template(CASH_FLOW);
        Optional<AccFiscalYear> previous = previousYear(year);
        Map<String, BigDecimal> current = flows(year, previous.orElse(null));
        Map<String, BigDecimal> before = previous.map(item -> flows(item, previousYear(item).orElse(null))).orElse(null);
        Computed now = new Computed(new HashMap<>(), new HashMap<>(), current, new HashMap<>(), List.of(), BigDecimal.ZERO);
        totals(template.lines, now);
        Computed earlier = null;
        if (before != null) {
            earlier = new Computed(new HashMap<>(), new HashMap<>(), before, new HashMap<>(), List.of(), BigDecimal.ZERO);
            totals(template.lines, earlier);
        }
        Computed closing = computeBalanceSheet(template(BALANCE_SHEET), year.getEndDate());
        BigDecimal treasury = closing.net.getOrDefault("BT", BigDecimal.ZERO).subtract(closing.net.getOrDefault("DT", BigDecimal.ZERO));
        BigDecimal zh = now.net.getOrDefault("ZH", BigDecimal.ZERO);
        List<Check> checks = List.of(new Check("TREASURY_MATCH", "Tresorerie du tableau des flux = tresorerie du bilan (actif moins passif)",
                zh.compareTo(treasury) == 0, zh.subtract(treasury)));
        return statement(template, year.getStartDate(), year.getEndDate(), previous.map(AccFiscalYear::getEndDate).orElse(null), now, earlier, checks);
    }

    /** Resultat net d'une periode : produits moins charges des classes 6 a 8 (positif : benefice). */
    BigDecimal incomeResult(LocalDate from, LocalDate to) {
        BigDecimal result = BigDecimal.ZERO;
        for (Map.Entry<String, BigDecimal> entry : balances(from, to).entrySet()) {
            if (isIncome(entry.getKey())) {
                result = result.subtract(entry.getValue());
            }
        }
        return result;
    }

    // ================================================================== correspondance

    @Transactional(readOnly = true)
    public List<MappingView> mappings(String templateCode) {
        require(READ, "ACCOUNTING_REPORT:READ");
        Template template = template(templateCode);
        return template.mappings.stream().sorted(Comparator.comparing(AccStatementMapping::getAccountPrefix))
                .map(item -> new MappingView(item.getId(), item.getLineCode(), item.getAccountPrefix(), item.getBalanceSide(), item.getValueColumn()))
                .toList();
    }

    @Transactional
    public MappingView addMapping(String templateCode, MappingRequest request) {
        require(EDIT);
        Template template = template(templateCode);
        if (request == null || !StringUtils.hasText(request.lineCode()) || !StringUtils.hasText(request.accountPrefix())) {
            throw new BadRequestException("Indiquez le poste et le prefixe de compte");
        }
        String line = request.lineCode().trim().toUpperCase(Locale.ROOT);
        if (template.lines.stream().noneMatch(item -> item.getCode().equals(line) && "DETAIL".equals(item.getLineType()))) {
            throw new BadRequestException("Le poste " + line + " n'existe pas ou est un total");
        }
        String side = upper(request.balanceSide(), "ANY");
        String column = upper(request.valueColumn(), "GROSS");
        if (!List.of("ANY", "DEBIT", "CREDIT").contains(side) || !List.of("GROSS", "DEPRECIATION").contains(column)) {
            throw new BadRequestException("Sens ANY, DEBIT ou CREDIT ; colonne GROSS ou DEPRECIATION");
        }
        String prefix = request.accountPrefix().trim();
        AccStatementMapping saved = template.mappings.stream()
                .filter(item -> item.getAccountPrefix().equals(prefix) && item.getBalanceSide().equals(side) && item.getValueColumn().equals(column))
                .findFirst().orElseGet(() -> AccStatementMapping.builder().templateId(template.template.getId()).accountPrefix(prefix)
                        .balanceSide(side).valueColumn(column).build());
        saved.setLineCode(line);
        saved = mappings.save(saved);
        return new MappingView(saved.getId(), saved.getLineCode(), saved.getAccountPrefix(), saved.getBalanceSide(), saved.getValueColumn());
    }

    @Transactional
    public void deleteMapping(Long id) {
        require(EDIT);
        mappings.deleteById(id);
    }

    // ================================================================== codes de flux

    @Transactional
    public Map<String, Object> setEntryFlowCode(String reference, String code) {
        require(EDIT);
        String value = flowCode(code);
        int updated = jdbc.update("UPDATE acc_entry SET cash_flow_code = ? WHERE entry_number = ? OR provisional_number = ?", value, reference, reference);
        if (updated == 0) {
            throw new ResourceNotFoundException("Piece introuvable : " + reference);
        }
        return Map.of("reference", reference, "cashFlowCode", value == null ? "" : value);
    }

    /** Code de flux de toutes les versions d'un schema d'imputation. */
    @Transactional
    public Map<String, Object> setRuleFlowCode(String ruleCode, String code) {
        require(EDIT);
        String value = flowCode(code);
        int updated = jdbc.update("UPDATE acc_posting_rule SET cash_flow_code = ? WHERE code = ?", value, ruleCode);
        if (updated == 0) {
            throw new ResourceNotFoundException("Schema introuvable : " + ruleCode);
        }
        return Map.of("rule", ruleCode, "cashFlowCode", value == null ? "" : value);
    }

    private static String flowCode(String code) {
        if (!StringUtils.hasText(code)) {
            return null;
        }
        String value = code.trim().toUpperCase(Locale.ROOT);
        if (!FLOW_CODES.contains(value)) {
            throw new BadRequestException("Code de flux inconnu : " + value + " (" + String.join(", ", FLOW_CODES) + ")");
        }
        return value;
    }

    // ================================================================== calcul

    private record Template(AccStatementTemplate template, List<AccStatementLine> lines, List<AccStatementMapping> mappings) {
    }

    /** Montants calcules par poste ; {@code resultCarried} : resultat ajoute au poste du resultat. */
    private record Computed(Map<String, BigDecimal> gross, Map<String, BigDecimal> depreciation, Map<String, BigDecimal> net,
                            Map<String, List<String>> accounts, List<Unclassified> unclassified, BigDecimal resultCarried) {
    }

    private Template template(String code) {
        AccStatementTemplate template = templates.findFirstByCodeAndActiveTrueOrderByVersionDesc(code)
                .orElseThrow(() -> new ResourceNotFoundException("Modele d'etat introuvable : " + code));
        return new Template(template, statementLines.findAllByTemplateIdOrderByPositionAsc(template.getId()), mappings.findAllByTemplateId(template.getId()));
    }

    private Computed computeBalanceSheet(Template template, LocalDate date) {
        LocalDate scope = scopeStart(date);
        AccFiscalYear year = yearOf(date);
        // Comptes de tiers par tiers : un client crediteur ou un fournisseur debiteur change de cote (soldes anormaux reclasses).
        Map<String, BigDecimal> balances = new TreeMap<>();
        jdbc.query("SELECT l.account_number, CASE WHEN l.account_number LIKE '4%' THEN COALESCE(l.third_party_code, '') ELSE '' END, "
                        + "COALESCE(SUM(l.debit), 0) - COALESCE(SUM(l.credit), 0) FROM acc_entry_line l JOIN acc_entry e ON e.id = l.entry_id "
                        + "WHERE e.status IN ('VALIDATED', 'REVERSED') AND e.entry_date BETWEEN ? AND ? GROUP BY 1, 2",
                rs -> {
                    String account = rs.getString(1);
                    if (isBalanceSheet(account)) {
                        String party = rs.getString(2);
                        balances.merge(party.isEmpty() ? account : account + "|" + party, rs.getBigDecimal(3), BigDecimal::add);
                    }
                }, scope, date);
        // Resultat des exercices anterieurs non clos : en instance d'affectation.
        if (scope.isBefore(year.getStartDate())) {
            BigDecimal earlier = incomeResult(scope, year.getStartDate().minusDays(1));
            if (earlier.signum() != 0) {
                balances.merge(PENDING_RESULT_ACCOUNT, earlier.negate(), BigDecimal::add);
            }
        }
        Computed computed = classify(template, balances);
        BigDecimal result = incomeResult(year.getStartDate(), date);
        template.lines.stream().filter(line -> "@RESULT".equals(line.getFormula())).forEach(line ->
                computed.net.merge(line.getCode(), result, BigDecimal::add));
        Computed withResult = new Computed(computed.gross, computed.depreciation, computed.net, computed.accounts, computed.unclassified, result);
        totals(template.lines, withResult);
        return withResult;
    }

    private Computed computeIncome(Template template, LocalDate from, LocalDate to) {
        Map<String, BigDecimal> balances = new TreeMap<>();
        balances(from, to).forEach((account, amount) -> {
            if (isIncome(account)) {
                balances.put(account, amount);
            }
        });
        Computed computed = classify(template, balances);
        totals(template.lines, computed);
        return computed;
    }

    /** Range chaque solde dans son poste ; le prefixe le plus long l'emporte, un sens precis avant ANY. */
    private Computed classify(Template template, Map<String, BigDecimal> balances) {
        Map<String, AccStatementLine> lines = template.lines.stream().collect(Collectors.toMap(AccStatementLine::getCode, line -> line));
        Map<String, BigDecimal> gross = new HashMap<>();
        Map<String, BigDecimal> depreciation = new HashMap<>();
        Map<String, BigDecimal> net = new HashMap<>();
        Map<String, List<String>> used = new HashMap<>();
        List<Unclassified> unclassified = new ArrayList<>();
        Map<String, AccAccount> chart = accounts.findAllByOrderByNumberAsc().stream().collect(Collectors.toMap(AccAccount::getNumber, item -> item, (a, b) -> a));
        Map<String, BigDecimal> orphans = new TreeMap<>();
        for (Map.Entry<String, BigDecimal> entry : balances.entrySet()) {
            BigDecimal balance = entry.getValue();
            if (balance.signum() == 0) {
                continue;
            }
            String number = entry.getKey().contains("|") ? entry.getKey().substring(0, entry.getKey().indexOf('|')) : entry.getKey();
            String side = balance.signum() > 0 ? "DEBIT" : "CREDIT";
            AccStatementMapping mapping = template.mappings.stream()
                    .filter(item -> number.startsWith(item.getAccountPrefix()))
                    .filter(item -> "ANY".equals(item.getBalanceSide()) || side.equals(item.getBalanceSide()))
                    .max(Comparator.<AccStatementMapping>comparingInt(item -> item.getAccountPrefix().length())
                            .thenComparing(item -> "ANY".equals(item.getBalanceSide()) ? 0 : 1))
                    .orElse(null);
            AccStatementLine line = mapping == null ? null : lines.get(mapping.getLineCode());
            if (line == null) {
                orphans.merge(number, balance, BigDecimal::add);
                continue;
            }
            List<String> listed = used.computeIfAbsent(line.getCode(), key -> new ArrayList<>());
            if (!listed.contains(number)) {
                listed.add(number);
            }
            String section = line.getSection();
            if ("ASSET".equals(section)) {
                if ("DEPRECIATION".equals(mapping.getValueColumn())) {
                    depreciation.merge(line.getCode(), balance.negate(), BigDecimal::add);
                } else {
                    gross.merge(line.getCode(), balance, BigDecimal::add);
                }
            } else {
                BigDecimal value = "CHARGE".equals(section) ? balance : balance.negate();
                net.merge(line.getCode(), value, BigDecimal::add);
            }
        }
        orphans.forEach((number, balance) -> {
            AccAccount account = chart.get(number);
            unclassified.add(new Unclassified(number, account == null ? null : account.getLabel(), balance));
        });
        for (AccStatementLine line : template.lines) {
            if ("ASSET".equals(line.getSection()) && "DETAIL".equals(line.getLineType())) {
                net.put(line.getCode(), gross.getOrDefault(line.getCode(), BigDecimal.ZERO).subtract(depreciation.getOrDefault(line.getCode(), BigDecimal.ZERO)));
            }
        }
        return new Computed(gross, depreciation, net, used, unclassified, BigDecimal.ZERO);
    }

    /** Totaux et sous-totaux, dans l'ordre des postes : une formule ne cite que des postes deja calcules. */
    private static void totals(List<AccStatementLine> lines, Computed computed) {
        for (AccStatementLine line : lines) {
            if (!"TOTAL".equals(line.getLineType()) || !StringUtils.hasText(line.getFormula())) {
                continue;
            }
            computed.net.put(line.getCode(), formula(line.getFormula(), computed.net));
            if ("ASSET".equals(line.getSection())) {
                computed.gross.put(line.getCode(), formula(line.getFormula(), computed.gross));
                computed.depreciation.put(line.getCode(), formula(line.getFormula(), computed.depreciation));
            }
        }
    }

    /** Formule d'un total : codes de postes separes par + ou - ({@code XG+XH-RQ-RS}). */
    static BigDecimal formula(String formula, Map<String, BigDecimal> values) {
        BigDecimal total = BigDecimal.ZERO;
        int sign = 1;
        StringBuilder code = new StringBuilder();
        for (char character : (formula.replace(" ", "") + "+").toCharArray()) {
            if (character == '+' || character == '-') {
                if (!code.isEmpty()) {
                    BigDecimal value = values.getOrDefault(code.toString(), BigDecimal.ZERO);
                    total = sign > 0 ? total.add(value) : total.subtract(value);
                    code.setLength(0);
                }
                sign = character == '+' ? 1 : -1;
            } else {
                code.append(character);
            }
        }
        return total;
    }

    private Statement statement(Template template, LocalDate from, LocalDate to, LocalDate previousTo, Computed current, Computed before,
                                List<Check> checks) {
        boolean asset = BALANCE_SHEET.equals(template.template.getCode());
        List<Row> rows = new ArrayList<>();
        for (AccStatementLine line : template.lines) {
            boolean withColumns = asset && "ASSET".equals(line.getSection());
            rows.add(new Row(line.getCode(), line.getLabel(), line.getLevel() == null ? 1 : line.getLevel(), line.getLineType(), line.getSection(),
                    line.getNoteRef(),
                    withColumns ? current.gross.getOrDefault(line.getCode(), BigDecimal.ZERO) : null,
                    withColumns ? current.depreciation.getOrDefault(line.getCode(), BigDecimal.ZERO) : null,
                    current.net.getOrDefault(line.getCode(), BigDecimal.ZERO),
                    before == null ? null : before.net.getOrDefault(line.getCode(), BigDecimal.ZERO),
                    current.accounts.getOrDefault(line.getCode(), List.of())));
        }
        boolean provisional = fiscalYears.findAllByOrderByStartDateAsc().stream()
                .filter(year -> !to.isBefore(year.getStartDate()) && !to.isAfter(year.getEndDate()))
                .noneMatch(year -> year.getStatus() == FiscalYearStatus.CLOSED);
        return new Statement(template.template.getCode(), template.template.getLabel(), template.template.getVersion(), from, to, previousTo,
                before == null, provisional, rows, current.unclassified, checks, Instant.now(), actors.current());
    }

    // ================================================================== tableau des flux

    /**
     * Postes du tableau des flux. Les mouvements excluent les a-nouveaux (journaux de type ouverture).
     * La CAFG part du resultat net et ajoute les dotations et la valeur comptable des cessions, moins
     * les reprises et les produits de cession ; les variations portent sur les valeurs brutes de
     * l'actif circulant et sur le passif circulant hors provisions a court terme et dividendes.
     */
    private Map<String, BigDecimal> flows(AccFiscalYear year, AccFiscalYear previous) {
        Template balanceTemplate = template(BALANCE_SHEET);
        Computed closing = computeBalanceSheet(balanceTemplate, year.getEndDate());
        Computed opening = previous == null ? null : computeBalanceSheet(balanceTemplate, previous.getEndDate());
        Computed income = computeIncome(template(INCOME_STATEMENT), year.getStartDate(), year.getEndDate());
        Map<String, BigDecimal[]> moves = movements(year.getStartDate(), year.getEndDate());
        Map<String, BigDecimal> flows = new HashMap<>();
        flows.put("ZA", opening == null ? BigDecimal.ZERO
                : opening.net.getOrDefault("BT", BigDecimal.ZERO).subtract(opening.net.getOrDefault("DT", BigDecimal.ZERO)));
        BigDecimal cafg = income.net.getOrDefault("XI", BigDecimal.ZERO).add(value(income, "RL")).add(value(income, "RN"))
                .subtract(value(income, "TJ")).subtract(value(income, "TL")).subtract(value(income, "TN")).add(value(income, "RO"));
        flows.put("FA", cafg);
        flows.put("FB", variation(closing, opening, true, "BA"));
        flows.put("FC", variation(closing, opening, true, "BB"));
        flows.put("FD", variation(closing, opening, true, "BH", "BI", "BJ"));
        BigDecimal dividendsPayable = creditBalance(year.getEndDate(), "465")
                .subtract(previous == null ? BigDecimal.ZERO : creditBalance(previous.getEndDate(), "465"));
        flows.put("FE", variation(closing, opening, false, "DH", "DI", "DJ", "DK", "DM").subtract(dividendsPayable));
        flows.put("FF", debit(moves, prefix -> prefix.startsWith("20") || prefix.startsWith("21")));
        flows.put("FG", debit(moves, prefix -> prefix.startsWith("22") || prefix.startsWith("23") || prefix.startsWith("24") || prefix.startsWith("25")));
        flows.put("FH", debit(moves, prefix -> prefix.startsWith("26") || prefix.startsWith("27")));
        flows.put("FI", value(income, "TN"));
        flows.put("FJ", credit(moves, prefix -> prefix.startsWith("27")));
        BigDecimal capital = credit(moves, prefix -> prefix.startsWith("10")).subtract(debit(moves, prefix -> prefix.startsWith("10")));
        // Codes de flux portes par les pieces (ou leur schema) : la tresorerie de ces pieces remplace l'estimation par les comptes.
        flows.put("FK", capital.signum() > 0 ? capital : BigDecimal.ZERO);
        flows.put("FM", capital.signum() < 0 ? capital.negate() : BigDecimal.ZERO);
        flows.put("FL", credit(moves, prefix -> prefix.startsWith("14")));
        flows.put("FN", debit(moves, prefix -> prefix.startsWith("465")));
        flows.put("FO", credit(moves, prefix -> prefix.startsWith("161") || prefix.startsWith("162")));
        flows.put("FP", credit(moves, prefix -> (prefix.startsWith("16") && !prefix.startsWith("161") && !prefix.startsWith("162"))
                || prefix.startsWith("17") || prefix.startsWith("18")));
        flows.put("FQ", debit(moves, prefix -> prefix.startsWith("16") || prefix.startsWith("17") || prefix.startsWith("18")));
        flows.putAll(codedFlows(year));
        return flows;
    }

    /** Codes de flux reconnus : postes de detail des flux d'investissement et de financement. */
    public static final List<String> FLOW_CODES = List.of("FF", "FG", "FH", "FI", "FJ", "FK", "FL", "FM", "FN", "FO", "FP", "FQ");

    /**
     * Flux codes de l'exercice : mouvement de tresorerie (classe 5, hors 58 et hors a-nouveaux) des
     * pieces qui portent un code, ou dont le schema en porte un. Montant positif quel que soit le sens.
     */
    private Map<String, BigDecimal> codedFlows(AccFiscalYear year) {
        Map<String, BigDecimal> coded = new HashMap<>();
        jdbc.query("SELECT COALESCE(e.cash_flow_code, r.cash_flow_code), ABS(COALESCE(SUM(l.debit), 0) - COALESCE(SUM(l.credit), 0)) "
                        + "FROM acc_entry e JOIN acc_entry_line l ON l.entry_id = e.id LEFT JOIN acc_posting_event pe ON pe.id = e.posting_event_id "
                        + "LEFT JOIN acc_posting_rule r ON r.code = pe.rule_code AND r.version = pe.rule_version "
                        + "WHERE e.status IN ('VALIDATED', 'REVERSED') AND e.entry_date BETWEEN ? AND ? AND e.origin <> 'OPENING' "
                        + "AND COALESCE(e.cash_flow_code, r.cash_flow_code) IS NOT NULL AND l.account_number LIKE '5%' AND l.account_number NOT LIKE '58%' "
                        + "GROUP BY e.id, COALESCE(e.cash_flow_code, r.cash_flow_code)",
                rs -> {
                    String code = rs.getString(1);
                    if (FLOW_CODES.contains(code)) {
                        coded.merge(code, rs.getBigDecimal(2), BigDecimal::add);
                    }
                }, year.getStartDate(), year.getEndDate());
        return coded;
    }

    private static BigDecimal value(Computed computed, String code) {
        return computed.net.getOrDefault(code, BigDecimal.ZERO);
    }

    /** Variation d'un groupe de postes : brut pour l'actif, net pour le passif. */
    private static BigDecimal variation(Computed closing, Computed opening, boolean asset, String... codes) {
        BigDecimal total = BigDecimal.ZERO;
        for (String code : codes) {
            BigDecimal end = (asset ? closing.gross : closing.net).getOrDefault(code, BigDecimal.ZERO);
            BigDecimal start = opening == null ? BigDecimal.ZERO : (asset ? opening.gross : opening.net).getOrDefault(code, BigDecimal.ZERO);
            total = total.add(end.subtract(start));
        }
        return total;
    }

    /** Solde crediteur, a une date, des comptes de bilan d'un prefixe. */
    private BigDecimal creditBalance(LocalDate date, String prefix) {
        return balances(scopeStart(date), date).entrySet().stream().filter(entry -> entry.getKey().startsWith(prefix))
                .map(Map.Entry::getValue).reduce(BigDecimal.ZERO, BigDecimal::add).negate();
    }

    private static BigDecimal debit(Map<String, BigDecimal[]> moves, Predicate<String> filter) {
        return moves.entrySet().stream().filter(entry -> filter.test(entry.getKey())).map(entry -> entry.getValue()[0]).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal credit(Map<String, BigDecimal[]> moves, Predicate<String> filter) {
        return moves.entrySet().stream().filter(entry -> filter.test(entry.getKey())).map(entry -> entry.getValue()[1]).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Mouvements de l'exercice hors a-nouveaux : compte, debit, credit. */
    private Map<String, BigDecimal[]> movements(LocalDate from, LocalDate to) {
        Map<String, BigDecimal[]> result = new HashMap<>();
        jdbc.query("SELECT l.account_number, COALESCE(SUM(l.debit), 0), COALESCE(SUM(l.credit), 0) FROM acc_entry_line l "
                        + "JOIN acc_entry e ON e.id = l.entry_id JOIN acc_journal j ON j.code = e.journal_code "
                        + "WHERE e.status IN ('VALIDATED', 'REVERSED') AND e.entry_date BETWEEN ? AND ? AND j.type <> 'OPENING' GROUP BY l.account_number",
                rs -> {
                    result.put(rs.getString(1), new BigDecimal[]{rs.getBigDecimal(2), rs.getBigDecimal(3)});
                }, from, to);
        return result;
    }

    // ================================================================== soldes

    /** Soldes (debit moins credit) des pieces comptabilisees datees de la periode, par compte. */
    Map<String, BigDecimal> balances(LocalDate from, LocalDate to) {
        Map<String, BigDecimal> result = new TreeMap<>();
        jdbc.query("SELECT l.account_number, COALESCE(SUM(l.debit), 0) - COALESCE(SUM(l.credit), 0) FROM acc_entry_line l "
                        + "JOIN acc_entry e ON e.id = l.entry_id WHERE e.status IN ('VALIDATED', 'REVERSED') AND e.entry_date BETWEEN ? AND ? "
                        + "GROUP BY l.account_number",
                rs -> {
                    result.put(rs.getString(1), rs.getBigDecimal(2));
                }, from, to);
        return result;
    }

    /**
     * Debut du perimetre des comptes de bilan a une date : lendemain du dernier exercice clos avant
     * cette date (ses a-nouveaux sont dans l'exercice suivant), sinon depuis toujours.
     */
    LocalDate scopeStart(LocalDate date) {
        return fiscalYears.findAllByOrderByStartDateAsc().stream()
                .filter(year -> year.getStatus() == FiscalYearStatus.CLOSED && year.getEndDate().isBefore(date))
                .map(year -> year.getEndDate().plusDays(1))
                .max(Comparator.naturalOrder())
                .orElse(LocalDate.of(1900, 1, 1));
    }

    static boolean isBalanceSheet(String account) {
        return account != null && !account.isEmpty() && "12345".indexOf(account.charAt(0)) >= 0;
    }

    static boolean isIncome(String account) {
        return account != null && !account.isEmpty() && "678".indexOf(account.charAt(0)) >= 0;
    }

    // ================================================================== exercices

    AccFiscalYear year(String code) {
        return fiscalYears.findByCode(code == null ? null : code.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Exercice introuvable : " + code));
    }

    AccFiscalYear yearOf(LocalDate date) {
        return fiscalYears.findAllByOrderByStartDateAsc().stream()
                .filter(year -> !date.isBefore(year.getStartDate()) && !date.isAfter(year.getEndDate()))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Aucun exercice ne couvre le " + date));
    }

    Optional<AccFiscalYear> previousYear(AccFiscalYear year) {
        return fiscalYears.findAllByOrderByStartDateAsc().stream()
                .filter(item -> item.getEndDate().isBefore(year.getStartDate()))
                .max(Comparator.comparing(AccFiscalYear::getEndDate));
    }

    private LocalDate currentYearEnd() {
        LocalDate today = LocalDate.now();
        return fiscalYears.findAllByOrderByStartDateAsc().stream()
                .filter(year -> !today.isBefore(year.getStartDate()) && !today.isAfter(year.getEndDate()))
                .map(AccFiscalYear::getEndDate).findFirst().orElse(today);
    }

    // ================================================================== export

    /** Etat en tableau, pour le CSV et le PDF communs des etats comptables. */
    public FixedAssetReportService.Table table(Statement statement) {
        boolean columns = BALANCE_SHEET.equals(statement.code());
        List<String> headers = new ArrayList<>(List.of("Ref", "Poste", "Note"));
        List<Boolean> numeric = new ArrayList<>(List.of(false, false, false));
        if (columns) {
            headers.addAll(List.of("Brut", "Amort. et deprec.", "Net"));
            numeric.addAll(List.of(true, true, true));
        } else {
            headers.add("Exercice");
            numeric.add(true);
        }
        headers.add(statement.firstYear() ? "N-1 (premier exercice)" : "N-1");
        numeric.add(true);
        List<List<Object>> rows = new ArrayList<>();
        for (Row row : statement.rows()) {
            List<Object> cells = new ArrayList<>(List.of(row.code(), "  ".repeat(Math.max(0, row.level() - 1)) + row.label(),
                    row.noteRef() == null ? "" : row.noteRef()));
            if (columns) {
                cells.add(row.gross() == null ? "" : row.gross());
                cells.add(row.depreciation() == null ? "" : row.depreciation());
            }
            cells.add(row.net());
            cells.add(row.previous() == null ? "" : row.previous());
            rows.add(cells);
        }
        Map<String, Object> totals = new LinkedHashMap<>();
        for (Check check : statement.checks()) {
            totals.put(check.label(), check.ok() ? "OK" : "Ecart " + check.difference().stripTrailingZeros().toPlainString());
        }
        if (!statement.unclassified().isEmpty()) {
            totals.put("Comptes non classes", statement.unclassified().stream().map(Unclassified::accountNumber).collect(Collectors.joining(", ")));
        }
        String title = statement.title() + " au " + statement.to() + (statement.provisional() ? " (PROVISOIRE)" : "");
        return new FixedAssetReportService.Table(statement.code().toLowerCase(Locale.ROOT), title, headers, numeric, rows, totals);
    }

    // ================================================================== droits

    void require(String... authorities) {
        for (String authority : authorities) {
            if (actors.hasAuthority(authority)) {
                return;
            }
        }
        throw new ForbiddenException("Action reservee : " + String.join(" ou ", authorities));
    }

    private static String upper(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : fallback;
    }
}
