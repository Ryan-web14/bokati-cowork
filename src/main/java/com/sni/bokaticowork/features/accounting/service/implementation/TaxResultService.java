package com.sni.bokaticowork.features.accounting.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.accounting.dto.AccountingRequests;
import com.sni.bokaticowork.features.accounting.enums.EntryOrigin;
import com.sni.bokaticowork.features.accounting.enums.FiscalYearStatus;
import com.sni.bokaticowork.features.accounting.model.AccAccount;
import com.sni.bokaticowork.features.accounting.model.AccEntry;
import com.sni.bokaticowork.features.accounting.model.AccFiscalYear;
import com.sni.bokaticowork.features.accounting.model.AccTaxAdjustment;
import com.sni.bokaticowork.features.accounting.model.AccTaxResult;
import com.sni.bokaticowork.features.accounting.repository.AccAccountRepository;
import com.sni.bokaticowork.features.accounting.repository.AccTaxAdjustmentRepository;
import com.sni.bokaticowork.features.accounting.repository.AccTaxResultRepository;
import com.sni.bokaticowork.features.accounting.service.support.AccountingSettings;
import com.sni.bokaticowork.features.payment.service.support.CashActorResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Passage du resultat comptable au resultat fiscal (lot F15, plan des etats, section 5).
 *
 * <p>Reintegrations et deductions : lignes saisies par le comptable, plus des lignes proposees par
 * le logiciel (impot sur le resultat comptabilise en 89, charges des comptes marques non
 * deductibles, provisions non deductibles du lot F19, interets excedentaires des comptes courants
 * du lot F17). Les deficits des exercices precedents s'imputent sur le benefice ; le reste se
 * reporte. Impot : taux sur le resultat fiscal positif, et au moins le minimum de perception sur
 * le chiffre d'affaires. Les taux sont des parametres a valider par l'expert-comptable.</p>
 */
@Service
@Transactional
@RequiredArgsConstructor
public class TaxResultService {

    public static final String REINTEGRATION = "REINTEGRATION";
    public static final String DEDUCTION = "DEDUCTION";

    private final JdbcTemplate jdbc;
    private final FinancialStatementService statements;
    private final AccountingEntryService entries;
    private final AccAccountRepository accounts;
    private final AccTaxAdjustmentRepository adjustments;
    private final AccTaxResultRepository results;
    private final AccountingSettings settings;
    private final CashActorResolver actors;
    private final ObjectProvider<ProvisionReportService> provisionReports;
    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    public record Adjustment(Long id, String kind, String code, String label, String accountNumber, BigDecimal amount, String source, String comment) {
    }

    public record AdjustmentRequest(String kind, String code, String label, String accountNumber, BigDecimal amount, String comment) {
    }

    public record TaxResult(String fiscalYear, BigDecimal accountingResult, List<Adjustment> reintegrations, List<Adjustment> deductions,
                            BigDecimal totalReintegrations, BigDecimal totalDeductions, BigDecimal resultBeforeLosses, BigDecimal lossesAvailable,
                            BigDecimal lossesUsed, BigDecimal taxResult, BigDecimal lossCarriedForward, BigDecimal rate, BigDecimal tax,
                            BigDecimal turnover, BigDecimal minimumRate, BigDecimal minimumTax, BigDecimal taxDue, BigDecimal taxBooked,
                            String entryNumber, String status) {
    }

    // ================================================================== calcul

    /** Tableau de determination du resultat fiscal d'un exercice, recalcule a chaque lecture. */
    @Transactional(readOnly = true)
    public TaxResult compute(String yearCode) {
        statements.require(FinancialStatementService.READ, "ACCOUNTING_REPORT:READ");
        return computeFor(statements.year(yearCode));
    }

