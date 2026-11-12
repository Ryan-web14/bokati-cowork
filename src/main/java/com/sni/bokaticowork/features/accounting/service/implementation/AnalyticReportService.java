package com.sni.bokaticowork.features.accounting.service.implementation;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

/**
 * Etats analytiques (lot F9) : compte de resultat par section, analyse d'un poste de depense,
 * tableau de bord des depenses, balance analytique. Les montants viennent des ventilations des
 * pieces comptabilisees (validees ou contre-passees : la contre-passation est elle-meme ventilee).
 */
@Service
@RequiredArgsConstructor
public class AnalyticReportService {

    private static final String BASE = " FROM acc_analytic_allocation a JOIN acc_entry_line l ON l.id = a.entry_line_id "
            + "JOIN acc_entry e ON e.id = a.entry_id WHERE e.status IN ('VALIDATED', 'REVERSED') ";
    /** Signe d'une charge : debit positif ; d'un produit : credit positif. */
    private static final String CHARGE = "CASE WHEN l.debit > 0 THEN a.amount ELSE -a.amount END";
    private static final String PRODUCT = "CASE WHEN l.credit > 0 THEN a.amount ELSE -a.amount END";

    private final DataSource dataSource;

    public record SectionResult(String section, String label, BigDecimal products, BigDecimal directCharges, BigDecimal distributedIn,
                                BigDecimal distributedOut, BigDecimal margin, BigDecimal result) {
    }

    public record Result(String axis, LocalDate from, LocalDate to, List<SectionResult> sections, SectionResult total) {
    }

    /** Compte de resultat analytique d'un axe sur une periode. */
    public Result result(String axis, LocalDate from, LocalDate to) {
        JdbcTemplate jdbc = jdbc();
        Map<String, BigDecimal[]> rows = new TreeMap<>();
        jdbc.query("SELECT a.section_code, SUM(CASE WHEN l.account_number LIKE '7%' THEN " + PRODUCT + " ELSE 0 END) AS products, "
                        + "SUM(CASE WHEN l.account_number LIKE '6%' THEN " + CHARGE + " ELSE 0 END) AS charges" + BASE
                        + "AND a.axis_code = ? AND e.entry_date BETWEEN ? AND ? GROUP BY a.section_code",
                rs -> {
                    rows.put(rs.getString(1), new BigDecimal[]{rs.getBigDecimal(2), rs.getBigDecimal(3), BigDecimal.ZERO, BigDecimal.ZERO});
                }, axis, Date.valueOf(from), Date.valueOf(to));
        jdbc.query("SELECT d.from_section, d.to_section, d.amount FROM acc_analytic_distribution d JOIN acc_period p ON p.code = d.period_code "
                        + "WHERE d.axis_code = ? AND p.start_date >= ? AND p.end_date <= ?",
                rs -> {
                    BigDecimal amount = rs.getBigDecimal(3);
                    rows.computeIfAbsent(rs.getString(1), key -> zeros())[3] = rows.get(rs.getString(1))[3].add(amount);
                    rows.computeIfAbsent(rs.getString(2), key -> zeros())[2] = rows.get(rs.getString(2))[2].add(amount);
                }, axis, Date.valueOf(from), Date.valueOf(to));
        Map<String, String> labels = labels(axis);
        List<SectionResult> sections = new ArrayList<>();
        BigDecimal[] total = zeros();
        rows.forEach((section, values) -> {
            BigDecimal margin = values[0].subtract(values[1]);
            BigDecimal result = margin.subtract(values[2]).add(values[3]);
            sections.add(new SectionResult(section, labels.get(section), scale(values[0]), scale(values[1]), scale(values[2]), scale(values[3]),
                    scale(margin), scale(result)));
            for (int index = 0; index < 4; index++) {
                total[index] = total[index].add(values[index]);
            }
        });
        BigDecimal margin = total[0].subtract(total[1]);
        return new Result(axis, from, to, sections, new SectionResult("TOTAL", "Total", scale(total[0]), scale(total[1]), scale(total[2]),
                scale(total[3]), scale(margin), scale(margin.subtract(total[2]).add(total[3]))));
    }

