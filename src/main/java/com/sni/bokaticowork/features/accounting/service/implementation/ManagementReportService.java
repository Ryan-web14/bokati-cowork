package com.sni.bokaticowork.features.accounting.service.implementation;

import com.sni.bokaticowork.features.accounting.model.AccAccount;
import com.sni.bokaticowork.features.accounting.model.AccFiscalYear;
import com.sni.bokaticowork.features.accounting.repository.AccAccountRepository;
import com.sni.bokaticowork.features.accounting.service.support.AccountingSettings;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Etats de gestion (lot F15, plan des etats, sections 8 et 9) : soldes intermediaires de gestion
 * mensuels, tableau de bord financier, ratios, previsionnel de tresorerie a 13 semaines et 12 mois.
 * Tout se calcule depuis la comptabilite (balance, pieces ouvertes, echeanciers d'emprunt,
 * abonnements a facturer).
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ManagementReportService {

    /** Soldes intermediaires de gestion repris du compte de resultat. */
    public static final List<String> SIG = List.of("XA", "XB", "XC", "XD", "XE", "XF", "XG", "XH", "XI");

    private final JdbcTemplate jdbc;
    private final FinancialStatementService statements;
    private final BalanceJustificationService justifications;
    private final AccAccountRepository accounts;
    private final AccountingSettings settings;

    // ================================================================== soldes de gestion mensuels

    public record SigMonth(String month, Map<String, BigDecimal> values, Map<String, BigDecimal> cumulative, Map<String, BigDecimal> previousYear) {
    }

    public record Sig(String fiscalYear, Map<String, String> labels, List<SigMonth> months, boolean firstYear) {
    }

    /** Soldes de gestion de chaque mois de l'exercice : mois, cumul, meme mois de N-1. */
    public Sig sig(String yearCode) {
        statements.require(FinancialStatementService.READ, "ACCOUNTING_REPORT:READ");
        AccFiscalYear year = StringUtils.hasText(yearCode) ? statements.year(yearCode) : statements.yearOf(LocalDate.now());
        Optional<AccFiscalYear> previous = statements.previousYear(year);
        Map<String, String> labels = new LinkedHashMap<>();
        FinancialStatementService.Statement first = statements.incomeStatementFor(year.getStartDate(), year.getStartDate().plusMonths(1).minusDays(1));
        first.rows().stream().filter(row -> SIG.contains(row.code())).forEach(row -> labels.put(row.code(), row.label()));
        List<SigMonth> months = new ArrayList<>();
        YearMonth cursor = YearMonth.from(year.getStartDate());
        YearMonth last = YearMonth.from(year.getEndDate());
        while (!cursor.isAfter(last)) {
            LocalDate start = cursor.atDay(1).isBefore(year.getStartDate()) ? year.getStartDate() : cursor.atDay(1);
            LocalDate end = cursor.atEndOfMonth().isAfter(year.getEndDate()) ? year.getEndDate() : cursor.atEndOfMonth();
            Map<String, BigDecimal> values = pick(statements.incomeStatementFor(start, end));
            Map<String, BigDecimal> cumulative = pick(statements.incomeStatementFor(year.getStartDate(), end));
            Map<String, BigDecimal> before = null;
            if (previous.isPresent()) {
                YearMonth shifted = cursor.minusYears(1);
                LocalDate previousStart = shifted.atDay(1).isBefore(previous.get().getStartDate()) ? previous.get().getStartDate() : shifted.atDay(1);
                LocalDate previousEnd = shifted.atEndOfMonth().isAfter(previous.get().getEndDate()) ? previous.get().getEndDate() : shifted.atEndOfMonth();
                if (!previousStart.isAfter(previousEnd)) {
                    before = pick(statements.incomeStatementFor(previousStart, previousEnd));
                }
            }
            months.add(new SigMonth(cursor.toString(), values, cumulative, before));
            cursor = cursor.plusMonths(1);
        }
        return new Sig(year.getCode(), labels, months, previous.isEmpty());
    }

    private static Map<String, BigDecimal> pick(FinancialStatementService.Statement statement) {
        Map<String, BigDecimal> values = new LinkedHashMap<>();
        SIG.forEach(code -> values.put(code, statement.net(code)));
        return values;
    }

    // ================================================================== tableau de bord

    public record Amount(String label, BigDecimal value) {
    }

    public record Dashboard(LocalDate date, BigDecimal revenueMonth, BigDecimal revenueYearToDate, BigDecimal revenuePreviousYearToDate,
                            BigDecimal addedValue, BigDecimal ebitda, BigDecimal netResult, BigDecimal treasury, BigDecimal receivables,
                            BigDecimal overdueReceivables, Integer dso, BigDecimal staffAdvances, List<Amount> expensesByNature) {
    }

    /**
     * Tableau de bord a une date : chiffre d'affaires du mois et cumule (et N-1), valeur ajoutee,
     * EBE, resultat, tresorerie nette, creances et creances echues, delai moyen de paiement des
     * clients, avances au personnel, depenses du mois par nature.
     */
    public Dashboard dashboard(LocalDate at) {
        statements.require(FinancialStatementService.READ, "ACCOUNTING_REPORT:READ");
        LocalDate date = at == null ? LocalDate.now() : at;
        AccFiscalYear year = statements.yearOf(date);
        FinancialStatementService.Statement month = statements.incomeStatementFor(max(date.withDayOfMonth(1), year.getStartDate()), date);
        FinancialStatementService.Statement ytd = statements.incomeStatementFor(year.getStartDate(), date);
        BigDecimal previousYtd = statements.previousYear(year)
                .map(previous -> statements.incomeStatementFor(previous.getStartDate(), min(date.minusYears(1), previous.getEndDate())).net("XB"))
                .orElse(null);
        FinancialStatementService.Statement balance = statements.balanceSheetAt(date);
        BigDecimal treasury = balance.net("BT").subtract(balance.net("DT"));
        BalanceJustificationService.Justification clients = justifications.compute("411", null, date);
        BigDecimal receivables = clients.bookBalance();
        BigDecimal overdue = clients.items().stream().filter(item -> item.daysOverdue() != null && item.daysOverdue() > 0 && item.amount().signum() > 0)
                .map(BalanceJustificationService.OpenItem::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        // Delai moyen de paiement : encours clients rapporte aux facturations (debits 411 hors a-nouveaux) des 90 derniers jours.
        BigDecimal billed = jdbc.queryForObject("SELECT COALESCE(SUM(l.debit), 0) FROM acc_entry_line l JOIN acc_entry e ON e.id = l.entry_id "
                + "WHERE l.account_number LIKE '411%' AND e.status IN ('VALIDATED', 'REVERSED') AND e.origin <> 'OPENING' AND e.entry_date BETWEEN ? AND ?",
                BigDecimal.class, date.minusDays(89), date);
        Integer dso = billed == null || billed.signum() <= 0 || receivables.signum() <= 0 ? null
                : receivables.multiply(new BigDecimal("90")).divide(billed, 0, RoundingMode.HALF_UP).intValue();
        BigDecimal advances = statements.balances(statements.scopeStart(date), date).entrySet().stream()
                .filter(entry -> entry.getKey().startsWith("421")).map(Map.Entry::getValue).reduce(BigDecimal.ZERO, BigDecimal::add).max(BigDecimal.ZERO);
        Map<String, AccAccount> chart = accounts.findAllByOrderByNumberAsc().stream().collect(Collectors.toMap(AccAccount::getNumber, item -> item, (a, b) -> a));
        Map<String, BigDecimal> natures = new TreeMap<>();
        statements.balances(max(date.withDayOfMonth(1), year.getStartDate()), date).forEach((account, amount) -> {
            if (account.startsWith("6") && account.length() >= 2) {
                natures.merge(account.substring(0, 2), amount, BigDecimal::add);
            }
        });
        List<Amount> expenses = natures.entrySet().stream().filter(entry -> entry.getValue().signum() != 0)
                .map(entry -> new Amount(entry.getKey() + " " + (chart.containsKey(entry.getKey()) ? chart.get(entry.getKey()).getLabel() : ""), entry.getValue()))
                .toList();
        return new Dashboard(date, month.net("XB"), ytd.net("XB"), previousYtd, ytd.net("XC"), ytd.net("XD"), ytd.net("XI"), treasury, receivables,
                overdue, dso, advances, expenses);
    }

    // ================================================================== ratios

    public record Ratio(String code, String label, BigDecimal value, String unit, String formula) {
    }

    /** Ratios de structure, de liquidite, d'activite et de rentabilite, a une date (cumul de l'exercice). */
    public List<Ratio> ratios(LocalDate at) {
        statements.require(FinancialStatementService.READ, "ACCOUNTING_REPORT:READ");
        LocalDate date = at == null ? LocalDate.now() : at;
        AccFiscalYear year = statements.yearOf(date);
        FinancialStatementService.Statement balance = statements.balanceSheetAt(date);
        FinancialStatementService.Statement income = statements.incomeStatementFor(year.getStartDate(), date);
        long days = Math.max(1, ChronoUnit.DAYS.between(year.getStartDate(), date) + 1);
        BigDecimal currentLiabilities = balance.net("DP").add(balance.net("DT"));
        BigDecimal purchases = income.net("RA").add(income.net("RC")).add(income.net("RE")).add(income.net("RG")).add(income.net("RH"));
        List<Ratio> ratios = new ArrayList<>();
        ratios.add(ratio("GENERAL_LIQUIDITY", "Liquidite generale", balance.net("BK").add(balance.net("BT")), currentLiabilities, null, "",
                "(actif circulant + tresorerie actif) / (passif circulant + tresorerie passif)"));
        ratios.add(ratio("QUICK_LIQUIDITY", "Liquidite reduite", balance.net("BK").subtract(balance.net("BB")).add(balance.net("BT")), currentLiabilities, null, "",
                "(actif circulant - stocks + tresorerie actif) / dettes a court terme"));
        ratios.add(ratio("CASH_LIQUIDITY", "Liquidite immediate", balance.net("BT"), currentLiabilities, null, "", "tresorerie actif / dettes a court terme"));
        ratios.add(ratio("FINANCIAL_AUTONOMY", "Autonomie financiere", balance.net("CP"), balance.net("DZ"), new BigDecimal("100"), "%",
                "capitaux propres / total du bilan"));
        ratios.add(ratio("DEBT", "Endettement", balance.net("DD"), balance.net("CP"), new BigDecimal("100"), "%", "dettes financieres / capitaux propres"));
        ratios.add(ratio("CUSTOMER_DAYS", "Delai de paiement des clients", balance.rows().stream().filter(row -> row.code().equals("BI")).findFirst()
                .map(FinancialStatementService.Row::gross).orElse(BigDecimal.ZERO), income.net("XB"), BigDecimal.valueOf(days), "jours",
                "creances clients / chiffre d'affaires x jours ecoules"));
        ratios.add(ratio("SUPPLIER_DAYS", "Delai de paiement des fournisseurs", balance.net("DJ"), purchases, BigDecimal.valueOf(days), "jours",
                "dettes fournisseurs / achats et services exterieurs x jours ecoules"));
        ratios.add(ratio("STAFF_WEIGHT", "Poids des charges de personnel", income.net("RK"), income.net("XC"), new BigDecimal("100"), "%",
                "charges de personnel / valeur ajoutee"));
        ratios.add(ratio("EBITDA_MARGIN", "Taux de marge brute d'exploitation", income.net("XD"), income.net("XB"), new BigDecimal("100"), "%",
                "excedent brut d'exploitation / chiffre d'affaires"));
        ratios.add(ratio("NET_MARGIN", "Rentabilite nette", income.net("XI"), income.net("XB"), new BigDecimal("100"), "%", "resultat net / chiffre d'affaires"));
        ratios.add(ratio("RETURN_ON_EQUITY", "Rentabilite financiere", income.net("XI"), balance.net("CP"), new BigDecimal("100"), "%",
                "resultat net / capitaux propres"));
        return ratios;
    }

    private static Ratio ratio(String code, String label, BigDecimal numerator, BigDecimal denominator, BigDecimal factor, String unit, String formula) {
        BigDecimal value = denominator == null || denominator.signum() == 0 ? null
                : numerator.multiply(factor == null ? BigDecimal.ONE : factor).divide(denominator, factor == null ? 4 : 2, RoundingMode.HALF_UP);
        return new Ratio(code, label, value, unit, formula);
    }

    // ================================================================== previsionnel de tresorerie

    public record Bucket(String label, LocalDate start, LocalDate end, Map<String, BigDecimal> inflows, Map<String, BigDecimal> outflows,
                         BigDecimal net, BigDecimal closing) {
    }

    public record Forecast(LocalDate date, String horizon, BigDecimal opening, List<Bucket> buckets, List<String> assumptions) {
    }

    /**
     * Previsionnel de tresorerie a partir de la tresorerie nette du jour : encaissements des pieces
     * clients ouvertes (a leur echeance, les echues en premiere periode) et des abonnements a
     * facturer ; decaissements des pieces fournisseurs ouvertes, des echeances d'emprunt, et des
     * charges de personnel et charges courantes a leur moyenne mensuelle recente.
     *
     * @param horizon {@code WEEKS} (13 semaines) ou {@code MONTHS} (12 mois)
     */
    public Forecast forecast(LocalDate at, String horizon) {
        statements.require(FinancialStatementService.READ, "ACCOUNTING_REPORT:READ");
        LocalDate date = at == null ? LocalDate.now() : at;
        boolean weeks = !"MONTHS".equalsIgnoreCase(horizon);
        int termDays = integer("accounting.forecast.payment-term-days", 30);
        int history = Math.max(1, integer("accounting.forecast.history-months", 3));
        List<LocalDate[]> ranges = new ArrayList<>();
        if (weeks) {
            for (int index = 0; index < 13; index++) {
                ranges.add(new LocalDate[]{date.plusWeeks(index), date.plusWeeks(index + 1).minusDays(1)});
            }
        } else {
            YearMonth month = YearMonth.from(date);
            for (int index = 0; index < 12; index++) {
                YearMonth current = month.plusMonths(index);
                ranges.add(new LocalDate[]{index == 0 ? date : current.atDay(1), current.atEndOfMonth()});
            }
        }
        LocalDate horizonEnd = ranges.get(ranges.size() - 1)[1];
        List<Map<String, BigDecimal>> inflows = new ArrayList<>();
        List<Map<String, BigDecimal>> outflows = new ArrayList<>();
        ranges.forEach(range -> {
            inflows.add(new TreeMap<>());
            outflows.add(new TreeMap<>());
        });
        // Pieces clients ouvertes : encaissement a l'echeance (a defaut, date de piece plus delai) ; echues en premiere periode.
        for (BalanceJustificationService.OpenItem item : justifications.compute("411", null, date).items()) {
            if (item.amount().signum() > 0) {
                LocalDate due = item.dueDate() != null ? item.dueDate() : item.date().plusDays(termDays);
                put(inflows, ranges, due.isBefore(date) ? date : due, "Clients", item.amount());
            }
        }
        // Abonnements actifs a facturer dans l'horizon, a chaque echeance de leur cycle.
        jdbc.query("SELECT next_billing_date, total_amount, billing_cycle FROM subscription WHERE status = 'ACTIVE' AND next_billing_date IS NOT NULL "
                        + "AND next_billing_date <= ? AND total_amount > 0",
                rs -> {
                    LocalDate next = rs.getDate(1).toLocalDate();
                    BigDecimal amount = rs.getBigDecimal(2);
                    String cycle = rs.getString(3);
                    int guard = 0;
                    while (!next.isAfter(horizonEnd) && guard++ < 400) {
                        if (!next.isBefore(date)) {
                            put(inflows, ranges, next, "Abonnements a facturer", amount);
                        }
                        LocalDate following = switch (cycle == null ? "" : cycle) {
                            case "DAILY" -> next.plusDays(1);
                            case "WEEKLY" -> next.plusWeeks(1);
                            case "MONTHLY" -> next.plusMonths(1);
                            case "QUARTERLY" -> next.plusMonths(3);
                            case "YEARLY" -> next.plusYears(1);
                            default -> null;
                        };
                        if (following == null) {
                            break;
                        }
                        next = following;
                    }
                }, horizonEnd);
        // Pieces fournisseurs ouvertes.
        for (BalanceJustificationService.OpenItem item : justifications.compute("401", null, date).items()) {
            if (item.amount().signum() < 0) {
                LocalDate due = item.dueDate() != null ? item.dueDate() : item.date().plusDays(termDays);
                put(outflows, ranges, due.isBefore(date) ? date : due, "Fournisseurs", item.amount().negate());
            }
        }
        // Echeances d'emprunt prevues (version courante de l'echeancier).
        jdbc.query("SELECT i.due_date, i.payment FROM acc_loan_installment i JOIN acc_loan l ON l.id = i.loan_id AND i.version = l.schedule_version "
                        + "WHERE l.status = 'ACTIVE' AND i.status = 'PLANNED' AND i.due_date BETWEEN ? AND ?",
                rs -> {
                    put(outflows, ranges, rs.getDate(1).toLocalDate(), "Emprunts", rs.getBigDecimal(2));
                }, date, horizonEnd);
        // Personnel et charges courantes : moyenne mensuelle des derniers mois, payee en fin de mois.
        LocalDate historyStart = YearMonth.from(date).minusMonths(history).atDay(1);
        LocalDate historyEnd = YearMonth.from(date).minusMonths(1).atEndOfMonth();
        BigDecimal staff = monthlyAverage(historyStart, historyEnd, history, "66");
        BigDecimal running = monthlyAverage(historyStart, historyEnd, history, "61", "62", "63", "64", "65");
        YearMonth month = YearMonth.from(date);
        while (!month.atEndOfMonth().isAfter(horizonEnd.plusMonths(1))) {
            LocalDate end = month.atEndOfMonth();
            if (!end.isBefore(date) && !end.isAfter(horizonEnd)) {
                if (staff.signum() > 0) {
                    put(outflows, ranges, end, "Personnel (moyenne)", staff);
                }
                if (running.signum() > 0) {
                    put(outflows, ranges, end, "Charges courantes (moyenne)", running);
                }
            }
            month = month.plusMonths(1);
        }
        FinancialStatementService.Statement balance = statements.balanceSheetAt(date);
        BigDecimal opening = balance.net("BT").subtract(balance.net("DT"));
        BigDecimal cash = opening;
        List<Bucket> buckets = new ArrayList<>();
        for (int index = 0; index < ranges.size(); index++) {
            BigDecimal in = inflows.get(index).values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal out = outflows.get(index).values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal net = in.subtract(out);
            cash = cash.add(net);
            LocalDate[] range = ranges.get(index);
            String label = weeks ? "S" + (index + 1) + " du " + range[0] : YearMonth.from(range[1]).toString();
            buckets.add(new Bucket(label, range[0], range[1], inflows.get(index), outflows.get(index), net, cash));
        }
        List<String> assumptions = List.of(
                "Tresorerie de depart : tresorerie nette du bilan au " + date,
                "Pieces sans echeance : date de la piece plus " + termDays + " jours ; pieces echues : premiere periode",
                "Abonnements actifs : montant total a chaque echeance de facturation de leur cycle",
                "Personnel et charges courantes : moyenne mensuelle des " + history + " derniers mois, en fin de mois",
                "Les charges deja facturees (pieces fournisseurs ouvertes) s'ajoutent a la moyenne : prudence");
        return new Forecast(date, weeks ? "WEEKS" : "MONTHS", opening, buckets, assumptions);
    }

    private static void put(List<Map<String, BigDecimal>> buckets, List<LocalDate[]> ranges, LocalDate day, String category, BigDecimal amount) {
        for (int index = 0; index < ranges.size(); index++) {
            if (!day.isBefore(ranges.get(index)[0]) && !day.isAfter(ranges.get(index)[1])) {
                buckets.get(index).merge(category, amount, BigDecimal::add);
                return;
            }
        }
    }

    private BigDecimal monthlyAverage(LocalDate from, LocalDate to, int months, String... prefixes) {
        if (from.isAfter(to)) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = statements.balances(from, to).entrySet().stream()
                .filter(entry -> java.util.Arrays.stream(prefixes).anyMatch(entry.getKey()::startsWith))
                .map(Map.Entry::getValue).reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(months), 0, RoundingMode.HALF_UP);
    }

    // ================================================================== export

    public FixedAssetReportService.Table sigTable(Sig sig) {
        List<String> columns = new ArrayList<>(List.of("Mois"));
        SIG.forEach(code -> columns.add(sig.labels().getOrDefault(code, code)));
        List<Boolean> numeric = new ArrayList<>(List.of(false));
        SIG.forEach(code -> numeric.add(true));
        List<List<Object>> rows = new ArrayList<>();
        for (SigMonth month : sig.months()) {
            List<Object> row = new ArrayList<>(List.of(month.month()));
            SIG.forEach(code -> row.add(month.values().get(code)));
            rows.add(row);
        }
        Map<String, Object> totals = new LinkedHashMap<>();
        if (!sig.months().isEmpty()) {
            SigMonth last = sig.months().get(sig.months().size() - 1);
            SIG.forEach(code -> totals.put("Cumul " + sig.labels().getOrDefault(code, code).toLowerCase(Locale.ROOT), last.cumulative().get(code)));
        }
        return new FixedAssetReportService.Table("sig-" + sig.fiscalYear(), "Soldes intermediaires de gestion " + sig.fiscalYear(), columns, numeric, rows, totals);
    }

    public FixedAssetReportService.Table forecastTable(Forecast forecast) {
        List<List<Object>> rows = new ArrayList<>();
        for (Bucket bucket : forecast.buckets()) {
            rows.add(List.of(bucket.label(), bucket.inflows().values().stream().reduce(BigDecimal.ZERO, BigDecimal::add),
                    bucket.outflows().values().stream().reduce(BigDecimal.ZERO, BigDecimal::add), bucket.net(), bucket.closing()));
        }
        Map<String, Object> totals = new LinkedHashMap<>();
        totals.put("Tresorerie de depart", forecast.opening());
        forecast.assumptions().forEach(item -> totals.put("Hypothese " + (totals.size()), item));
        return new FixedAssetReportService.Table("previsionnel-tresorerie", "Previsionnel de tresorerie au " + forecast.date(),
                List.of("Periode", "Encaissements", "Decaissements", "Solde", "Tresorerie fin"), List.of(false, true, true, true, true), rows, totals);
    }

    private int integer(String key, int fallback) {
        try {
            return Integer.parseInt(settings.text(key, String.valueOf(fallback)).trim());
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    private static LocalDate max(LocalDate a, LocalDate b) {
        return a.isAfter(b) ? a : b;
    }

    private static LocalDate min(LocalDate a, LocalDate b) {
        return a.isBefore(b) ? a : b;
    }
}
