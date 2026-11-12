package com.sni.bokaticowork.features.accounting.service.implementation;

import com.sni.bokaticowork.core.accounting.BudgetControl;
import com.sni.bokaticowork.features.accounting.model.*;
import com.sni.bokaticowork.features.accounting.repository.*;
import com.sni.bokaticowork.features.payment.model.CashOperationNature;
import com.sni.bokaticowork.features.payment.repository.CashOperationNatureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

/**
 * Controle budgetaire (lot F16) : rattachement d'une depense a son poste et a sa ligne de budget,
 * quatre montants d'une ligne (budgete, engage, realise, disponible), engagement, solde, liberation.
 *
 * <p>Le poste d'une depense vient, dans l'ordre : de la nature d'operation (correspondance
 * {@code NATURE}), du poste de depense de la nature ({@code EXPENSE_ITEM}), du compte (prefixe
 * {@code ACCOUNT}), du poste de depense du compte. Un poste qui se controle au niveau de son parent
 * remonte au parent. La ligne est celle du poste et de la section, a defaut celle du poste sans
 * section, dans le budget actif de l'exercice.</p>
 *
 * <p>Le realise vient de la comptabilite generale : pieces comptabilisees de l'exercice sur les
 * comptes et les postes de depense rattaches au poste et a ses sous-postes, filtrees sur la section
 * de la ligne par la ventilation analytique.</p>
 */
@Service
@Transactional
@RequiredArgsConstructor
public class BudgetEngine implements BudgetControl {

    static final Set<String> OPEN_FORECASTS = Set.of("PLANNED", "COMMITTED");

    private final AccBudgetItemRepository itemRepository;
    private final AccBudgetItemMappingRepository mappingRepository;
    private final AccBudgetRepository budgetRepository;
    private final AccBudgetLineRepository lineRepository;
    private final AccBudgetCommitmentRepository commitmentRepository;
    private final AccBudgetForecastRepository forecastRepository;
    private final AccBudgetOverrideRepository overrideRepository;
    private final AccAnalyticSectionAccountRepository sectionAccountRepository;
    private final AccPeriodRepository periodRepository;
    private final AccFiscalYearRepository fiscalYearRepository;
    private final CashOperationNatureRepository natureRepository;
    private final DataSource dataSource;

    // ------------------------------------------------------------------ rattachement

    /** Poste budgetaire d'une depense (avant remontee au parent), ou vide. */
    public Optional<AccBudgetItem> itemOf(Spend spend) {
        Optional<CashOperationNature> nature = StringUtils.hasText(spend.natureCode())
                ? natureRepository.findByCode(spend.natureCode().trim().toUpperCase(Locale.ROOT)) : Optional.empty();
        List<String> candidates = new ArrayList<>();
        nature.ifPresent(found -> mappingRepository.findByKindAndValue("NATURE", found.getCode()).ifPresent(m -> candidates.add(m.getItemCode())));
        nature.map(CashOperationNature::getDefaultSection).flatMap(section -> mappingRepository.findByKindAndValue("EXPENSE_ITEM", section))
                .ifPresent(m -> candidates.add(m.getItemCode()));
        String account = StringUtils.hasText(spend.accountNumber()) ? spend.accountNumber().trim()
                : nature.map(CashOperationNature::getAccountNumber).orElse(null);
        if (account != null) {
            mappingRepository.findByKind("ACCOUNT").stream().filter(m -> account.startsWith(m.getValue()))
                    .max(Comparator.comparingInt(m -> m.getValue().length())).ifPresent(m -> candidates.add(m.getItemCode()));
            sectionAccountRepository.findByAxisCode("POSTE").stream().filter(m -> account.startsWith(m.getAccountPrefix()))
                    .max(Comparator.comparingInt(m -> m.getAccountPrefix().length()))
                    .flatMap(m -> mappingRepository.findByKindAndValue("EXPENSE_ITEM", m.getSectionCode())).ifPresent(m -> candidates.add(m.getItemCode()));
        }
        return candidates.stream().findFirst().flatMap(itemRepository::findByCode).map(this::active);
    }

    private AccBudgetItem active(AccBudgetItem item) {
        AccBudgetItem current = item;
        for (int guard = 0; guard < 5 && "MERGED".equals(current.getStatus()) && current.getMergedInto() != null; guard++) {
            current = itemRepository.findByCode(current.getMergedInto()).orElse(current);
        }
        return current;
    }

