package com.sni.bokaticowork.features.accounting.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.accounting.model.FaCategory;
import com.sni.bokaticowork.features.accounting.model.FixedAsset;
import com.sni.bokaticowork.features.accounting.model.FixedAssetDisposal;
import com.sni.bokaticowork.features.accounting.model.FixedAssetImpairment;
import com.sni.bokaticowork.features.accounting.model.FixedAssetInventoryCampaign;
import com.sni.bokaticowork.features.accounting.model.FixedAssetSchedule;
import com.sni.bokaticowork.features.accounting.repository.FaCategoryRepository;
import com.sni.bokaticowork.features.accounting.repository.FixedAssetDisposalRepository;
import com.sni.bokaticowork.features.accounting.repository.FixedAssetImpairmentRepository;
import com.sni.bokaticowork.features.accounting.repository.FixedAssetInventoryCampaignRepository;
import com.sni.bokaticowork.features.accounting.repository.FixedAssetRepository;
import com.sni.bokaticowork.features.accounting.repository.FixedAssetScheduleRepository;
import com.sni.bokaticowork.features.accounting.service.support.AccountingSettings;
import com.sni.bokaticowork.features.fund.voucher.VoucherService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Etats du fichier des immobilisations (lot F18, section 11), controles quotidiens et liste de
 * cloture (section 12). Chaque etat est un tableau : rendu JSON, CSV ou PDF.
 */
@Service
@RequiredArgsConstructor
public class FixedAssetReportService {

    private static final ZoneId ZONE = ZoneId.of("Africa/Brazzaville");
    private static final BigDecimal TOLERANCE = new BigDecimal("0.5");

    private final FixedAssetService fixedAssets;
    private final org.springframework.beans.factory.ObjectProvider<LoanService> loans;
    private final FixedAssetRepository assets;
    private final FaCategoryRepository categories;
    private final FixedAssetScheduleRepository schedules;
    private final FixedAssetDisposalRepository disposals;
    private final FixedAssetImpairmentRepository impairments;
    private final FixedAssetInventoryCampaignRepository campaigns;
    private final AccountingSettings settings;
    private final VoucherService vouchers;
    private final DataSource dataSource;
    private final EntityManager entityManager;

    /** Un etat : titre, colonnes (numeriques ou non) et lignes. */
    public record Table(String code, String title, List<String> columns, List<Boolean> numeric, List<List<Object>> rows,
                        Map<String, Object> totals) {
    }

    /** Un ecart de controle : gravite, sujet, message. */
    public record Finding(String severity, String subject, String message) {
    }

    public static final List<String> REPORTS = List.of("registry", "note-3a", "note-3b", "note-3c", "note-3d", "impairments", "forecast",
            "fully-depreciated", "pending", "warranties", "holders", "reconciliation", "cost-by-resource");

    // ================================================================== fichier

    /** Fichier des immobilisations, filtre : les fiches elles-memes, pour l'ecran de liste. */
    @Transactional(readOnly = true)
    public List<FixedAsset> list(String status, String category, String site, String resource, String holder) {
        fixedAssets.require(FixedAssetService.READ);
        return assets.findAllByOrderByCodeAsc().stream()
                .filter(asset -> matches(asset.getAccountingStatus(), status) && matches(asset.getCategoryCode(), category)
                        && matches(asset.getSite(), site) && matches(asset.getResourceCode(), resource) && matches(asset.getHolder(), holder))
                .toList();
    }

    // ================================================================== etats

    @Transactional(readOnly = true)
    public Table report(String report, Integer year, LocalDate date, String category, String site, String resource, String holder) {
        fixedAssets.require(FixedAssetService.READ);
        int fiscalYear = year != null ? year : LocalDate.now(ZONE).getYear();
        LocalDate at = date != null ? date : LocalDate.now(ZONE);
        return switch (report == null ? "" : report.trim().toLowerCase(Locale.ROOT)) {
            case "registry" -> registry(at, category, site, resource, holder);
            case "note-3a" -> note3a(fiscalYear);
            case "note-3b" -> note3b(at);
            case "note-3c" -> note3c(fiscalYear);
            case "note-3d" -> note3d(fiscalYear);
            case "impairments" -> impairmentTable();
            case "forecast" -> forecast(at);
            case "fully-depreciated" -> fullyDepreciated(at);
            case "pending" -> pending(at);
            case "warranties" -> warranties(at);
            case "holders" -> holders();
            case "reconciliation" -> reconciliation();
            case "cost-by-resource" -> costByResource(fiscalYear);
            default -> throw new BadRequestException("Etat inconnu : " + report + ". Etats disponibles : " + String.join(", ", REPORTS));
        };
    }