    public record Month(String month, BigDecimal amount) {
    }

    public record Breakdown(String key, String label, BigDecimal amount) {
    }

    public record Detail(LocalDate date, String entryNumber, String journal, String label, String account, String thirdParty, BigDecimal amount) {
    }

    public record ItemAnalysis(String item, String label, String family, String costBehavior, LocalDate from, LocalDate to, BigDecimal period,
                               BigDecimal yearToDate, BigDecimal previousYearSamePeriod, BigDecimal variancePercent, BigDecimal projectionYearEnd,
                               List<Month> monthly, List<Breakdown> bySite, List<Breakdown> byCentre, List<Breakdown> bySupplier,
                               List<Breakdown> byJournal, List<Detail> detail, List<String> alerts) {
    }

    /** Analyse d'un poste de depense (section de l'axe POSTE). */
    public ItemAnalysis item(String code, LocalDate from, LocalDate to) {
        JdbcTemplate jdbc = jdbc();
        Map<String, Object> section = jdbc.queryForList("SELECT label, family, cost_behavior FROM acc_analytic_section WHERE axis_code = 'POSTE' AND code = ?",
                code).stream().findFirst().orElse(Map.of());
        BigDecimal period = charges(code, from, to);
        LocalDate yearStart = LocalDate.of(to.getYear(), 1, 1);
        BigDecimal ytd = charges(code, yearStart, to);
        BigDecimal previous = charges(code, from.minusYears(1), to.minusYears(1));
        BigDecimal variance = previous.signum() == 0 ? null
                : period.subtract(previous).multiply(BigDecimal.valueOf(100)).divide(previous, 1, RoundingMode.HALF_UP);
        List<Month> monthly = new ArrayList<>();
        YearMonth end = YearMonth.from(to);
        for (int index = 11; index >= 0; index--) {
            YearMonth month = end.minusMonths(index);
            monthly.add(new Month(month.toString(), charges(code, month.atDay(1), month.atEndOfMonth())));
        }
        long monthsElapsed = Math.max(1, to.getMonthValue());
        BigDecimal projection = ytd.multiply(BigDecimal.valueOf(12)).divide(BigDecimal.valueOf(monthsElapsed), 0, RoundingMode.HALF_UP);
        List<Breakdown> bySite = otherAxis(code, "SITE", from, to);
        List<Breakdown> byCentre = otherAxis(code, "CENTRE", from, to);
        List<Breakdown> bySupplier = new ArrayList<>();
        List<Breakdown> byJournal = new ArrayList<>();
        jdbc.query("SELECT e.journal_code, SUM(" + CHARGE + ")" + BASE + "AND a.axis_code = 'POSTE' AND a.section_code = ? AND e.entry_date BETWEEN ? AND ? "
                        + "AND l.account_number LIKE '6%' GROUP BY e.journal_code ORDER BY 2 DESC",
                rs -> {
                    byJournal.add(new Breakdown(rs.getString(1), rs.getString(1), scale(rs.getBigDecimal(2))));
                }, code, Date.valueOf(from), Date.valueOf(to));
        // Fournisseur : le tiers porte par une autre ligne de la piece (401, 421), sinon le libelle de la piece.
        jdbc.query("SELECT COALESCE((SELECT MIN(o.third_party_code) FROM acc_entry_line o WHERE o.entry_id = e.id AND o.third_party_code IS NOT NULL), "
                        + "'(sans tiers)') AS supplier, SUM(" + CHARGE + ")" + BASE + "AND a.axis_code = 'POSTE' AND a.section_code = ? "
                        + "AND e.entry_date BETWEEN ? AND ? AND l.account_number LIKE '6%' GROUP BY supplier ORDER BY 2 DESC LIMIT 10",
                rs -> {
                    bySupplier.add(new Breakdown(rs.getString(1), rs.getString(1), scale(rs.getBigDecimal(2))));
                }, code, Date.valueOf(from), Date.valueOf(to));
        List<Detail> detail = new ArrayList<>();
        jdbc.query("SELECT e.entry_date, COALESCE(e.entry_number, e.provisional_number), e.journal_code, l.label, l.account_number, "
                        + "(SELECT MIN(o.third_party_code) FROM acc_entry_line o WHERE o.entry_id = e.id AND o.third_party_code IS NOT NULL), " + CHARGE
                        + BASE + "AND a.axis_code = 'POSTE' AND a.section_code = ? AND e.entry_date BETWEEN ? AND ? AND l.account_number LIKE '6%' "
                        + "ORDER BY e.entry_date DESC, e.id DESC LIMIT 500",
                rs -> {
                    detail.add(new Detail(rs.getDate(1).toLocalDate(), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5),
                            rs.getString(6), scale(rs.getBigDecimal(7))));
                }, code, Date.valueOf(from), Date.valueOf(to));
        List<String> alerts = new ArrayList<>();
        BigDecimal lastThree = monthly.subList(8, 11).stream().map(Month::amount).reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(3), 0, RoundingMode.HALF_UP);
        BigDecimal current = monthly.get(11).amount();
        if (lastThree.signum() > 0 && current.compareTo(lastThree.multiply(new BigDecimal("1.3"))) > 0) {
            alerts.add("Hausse de " + current.subtract(lastThree).multiply(BigDecimal.valueOf(100)).divide(lastThree, 0, RoundingMode.HALF_UP)
                    + " % par rapport a la moyenne des trois mois precedents");
        }
        return new ItemAnalysis(code, (String) section.get("label"), (String) section.get("family"), (String) section.get("cost_behavior"), from, to,
                period, ytd, previous, variance, projection, monthly, bySite, byCentre, bySupplier, byJournal, detail, alerts);
    }

    public record DashboardRow(String item, String label, String family, BigDecimal period, BigDecimal previousPeriod, BigDecimal variancePercent,
                               BigDecimal averageThreeMonths, BigDecimal drift) {
    }

    /** Tableau de bord des depenses : tous les postes, tries par derive ; les 5 qui derivent le plus en tete. */
    public List<DashboardRow> dashboard(LocalDate from, LocalDate to) {
        JdbcTemplate jdbc = jdbc();
        long days = java.time.temporal.ChronoUnit.DAYS.between(from, to) + 1;
        LocalDate previousFrom = from.minusDays(days);
        LocalDate previousTo = from.minusDays(1);
        List<DashboardRow> rows = new ArrayList<>();
        for (Map<String, Object> section : jdbc.queryForList("SELECT code, label, family FROM acc_analytic_section WHERE axis_code = 'POSTE' "
                + "AND status IN ('ACTIVE', 'PENDING_APPROVAL', 'CLOSED') ORDER BY sort_order, code")) {
            String code = (String) section.get("code");
            BigDecimal period = charges(code, from, to);
            BigDecimal previous = charges(code, previousFrom, previousTo);
            BigDecimal average = charges(code, from.minusMonths(3), from.minusDays(1)).divide(BigDecimal.valueOf(3), 0, RoundingMode.HALF_UP);
            if (period.signum() == 0 && previous.signum() == 0 && average.signum() == 0) {
                continue;
            }
            BigDecimal variance = previous.signum() == 0 ? null
                    : period.subtract(previous).multiply(BigDecimal.valueOf(100)).divide(previous, 1, RoundingMode.HALF_UP);
            rows.add(new DashboardRow(code, (String) section.get("label"), (String) section.get("family"), period, previous, variance, average,
                    period.subtract(previous)));
        }
        rows.sort(Comparator.comparing(DashboardRow::drift).reversed());
        return rows;
    }

    public record BalanceLine(String section, String label, String account, BigDecimal debit, BigDecimal credit, BigDecimal balance) {
    }

    /** Balance analytique d'un axe : par section et par compte. */
    public List<BalanceLine> balance(String axis, LocalDate from, LocalDate to) {
        Map<String, String> labels = labels(axis);
        List<BalanceLine> lines = new ArrayList<>();
        jdbc().query("SELECT a.section_code, l.account_number, SUM(CASE WHEN l.debit > 0 THEN a.amount ELSE 0 END), "
                        + "SUM(CASE WHEN l.credit > 0 THEN a.amount ELSE 0 END)" + BASE + "AND a.axis_code = ? AND e.entry_date BETWEEN ? AND ? "
                        + "GROUP BY a.section_code, l.account_number ORDER BY a.section_code, l.account_number",
                rs -> {
                    BigDecimal debit = scale(rs.getBigDecimal(3));
                    BigDecimal credit = scale(rs.getBigDecimal(4));
                    lines.add(new BalanceLine(rs.getString(1), labels.get(rs.getString(1)), rs.getString(2), debit, credit, debit.subtract(credit)));
                }, axis, Date.valueOf(from), Date.valueOf(to));
        return lines;
    }

    /** Controle : ventile face a la comptabilite generale (lignes des comptes concernes). */
    public Map<String, BigDecimal> control(String axis, LocalDate from, LocalDate to) {
        JdbcTemplate jdbc = jdbc();
        BigDecimal allocated = Optional.ofNullable(jdbc.queryForObject("SELECT COALESCE(SUM(a.amount), 0)" + BASE
                + "AND a.axis_code = ? AND e.entry_date BETWEEN ? AND ?", BigDecimal.class, axis, Date.valueOf(from), Date.valueOf(to))).orElse(BigDecimal.ZERO);
        BigDecimal general = Optional.ofNullable(jdbc.queryForObject("SELECT COALESCE(SUM(l.debit + l.credit), 0) FROM acc_entry_line l JOIN acc_entry e "
                + "ON e.id = l.entry_id WHERE e.status IN ('VALIDATED', 'REVERSED') AND (l.account_number LIKE '6%' OR l.account_number LIKE '7%') "
                + "AND e.entry_date BETWEEN ? AND ?", BigDecimal.class, Date.valueOf(from), Date.valueOf(to))).orElse(BigDecimal.ZERO);
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        result.put("allocated", scale(allocated));
        result.put("general", scale(general));
        result.put("notAllocated", scale(general.subtract(allocated)));
        return result;
    }

    private BigDecimal charges(String item, LocalDate from, LocalDate to) {
        BigDecimal value = jdbc().queryForObject("SELECT COALESCE(SUM(" + CHARGE + "), 0)" + BASE + "AND a.axis_code = 'POSTE' AND a.section_code = ? "
                + "AND e.entry_date BETWEEN ? AND ? AND l.account_number LIKE '6%'", BigDecimal.class, item, Date.valueOf(from), Date.valueOf(to));
        return scale(value == null ? BigDecimal.ZERO : value);
    }

    /** Decomposition d'un poste sur un autre axe (meme ligne, autre ventilation). */
    private List<Breakdown> otherAxis(String item, String axis, LocalDate from, LocalDate to) {
        Map<String, String> labels = labels(axis);
        List<Breakdown> result = new ArrayList<>();
        jdbc().query("SELECT o.section_code, SUM(CASE WHEN l.debit > 0 THEN o.amount ELSE -o.amount END) FROM acc_analytic_allocation a "
                        + "JOIN acc_analytic_allocation o ON o.entry_line_id = a.entry_line_id AND o.axis_code = ? JOIN acc_entry_line l ON l.id = a.entry_line_id "
                        + "JOIN acc_entry e ON e.id = a.entry_id WHERE e.status IN ('VALIDATED', 'REVERSED') AND a.axis_code = 'POSTE' AND a.section_code = ? "
                        + "AND e.entry_date BETWEEN ? AND ? GROUP BY o.section_code ORDER BY 2 DESC",
                rs -> {
                    result.add(new Breakdown(rs.getString(1), labels.get(rs.getString(1)), scale(rs.getBigDecimal(2))));
                }, axis, item, Date.valueOf(from), Date.valueOf(to));
        return result;
    }

    private Map<String, String> labels(String axis) {
        Map<String, String> labels = new HashMap<>();
        jdbc().query("SELECT code, label FROM acc_analytic_section WHERE axis_code = ?", rs -> {
            labels.put(rs.getString(1), rs.getString(2));
        }, axis);
        return labels;
    }

    private JdbcTemplate jdbc() {
        return new JdbcTemplate(dataSource);
    }

    private static BigDecimal[] zeros() {
        return new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO};
    }

    private static BigDecimal scale(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(4, RoundingMode.HALF_UP);
    }
}