    TaxResult computeFor(AccFiscalYear year) {
        BigDecimal accountingResult = statements.incomeResult(year.getStartDate(), year.getEndDate());
        Map<String, BigDecimal> balances = statements.balances(year.getStartDate(), year.getEndDate());
        List<Adjustment> reintegrations = new ArrayList<>();
        List<Adjustment> deductions = new ArrayList<>();
        // Impot sur le resultat comptabilise : il n'est pas deductible de sa propre base.
        BigDecimal booked = balances.entrySet().stream().filter(entry -> entry.getKey().startsWith("89")).map(Map.Entry::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (booked.signum() != 0) {
            reintegrations.add(new Adjustment(null, REINTEGRATION, "IS", "Impot sur le resultat comptabilise (89)", "89", booked, "AUTO", null));
        }
        // Comptes marques non deductibles (en tout ou partie).
        for (AccAccount account : accounts.findAllByOrderByNumberAsc()) {
            if (account.getNonDeductibleRate() == null || account.getNonDeductibleRate().signum() <= 0 || account.getNumber().startsWith("89")) {
                continue;
            }
            BigDecimal amount = balances.entrySet().stream().filter(entry -> entry.getKey().startsWith(account.getNumber()))
                    .map(Map.Entry::getValue).reduce(BigDecimal.ZERO, BigDecimal::add)
                    .multiply(account.getNonDeductibleRate()).divide(new BigDecimal("100"), 0, RoundingMode.HALF_UP);
            if (amount.signum() > 0) {
                reintegrations.add(new Adjustment(null, REINTEGRATION, "ND-" + account.getNumber(), "Charges non deductibles : " + account.getLabel()
                        + " (" + account.getNonDeductibleRate().stripTrailingZeros().toPlainString() + " %)", account.getNumber(), amount, "AUTO", null));
            }
        }
        // Provisions non deductibles (lot F19).
        ProvisionReportService provisions = provisionReports.getIfAvailable();
        if (provisions != null) {
            FixedAssetReportService.Table table = provisions.reintegrations(year.getEndDate().getYear());
            BigDecimal add = amount(table.totals().get("A reintegrer"));
            BigDecimal remove = amount(table.totals().get("A deduire"));
            if (add.signum() > 0) {
                reintegrations.add(new Adjustment(null, REINTEGRATION, "PROV", "Provisions et depreciations non deductibles (dotations)", null, add, "AUTO", null));
            }
            if (remove.signum() > 0) {
                deductions.add(new Adjustment(null, DEDUCTION, "PROV", "Reprises de provisions non deductibles deja reintegrees", null, remove, "AUTO", null));
            }
        }
        // Interets des comptes courants d'associes au-dela du taux admis (lot F17).
        BigDecimal interest = jdbc.queryForObject("SELECT COALESCE(SUM(non_deductible), 0) FROM acc_partner_interest WHERE to_date BETWEEN ? AND ?",
                BigDecimal.class, year.getStartDate(), year.getEndDate());
        if (interest != null && interest.signum() > 0) {
            reintegrations.add(new Adjustment(null, REINTEGRATION, "CCA", "Interets de comptes courants au-dela du taux admis", null, interest, "AUTO", null));
        }
        for (AccTaxAdjustment item : adjustments.findAllByFiscalYearCodeOrderByCreatedAtAsc(year.getCode())) {
            Adjustment line = new Adjustment(item.getId(), item.getKind(), item.getCode(), item.getLabel(), item.getAccountNumber(), item.getAmount(),
                    "MANUAL", item.getComment());
            (REINTEGRATION.equals(item.getKind()) ? reintegrations : deductions).add(line);
        }
        BigDecimal totalReintegrations = reintegrations.stream().map(Adjustment::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalDeductions = deductions.stream().map(Adjustment::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal beforeLosses = accountingResult.add(totalReintegrations).subtract(totalDeductions);
        BigDecimal available = lossesAvailable(year);
        BigDecimal used = beforeLosses.signum() > 0 ? available.min(beforeLosses) : BigDecimal.ZERO;
        BigDecimal taxResult = beforeLosses.subtract(used);
        BigDecimal carried = available.subtract(used).add(beforeLosses.signum() < 0 ? beforeLosses.negate() : BigDecimal.ZERO);
        BigDecimal rate = decimal("accounting.tax.corporate-rate", "28");
        BigDecimal tax = taxResult.signum() > 0 ? taxResult.multiply(rate).divide(new BigDecimal("100"), 0, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        BigDecimal turnover = statements.incomeStatementFor(year.getStartDate(), year.getEndDate()).net("XB");
        BigDecimal minimumRate = decimal("accounting.tax.minimum-rate", "1");
        BigDecimal minimum = turnover.signum() > 0 ? turnover.multiply(minimumRate).divide(new BigDecimal("100"), 0, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        Optional<AccTaxResult> saved = results.findByFiscalYearCode(year.getCode());
        return new TaxResult(year.getCode(), accountingResult, reintegrations, deductions, totalReintegrations, totalDeductions, beforeLosses, available,
                used, taxResult, carried, rate, tax, turnover, minimumRate, minimum, tax.max(minimum), booked,
                saved.map(AccTaxResult::getEntryNumber).orElse(null), saved.map(AccTaxResult::getStatus).orElse(null));
    }

    /** Deficits reportables : le reste de l'exercice precedent arrete, sinon le parametre d'ouverture. */
    private BigDecimal lossesAvailable(AccFiscalYear year) {
        return statements.previousYear(year).flatMap(previous -> results.findByFiscalYearCode(previous.getCode()))
                .map(AccTaxResult::getLossCarriedForward)
                .orElseGet(() -> decimal("accounting.tax.opening-loss", "0"));
    }

    // ================================================================== saisie

    public Adjustment add(String yearCode, AdjustmentRequest request) {
        statements.require(FinancialStatementService.MANAGE);
        AccFiscalYear year = openYear(yearCode);
        if (request == null || !StringUtils.hasText(request.label()) || request.amount() == null || request.amount().signum() <= 0) {
            throw new BadRequestException("Indiquez le libelle et un montant positif");
        }
        String kind = request.kind() == null ? "" : request.kind().trim().toUpperCase(Locale.ROOT);
        if (!List.of(REINTEGRATION, DEDUCTION).contains(kind)) {
            throw new BadRequestException("Type REINTEGRATION ou DEDUCTION");
        }
        AccTaxAdjustment saved = adjustments.save(AccTaxAdjustment.builder().fiscalYearCode(year.getCode()).kind(kind)
                .code(StringUtils.hasText(request.code()) ? request.code().trim() : null).label(request.label().trim())
                .accountNumber(StringUtils.hasText(request.accountNumber()) ? request.accountNumber().trim() : null).amount(request.amount())
                .comment(StringUtils.hasText(request.comment()) ? request.comment().trim() : null).createdBy(actors.resolve("SYSTEM"))
                .createdAt(Instant.now()).build());
        return new Adjustment(saved.getId(), saved.getKind(), saved.getCode(), saved.getLabel(), saved.getAccountNumber(), saved.getAmount(), "MANUAL",
                saved.getComment());
    }

    public void delete(Long id) {
        statements.require(FinancialStatementService.MANAGE);
        AccTaxAdjustment item = adjustments.findById(id).orElseThrow(() -> new ResourceNotFoundException("Ligne introuvable : " + id));
        openYear(item.getFiscalYearCode());
        adjustments.delete(item);
    }

    /** Taux de non-deductibilite d'un compte (vide ou zero : deductible). */
    public AccAccount nonDeductible(String accountNumber, BigDecimal rate) {
        statements.require(FinancialStatementService.MANAGE);
        AccAccount account = accounts.findByNumber(accountNumber).orElseThrow(() -> new ResourceNotFoundException("Compte introuvable : " + accountNumber));
        if (rate != null && (rate.signum() < 0 || rate.compareTo(new BigDecimal("100")) > 0)) {
            throw new BadRequestException("Taux entre 0 et 100");
        }
        account.setNonDeductibleRate(rate == null || rate.signum() == 0 ? null : rate);
        return accounts.save(account);
    }

    // ================================================================== arrete et impot

    /** Arrete le resultat fiscal de l'exercice (base du report des deficits de l'exercice suivant). */
    public TaxResult save(String yearCode) {
        statements.require(FinancialStatementService.MANAGE);
        AccFiscalYear year = statements.year(yearCode);
        TaxResult computed = computeFor(year);
        AccTaxResult saved = results.findByFiscalYearCode(year.getCode()).orElseGet(() -> AccTaxResult.builder().fiscalYearCode(year.getCode()).build());
        saved.setAccountingResult(computed.accountingResult());
        saved.setReintegrations(computed.totalReintegrations());
        saved.setDeductions(computed.totalDeductions());
        saved.setLossesUsed(computed.lossesUsed());
        saved.setTaxResult(computed.taxResult());
        saved.setLossCarriedForward(computed.lossCarriedForward());
        saved.setRate(computed.rate());
        saved.setTax(computed.tax());
        saved.setMinimumTax(computed.minimumTax());
        saved.setTaxDue(computed.taxDue());
        saved.setStatus(saved.getEntryNumber() == null ? "DRAFT" : "POSTED");
        saved.setComputedBy(actors.resolve("SYSTEM"));
        saved.setComputedAt(Instant.now());
        results.save(saved);
        return computeFor(year);
    }

    /**
     * Comptabilise l'impot du (D 891, C 441) au dernier jour de l'exercice, pour la part non encore
     * comptabilisee. Le resultat fiscal n'en change pas : l'impot comptabilise est reintegre.
     */
    public TaxResult post(String yearCode) {
        statements.require(FinancialStatementService.MANAGE);
        AccFiscalYear year = openYear(yearCode);
        TaxResult computed = save(year.getCode());
        BigDecimal amount = computed.taxDue().subtract(computed.taxBooked());
        if (amount.signum() <= 0) {
            throw new BadRequestException("L'impot de " + plain(computed.taxDue()) + " est deja comptabilise");
        }
        String actor = actors.resolve("SYSTEM");
        AccEntry entry = entries.post(new AccountingEntryService.AutoPost(settings.text("accounting.tax.journal", "OD"), year.getEndDate(), null,
                "Impot sur le resultat de l'exercice " + year.getCode(), EntryOrigin.CLOSING, "TAX_RESULT", year.getCode(),
                List.of(new AccountingRequests.EntryLine(settings.text("accounting.tax.expense-account", "891"), null, "Impot sur le resultat " + year.getCode(),
                                amount, null, null, null, null, null, null, null, year.getCode()),
                        new AccountingRequests.EntryLine(settings.text("accounting.tax.payable-account", "441"), null, "Impot sur le resultat " + year.getCode(),
                                null, amount, null, null, null, null, null, null, year.getCode())),
                actor, true, null));
        if (entry.getStatus() != com.sni.bokaticowork.features.accounting.enums.EntryStatus.VALIDATED) {
            throw new BadRequestException("L'ecriture d'impot n'a pas pu etre validee : " + entry.getPendingDetail());
        }
        AccTaxResult saved = results.findByFiscalYearCode(year.getCode()).orElseThrow();
        saved.setEntryNumber(entry.getEntryNumber());
        saved.setStatus("POSTED");
        results.save(saved);
        // Les soldes se relisent en JDBC : l'ecriture doit etre en base avant.
        entityManager.flush();
        return computeFor(year);
    }

    /** Tableau du resultat fiscal, pour la liasse et l'export. */
    public FixedAssetReportService.Table table(TaxResult result) {
        List<List<Object>> rows = new ArrayList<>();
        rows.add(List.of("RC", "Resultat comptable de l'exercice", result.accountingResult()));
        for (Adjustment item : result.reintegrations()) {
            rows.add(List.of("R", "Reintegration : " + item.label(), item.amount()));
        }
        rows.add(List.of("TR", "Total des reintegrations", result.totalReintegrations()));
        for (Adjustment item : result.deductions()) {
            rows.add(List.of("D", "Deduction : " + item.label(), item.amount()));
        }
        rows.add(List.of("TD", "Total des deductions", result.totalDeductions()));
        rows.add(List.of("RAD", "Resultat avant imputation des deficits", result.resultBeforeLosses()));
        rows.add(List.of("DEF", "Deficits anterieurs imputes", result.lossesUsed()));
        rows.add(List.of("RF", "Resultat fiscal", result.taxResult()));
        rows.add(List.of("DR", "Deficit reportable en fin d'exercice", result.lossCarriedForward()));
        rows.add(List.of("IS", "Impot au taux de " + result.rate().stripTrailingZeros().toPlainString() + " %", result.tax()));
        rows.add(List.of("MIN", "Minimum de perception (" + result.minimumRate().stripTrailingZeros().toPlainString() + " % du chiffre d'affaires)",
                result.minimumTax()));
        rows.add(List.of("DU", "Impot du", result.taxDue()));
        return new FixedAssetReportService.Table("resultat-fiscal-" + result.fiscalYear(), "Passage au resultat fiscal " + result.fiscalYear(),
                List.of("Ref", "Libelle", "Montant"), List.of(false, false, true), rows, Map.of());
    }

    // ================================================================== outils

    private AccFiscalYear openYear(String code) {
        AccFiscalYear year = statements.year(code);
        if (year.getStatus() == FiscalYearStatus.CLOSED) {
            throw new BadRequestException("L'exercice " + year.getCode() + " est clos");
        }
        return year;
    }

    private BigDecimal decimal(String key, String fallback) {
        try {
            return new BigDecimal(settings.text(key, fallback).trim());
        } catch (RuntimeException exception) {
            return new BigDecimal(fallback);
        }
    }

    private static BigDecimal amount(Object value) {
        return value instanceof BigDecimal decimal ? decimal : BigDecimal.ZERO;
    }

    private static String plain(BigDecimal value) {
        return value == null ? "0" : value.stripTrailingZeros().toPlainString();
    }
}
