package com.sni.bokaticowork.features.accounting.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.accounting.model.AccReceivableImpairment;
import com.sni.bokaticowork.features.accounting.model.AccReceivableStatus;
import com.sni.bokaticowork.features.accounting.model.AccReceivableWriteoff;
import com.sni.bokaticowork.features.accounting.model.AccRisk;
import com.sni.bokaticowork.features.accounting.model.AccRiskProvisionMove;
import com.sni.bokaticowork.features.accounting.repository.AccReceivableImpairmentRepository;
import com.sni.bokaticowork.features.accounting.repository.AccReceivableStatusRepository;
import com.sni.bokaticowork.features.accounting.repository.AccReceivableWriteoffRepository;
import com.sni.bokaticowork.features.accounting.repository.AccRiskProvisionMoveRepository;
import com.sni.bokaticowork.features.accounting.repository.AccRiskRepository;
import com.sni.bokaticowork.features.payment.service.support.CashActorResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Etats du lot F19 (section 11). Meme forme que les etats des immobilisations : tableau rendu en
 * JSON, CSV ou PDF par {@link FixedAssetReportService}.
 */
@Service
@RequiredArgsConstructor
public class ProvisionReportService {

    private static final ZoneId ZONE = ZoneId.of("Africa/Brazzaville");
    public static final List<String> REPORTS = List.of("aging", "doubtful", "impairment-variation", "provision-variation", "risk-register",
            "contingent-liabilities", "losses", "tax-reintegrations", "indicators");

    private final ReceivableRiskService receivables;
    private final AccReceivableStatusRepository statuses;
    private final AccReceivableImpairmentRepository impairments;
    private final AccReceivableWriteoffRepository writeoffs;
    private final AccRiskRepository risks;
    private final AccRiskProvisionMoveRepository moves;
    private final CashActorResolver actors;

    @Transactional(readOnly = true)
    public FixedAssetReportService.Table report(String report, Integer year, LocalDate date, String segment) {
        if (!actors.hasAuthority(ReceivableRiskService.READ) && !actors.hasAuthority(RiskProvisionService.MANAGE)) {
            throw new com.sni.bokaticowork.core.exception.customs.ForbiddenException("Action reservee : " + ReceivableRiskService.READ);
        }
        int fiscalYear = year != null ? year : LocalDate.now(ZONE).getYear();
        return switch (report == null ? "" : report.trim().toLowerCase(Locale.ROOT)) {
            case "aging" -> aging(date, segment);
            case "doubtful" -> doubtful();
            case "impairment-variation" -> impairmentVariation(fiscalYear);
            case "provision-variation" -> provisionVariation(fiscalYear);
            case "risk-register" -> register();
            case "contingent-liabilities" -> contingent();
            case "losses" -> losses(fiscalYear);
            case "tax-reintegrations" -> reintegrations(fiscalYear);
            case "indicators" -> indicators(date);
            default -> throw new BadRequestException("Etat inconnu : " + report + ". Etats disponibles : " + String.join(", ", REPORTS));
        };
    }