    /** Fichier des immobilisations a une date : valeurs reconstituees depuis les dotations comptabilisees. */
    Table registry(LocalDate at, String category, String site, String resource, String holder) {
        String period = YearMonth.from(at).toString();
        Map<String, FaCategory> byCode = categoryMap();
        List<List<Object>> rows = new ArrayList<>();
        BigDecimal gross = BigDecimal.ZERO;
        BigDecimal cumulative = BigDecimal.ZERO;
        BigDecimal impaired = BigDecimal.ZERO;
        for (FixedAsset asset : assets.findAllByOrderByCodeAsc()) {
            if (!inFile(asset, at) || !matches(asset.getCategoryCode(), category) || !matches(asset.getSite(), site)
                    || !matches(asset.getResourceCode(), resource) || !matches(asset.getHolder(), holder)) {
                continue;
            }
            BigDecimal depreciation = depreciationAt(asset, period);
            BigDecimal impairment = impairmentAt(asset, at);
            BigDecimal net = value(asset.getEntryValue()).subtract(depreciation).subtract(impairment);
            gross = gross.add(value(asset.getEntryValue()));
            cumulative = cumulative.add(depreciation);
            impaired = impaired.add(impairment);
            rows.add(List.of(asset.getCode(), nz(asset.getDesignation()), label(byCode, asset.getCategoryCode()), nz(asset.getSite()),
                    nz(asset.getResourceCode()), dateText(asset.getInServiceDate()), value(asset.getEntryValue()), depreciation, impairment, net,
                    nz(asset.getAccountingStatus()), nz(asset.getOperationalStatus())));
        }
        return new Table("registry", "Fichier des immobilisations au " + dateText(at),
                List.of("Code", "Designation", "Categorie", "Site", "Ressource", "Mise en service", "Valeur brute", "Amortissements",
                        "Depreciations", "Valeur nette", "Etat comptable", "Etat d'usage"),
                List.of(false, false, false, false, false, false, true, true, true, true, false, false), rows,
                totals("Valeur brute", gross, "Amortissements", cumulative, "Depreciations", impaired,
                        "Valeur nette", gross.subtract(cumulative).subtract(impaired)));
    }