    /** Ligne de budget actif qui controle la depense. */
    public Optional<AccBudgetLine> lineOf(Spend spend, LocalDate date) {
        Optional<AccBudget> budget = activeBudget(date);
        Optional<AccBudgetItem> item = itemOf(spend);
        if (budget.isEmpty() || item.isEmpty()) {
            return Optional.empty();
        }
        AccBudgetItem control = item.get();
        if (Boolean.TRUE.equals(control.getControlAtParent()) && control.getParentCode() != null) {
            control = itemRepository.findByCode(control.getParentCode()).orElse(control);
        }
        List<AccBudgetLine> lines = lineRepository.findByBudgetIdOrderByItemCodeAscSectionCodeAsc(budget.get().getId());
        String itemCode = control.getCode();
        String section = StringUtils.hasText(spend.sectionCode()) ? spend.sectionCode().trim().toUpperCase(Locale.ROOT) : null;
        return lines.stream().filter(line -> line.getItemCode().equals(itemCode) && Objects.equals(line.getSectionCode(), section)).findFirst()
                .or(() -> lines.stream().filter(line -> line.getItemCode().equals(itemCode) && line.getSectionCode() == null).findFirst());
    }

    public Optional<AccBudget> activeBudget(LocalDate date) {
        return periodRepository.findCovering(date).flatMap(period -> fiscalYearRepository.findById(period.getFiscalYearId()))
                .flatMap(year -> budgetRepository.findByFiscalYearCodeAndStatus(year.getCode(), "ACTIVE"));
    }

    // ------------------------------------------------------------------ montants

    public record Amounts(BigDecimal budgeted, BigDecimal committed, BigDecimal realized, BigDecimal available) {
    }