    FixedAssetReportService.Table aging(LocalDate date, String segment) {
        List<List<Object>> rows = new ArrayList<>();
        BigDecimal[] buckets = new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO};
        for (ReceivableRiskService.ReceivableView view : receivables.aging(date, segment)) {
            int bucket = view.daysOverdue() <= 0 ? 0 : view.daysOverdue() <= 30 ? 1 : view.daysOverdue() <= 60 ? 2 : view.daysOverdue() <= 90 ? 3 : 4;
            List<Object> row = new ArrayList<>(List.of(view.customerCode() == null ? "" : view.customerCode(), view.documentNumber(), view.status()));
            for (int i = 0; i < 5; i++) {
                row.add(i == bucket ? view.balanceDue() : BigDecimal.ZERO);
            }
            row.add(view.impairment());
            row.add(view.net());
            rows.add(row);
            buckets[bucket] = buckets[bucket].add(view.balanceDue());
            buckets[5] = buckets[5].add(view.impairment());
            buckets[6] = buckets[6].add(view.net());
        }
        Map<String, Object> totals = new LinkedHashMap<>();
        String[] labels = {"Non echu", "1 a 30 jours", "31 a 60 jours", "61 a 90 jours", "Plus de 90 jours", "Depreciations", "Net"};
        for (int i = 0; i < labels.length; i++) {
            totals.put(labels[i], buckets[i]);
        }
        return new FixedAssetReportService.Table("aging", "Balance agee avec risque",
                List.of("Client", "Facture", "Etat", "Non echu", "1 a 30", "31 a 60", "61 a 90", "Plus de 90", "Depreciation", "Net"),
                List.of(false, false, false, true, true, true, true, true, true, true), rows, totals);
    }

    FixedAssetReportService.Table doubtful() {
        List<List<Object>> rows = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (AccReceivableStatus status : statuses.findAllByStatusIn(ReceivableRiskService.AT_RISK)) {
            AccReceivableImpairment last = impairments.findAllByDocumentNumberOrderByCreatedAtAsc(status.getDocumentNumber()).stream()
                    .filter(item -> "ADJUSTMENT".equals(item.getKind())).reduce((a, b) -> b).orElse(null);
            rows.add(List.of(status.getDocumentNumber(), nz(status.getCustomerCode()), status.getStatus(), status.getSince().toString(),
                    nz(status.getReason()), status.getAmountTtc(), last == null ? BigDecimal.ZERO : last.getBaseHt(),
                    last == null ? BigDecimal.ZERO : last.getGuarantees(), last == null ? "" : last.getMethod(),
                    last == null ? BigDecimal.ZERO : last.getRate(), status.getImpairment(), Boolean.TRUE.equals(status.getDeductible()) ? "Oui" : "Non"));
            total = total.add(status.getImpairment());
        }
        return new FixedAssetReportService.Table("doubtful", "Etat des creances douteuses et litigieuses",
                List.of("Facture", "Client", "Etat", "Classee le", "Motif", "Reste du TTC", "Base HT", "Garanties", "Methode", "Taux", "Depreciation", "Deductible"),
                List.of(false, false, false, false, false, true, true, true, false, true, true, false), rows, Map.of("Depreciations", total));
    }

    /** Variation des depreciations de creances : debut, dotations, reprises, fin, par compte. */
    FixedAssetReportService.Table impairmentVariation(int year) {
        Map<String, BigDecimal[]> byAccount = new TreeMap<>();
        for (AccReceivableStatus status : statuses.findAll()) {
            String account = receivables.allowanceAccount(ReceivableRiskService.DISPUTED.equals(status.getStatus()) ? status.getStatus()
                    : ReceivableRiskService.DOUBTFUL);
            for (AccReceivableImpairment line : impairments.findAllByDocumentNumberOrderByCreatedAtAsc(status.getDocumentNumber())) {
                BigDecimal[] values = byAccount.computeIfAbsent(account, key -> zeros(4));
                int lineYear = Integer.parseInt(line.getPeriodCode().substring(0, 4));
                if (lineYear < year) {
                    values[0] = values[0].add(line.getDifference());
                } else if (lineYear == year) {
                    if (line.getDifference().signum() > 0) {
                        values[1] = values[1].add(line.getDifference());
                    } else {
                        values[2] = values[2].add(line.getDifference().negate());
                    }
                }
            }
        }
        for (AccRisk risk : risks.findAllByOrderByCodeAsc()) {
            RiskProvisionService.NatureAccounts nature = RiskProvisionService.NATURES.get(risk.getNature());
            if (nature == null || !nature.asset()) {
                continue;
            }
            addMoves(byAccount, nature.provision(), risk, year);
        }
        byAccount.values().forEach(values -> values[3] = values[0].add(values[1]).subtract(values[2]));
        return table("impairment-variation", "Variation des depreciations " + year, List.of("Debut", "Dotations", "Reprises", "Fin"), byAccount);
    }

    /** Variation des provisions pour risques et charges, par nature. */
    FixedAssetReportService.Table provisionVariation(int year) {
        Map<String, BigDecimal[]> byNature = new TreeMap<>();
        for (AccRisk risk : risks.findAllByOrderByCodeAsc()) {
            RiskProvisionService.NatureAccounts nature = RiskProvisionService.NATURES.get(risk.getNature());
            if (nature == null || nature.asset()) {
                continue;
            }
            BigDecimal[] values = byNature.computeIfAbsent(risk.getNature(), key -> zeros(6));
            for (AccRiskProvisionMove move : moves.findAllByRiskIdOrderByMoveDateAscIdAsc(risk.getId())) {
                int moveYear = move.getMoveDate().getYear();
                BigDecimal signed = "ALLOWANCE".equals(move.getKind()) ? move.getAmount() : "RECLASS".equals(move.getKind()) ? BigDecimal.ZERO
                        : move.getAmount().negate();
                if (moveYear < year) {
                    values[0] = values[0].add(signed);
                } else if (moveYear == year) {
                    switch (move.getKind()) {
                        case "ALLOWANCE" -> values[1] = values[1].add(move.getAmount());
                        case "USED_RELEASE" -> values[2] = values[2].add(move.getAmount());
                        case "UNUSED_RELEASE" -> values[3] = values[3].add(move.getAmount());
                        default -> values[4] = values[4].add(move.getAmount());
                    }
                }
            }
            values[5] = values[0].add(values[1]).subtract(values[2]).subtract(values[3]);
        }
        return table("provision-variation", "Variation des provisions pour risques et charges " + year,
                List.of("Debut", "Dotations", "Reprises utilisees", "Reprises non utilisees", "Reclassements a court terme", "Fin"), byNature);
    }

    FixedAssetReportService.Table register() {
        List<List<Object>> rows = new ArrayList<>();
        for (AccRisk risk : risks.findAllByOrderByCodeAsc()) {
            rows.add(List.of(risk.getCode(), risk.getNature(), risk.getTitle(), nz(risk.getThirdParty()), risk.getProbability(),
                    risk.getEstimatedAmount(), risk.getProvisionAmount(), risk.getTerm(), risk.getOutflowDate() == null ? "" : risk.getOutflowDate().toString(),
                    risk.getNextDeadline() == null ? "" : risk.getNextDeadline().toString(), risk.getStatus()));
        }
        return new FixedAssetReportService.Table("risk-register", "Registre des risques",
                List.of("Code", "Nature", "Intitule", "Tiers", "Probabilite", "Estimation", "Provision", "Terme", "Sortie probable", "Prochaine echeance", "Etat"),
                List.of(false, false, false, false, false, true, true, false, false, false, false), rows, Map.of());
    }

    FixedAssetReportService.Table contingent() {
        List<List<Object>> rows = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (AccRisk risk : risks.findAllByStatus("OPEN")) {
            RiskProvisionService.NatureAccounts nature = RiskProvisionService.NATURES.get(risk.getNature());
            if (!"POSSIBLE".equals(risk.getProbability()) || nature == null || nature.asset()) {
                continue;
            }
            rows.add(List.of(risk.getCode(), risk.getTitle(), nz(risk.getThirdParty()), risk.getEstimatedAmount(), nz(risk.getEstimationMethod())));
            total = total.add(risk.getEstimatedAmount());
        }
        return new FixedAssetReportService.Table("contingent-liabilities", "Passifs eventuels", List.of("Code", "Intitule", "Tiers", "Estimation", "Methode"),
                List.of(false, false, false, true, false), rows, Map.of("Total", total));
    }

    FixedAssetReportService.Table losses(int year) {
        List<List<Object>> rows = new ArrayList<>();
        BigDecimal ht = BigDecimal.ZERO;
        BigDecimal vat = BigDecimal.ZERO;
        for (AccReceivableWriteoff writeoff : writeoffs.findAllByOrderByCreatedAtDesc()) {
            if (!"APPROVED".equals(writeoff.getStatus()) || writeoff.getApprovedAt() == null || writeoff.getApprovedAt().atZone(ZONE).getYear() != year) {
                continue;
            }
            rows.add(List.of(nz(writeoff.getVoucherNumber()), nz(writeoff.getCustomerCode()), writeoff.getDocumentNumbers(), writeoff.getReason(),
                    writeoff.getAmountHt(), writeoff.getAmountVat(), writeoff.getImpairmentReleased(), nz(writeoff.getApprovedBy())));
            ht = ht.add(writeoff.getAmountHt());
            vat = vat.add(writeoff.getAmountVat());
        }
        List<List<Object>> recoveries = statuses.findAllByStatusIn(List.of(ReceivableRiskService.RECOVERED_AFTER_LOSS)).stream()
                .map(status -> List.<Object>of("Recuperation", nz(status.getCustomerCode()), status.getDocumentNumber(), "", BigDecimal.ZERO,
                        BigDecimal.ZERO, BigDecimal.ZERO, "")).toList();
        rows.addAll(recoveries);
        return new FixedAssetReportService.Table("losses", "Pertes sur creances " + year,
                List.of("Decision", "Client", "Factures", "Motif", "HT", "TVA", "Depreciation reprise", "Valide par"),
                List.of(false, false, false, false, true, true, true, false), rows, Map.of("HT", ht, "TVA", vat));
    }

    /** Depreciations et provisions non deductibles dotees ou reprises dans l'exercice. */
    FixedAssetReportService.Table reintegrations(int year) {
        List<List<Object>> rows = new ArrayList<>();
        BigDecimal reintegrated = BigDecimal.ZERO;
        BigDecimal deducted = BigDecimal.ZERO;
        for (AccReceivableStatus status : statuses.findAll()) {
            if (Boolean.TRUE.equals(status.getDeductible())) {
                continue;
            }
            for (AccReceivableImpairment line : impairments.findAllByDocumentNumberOrderByCreatedAtAsc(status.getDocumentNumber())) {
                if (!line.getPeriodCode().startsWith(String.valueOf(year))) {
                    continue;
                }
                rows.add(List.of(status.getDocumentNumber(), "Depreciation de creance", line.getDifference().max(BigDecimal.ZERO),
                        line.getDifference().min(BigDecimal.ZERO).negate()));
                reintegrated = reintegrated.add(line.getDifference().max(BigDecimal.ZERO));
                deducted = deducted.add(line.getDifference().min(BigDecimal.ZERO).negate());
            }
        }
        for (AccRisk risk : risks.findAllByOrderByCodeAsc()) {
            if (Boolean.TRUE.equals(risk.getDeductible())) {
                continue;
            }
            for (AccRiskProvisionMove move : moves.findAllByRiskIdOrderByMoveDateAscIdAsc(risk.getId())) {
                if (move.getMoveDate().getYear() != year || "RECLASS".equals(move.getKind())) {
                    continue;
                }
                boolean allowance = "ALLOWANCE".equals(move.getKind());
                rows.add(List.of(risk.getCode(), risk.getTitle(), allowance ? move.getAmount() : BigDecimal.ZERO, allowance ? BigDecimal.ZERO : move.getAmount()));
                reintegrated = reintegrated.add(allowance ? move.getAmount() : BigDecimal.ZERO);
                deducted = deducted.add(allowance ? BigDecimal.ZERO : move.getAmount());
            }
        }
        return new FixedAssetReportService.Table("tax-reintegrations", "Reintegrations et deductions fiscales " + year,
                List.of("Reference", "Objet", "A reintegrer", "A deduire"), List.of(false, false, true, true), rows,
                Map.of("A reintegrer", reintegrated, "A deduire", deducted));
    }

    FixedAssetReportService.Table indicators(LocalDate date) {
        List<ReceivableRiskService.ReceivableView> aging = receivables.aging(date, null);
        BigDecimal outstanding = aging.stream().map(ReceivableRiskService.ReceivableView::balanceDue).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal atRisk = aging.stream().filter(view -> ReceivableRiskService.AT_RISK.contains(view.status()))
                .map(ReceivableRiskService.ReceivableView::balanceDue).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal impaired = aging.stream().map(ReceivableRiskService.ReceivableView::impairment).reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, BigDecimal> bySegment = new TreeMap<>();
        aging.stream().filter(view -> ReceivableRiskService.AT_RISK.contains(view.status()))
                .forEach(view -> bySegment.merge(view.customerType() == null ? "?" : view.customerType(), view.balanceDue(), BigDecimal::add));
        List<List<Object>> rows = new ArrayList<>();
        rows.add(List.of("Creances echues et a risque", outstanding));
        rows.add(List.of("Dont douteuses et litigieuses", atRisk));
        rows.add(List.of("Taux de creances douteuses (%)", percent(atRisk, outstanding)));
        rows.add(List.of("Depreciations", impaired));
        rows.add(List.of("Taux de couverture des creances a risque (%)", percent(impaired, atRisk)));
        bySegment.forEach((segment, amount) -> rows.add(List.of("Creances a risque, segment " + segment, amount)));
        return new FixedAssetReportService.Table("indicators", "Indicateurs du recouvrement", List.of("Indicateur", "Valeur"), List.of(false, true),
                rows, Map.of());
    }

    // ================================================================== outils

    private void addMoves(Map<String, BigDecimal[]> byAccount, String account, AccRisk risk, int year) {
        BigDecimal[] values = byAccount.computeIfAbsent(account, key -> zeros(4));
        for (AccRiskProvisionMove move : moves.findAllByRiskIdOrderByMoveDateAscIdAsc(risk.getId())) {
            BigDecimal signed = "ALLOWANCE".equals(move.getKind()) ? move.getAmount() : move.getAmount().negate();
            if (move.getMoveDate().getYear() < year) {
                values[0] = values[0].add(signed);
            } else if (move.getMoveDate().getYear() == year) {
                if (signed.signum() > 0) {
                    values[1] = values[1].add(signed);
                } else {
                    values[2] = values[2].add(signed.negate());
                }
            }
        }
    }

    private static FixedAssetReportService.Table table(String code, String title, List<String> valueColumns, Map<String, BigDecimal[]> byKey) {
        List<String> columns = new ArrayList<>(List.of("Compte ou nature"));
        columns.addAll(valueColumns);
        List<Boolean> numeric = new ArrayList<>(List.of(false));
        valueColumns.forEach(column -> numeric.add(true));
        BigDecimal[] sum = zeros(valueColumns.size());
        List<List<Object>> rows = new ArrayList<>();
        byKey.forEach((key, values) -> {
            List<Object> row = new ArrayList<>(List.of(key));
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
        return new FixedAssetReportService.Table(code, title, columns, numeric, rows, totals);
    }

    private static BigDecimal percent(BigDecimal part, BigDecimal whole) {
        return whole.signum() == 0 ? BigDecimal.ZERO : part.multiply(BigDecimal.valueOf(100)).divide(whole, 1, RoundingMode.HALF_UP);
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
