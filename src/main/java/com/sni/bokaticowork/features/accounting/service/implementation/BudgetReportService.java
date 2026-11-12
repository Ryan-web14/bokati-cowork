package com.sni.bokaticowork.features.accounting.service.implementation;

import com.sni.bokaticowork.features.accounting.model.*;
import com.sni.bokaticowork.features.accounting.repository.*;
import com.sni.bokaticowork.features.accounting.service.support.AccountingNotifier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

/**
 * Suivi budgetaire (lot F16, analytique-et-budget section 5) : suivi par ligne et par mois,
 * engagements ouverts, prevu et non prevu, suivi des previsions, projection de fin d'exercice ;
 * alertes de consommation et rappels des previsions.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BudgetReportService {

    private final AccBudgetRepository budgetRepository;
    private final AccBudgetLineRepository lineRepository;
    private final AccBudgetLineMonthRepository monthRepository;
    private final AccBudgetCommitmentRepository commitmentRepository;
    private final AccBudgetForecastRepository forecastRepository;
    private final AccBudgetItemRepository itemRepository;
    private final BudgetEngine engine;
    private final BudgetService budgets;
    private final AccountingNotifier notifier;

    public record Month(String month, BigDecimal budgeted, BigDecimal realizedCumulative, BigDecimal budgetedCumulative) {
    }

    public record FollowUp(BudgetService.Line line, List<Month> months, BigDecimal projection, BigDecimal projectedVariance) {
    }

    /** Suivi de toutes les lignes du budget actif d'un exercice, avec projection de fin d'exercice. */
    public List<FollowUp> followUp(String fiscalYearCode, YearMonth at) {
        AccBudget budget = budgetRepository.findByFiscalYearCodeAndStatus(fiscalYearCode, "ACTIVE").orElse(null);
        if (budget == null) {
            return List.of();
        }
        YearMonth until = at == null ? YearMonth.now() : at;
        List<FollowUp> result = new ArrayList<>();
        for (AccBudgetLine line : lineRepository.findByBudgetIdOrderByItemCodeAscSectionCodeAsc(budget.getId())) {
            List<Month> months = new ArrayList<>();
            BigDecimal cumulativeBudget = BigDecimal.ZERO;
            List<AccBudgetLineMonth> plan = monthRepository.findByLineIdOrderByMonthAsc(line.getId());
            int year = Integer.parseInt(fiscalYearCode);
            for (AccBudgetLineMonth month : plan) {
                YearMonth ym = YearMonth.of(year, month.getMonth());
                cumulativeBudget = cumulativeBudget.add(month.getAmount());
                BigDecimal realized = ym.isAfter(until) ? null : engine.realized(line, ym);
                months.add(new Month(ym.toString(), month.getAmount(), realized == null ? null : BudgetEngine.scale(realized),
                        BudgetEngine.scale(cumulativeBudget)));
            }
            BudgetService.Line view = budgets.lineView(line);
            int elapsed = until.getYear() == year ? until.getMonthValue() : until.getYear() > year ? 12 : 0;
            BigDecimal projection = elapsed == 0 ? view.committed()
                    : view.realized().multiply(BigDecimal.valueOf(12)).divide(BigDecimal.valueOf(elapsed), 0, RoundingMode.HALF_UP).max(view.realized())
                    .add(view.committed());
            result.add(new FollowUp(view, months, BudgetEngine.scale(projection), BudgetEngine.scale(line.getAnnualAmount().subtract(projection))));
        }
        return result;
    }

    public record Commitment(String sourceType, String reference, String itemCode, String sectionCode, BigDecimal amount, BigDecimal open,
                             String forecastNumber, Boolean unplanned, String unplannedCode, String createdBy, java.time.Instant createdAt, long ageDays) {
    }

    public List<Commitment> openCommitments() {
        LocalDate today = LocalDate.now();
        return commitmentRepository.findByStatusOrderByCreatedAtAsc("OPEN").stream().map(c -> {
            AccBudgetLine line = lineRepository.findById(c.getLineId()).orElse(null);
            return new Commitment(c.getSourceType(), c.getReference(), line == null ? null : line.getItemCode(), line == null ? null : line.getSectionCode(),
                    c.getAmount(), c.getAmount().subtract(c.getSettledAmount()), c.getForecastNumber(), c.getUnplanned(), c.getUnplannedCode(),
                    c.getCreatedBy(), c.getCreatedAt(), java.time.temporal.ChronoUnit.DAYS.between(c.getCreatedAt().atZone(java.time.ZoneOffset.UTC).toLocalDate(), today));
        }).toList();
    }

    public record PlannedSplit(String itemCode, BigDecimal planned, BigDecimal unplanned, BigDecimal unplannedPct, Map<String, BigDecimal> reasons) {
    }

    /** Prevu et non prevu par poste (engagements de la periode, soldes ou ouverts). */
    public List<PlannedSplit> plannedSplit() {
        Map<String, BigDecimal[]> totals = new TreeMap<>();
        Map<String, Map<String, BigDecimal>> reasons = new HashMap<>();
        for (AccBudgetCommitment c : commitmentRepository.findAll()) {
            if ("RELEASED".equals(c.getStatus()) || "EXPIRED".equals(c.getStatus())) {
                continue;
            }
            String item = lineRepository.findById(c.getLineId()).map(AccBudgetLine::getItemCode).orElse("?");
            BigDecimal amount = "SETTLED".equals(c.getStatus()) ? c.getSettledAmount() : c.getAmount();
            BigDecimal[] pair = totals.computeIfAbsent(item, key -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            if (Boolean.TRUE.equals(c.getUnplanned())) {
                pair[1] = pair[1].add(amount);
                reasons.computeIfAbsent(item, key -> new TreeMap<>()).merge(c.getUnplannedCode() == null ? "AUTRE" : c.getUnplannedCode(), amount, BigDecimal::add);
            } else {
                pair[0] = pair[0].add(amount);
            }
        }
        List<PlannedSplit> result = new ArrayList<>();
        totals.forEach((item, pair) -> {
            BigDecimal total = pair[0].add(pair[1]);
            result.add(new PlannedSplit(item, BudgetEngine.scale(pair[0]), BudgetEngine.scale(pair[1]),
                    total.signum() == 0 ? BigDecimal.ZERO : pair[1].multiply(BigDecimal.valueOf(100)).divide(total, 1, RoundingMode.HALF_UP),
                    reasons.getOrDefault(item, Map.of())));
        });
        return result;
    }

    public record ForecastRow(AccBudgetForecast forecast, Boolean overdue, BigDecimal gap) {
    }

    /** Suivi des previsions : echues sans demande, ecart prevu et realise. */
    public List<ForecastRow> forecastFollowUp() {
        YearMonth now = YearMonth.now();
        return forecastRepository.findAll(org.springframework.data.domain.Sort.by("plannedMonth")).stream()
                .map(f -> new ForecastRow(f, "PLANNED".equals(f.getStatus()) && YearMonth.parse(f.getPlannedMonth()).isBefore(now),
                        "REALIZED".equals(f.getStatus()) ? f.getRealizedAmount().subtract(f.getAmount()) : null))
                .toList();
    }

    /** Reserve pour imprevus par poste : budget non couvert par une prevision ouverte ou realisee. */
    public Map<String, BigDecimal> reserve(String fiscalYearCode) {
        Map<String, BigDecimal> result = new TreeMap<>();
        budgetRepository.findByFiscalYearCodeAndStatus(fiscalYearCode, "ACTIVE").ifPresent(budget -> {
            for (AccBudgetLine line : lineRepository.findByBudgetIdOrderByItemCodeAscSectionCodeAsc(budget.getId())) {
                BigDecimal forecasted = forecastRepository.findByItemCodeOrderByPlannedMonthAsc(line.getItemCode()).stream()
                        .filter(f -> !Set.of("CANCELLED", "POSTPONED").contains(f.getStatus()) && f.getPlannedMonth().startsWith(fiscalYearCode))
                        .map(f -> f.getAmount().multiply(BigDecimal.valueOf(f.getOccurrences()))).reduce(BigDecimal.ZERO, BigDecimal::add);
                result.merge(line.getItemCode() + (line.getSectionCode() == null ? "" : "/" + line.getSectionCode()),
                        BudgetEngine.scale(line.getAnnualAmount().subtract(forecasted)), BigDecimal::add);
            }
        });
        return result;
    }

    // ------------------------------------------------------------------ alertes

    /** Seuils de consommation (75, 90, 100 %) et previsions arrivees a echeance sans demande. */
    @Transactional
    public int alerts(String fiscalYearCode) {
        int sent = 0;
        AccBudget budget = budgetRepository.findByFiscalYearCodeAndStatus(fiscalYearCode, "ACTIVE").orElse(null);
        if (budget != null) {
            for (AccBudgetLine line : lineRepository.findByBudgetIdOrderByItemCodeAscSectionCodeAsc(budget.getId())) {
                if (line.getAnnualAmount().signum() == 0) {
                    continue;
                }
                BudgetEngine.Amounts amounts = engine.amounts(line);
                int pct = amounts.committed().add(amounts.realized()).multiply(BigDecimal.valueOf(100))
                        .divide(line.getAnnualAmount(), 0, RoundingMode.DOWN).intValue();
                int threshold = pct >= 100 ? 100 : pct >= 90 ? 90 : pct >= 75 ? 75 : 0;
                if (threshold > line.getAlertedPct()) {
                    String label = itemRepository.findByCode(line.getItemCode()).map(AccBudgetItem::getLabel).orElse(line.getItemCode());
                    notifier.notifyAccountants("Budget consomme a " + pct + " % · " + label,
                            "La ligne " + label + (line.getSectionCode() == null ? "" : " / " + line.getSectionCode()) + " est consommee a " + pct
                                    + " % (engage " + BudgetEngine.plain(amounts.committed()) + ", realise " + BudgetEngine.plain(amounts.realized())
                                    + ", budget " + BudgetEngine.plain(line.getAnnualAmount()) + " XAF).",
                            line.getItemCode(), "/accounting/budget", line.getResponsible());
                    line.setAlertedPct(threshold);
                    lineRepository.save(line);
                    sent++;
                }
            }
        }
        YearMonth next = YearMonth.now().plusMonths(1);
        for (AccBudgetForecast forecast : forecastRepository.findByStatusInOrderByPlannedMonthAsc(List.of("PLANNED"))) {
            YearMonth month = YearMonth.parse(forecast.getPlannedMonth());
            if (!month.isAfter(next) && (forecast.getRemindedAt() == null
                    || forecast.getRemindedAt().isBefore(java.time.Instant.now().minus(java.time.Duration.ofDays(7))))) {
                notifier.notifyAccountants("Prevision sans demande · " + forecast.getSubject(),
                        "La prevision " + forecast.getNumber() + " (" + forecast.getSubject() + ", " + BudgetEngine.plain(forecast.getAmount()) + " XAF, "
                                + forecast.getPlannedMonth() + ") n'a pas encore de demande" + (month.isBefore(YearMonth.now()) ? " et son mois est passe : reportez-la ou annulez-la." : "."),
                        forecast.getNumber(), "/accounting/budget/forecasts", forecast.getResponsible());
                forecast.setRemindedAt(java.time.Instant.now());
                forecastRepository.save(forecast);
                sent++;
            }
        }
        return sent;
    }
}