    public Amounts amounts(AccBudgetLine line) {
        BigDecimal committed = commitmentRepository.findByLineIdAndStatus(line.getId(), "OPEN").stream()
                .map(c -> c.getAmount().subtract(c.getSettledAmount())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal realized = realized(line, null);
        BigDecimal budgeted = line.getAnnualAmount();
        return new Amounts(scale(budgeted), scale(committed), scale(realized), scale(budgeted.subtract(committed).subtract(realized)));
    }

    /** Realise d'une ligne sur l'exercice de son budget, jusqu'a un mois donne (inclus) si indique. */
    public BigDecimal realized(AccBudgetLine line, YearMonth until) {
        return realized(line, budgetRepository.findById(line.getBudgetId()).orElseThrow().getFiscalYearCode(), until);
    }

    /** Realise du poste et de la section d'une ligne sur un exercice donne. */
    public BigDecimal realized(AccBudgetLine line, String fiscalYearCode, YearMonth until) {
        AccBudgetItem item = itemRepository.findByCode(line.getItemCode()).orElseThrow();
        Set<String> codes = descendants(item.getCode());
        List<AccBudgetItemMapping> mappings = new ArrayList<>();
        codes.forEach(code -> mappings.addAll(mappingRepository.findByItemCode(code)));
        List<String> prefixes = mappings.stream().filter(m -> "ACCOUNT".equals(m.getKind())).map(AccBudgetItemMapping::getValue).toList();
        List<String> postes = mappings.stream().filter(m -> "EXPENSE_ITEM".equals(m.getKind())).map(AccBudgetItemMapping::getValue).toList();
        if (prefixes.isEmpty() && postes.isEmpty()) {
            return BigDecimal.ZERO;
        }
        boolean product = "PRODUCT".equals(item.getItemType());
        StringBuilder sql = new StringBuilder("SELECT COALESCE(SUM(").append(product ? "l.credit - l.debit" : "l.debit - l.credit")
                .append("), 0) FROM acc_entry_line l JOIN acc_entry e ON e.id = l.entry_id WHERE e.status IN ('VALIDATED', 'REVERSED') ")
                .append("AND e.fiscal_year_code = ? AND e.journal_code <> 'CAG' AND (");
        List<Object> args = new ArrayList<>(List.of(fiscalYearCode));
        List<String> clauses = new ArrayList<>();
        for (String prefix : prefixes) {
            clauses.add("l.account_number LIKE ?");
            args.add(prefix + "%");
        }
        if (!postes.isEmpty()) {
            clauses.add("(l.account_number LIKE '6%' AND EXISTS (SELECT 1 FROM acc_analytic_allocation a WHERE a.entry_line_id = l.id "
                    + "AND a.axis_code = 'POSTE' AND a.section_code IN (" + String.join(",", Collections.nCopies(postes.size(), "?")) + ")))");
            args.addAll(postes);
        }
        sql.append(String.join(" OR ", clauses)).append(")");
        if (line.getSectionCode() != null) {
            sql.append(" AND EXISTS (SELECT 1 FROM acc_analytic_allocation s WHERE s.entry_line_id = l.id AND s.section_code = ?)");
            args.add(line.getSectionCode());
        }
        if (until != null) {
            sql.append(" AND e.entry_date <= ?");
            args.add(java.sql.Date.valueOf(until.atEndOfMonth()));
        }
        BigDecimal value = new JdbcTemplate(dataSource).queryForObject(sql.toString(), BigDecimal.class, args.toArray());
        return value == null ? BigDecimal.ZERO : value;
    }

    Set<String> descendants(String code) {
        Set<String> result = new LinkedHashSet<>(List.of(code));
        List<AccBudgetItem> all = itemRepository.findAllByOrderByCodeAsc();
        boolean grew = true;
        while (grew) {
            grew = false;
            for (AccBudgetItem item : all) {
                if (item.getParentCode() != null && result.contains(item.getParentCode()) && result.add(item.getCode())) {
                    grew = true;
                }
            }
        }
        return result;
    }

    String controlLevel(AccBudgetLine line) {
        if (line.getControlLevel() != null) {
            return line.getControlLevel();
        }
        return itemRepository.findByCode(line.getItemCode()).map(AccBudgetItem::getControlLevel).orElse("ALERT");
    }

    BigDecimal tolerance(AccBudgetLine line) {
        if (line.getTolerancePct() != null) {
            return line.getTolerancePct();
        }
        return itemRepository.findByCode(line.getItemCode()).map(AccBudgetItem::getTolerancePct).orElse(BigDecimal.ZERO);
    }

    // ------------------------------------------------------------------ controle

    @Override
    @Transactional(readOnly = true)
    public Impact check(List<Spend> spends, LocalDate date) {
        if (activeBudget(date).isEmpty()) {
            return new Impact(false, List.of(), false, List.of(), List.of("Aucun budget actif pour l'exercice : pas de controle budgetaire"));
        }
        Map<Long, BigDecimal> requested = new LinkedHashMap<>();
        Map<Long, AccBudgetLine> lines = new HashMap<>();
        List<String> unbudgeted = new ArrayList<>();
        for (Spend spend : spends) {
            Optional<AccBudgetLine> line = lineOf(spend, date);
            if (line.isEmpty()) {
                unbudgeted.add((spend.natureCode() != null ? spend.natureCode() : spend.accountNumber()) + " : aucune ligne de budget");
                continue;
            }
            lines.put(line.get().getId(), line.get());
            requested.merge(line.get().getId(), spend.amount(), BigDecimal::add);
        }
        List<LineImpact> impacts = new ArrayList<>();
        List<String> messages = new ArrayList<>();
        boolean blocked = false;
        for (Map.Entry<Long, BigDecimal> entry : requested.entrySet()) {
            AccBudgetLine line = lines.get(entry.getKey());
            Amounts amounts = amounts(line);
            BigDecimal after = amounts.available().subtract(entry.getValue());
            BigDecimal tolerance = line.getAnnualAmount().multiply(tolerance(line)).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            String level = controlLevel(line);
            Status status = after.signum() >= 0 ? Status.GREEN : after.add(tolerance).signum() >= 0 ? Status.ORANGE : Status.RED;
            if (Boolean.TRUE.equals(line.getFrozen())) {
                status = Status.RED;
                messages.add("Ligne " + line.getItemCode() + " gelee : aucun engagement possible");
            }
            if (status == Status.RED && ("BLOCKING".equals(level) || Boolean.TRUE.equals(line.getFrozen()))) {
                blocked = true;
                messages.add("Depassement bloquant sur " + line.getItemCode() + (line.getSectionCode() == null ? "" : " / " + line.getSectionCode())
                        + " : disponible " + plain(amounts.available()) + " XAF pour " + plain(entry.getValue()) + " XAF demandes");
            } else if (status != Status.GREEN && !"INFO".equals(level)) {
                messages.add("Alerte budgetaire sur " + line.getItemCode() + " : disponible apres " + plain(after) + " XAF");
            }
            AccBudgetItem item = itemRepository.findByCode(line.getItemCode()).orElseThrow();
            impacts.add(new LineImpact(line.getId(), item.getCode(), item.getLabel(), line.getSectionCode(), level, amounts.budgeted(),
                    amounts.committed(), amounts.realized(), amounts.available(), scale(entry.getValue()), scale(after), status,
                    openForecasts(line, date)));
        }
        return new Impact(true, impacts, blocked, unbudgeted, messages);
    }

    /** Previsions ouvertes du poste (et de la section), a deux mois pres. */
    List<String> openForecasts(AccBudgetLine line, LocalDate date) {
        YearMonth month = YearMonth.from(date);
        return forecastRepository.findByItemCodeOrderByPlannedMonthAsc(line.getItemCode()).stream()
                .filter(forecast -> OPEN_FORECASTS.contains(forecast.getStatus()))
                .filter(forecast -> line.getSectionCode() == null || Objects.equals(line.getSectionCode(), forecast.getSectionCode()))
                .filter(forecast -> Math.abs(YearMonth.parse(forecast.getPlannedMonth()).until(month, java.time.temporal.ChronoUnit.MONTHS)) <= 2
                        || !"NONE".equals(forecast.getRecurrence()))
                .map(forecast -> forecast.getNumber() + " : " + forecast.getSubject() + " (" + plain(forecast.getAmount()) + " XAF, " + forecast.getPlannedMonth() + ")")
                .toList();
    }

    @Override
    public void commit(String sourceType, String reference, List<Spend> spends, LocalDate date, String forecastNumber, boolean unplanned,
                       String unplannedCode, String unplannedText, String actor) {
        Map<Long, BigDecimal> perLine = new LinkedHashMap<>();
        for (Spend spend : spends) {
            lineOf(spend, date).ifPresent(line -> perLine.merge(line.getId(), spend.amount(), BigDecimal::add));
        }
        Set<Long> existing = new HashSet<>();
        commitmentRepository.findBySourceTypeAndReference(sourceType, reference).stream().filter(c -> "OPEN".equals(c.getStatus()))
                .forEach(c -> existing.add(c.getLineId()));
        perLine.forEach((lineId, amount) -> {
            if (existing.contains(lineId)) {
                return;
            }
            lineRepository.lock(lineId);
            commitmentRepository.save(AccBudgetCommitment.builder().lineId(lineId).sourceType(sourceType).reference(reference).amount(scale(amount))
                    .forecastNumber(forecastNumber).unplanned(unplanned).unplannedCode(unplannedCode).unplannedText(unplannedText)
                    .createdBy(actor == null ? "SYSTEM" : actor).build());
        });
        if (StringUtils.hasText(forecastNumber)) {
            forecastRepository.findByNumber(forecastNumber).ifPresent(forecast -> {
                if ("PLANNED".equals(forecast.getStatus())) {
                    forecast.setStatus("COMMITTED");
                    forecastRepository.save(forecast);
                }
            });
        }
    }

    @Override
    public void settle(String sourceType, String reference, BigDecimal realized) {
        BigDecimal left = realized == null ? BigDecimal.ZERO : realized;
        for (AccBudgetCommitment commitment : commitmentRepository.findBySourceTypeAndReference(sourceType, reference)) {
            if (!"OPEN".equals(commitment.getStatus())) {
                continue;
            }
            BigDecimal part = commitment.getAmount().min(left.max(BigDecimal.ZERO));
            left = left.subtract(part);
            commitment.setSettledAmount(scale(part));
            commitment.setStatus("SETTLED");
            commitment.setClosedAt(Instant.now());
            commitmentRepository.save(commitment);
            if (commitment.getForecastNumber() != null) {
                BigDecimal settled = part;
                forecastRepository.findByNumber(commitment.getForecastNumber()).ifPresent(forecast -> {
                    forecast.setRealizedAmount(forecast.getRealizedAmount().add(settled));
                    forecast.setStatus("NONE".equals(forecast.getRecurrence()) ? "REALIZED" : "PLANNED");
                    forecastRepository.save(forecast);
                });
            }
        }
    }

    @Override
    public void release(String sourceType, String reference, boolean expired) {
        for (AccBudgetCommitment commitment : commitmentRepository.findBySourceTypeAndReference(sourceType, reference)) {
            if ("OPEN".equals(commitment.getStatus())) {
                commitment.setStatus(expired ? "EXPIRED" : "RELEASED");
                commitment.setClosedAt(Instant.now());
                commitmentRepository.save(commitment);
                if (commitment.getForecastNumber() != null) {
                    forecastRepository.findByNumber(commitment.getForecastNumber()).filter(f -> "COMMITTED".equals(f.getStatus())).ifPresent(f -> {
                        f.setStatus("PLANNED");
                        forecastRepository.save(f);
                    });
                }
            }
        }
    }

    @Override
    public void recordOverride(String reference, Impact impact, String reason, String actor) {
        for (LineImpact line : impact.lines()) {
            if (line.status() == Status.RED) {
                overrideRepository.save(AccBudgetOverride.builder().lineId(line.lineId()).reference(reference)
                        .overrun(line.availableAfter().negate()).reason(reason).grantedBy(actor).build());
            }
        }
    }

    static BigDecimal scale(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(4, RoundingMode.HALF_UP);
    }

    static String plain(BigDecimal value) {
        return value == null ? "0" : value.setScale(0, RoundingMode.HALF_UP).toPlainString();
    }
}
