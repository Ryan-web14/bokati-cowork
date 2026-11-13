package com.sni.bokaticowork.features.accounting.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.accounting.model.AccAccrual;
import com.sni.bokaticowork.features.accounting.model.AccDeferralSchedule;
import com.sni.bokaticowork.features.accounting.repository.AccAccrualRepository;
import com.sni.bokaticowork.features.accounting.repository.AccDeferralScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/** Etats du lot F20 (section 10), rendus en JSON, CSV ou PDF par {@link FixedAssetReportService}. */
@Service
@RequiredArgsConstructor
public class DeferralReportService {

    private static final ZoneId ZONE = ZoneId.of("Africa/Brazzaville");
    public static final List<String> REPORTS = List.of("revenue-detail", "expense-detail", "revenue-forecast", "billed-vs-recognized", "accruals",
            "pass-credits", "anomalies");

    private final DeferralService deferrals;
    private final AccDeferralScheduleRepository schedules;
    private final AccAccrualRepository accruals;

    @Transactional(readOnly = true)
    public FixedAssetReportService.Table report(String report, Integer year, LocalDate date) {
        // Droit de lecture des constatations d'avance, controle par le service.
        deferrals.accruals(null);
        LocalDate at = date != null ? date : LocalDate.now(ZONE);
        int fiscalYear = year != null ? year : at.getYear();
        return switch (report == null ? "" : report.trim().toLowerCase(Locale.ROOT)) {
            case "revenue-detail" -> detail(DeferralService.REVENUE, at);
            case "expense-detail" -> detail(DeferralService.EXPENSE, at);
            case "revenue-forecast" -> forecast(at);
            case "billed-vs-recognized" -> billedVsRecognized(fiscalYear);
            case "accruals" -> accrualTable(fiscalYear);
            case "pass-credits" -> passCredits(at);
            case "anomalies" -> anomalies(at);
            default -> throw new BadRequestException("Etat inconnu : " + report + ". Etats disponibles : " + String.join(", ", REPORTS));
        };
    }

    /** Detail par ligne a une date : justificatif du 477 ou du 476. */
    FixedAssetReportService.Table detail(String kind, LocalDate at) {
        List<List<Object>> rows = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (DeferralService.DetailLine line : deferrals.detail(kind, at)) {
            rows.add(List.of(nz(line.thirdParty()), nz(line.documentNumber()), nz(line.label()), nz(line.activity()), line.start().toString(),
                    line.end().toString(), line.amount(), line.recognized(), line.remaining()));
            total = total.add(line.remaining());
        }
        return new FixedAssetReportService.Table(DeferralService.REVENUE.equals(kind) ? "revenue-detail" : "expense-detail",
                (DeferralService.REVENUE.equals(kind) ? "Produits constates d'avance au " : "Charges constatees d'avance au ") + at,
                List.of("Tiers", "Piece", "Libelle", "Activite", "Debut", "Fin", "Montant HT", "Reconnu", "Restant"),
                List.of(false, false, false, false, false, false, true, true, true), rows, Map.of("Restant", total));
    }

    /** Chiffre d'affaires deja facture a reconnaitre, mois par mois sur 12 mois, par activite. */
    FixedAssetReportService.Table forecast(LocalDate at) {
        YearMonth from = YearMonth.from(at);
        Map<String, BigDecimal[]> byActivity = new TreeMap<>();
        for (AccDeferralSchedule schedule : schedules.findAllByKindOrderByIdAsc(DeferralService.REVENUE)) {
            BigDecimal[] values = byActivity.computeIfAbsent(nz(schedule.getActivity()), key -> zeros(12));
            BigDecimal previous = deferrals.need(schedule, from.atEndOfMonth());
            for (int i = 0; i < 12; i++) {
                BigDecimal next = deferrals.need(schedule, from.plusMonths(i + 1).atEndOfMonth());
                values[i] = values[i].add(previous.subtract(next));
                previous = next;
            }
        }
        List<String> columns = new ArrayList<>(List.of("Activite"));
        for (int i = 1; i <= 12; i++) {
            columns.add(from.plusMonths(i).toString());
        }
        return table("revenue-forecast", "Chiffre d'affaires facture a reconnaitre", columns, byActivity);
    }

