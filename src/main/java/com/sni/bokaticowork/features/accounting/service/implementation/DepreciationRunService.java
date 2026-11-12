package com.sni.bokaticowork.features.accounting.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.accounting.dto.AccountingRequests;
import com.sni.bokaticowork.features.accounting.enums.PeriodStatus;
import com.sni.bokaticowork.features.accounting.model.*;
import com.sni.bokaticowork.features.accounting.repository.*;
import com.sni.bokaticowork.features.accounting.service.support.AccountingNotifier;
import com.sni.bokaticowork.features.accounting.service.support.AccountingSettings;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

/**
 * Dotations aux amortissements (lot F18) : simulation, puis une piece par categorie et par
 * section analytique pour la periode, avec le detail par fiche ; rattrapage des mois non dotes
 * (bien mis en service apres coup, periode verrouillee) sur la periode ouverte ; reprise de la
 * subvention d'investissement au rythme de l'amortissement ; amortissement derogatoire quand le
 * plan fiscal differe. Une fiche ne recoit qu'une dotation par mois : relancer ne double rien.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class DepreciationRunService {

    public static final String RUN = "FIXED_ASSET:DEPRECIATION_RUN";

    private final FixedAssetService fixedAssets;
    private final FixedAssetRepository assets;
    private final FixedAssetScheduleRepository schedules;
    private final FixedAssetMeterReadingRepository readings;
    private final FaCategoryRepository categories;
    private final AccPeriodRepository periods;
    private final AccountingSettings settings;
    private final AccountingNotifier notifier;

    public record RunLine(String assetCode, String designation, String categoryCode, String sections, String period, BigDecimal amount,
                          boolean catchUp, boolean estimated) {
    }

    public record RunResult(String period, boolean simulated, int assets, BigDecimal total, BigDecimal derogatory, BigDecimal subsidyRelease,
                            List<String> entries, List<RunLine> lines, Map<String, BigDecimal> byCategory, Map<String, BigDecimal> bySection,
                            List<String> warnings) {
    }

    /** Simulation : ce que la periode recevrait, sans rien ecrire. */
    @Transactional(readOnly = true)
    public RunResult preview(String period) {
        return execute(FixedAssetService.month(period), true, false);
    }

    /** Dotations d'une periode lancees (ou relancees) a la main. */
    public RunResult run(String period) {
        fixedAssets.require(RUN);
        return execute(FixedAssetService.month(period), false, false);
    }

    /** Traitement automatique du mois ecoule (premier jour du mois) ; une periode verrouillee est rattrapee plus tard. */
    public RunResult runAutomatically(LocalDate today) {
        return execute(YearMonth.from(today).minusMonths(1), false, true);
    }

    private RunResult execute(YearMonth month, boolean simulate, boolean automatic) {
        String code = month.toString();
        List<String> warnings = new ArrayList<>();
        AccPeriod period = periods.findByCode(code).orElse(null);
        if (period == null) {
            if (simulate || automatic) {
                return empty(code, simulate, List.of("Periode " + code + " inconnue : creez l'exercice"));
            }
            throw new BadRequestException("Periode " + code + " inconnue : creez l'exercice");
        }
        boolean open = period.getStatus() == PeriodStatus.OPEN;
        if (!open && !simulate) {
            if (automatic) {
                markSkipped(code);
                return empty(code, false, List.of("Periode " + code + " verrouillee : dotations rattrapees sur la prochaine periode ouverte"));
            }
            throw new BadRequestException("La periode " + code + " est verrouillee ou close : les dotations seront rattrapees sur la periode ouverte suivante");
        }
        if (!open) {
            warnings.add("Periode " + code + " verrouillee ou close : les dotations iront sur la periode ouverte suivante");
        }
        Map<Long, FixedAsset> byId = new HashMap<>();
        Map<String, FaCategory> categoryCache = new HashMap<>();
        Map<String, List<FixedAssetSchedule>> groups = new TreeMap<>();
        Map<Long, BigDecimal> amounts = new HashMap<>();
        List<RunLine> lines = new ArrayList<>();
        Map<String, BigDecimal> byCategory = new TreeMap<>();
        Map<String, BigDecimal> bySection = new TreeMap<>();
        Set<Long> estimatedUnits = new HashSet<>();
        for (FixedAssetSchedule row : schedules.findAllByStatusInAndPeriodCodeLessThanEqualOrderByPeriodCodeAsc(FixedAssetService.OPEN, code)) {
            if (!FixedAssetService.ACCOUNTING.equals(row.getKind())) {
                continue;
            }
            FixedAsset asset = byId.computeIfAbsent(row.getAssetId(), id -> assets.findById(id).orElse(null));
            if (asset == null || !"IN_SERVICE".equals(asset.getAccountingStatus()) || !Objects.equals(row.getVersion(), asset.getPlanVersion())) {
                continue;
            }
            FaCategory category = categoryCache.computeIfAbsent(asset.getCategoryCode(), key -> categories.findById(key).orElse(null));
            if (category == null || category.getExpenseAccount() == null || category.getDepreciationAccount() == null) {
                warnings.add(asset.getCode() + " : categorie sans comptes d'amortissement");
                continue;
            }
            BigDecimal amount = row.getPlanned();
            boolean estimated = false;
            if ("UNITS".equals(fixedAssets.method(asset))) {
                Optional<FixedAssetMeterReading> reading = readings.findByAssetIdAndPeriodCode(asset.getId(), row.getPeriodCode());
                if (reading.isPresent() && asset.getTotalUnits() != null && asset.getTotalUnits().signum() > 0) {
                    BigDecimal remaining = asset.getEntryValue().subtract(asset.getResidualValue()).subtract(asset.getAccumulatedDepreciation())
                            .subtract(asset.getAccumulatedImpairment()).subtract(amounts.getOrDefault(asset.getId(), BigDecimal.ZERO)).max(BigDecimal.ZERO);
                    amount = asset.getEntryValue().subtract(asset.getResidualValue()).multiply(reading.get().getUnits())
                            .divide(asset.getTotalUnits(), 0, RoundingMode.HALF_UP).min(remaining);
                } else {
                    estimated = true;
                    estimatedUnits.add(asset.getId());
                    warnings.add(asset.getCode() + " : releve du compteur de " + row.getPeriodCode() + " absent, dotation estimee");
                }
            }
            row.setPlanned(amount);
            String sections = FixedAssetService.encode(fixedAssets.analytic(asset));
            String key = category.getCode() + "|" + (sections == null ? "" : sections);
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(row);
            amounts.merge(asset.getId(), amount, BigDecimal::add);
            boolean catchUp = row.getPeriodCode().compareTo(code) < 0;
            lines.add(new RunLine(asset.getCode(), asset.getDesignation(), category.getCode(), sections, row.getPeriodCode(), amount, catchUp, estimated));
            byCategory.merge(category.getCode(), amount, BigDecimal::add);
            bySection.merge(sections == null ? "(sans section)" : sections, amount, BigDecimal::add);
        }
        BigDecimal total = lines.stream().map(RunLine::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal subsidyTotal = BigDecimal.ZERO;
        List<String> entryNumbers = new ArrayList<>();
        if (simulate) {
            for (Map.Entry<Long, BigDecimal> entry : amounts.entrySet()) {
                subsidyTotal = subsidyTotal.add(subsidyRelease(byId.get(entry.getKey()), entry.getValue()));
            }
            return new RunResult(code, true, amounts.size(), total, derogatory(code, amounts.keySet(), byId, amounts, true, entryNumbers), subsidyTotal,
                    entryNumbers, lines, byCategory, bySection, warnings);
        }
        LocalDate date = month.atEndOfMonth();
        String journal = fixedAssets.journal();
        for (Map.Entry<String, List<FixedAssetSchedule>> group : groups.entrySet()) {
            FaCategory category = categoryCache.get(group.getKey().substring(0, group.getKey().indexOf('|')));
            String sectionKey = group.getKey().substring(group.getKey().indexOf('|') + 1);
            String label = "Dotations aux amortissements " + code + " " + category.getLabel() + (sectionKey.isEmpty() ? "" : " " + sectionKey);
            List<AccountingRequests.EntryLine> entryLines = new ArrayList<>();
            Map<Long, BigDecimal> perAsset = new LinkedHashMap<>();
            for (FixedAssetSchedule row : group.getValue()) {
                if (row.getPlanned().signum() <= 0) {
                    continue;
                }
                FixedAsset asset = byId.get(row.getAssetId());
                boolean catchUp = row.getPeriodCode().compareTo(code) < 0;
                String lineLabel = "Dotation " + asset.getCode() + " " + row.getPeriodCode() + (catchUp ? " (rattrapage)" : "");
                entryLines.add(fixedAssets.line(category.getExpenseAccount(), null, lineLabel, row.getPlanned(), null, asset.getCode(), fixedAssets.analytic(asset)));
                entryLines.add(fixedAssets.line(category.getDepreciationAccount(), null, lineLabel, null, row.getPlanned(), asset.getCode(), null));
                perAsset.merge(asset.getId(), row.getPlanned(), BigDecimal::add);
            }
            for (Map.Entry<Long, BigDecimal> item : perAsset.entrySet()) {
                FixedAsset asset = byId.get(item.getKey());
                BigDecimal release = subsidyRelease(asset, item.getValue());
                if (release.signum() > 0) {
                    String subsidyLabel = "Reprise de subvention " + asset.getCode() + " " + code;
                    entryLines.add(fixedAssets.line(asset.getSubsidyAccount(), null, subsidyLabel, release, null, asset.getCode(), null));
                    entryLines.add(fixedAssets.line(settings.text("accounting.fixed-asset.subsidy-income-account", "799"), null, subsidyLabel, null, release,
                            asset.getCode(), fixedAssets.analytic(asset)));
                    asset.setSubsidyReleased(asset.getSubsidyReleased().add(release));
                    subsidyTotal = subsidyTotal.add(release);
                }
            }
            String entryNumber = null;
            if (!entryLines.isEmpty()) {
                entryNumber = fixedAssets.book(journal, date, label, "FIXED_ASSET_DEPRECIATION",
                        code + "/" + category.getCode() + (sectionKey.isEmpty() ? "" : "/" + sectionKey), entryLines).getEntryNumber();
                entryNumbers.add(entryNumber);
            }
            for (FixedAssetSchedule row : group.getValue()) {
                FixedAsset asset = byId.get(row.getAssetId());
                boolean catchUp = row.getPeriodCode().compareTo(code) < 0;
                row.setPosted(row.getPlanned());
                row.setStatus(catchUp ? "CAUGHT_UP" : "POSTED");
                row.setEntryNumber(entryNumber);
                row.setPostedInPeriod(code);
                row.setPostedAt(Instant.now());
                if (catchUp) {
                    row.setNote("Rattrapage de " + row.getPeriodCode() + " dote en " + code);
                }
                schedules.save(row);
                asset.setAccumulatedDepreciation(asset.getAccumulatedDepreciation().add(row.getPlanned()));
            }
        }
        BigDecimal derogatory = derogatory(code, amounts.keySet(), byId, amounts, false, entryNumbers);
        for (Long id : amounts.keySet()) {
            FixedAsset asset = byId.get(id);
            asset.setUpdatedAt(Instant.now());
            assets.save(asset);
            if ("UNITS".equals(fixedAssets.method(asset)) && !estimatedUnits.contains(id)) {
                // Le releve reel change la suite : le reste est reparti sur les mois restants.
                fixedAssets.regenerate(asset, "Releve d'unites d'oeuvre " + code);
            }
            boolean remaining = fixedAssets.current(asset, FixedAssetService.ACCOUNTING).stream()
                    .anyMatch(row -> FixedAssetService.OPEN.contains(row.getStatus()));
            if (!remaining) {
                asset.setAccountingStatus("FULLY_DEPRECIATED");
                assets.save(asset);
                fixedAssets.event(asset, "FULLY_DEPRECIATED", date, null, null, "Bien totalement amorti", null, null, null);
            }
        }
        if (!estimatedUnits.isEmpty()) {
            notifier.notifyAccountants("Releves de compteur manquants", "Dotations estimees en " + code + " pour " + estimatedUnits.size()
                    + " bien(s) amorti(s) en unites d'oeuvre : saisissez leur releve.", code, "/accounting/fixed-assets", null);
        }
        return new RunResult(code, false, amounts.size(), total, derogatory, subsidyTotal, entryNumbers, lines, byCategory, bySection, warnings);
    }

    /**
     * Amortissement derogatoire : ecart entre le cumul fiscal et le cumul comptable a la fin de la
     * periode, moins le derogatoire deja inscrit. Dotation D 851 ; C 151, reprise D 151 ; C 861.
     */
    private BigDecimal derogatory(String code, Set<Long> touched, Map<Long, FixedAsset> byId, Map<Long, BigDecimal> amounts, boolean simulate,
                                  List<String> entryNumbers) {
        String reserve = settings.text("accounting.fixed-asset.derogatory-account", "151");
        String expense = settings.text("accounting.fixed-asset.derogatory-expense-account", "851");
        String income = settings.text("accounting.fixed-asset.derogatory-reversal-account", "861");
        List<AccountingRequests.EntryLine> lines = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (Long id : touched) {
            FixedAsset asset = byId.get(id);
            List<FixedAssetSchedule> tax = fixedAssets.current(asset, FixedAssetService.TAX);
            if (tax.isEmpty()) {
                continue;
            }
            BigDecimal taxCumulative = tax.stream().filter(row -> row.getPeriodCode().compareTo(code) <= 0 && !"CANCELLED".equals(row.getStatus()))
                    .map(row -> FixedAssetService.DONE.contains(row.getStatus()) ? row.getPosted() : row.getPlanned()).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal accounting = asset.getAccumulatedDepreciation().add(simulate ? amounts.getOrDefault(id, BigDecimal.ZERO) : BigDecimal.ZERO);
            BigDecimal delta = taxCumulative.subtract(accounting).max(BigDecimal.ZERO).subtract(asset.getAccumulatedDerogatory());
            if (delta.signum() == 0) {
                continue;
            }
            total = total.add(delta);
            String label = "Amortissement derogatoire " + asset.getCode() + " " + code;
            if (delta.signum() > 0) {
                lines.add(fixedAssets.line(expense, null, label, delta, null, asset.getCode(), null));
                lines.add(fixedAssets.line(reserve, null, label, null, delta, asset.getCode(), null));
            } else {
                lines.add(fixedAssets.line(reserve, null, label, delta.negate(), null, asset.getCode(), null));
                lines.add(fixedAssets.line(income, null, label, null, delta.negate(), asset.getCode(), null));
            }
            if (!simulate) {
                asset.setAccumulatedDerogatory(asset.getAccumulatedDerogatory().add(delta));
            }
        }
        if (simulate) {
            return total;
        }
        String entryNumber = null;
        if (!lines.isEmpty()) {
            entryNumber = fixedAssets.book(fixedAssets.journal(), YearMonth.parse(code).atEndOfMonth(), "Amortissements derogatoires " + code,
                    "FIXED_ASSET_DEROGATORY", code, lines).getEntryNumber();
            entryNumbers.add(entryNumber);
        }
        for (Long id : touched) {
            for (FixedAssetSchedule row : fixedAssets.current(byId.get(id), FixedAssetService.TAX)) {
                if (FixedAssetService.OPEN.contains(row.getStatus()) && row.getPeriodCode().compareTo(code) <= 0) {
                    row.setPosted(row.getPlanned());
                    row.setStatus("POSTED");
                    row.setEntryNumber(entryNumber);
                    row.setPostedInPeriod(code);
                    row.setPostedAt(Instant.now());
                    schedules.save(row);
                }
            }
        }
        return total;
    }

    /** Part de la subvention d'investissement reprise au resultat au rythme de l'amortissement. */
    private static BigDecimal subsidyRelease(FixedAsset asset, BigDecimal depreciation) {
        if (asset == null || asset.getSubsidyAccount() == null || asset.getSubsidyAmount().signum() <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal base = asset.getEntryValue().subtract(asset.getResidualValue());
        if (base.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal left = asset.getSubsidyAmount().subtract(asset.getSubsidyReleased()).max(BigDecimal.ZERO);
        return asset.getSubsidyAmount().multiply(depreciation).divide(base, 0, RoundingMode.HALF_UP).min(left);
    }

    private void markSkipped(String code) {
        for (FixedAssetSchedule row : schedules.findAllByStatusInAndPeriodCodeLessThanEqualOrderByPeriodCodeAsc(Set.of("PLANNED"), code)) {
            if (row.getPeriodCode().equals(code)) {
                row.setStatus("SKIPPED_LOCKED");
                row.setNote("Periode " + code + " verrouillee au traitement automatique");
                schedules.save(row);
            }
        }
    }

    private static RunResult empty(String code, boolean simulate, List<String> warnings) {
        return new RunResult(code, simulate, 0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, List.of(), List.of(), Map.of(), Map.of(), warnings);
    }
}