    /** Note 3A : valeurs brutes par categorie, debut, acquisitions, sorties, fin d'exercice. */
    Table note3a(int year) {
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);
        Map<String, BigDecimal[]> byCategory = new TreeMap<>();
        for (FixedAsset asset : assets.findAllByOrderByCodeAsc()) {
            if (!Boolean.TRUE.equals(asset.getBooked()) || asset.getParentId() != null && isSplit(asset)) {
                continue;
            }
            BigDecimal[] values = byCategory.computeIfAbsent(asset.getCategoryCode(), key -> zeros(4));
            LocalDate entry = entryDate(asset);
            LocalDate exit = exitDate(asset);
            BigDecimal gross = value(asset.getEntryValue());
            if (entry != null && entry.isBefore(start) && (exit == null || !exit.isBefore(start))) {
                values[0] = values[0].add(gross);
            }
            if (entry != null && !entry.isBefore(start) && !entry.isAfter(end)) {
                values[1] = values[1].add(gross);
            }
            if (exit != null && !exit.isBefore(start) && !exit.isAfter(end)) {
                values[2] = values[2].add(gross);
            }
            if (entry != null && !entry.isAfter(end) && (exit == null || exit.isAfter(end))) {
                values[3] = values[3].add(gross);
            }
        }
        return byCategoryTable("note-3a", "Tableau des immobilisations " + year + " (note 3A)",
                List.of("Valeur brute debut", "Acquisitions", "Sorties", "Valeur brute fin"), byCategory);
    }

    /** Note 3B : biens en location-acquisition, valeurs et dette restante de l'emprunt associe. */
    Table note3b(LocalDate at) {
        Map<String, BigDecimal> debts = new java.util.HashMap<>();
        try {
            for (LoanService.NoteLine line : loans.getObject().note(at)) {
                debts.put(line.code(), line.outstanding());
            }
        } catch (RuntimeException ex) {
            // Sans droit sur les emprunts, la dette n'est pas affichee.
        }
        List<List<Object>> rows = new ArrayList<>();
        BigDecimal gross = BigDecimal.ZERO;
        BigDecimal net = BigDecimal.ZERO;
        BigDecimal debt = BigDecimal.ZERO;
        for (FixedAsset asset : assets.findAllByOrderByCodeAsc()) {
            if (!StringUtils.hasText(asset.getLoanCode()) && !"LEASE".equals(asset.getEntryMode())) {
                continue;
            }
            if (!inFile(asset, at)) {
                continue;
            }
            BigDecimal value = value(asset.getEntryValue());
            BigDecimal netValue = value.subtract(value(asset.getAccumulatedDepreciation())).subtract(value(asset.getAccumulatedImpairment()));
            BigDecimal remaining = asset.getLoanCode() == null ? BigDecimal.ZERO : debts.getOrDefault(asset.getLoanCode(), BigDecimal.ZERO);
            rows.add(List.of(asset.getCode(), nz(asset.getDesignation()), nz(asset.getLoanCode()), value, value(asset.getAccumulatedDepreciation()),
                    netValue, remaining));
            gross = gross.add(value);
            net = net.add(netValue);
            debt = debt.add(remaining);
        }
        return new Table("note-3b", "Biens en location-acquisition au " + dateText(at) + " (note 3B)",
                List.of("Code", "Designation", "Emprunt", "Valeur brute", "Amortissements", "Valeur nette", "Dette restante"),
                List.of(false, false, false, true, true, true, true), rows, totals("Valeur brute", gross, "Valeur nette", net, "Dette restante", debt));
    }

    /** Note 3C : amortissements par categorie, cumul debut, dotations, sorties, cumul fin. */
    Table note3c(int year) {
        String first = YearMonth.of(year, 1).toString();
        String last = YearMonth.of(year, 12).toString();
        Map<String, BigDecimal[]> byCategory = new TreeMap<>();
        Map<Long, FixedAsset> byId = assets.findAllByOrderByCodeAsc().stream().collect(Collectors.toMap(FixedAsset::getId, Function.identity()));
        for (FixedAsset asset : byId.values()) {
            byCategory.computeIfAbsent(asset.getCategoryCode(), key -> zeros(4));
        }
        for (FixedAssetSchedule row : schedules.findAllByStatusInAndPeriodCodeLessThanEqualOrderByPeriodCodeAsc(FixedAssetService.DONE, last)) {
            FixedAsset asset = byId.get(row.getAssetId());
            if (asset == null || !FixedAssetService.ACCOUNTING.equals(row.getKind())) {
                continue;
            }
            BigDecimal[] values = byCategory.get(asset.getCategoryCode());
            BigDecimal amount = value(row.getPosted());
            if (row.getPeriodCode().compareTo(first) < 0) {
                values[0] = values[0].add(amount);
            } else {
                values[1] = values[1].add(amount);
            }
        }
        for (FixedAssetDisposal disposal : disposals.findAllByStatusOrderByDisposalDateAsc("VALIDATED")) {
            FixedAsset asset = byId.get(disposal.getAssetId());
            if (asset == null || disposal.getDisposalDate() == null) {
                continue;
            }
            BigDecimal[] values = byCategory.get(asset.getCategoryCode());
            int disposedYear = disposal.getDisposalDate().getYear();
            if (disposedYear < year) {
                values[0] = values[0].subtract(value(disposal.getAccumulatedDepreciation()));
            } else if (disposedYear == year) {
                values[2] = values[2].add(value(disposal.getAccumulatedDepreciation()));
            }
        }
        byCategory.values().forEach(values -> values[3] = values[0].add(values[1]).subtract(values[2]));
        return byCategoryTable("note-3c", "Tableau des amortissements " + year + " (note 3C)",
                List.of("Cumul debut", "Dotations", "Sorties", "Cumul fin"), byCategory);
    }

    /** Note 3D : plus et moins-values des biens sortis dans l'exercice. */
    Table note3d(int year) {
        List<List<Object>> rows = new ArrayList<>();
        BigDecimal result = BigDecimal.ZERO;
        for (FixedAssetDisposal disposal : disposals.findAllByStatusOrderByDisposalDateAsc("VALIDATED")) {
            if (disposal.getDisposalDate() == null || disposal.getDisposalDate().getYear() != year) {
                continue;
            }
            FixedAsset asset = assets.findById(disposal.getAssetId()).orElse(null);
            result = result.add(value(disposal.getGainLoss()));
            rows.add(List.of(asset == null ? "" : asset.getCode(), asset == null ? "" : nz(asset.getDesignation()), nz(disposal.getReason()),
                    dateText(disposal.getDisposalDate()), value(disposal.getGrossValue()), value(disposal.getAccumulatedDepreciation()),
                    value(disposal.getAccumulatedImpairment()), value(disposal.getNetValue()), value(disposal.getPrice()), value(disposal.getGainLoss())));
        }
        return new Table("note-3d", "Plus et moins-values de cession " + year + " (note 3D)",
                List.of("Code", "Designation", "Motif", "Date", "Valeur brute", "Amortissements", "Depreciations", "Valeur nette", "Prix",
                        "Resultat"),
                List.of(false, false, false, false, true, true, true, true, true, true), rows, totals("Resultat", result));
    }

    Table impairmentTable() {
        List<List<Object>> rows = new ArrayList<>();
        for (FixedAssetImpairment impairment : impairments.findAllByOrderByImpairmentDateAsc()) {
            FixedAsset asset = assets.findById(impairment.getAssetId()).orElse(null);
            rows.add(List.of(asset == null ? "" : asset.getCode(), asset == null ? "" : nz(asset.getDesignation()), nz(impairment.getKind()),
                    dateText(impairment.getImpairmentDate()), nz(impairment.getIndicator()), value(impairment.getCurrentValue()),
                    value(impairment.getAmount()), nz(impairment.getStatus())));
        }
        return new Table("impairments", "Depreciations des immobilisations",
                List.of("Code", "Designation", "Sens", "Date", "Indice", "Valeur actuelle", "Montant", "Etat"),
                List.of(false, false, false, false, false, true, true, false), rows, Map.of());
    }

    /** Dotations prevues des 12 prochains mois et des 5 prochains exercices, par categorie. */
    Table forecast(LocalDate at) {
        YearMonth from = YearMonth.from(at);
        Map<String, BigDecimal[]> byCategory = new TreeMap<>();
        for (FixedAsset asset : assets.findAllByOrderByCodeAsc()) {
            if (!"IN_SERVICE".equals(asset.getAccountingStatus())) {
                continue;
            }
            BigDecimal[] values = byCategory.computeIfAbsent(asset.getCategoryCode(), key -> zeros(6));
            for (FixedAssetSchedule row : schedules.findAllByAssetIdAndKindAndVersionOrderByPeriodCodeAsc(asset.getId(),
                    FixedAssetService.ACCOUNTING, asset.getPlanVersion())) {
                if (!FixedAssetService.OPEN.contains(row.getStatus())) {
                    continue;
                }
                YearMonth month = YearMonth.parse(row.getPeriodCode());
                if (month.isBefore(from)) {
                    continue;
                }
                if (month.isBefore(from.plusMonths(12))) {
                    values[0] = values[0].add(value(row.getPlanned()));
                }
                int offset = month.getYear() - from.getYear();
                if (offset >= 0 && offset < 5) {
                    values[1 + offset] = values[1 + offset].add(value(row.getPlanned()));
                }
            }
        }
        List<String> columns = new ArrayList<>(List.of("12 prochains mois"));
        for (int i = 0; i < 5; i++) {
            columns.add("Exercice " + (from.getYear() + i));
        }
        return byCategoryTable("forecast", "Dotations previsionnelles", columns, byCategory);
    }

    Table fullyDepreciated(LocalDate at) {
        List<List<Object>> rows = new ArrayList<>();
        for (FixedAsset asset : assets.findAllByOrderByCodeAsc()) {
            if (!"IN_SERVICE".equals(asset.getAccountingStatus())) {
                continue;
            }
            BigDecimal net = value(asset.getEntryValue()).subtract(value(asset.getAccumulatedDepreciation())).subtract(value(asset.getAccumulatedImpairment()));
            if (net.subtract(value(asset.getResidualValue())).abs().compareTo(TOLERANCE) > 0) {
                continue;
            }
            rows.add(List.of(asset.getCode(), nz(asset.getDesignation()), dateText(asset.getInServiceDate()), years(asset.getInServiceDate(), at),
                    nz(asset.getResourceCode()), nz(asset.getOperationalStatus())));
        }
        return new Table("fully-depreciated", "Biens totalement amortis encore en service",
                List.of("Code", "Designation", "Mise en service", "Anciennete (ans)", "Ressource", "Etat d'usage"),
                List.of(false, false, false, true, false, false), rows, Map.of());
    }

    Table pending(LocalDate at) {
        List<List<Object>> rows = new ArrayList<>();
        for (FixedAsset asset : assets.findAllByAccountingStatusIn(List.of("DRAFT", "IN_PROGRESS", "RECEIVED"))) {
            LocalDate since = asset.getReceivedDate() != null ? asset.getReceivedDate() : asset.getCreatedAt().atZone(ZONE).toLocalDate();
            rows.add(List.of(asset.getCode(), nz(asset.getDesignation()), nz(asset.getAccountingStatus()), dateText(since),
                    ChronoUnit.DAYS.between(since, at), value(asset.getEntryValue()), nz(asset.getSupplierInvoiceNumber())));
        }
        rows.sort(Comparator.comparing(row -> -((Number) row.get(4)).longValue()));
        return new Table("pending", "Biens en attente de mise en service et immobilisations en cours",
                List.of("Code", "Designation", "Etat", "Depuis", "Jours", "Valeur", "Facture"),
                List.of(false, false, false, false, true, true, false), rows, Map.of());
    }

    Table warranties(LocalDate at) {
        int days = (int) number("accounting.fixed-asset.warranty-alert-days", 30);
        List<List<Object>> rows = new ArrayList<>();
        for (FixedAsset asset : assets.findAllByOrderByCodeAsc()) {
            if ("DISPOSED".equals(asset.getAccountingStatus())) {
                continue;
            }
            addDue(rows, asset, "Garantie", asset.getWarrantyEnd(), at, days);
            addDue(rows, asset, "Contrat d'entretien", asset.getMaintenanceEnd(), at, days);
        }
        rows.sort(Comparator.comparing(row -> (String) row.get(3)));
        return new Table("warranties", "Garanties et contrats arrivant a echeance (" + days + " jours)",
                List.of("Code", "Designation", "Echeance", "Date", "Jours restants", "Prestataire"),
                List.of(false, false, false, false, true, false), rows, Map.of());
    }

    Table holders() {
        List<List<Object>> rows = new ArrayList<>();
        assets.findAllByOrderByCodeAsc().stream().filter(asset -> StringUtils.hasText(asset.getHolder()) && !"DISPOSED".equals(asset.getAccountingStatus()))
                .sorted(Comparator.comparing(FixedAsset::getHolder).thenComparing(FixedAsset::getCode))
                .forEach(asset -> rows.add(List.of(asset.getHolder(), asset.getCode(), nz(asset.getDesignation()), dateText(asset.getHolderSince()),
                        value(asset.getEntryValue()))));
        return new Table("holders", "Biens par detenteur", List.of("Detenteur", "Code", "Designation", "Depuis", "Valeur"),
                List.of(false, false, false, false, true), rows, Map.of());
    }

    /** Rapprochement du fichier et de la comptabilite, compte par compte (bloquant a la cloture). */
    Table reconciliation() {
        Map<String, BigDecimal[]> byAccount = new TreeMap<>();
        Map<String, FaCategory> byCode = categoryMap();
        for (FixedAsset asset : assets.findAllByOrderByCodeAsc()) {
            if (!Boolean.TRUE.equals(asset.getBooked()) || "DISPOSED".equals(asset.getAccountingStatus()) || isSplit(asset)) {
                continue;
            }
            FaCategory category = byCode.get(asset.getCategoryCode());
            if (category == null) {
                continue;
            }
            String assetAccount = "IN_PROGRESS".equals(asset.getAccountingStatus())
                    ? settings.text("accounting.fixed-asset.in-progress-account", "239") : category.getAssetAccount();
            byAccount.computeIfAbsent(assetAccount, key -> zeros(1))[0] =
                    byAccount.get(assetAccount)[0].add(value(asset.getEntryValue()));
            if (StringUtils.hasText(category.getDepreciationAccount())) {
                byAccount.computeIfAbsent(category.getDepreciationAccount(), key -> zeros(1))[0] =
                        byAccount.get(category.getDepreciationAccount())[0].add(value(asset.getAccumulatedDepreciation()));
            }
            if (StringUtils.hasText(category.getImpairmentAccount())) {
                byAccount.computeIfAbsent(category.getImpairmentAccount(), key -> zeros(1))[0] =
                        byAccount.get(category.getImpairmentAccount())[0].add(value(asset.getAccumulatedImpairment()));
            }
        }
        for (FaCategory category : byCode.values()) {
            for (String account : new String[]{category.getAssetAccount(), category.getDepreciationAccount(), category.getImpairmentAccount()}) {
                if (StringUtils.hasText(account)) {
                    byAccount.computeIfAbsent(account, key -> zeros(1));
                }
            }
        }
        Map<String, BigDecimal> balances = balances(byAccount.keySet());
        List<List<Object>> rows = new ArrayList<>();
        BigDecimal gaps = BigDecimal.ZERO;
        for (Map.Entry<String, BigDecimal[]> entry : byAccount.entrySet()) {
            String account = entry.getKey();
            BigDecimal ledger = balances.getOrDefault(account, BigDecimal.ZERO);
            // Les comptes 28 et 29 sont crediteurs : on les compare en valeur absolue.
            BigDecimal comparable = account.startsWith("28") || account.startsWith("29") ? ledger.negate() : ledger;
            BigDecimal gap = comparable.subtract(entry.getValue()[0]);
            gaps = gaps.add(gap.abs());
            rows.add(List.of(account, comparable, entry.getValue()[0], gap, gap.abs().compareTo(TOLERANCE) <= 0 ? "OK" : "ECART"));
        }
        return new Table("reconciliation", "Rapprochement du fichier des immobilisations et de la comptabilite",
                List.of("Compte", "Solde comptable", "Total des fiches", "Ecart", "Etat"), List.of(false, true, true, true, false), rows,
                totals("Ecarts", gaps));
    }

    /** Cout par espace : dotations de l'exercice par ressource. */
    Table costByResource(int year) {
        String first = YearMonth.of(year, 1).toString();
        String last = YearMonth.of(year, 12).toString();
        Map<Long, FixedAsset> byId = assets.findAllByOrderByCodeAsc().stream().collect(Collectors.toMap(FixedAsset::getId, Function.identity()));
        Map<String, BigDecimal> byResource = new TreeMap<>();
        for (FixedAssetSchedule row : schedules.findAllByStatusInAndPeriodCodeLessThanEqualOrderByPeriodCodeAsc(FixedAssetService.DONE, last)) {
            FixedAsset asset = byId.get(row.getAssetId());
            if (asset == null || !FixedAssetService.ACCOUNTING.equals(row.getKind()) || row.getPeriodCode().compareTo(first) < 0) {
                continue;
            }
            String key = StringUtils.hasText(asset.getResourceCode()) ? asset.getResourceCode() : "(" + nz(asset.getSite()) + ")";
            byResource.merge(key, value(row.getPosted()), BigDecimal::add);
        }
        List<List<Object>> rows = new ArrayList<>();
        byResource.forEach((resource, amount) -> rows.add(List.of(resource, amount)));
        return new Table("cost-by-resource", "Dotations " + year + " par espace", List.of("Ressource ou site", "Dotations"),
                List.of(false, true), rows, totals("Dotations", byResource.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add)));
    }

    // ================================================================== controles et cloture

    /** Controles quotidiens de la section 12. */
    @Transactional(readOnly = true)
    public List<Finding> controls(LocalDate today) {
        fixedAssets.require(FixedAssetService.READ);
        return controlsUnchecked(today);
    }

    /** Controles sans droit exige : pour le traitement de nuit. */
    @Transactional(readOnly = true)
    public List<Finding> controlsUnchecked(LocalDate today) {
        List<Finding> findings = new ArrayList<>();
        reconciliation().rows().stream().filter(row -> "ECART".equals(row.get(4)))
                .forEach(row -> findings.add(new Finding("BLOCKING", "Compte " + row.get(0),
                        "Le solde comptable (" + VoucherService.money((BigDecimal) row.get(1)) + ") differe du total des fiches ("
                                + VoucherService.money((BigDecimal) row.get(2)) + ")")));
        long draftDays = number("accounting.fixed-asset.draft-alert-days", 15);
        long commissionDays = number("accounting.fixed-asset.commission-alert-days", 30);
        for (FixedAsset asset : assets.findAllByAccountingStatusIn(List.of("DRAFT", "RECEIVED"))) {
            LocalDate since = asset.getReceivedDate() != null ? asset.getReceivedDate() : asset.getCreatedAt().atZone(ZONE).toLocalDate();
            long age = ChronoUnit.DAYS.between(since, today);
            if ("DRAFT".equals(asset.getAccountingStatus()) && age > draftDays) {
                findings.add(new Finding("WARNING", asset.getCode(), "Fiche a completer depuis " + age + " jours"));
            } else if ("RECEIVED".equals(asset.getAccountingStatus()) && age > commissionDays) {
                findings.add(new Finding("WARNING", asset.getCode(), "Bien recu non mis en service depuis " + age + " jours"));
            }
        }
        String previous = YearMonth.from(today).minusMonths(1).toString();
        long missing = schedules.findAllByStatusInAndPeriodCodeLessThanEqualOrderByPeriodCodeAsc(FixedAssetService.OPEN, previous).stream()
                .filter(row -> FixedAssetService.ACCOUNTING.equals(row.getKind())).map(FixedAssetSchedule::getAssetId).distinct().count();
        if (missing > 0) {
            findings.add(new Finding("WARNING", "Dotations", missing + " bien(s) avec des dotations non comptabilisees jusqu'a " + previous));
        }
        for (FixedAssetDisposal disposal : disposals.findAllByStatusOrderByDisposalDateAsc("VALIDATED")) {
            if (value(disposal.getPrice()).signum() > 0 && value(disposal.getCollectedAmount()).compareTo(value(disposal.getPrice())) < 0
                    && disposal.getDisposalDate() != null && disposal.getDisposalDate().plusDays(30).isBefore(today)) {
                findings.add(new Finding("WARNING", disposal.getVoucherNumber(), "Prix de cession non encaisse ("
                        + VoucherService.money(value(disposal.getPrice()).subtract(value(disposal.getCollectedAmount()))) + " restant)"));
            }
        }
        return findings;
    }

    /** Liste de cloture de l'exercice : vide quand la cloture peut se faire. */
    @Transactional(readOnly = true)
    public List<Finding> closingCheck(int year) {
        fixedAssets.require(FixedAssetService.READ);
        List<Finding> findings = new ArrayList<>();
        String last = YearMonth.of(year, 12).toString();
        long open = schedules.findAllByStatusInAndPeriodCodeLessThanEqualOrderByPeriodCodeAsc(FixedAssetService.OPEN, last).stream()
                .filter(row -> FixedAssetService.ACCOUNTING.equals(row.getKind())).count();
        if (open > 0) {
            findings.add(new Finding("BLOCKING", "Dotations", open + " dotation(s) de l'exercice " + year + " ou anterieures non comptabilisee(s)"));
        }
        reconciliation().rows().stream().filter(row -> "ECART".equals(row.get(4)))
                .forEach(row -> findings.add(new Finding("BLOCKING", "Compte " + row.get(0), "Ecart entre le fichier et la comptabilite : "
                        + VoucherService.money((BigDecimal) row.get(3)))));
        boolean inventoryRequired = !"false".equalsIgnoreCase(settings.text("accounting.fixed-asset.inventory-required", "true"));
        boolean inventoryDone = campaigns.findAllByOrderByCreatedAtDesc().stream()
                .anyMatch(campaign -> "CLOSED".equals(campaign.getStatus()) && campaign.getClosedAt() != null
                        && campaign.getClosedAt().atZone(ZONE).getYear() == year && StringUtils.hasText(campaign.getVoucherNumber()));
        if (inventoryRequired && !inventoryDone && !assets.findAllByAccountingStatusIn(List.of("IN_SERVICE")).isEmpty()) {
            findings.add(new Finding("BLOCKING", "Inventaire physique", "Aucune campagne d'inventaire close avec son PV dans l'exercice " + year));
        }
        for (FixedAsset asset : assets.findAllByAccountingStatusIn(List.of("IN_PROGRESS"))) {
            if (!StringUtils.hasText(asset.getSupplierInvoiceNumber()) && !StringUtils.hasText(asset.getNotes())) {
                findings.add(new Finding("BLOCKING", asset.getCode(), "Immobilisation en cours sans facture rattachee ni justification"));
            }
        }
        return findings;
    }

    /** Fiche imprimable d'un bien : identification, valeurs, plan en cours, historique. */
    @Transactional(readOnly = true)
    public byte[] sheetPdf(String code) {
        fixedAssets.require(FixedAssetService.READ);
        FixedAssetService.AssetView view = fixedAssets.view(code);
        FixedAsset asset = view.asset();
        List<VoucherService.Section> sections = new ArrayList<>(fixedAssets.identification(asset));
        sections.add(new VoucherService.Section("Situation").pair("État comptable", asset.getAccountingStatus())
                .pair("État d'usage", asset.getOperationalStatus()).pair("Mise en service", asset.getInServiceDate())
                .pair("Site", asset.getSite()).pair("Ressource", asset.getResourceCode()).pair("Emplacement", asset.getLocationLabel())
                .pair("Détenteur", asset.getHolder()).pair("Garantie jusqu'au", asset.getWarrantyEnd())
                .pair("Assureur", asset.getInsurer()).pair("Police", asset.getPolicyNumber()));
        sections.add(new VoucherService.Section("Valeurs").pair("Valeur d'entrée", VoucherService.money(value(asset.getEntryValue())))
                .pair("Amortissements cumulés", VoucherService.money(value(asset.getAccumulatedDepreciation())))
                .pair("Dépréciations", VoucherService.money(value(asset.getAccumulatedImpairment())))
                .pair("Valeur nette", VoucherService.money(value(view.netValue())))
                .pair("Mode et durée", nz(asset.getMethod()) + (asset.getDurationMonths() == null ? "" : ", " + asset.getDurationMonths() + " mois")));
        VoucherService.Section plan = new VoucherService.Section("Plan d'amortissement (version " + asset.getPlanVersion() + ")")
                .columns(List.of("Période", "Prévu", "Comptabilisé", "Cumul", "Valeur nette", "État"), List.of(false, true, true, true, true, false));
        view.schedule().stream().filter(row -> FixedAssetService.ACCOUNTING.equals(row.getKind()))
                .forEach(row -> plan.row(row.getPeriodCode(), VoucherService.money(value(row.getPlanned())), VoucherService.money(value(row.getPosted())),
                        VoucherService.money(value(row.getCumulative())), VoucherService.money(value(row.getNetValue())), row.getStatus()));
        sections.add(plan);
        VoucherService.Section history = new VoucherService.Section("Historique").columns(List.of("Date", "Événement", "Avant", "Après", "Motif"),
                List.of(false, false, false, false, false));
        view.events().forEach(event -> history.row(event.getEventDate(), event.getKind(), event.getBeforeValue(), event.getAfterValue(), event.getReason()));
        sections.add(history);
        VoucherService.Issued issued = new VoucherService.Issued(asset.getCode(), "IMM", asset.getCode(), asset.getEntryValue(),
                asset.getDesignation(), fixedAssets.actor(), Instant.now(), 0, null, null);
        return vouchers.render("Fiche d'immobilisation", issued, sections, null, null, List.of());
    }

    // ================================================================== exports

    public byte[] csv(Table table) {
        StringBuilder out = new StringBuilder("﻿");
        out.append(String.join(";", table.columns())).append("\r\n");
        for (List<Object> row : table.rows()) {
            out.append(row.stream().map(FixedAssetReportService::cell).collect(Collectors.joining(";"))).append("\r\n");
        }
        table.totals().forEach((label, amount) -> out.append(cell(label)).append(";").append(cell(amount)).append("\r\n"));
        return out.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Transactional(readOnly = true)
    public byte[] pdf(Table table) {
        VoucherService.Issued issued = new VoucherService.Issued("ETAT-" + table.code().toUpperCase(Locale.ROOT), "ETAT", table.code(), null,
                table.title(), fixedAssets.actor(), Instant.now(), 0, null, null);
        VoucherService.Section body = new VoucherService.Section(table.title()).columns(table.columns(), table.numeric());
        for (List<Object> row : table.rows()) {
            body.row(row.stream().map(cell -> cell instanceof BigDecimal amount ? VoucherService.money(amount) : cell).toArray());
        }
        List<VoucherService.Section> sections = new ArrayList<>(List.of(body));
        if (!table.totals().isEmpty()) {
            VoucherService.Section totals = new VoucherService.Section("Totaux");
            table.totals().forEach((label, amount) -> totals.pair(label, amount instanceof BigDecimal value ? VoucherService.money(value) : amount));
            sections.add(totals);
        }
        return vouchers.render(table.title(), issued, sections, null, null, List.of());
    }

    // ================================================================== outils

    private Table byCategoryTable(String code, String title, List<String> valueColumns, Map<String, BigDecimal[]> byCategory) {
        Map<String, FaCategory> labels = categoryMap();
        List<String> columns = new ArrayList<>(List.of("Categorie", "Libelle"));
        columns.addAll(valueColumns);
        List<Boolean> numeric = new ArrayList<>(List.of(false, false));
        valueColumns.forEach(column -> numeric.add(true));
        BigDecimal[] sum = zeros(valueColumns.size());
        List<List<Object>> rows = new ArrayList<>();
        byCategory.forEach((category, values) -> {
            List<Object> row = new ArrayList<>(List.of(category, label(labels, category)));
            for (int i = 0; i < values.length; i++) {
                row.add(values[i]);
                sum[i] = sum[i].add(values[i]);
            }
            rows.add(row);
        });
        Map<String, Object> totals = new LinkedHashMap<>();
        for (int i = 0; i < valueColumns.size(); i++) {
            totals.put(valueColumns.get(i), sum[i]);
        }
        return new Table(code, title, columns, numeric, rows, totals);
    }

    private Map<String, BigDecimal> balances(Set<String> accounts) {
        if (accounts.isEmpty()) {
            return Map.of();
        }
        // Les ecritures de la transaction en cours doivent etre visibles de la requete JDBC.
        entityManager.flush();
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        for (String account : accounts) {
            BigDecimal value = jdbc.queryForObject("SELECT COALESCE(SUM(l.debit - l.credit), 0) FROM acc_entry_line l "
                    + "JOIN acc_entry e ON e.id = l.entry_id WHERE l.account_number LIKE ? AND e.status IN ('VALIDATED', 'REVERSED')",
                    BigDecimal.class, account + "%");
            result.put(account, value == null ? BigDecimal.ZERO : value);
        }
        return result;
    }

    private BigDecimal depreciationAt(FixedAsset asset, String period) {
        if (YearMonth.now(ZONE).toString().compareTo(period) <= 0) {
            return value(asset.getAccumulatedDepreciation());
        }
        return schedules.findAllByAssetIdOrderByKindAscVersionAscPeriodCodeAsc(asset.getId()).stream()
                .filter(row -> FixedAssetService.ACCOUNTING.equals(row.getKind()) && FixedAssetService.DONE.contains(row.getStatus())
                        && row.getPeriodCode().compareTo(period) <= 0)
                .map(row -> value(row.getPosted())).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal impairmentAt(FixedAsset asset, LocalDate at) {
        if (!at.isBefore(LocalDate.now(ZONE))) {
            return value(asset.getAccumulatedImpairment());
        }
        return impairments.findAllByAssetIdOrderByCreatedAtAsc(asset.getId()).stream()
                .filter(item -> "POSTED".equals(item.getStatus()) && item.getImpairmentDate() != null && !item.getImpairmentDate().isAfter(at))
                .map(item -> "REVERSAL".equals(item.getKind()) ? value(item.getAmount()).negate() : value(item.getAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Le bien figure au fichier a cette date : comptabilise, entre, pas encore sorti. */
    private boolean inFile(FixedAsset asset, LocalDate at) {
        if (isSplit(asset)) {
            return false;
        }
        LocalDate entry = entryDate(asset);
        LocalDate exit = exitDate(asset);
        return entry != null && !entry.isAfter(at) && (exit == null || exit.isAfter(at));
    }

    /** Un bien decoupe en composants : ses composants portent les valeurs, pas lui. */
    private boolean isSplit(FixedAsset asset) {
        return "SPLIT".equals(asset.getAccountingStatus());
    }

    private static LocalDate entryDate(FixedAsset asset) {
        return asset.getAcquisitionDate() != null ? asset.getAcquisitionDate()
                : asset.getInServiceDate() != null ? asset.getInServiceDate() : asset.getCreatedAt().atZone(ZONE).toLocalDate();
    }

    private static LocalDate exitDate(FixedAsset asset) {
        return asset.getDisposedOn();
    }

    private void addDue(List<List<Object>> rows, FixedAsset asset, String kind, LocalDate end, LocalDate at, int days) {
        if (end == null || end.isBefore(at) || end.isAfter(at.plusDays(days))) {
            return;
        }
        rows.add(List.of(asset.getCode(), nz(asset.getDesignation()), kind, dateText(end), ChronoUnit.DAYS.between(at, end),
                nz(asset.getMaintenanceContract())));
    }

    private Map<String, FaCategory> categoryMap() {
        return categories.findAllByOrderByCodeAsc().stream().collect(Collectors.toMap(FaCategory::getCode, Function.identity(),
                (a, b) -> a, LinkedHashMap::new));
    }

    private long number(String key, long fallback) {
        try {
            return Long.parseLong(settings.text(key, String.valueOf(fallback)).trim());
        } catch (RuntimeException ex) {
            return fallback;
        }
    }

    private static boolean matches(String actual, String wanted) {
        return !StringUtils.hasText(wanted) || Objects.equals(actual == null ? null : actual.toUpperCase(Locale.ROOT),
                wanted.trim().toUpperCase(Locale.ROOT));
    }

    private static String label(Map<String, FaCategory> categories, String code) {
        FaCategory category = categories.get(code);
        return category == null ? nz(code) : category.getLabel();
    }

    private static Map<String, Object> totals(Object... pairs) {
        Map<String, Object> totals = new LinkedHashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            totals.put((String) pairs[i], pairs[i + 1]);
        }
        return totals;
    }

    private static BigDecimal[] zeros(int size) {
        BigDecimal[] values = new BigDecimal[size];
        java.util.Arrays.fill(values, BigDecimal.ZERO);
        return values;
    }

    private static long years(LocalDate since, LocalDate at) {
        return since == null ? 0 : ChronoUnit.YEARS.between(since, at);
    }

    private static BigDecimal value(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private static String nz(String value) {
        return value == null ? "" : value;
    }

    private static String dateText(LocalDate date) {
        return date == null ? "" : date.toString();
    }

    private static String cell(Object value) {
        if (value == null) {
            return "";
        }
        String text = value instanceof BigDecimal amount ? amount.stripTrailingZeros().toPlainString().replace('.', ',') : String.valueOf(value);
        return text.contains(";") || text.contains("\"") || text.contains("\n") ? "\"" + text.replace("\"", "\"\"") + "\"" : text;
    }
}