    /** Par mois : facture (montant des lignes a echeancier facturees ce mois), reconnu, variation du 477. */
    FixedAssetReportService.Table billedVsRecognized(int year) {
        Map<String, BigDecimal[]> byMonth = new TreeMap<>();
        for (AccDeferralSchedule schedule : schedules.findAllByKindOrderByIdAsc(DeferralService.REVENUE)) {
            YearMonth billed = YearMonth.from(schedule.getCreatedAt().atZone(ZONE));
            for (int month = 1; month <= 12; month++) {
                YearMonth period = YearMonth.of(year, month);
                BigDecimal[] values = byMonth.computeIfAbsent(period.toString(), key -> zeros(3));
                BigDecimal before = deferrals.need(schedule, period.minusMonths(1).atEndOfMonth());
                BigDecimal after = deferrals.need(schedule, period.atEndOfMonth());
                boolean billedThisMonth = billed.equals(period);
                if (billedThisMonth) {
                    values[0] = values[0].add(schedule.getAmount());
                }
                BigDecimal opening = YearMonth.from(schedule.getCreatedAt().atZone(ZONE)).isAfter(period.minusMonths(1)) ? BigDecimal.ZERO : before;
                BigDecimal invoicedNow = billedThisMonth ? schedule.getAmount() : BigDecimal.ZERO;
                values[1] = values[1].add(opening.add(invoicedNow).subtract(after));
                values[2] = values[2].add(after.subtract(opening));
            }
        }
        return table("billed-vs-recognized", "Chiffre d'affaires facture et reconnu " + year, List.of("Mois", "Facture (lignes a periode)", "Reconnu",
                "Variation du 477"), byMonth);
    }

    FixedAssetReportService.Table accrualTable(int year) {
        List<List<Object>> rows = new ArrayList<>();
        for (AccAccrual accrual : accruals.findAllByOrderByCreatedAtDesc()) {
            if (!accrual.getPeriodCode().startsWith(String.valueOf(year))) {
                continue;
            }
            rows.add(List.of(accrual.getKind(), accrual.getReference(), nz(accrual.getThirdParty()), accrual.getPeriodCode(), accrual.getEstimatedAmount(),
                    nz(accrual.getReversalEntryNumber()), nz(accrual.getActualReference()),
                    accrual.getActualAmount() == null ? BigDecimal.ZERO : accrual.getActualAmount(), accrual.getGap() == null ? BigDecimal.ZERO : accrual.getGap(),
                    accrual.getStatus()));
        }
        return new FixedAssetReportService.Table("accruals", "Factures a etablir et non parvenues " + year,
                List.of("Nature", "Reference", "Tiers", "Periode", "Estimation", "Extourne", "Facture reelle", "Montant reel", "Ecart", "Etat"),
                List.of(false, false, false, false, true, false, false, true, true, false), rows, Map.of());
    }

    FixedAssetReportService.Table passCredits(LocalDate at) {
        List<List<Object>> rows = new ArrayList<>();
        for (AccDeferralSchedule schedule : schedules.findAllByKindOrderByIdAsc(DeferralService.REVENUE)) {
            if (!"USAGE".equals(schedule.getMethod())) {
                continue;
            }
            rows.add(List.of(nz(schedule.getThirdParty()), nz(schedule.getDocumentNumber()), nz(schedule.getLabel()), schedule.getEndDate().toString(),
                    schedule.getTotalUnits() == null ? BigDecimal.ZERO : schedule.getTotalUnits(), deferrals.need(schedule, at)));
        }
        return new FixedAssetReportService.Table("pass-credits", "Credits de pass non consommes au " + at,
                List.of("Client", "Facture", "Pass", "Expiration", "Credits achetes", "Valeur restante"), List.of(false, false, false, false, true, true),
                rows, Map.of());
    }

    FixedAssetReportService.Table anomalies(LocalDate at) {
        List<List<Object>> rows = new ArrayList<>();
        for (DeferralService.Finding finding : deferrals.controls(at)) {
            rows.add(List.of(finding.severity(), finding.subject(), finding.message()));
        }
        return new FixedAssetReportService.Table("anomalies", "Anomalies de periode et ecarts", List.of("Gravite", "Sujet", "Detail"),
                List.of(false, false, false), rows, Map.of());
    }

    private static FixedAssetReportService.Table table(String code, String title, List<String> columns, Map<String, BigDecimal[]> byKey) {
        List<Boolean> numeric = new ArrayList<>(List.of(false));
        for (int i = 1; i < columns.size(); i++) {
            numeric.add(true);
        }
        List<List<Object>> rows = new ArrayList<>();
        BigDecimal[] sum = zeros(columns.size() - 1);
        byKey.forEach((key, values) -> {
            List<Object> row = new ArrayList<>(List.of(key));
            for (int i = 0; i < values.length; i++) {
                row.add(values[i]);
                sum[i] = sum[i].add(values[i]);
            }
            rows.add(row);
        });
        Map<String, Object> totals = new LinkedHashMap<>();
        for (int i = 1; i < columns.size(); i++) {
            totals.put(columns.get(i), sum[i - 1]);
        }
        return new FixedAssetReportService.Table(code, title, columns, numeric, rows, totals);
    }

    private static BigDecimal[] zeros(int size) {
        BigDecimal[] values = new BigDecimal[size];
        java.util.Arrays.fill(values, BigDecimal.ZERO);
        return values;
    }

    private static String nz(String value) {
        return value == null ? "" : value;
    }
}
