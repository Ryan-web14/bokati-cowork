package com.sni.bokaticowork.features.accounting.service.implementation;

import com.sni.bokaticowork.core.accounting.ReceivableRiskQuery;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ForbiddenException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.accounting.dto.AccountingRequests;
import com.sni.bokaticowork.features.accounting.enums.EntryOrigin;
import com.sni.bokaticowork.features.accounting.enums.EntryStatus;
import com.sni.bokaticowork.features.accounting.enums.PeriodStatus;
import com.sni.bokaticowork.features.accounting.model.AccEntry;
import com.sni.bokaticowork.features.accounting.model.AccPeriod;
import com.sni.bokaticowork.features.accounting.model.AccReceivableImpairment;
import com.sni.bokaticowork.features.accounting.model.AccReceivableProposal;
import com.sni.bokaticowork.features.accounting.model.AccReceivableStatus;
import com.sni.bokaticowork.features.accounting.model.AccReceivableWriteoff;
import com.sni.bokaticowork.features.accounting.repository.AccPeriodRepository;
import com.sni.bokaticowork.features.accounting.repository.AccReceivableImpairmentRepository;
import com.sni.bokaticowork.features.accounting.repository.AccReceivableProposalRepository;
import com.sni.bokaticowork.features.accounting.repository.AccReceivableStatusRepository;
import com.sni.bokaticowork.features.accounting.repository.AccReceivableWriteoffRepository;
import com.sni.bokaticowork.features.accounting.service.support.AccountingSettings;
import com.sni.bokaticowork.features.billing.dunning.model.DunningNotice;
import com.sni.bokaticowork.features.billing.dunning.model.DunningStep;
import com.sni.bokaticowork.features.billing.dunning.repository.DunningNoticeRepository;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.fund.voucher.VoucherService;
import com.sni.bokaticowork.features.payment.service.support.CashActorResolver;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Creances a risque (lot F19) : etat comptable de chaque facture, file de revue des classements,
 * reclassements 4161 et 4162 lettres, depreciation individuelle ou statistique sur base HT nette des
 * garanties, ajustement par periode, reprise au prorata des encaissements, passage en perte avec
 * grille de validation et statut {@code WRITTEN_OFF}, recuperation apres perte, politique d'acces.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReceivableRiskService implements ReceivableRiskQuery {

    public static final String READ = "RECEIVABLE_RISK:READ";
    public static final String CLASSIFY = "RECEIVABLE_RISK:CLASSIFY";
    public static final String IMPAIR = "RECEIVABLE_RISK:IMPAIR";
    public static final String WRITE_OFF_PREPARE = "RECEIVABLE_RISK:WRITE_OFF_PREPARE";
    public static final String WRITE_OFF_APPROVE = "RECEIVABLE_RISK:WRITE_OFF_APPROVE";
    public static final String WRITE_OFF_DIRECTION = "RECEIVABLE_RISK:WRITE_OFF_DIRECTION";
    public static final String OVERRIDE = "CUSTOMER_CREDIT:OVERRIDE";

    public static final String NORMAL = "NORMAL";
    public static final String DISPUTED = "DISPUTED";
    public static final String DOUBTFUL = "DOUBTFUL";
    public static final String RECOVERED = "RECOVERED";
    public static final String WRITTEN_OFF = "WRITTEN_OFF";
    public static final String RECOVERED_AFTER_LOSS = "RECOVERED_AFTER_LOSS";
    public static final Set<String> AT_RISK = Set.of(DISPUTED, DOUBTFUL);
    public static final List<String> WRITE_OFF_REASONS = List.of("INSOLVENCY_CLOSED", "DEBTOR_GONE", "RECOVERY_COST", "SETTLEMENT",
            "COURT_DECISION", "SMALL_OLD", "OTHER");

    private static final ZoneId ZONE = ZoneId.of("Africa/Brazzaville");

    private final AccReceivableStatusRepository statuses;
    private final AccReceivableProposalRepository proposals;
    private final AccReceivableImpairmentRepository impairments;
    private final AccReceivableWriteoffRepository writeoffs;
    private final AccPeriodRepository periodRepository;
    private final AccountingPeriodService periods;
    private final AccountingEntryService entries;
    private final AccountingSettings settings;
    private final BillingDocumentRepository documents;
    private final DunningNoticeRepository notices;
    private final VoucherService vouchers;
    private final CashActorResolver actors;
    private final DataSource dataSource;
    private final EntityManager entityManager;

    // ================================================================== consultation

    public record ReceivableView(String documentNumber, String customerCode, String customerType, String status, String account,
                                 LocalDate dueDate, long daysOverdue, BigDecimal balanceDue, BigDecimal baseHt, BigDecimal impairment,
                                 BigDecimal net, String reason, String voucherNumber) {
    }

    /** Balance agee avec risque, facture par facture : tranches, etat comptable, depreciation, net. */
    @Transactional(readOnly = true)
    public List<ReceivableView> aging(LocalDate date, String segment) {
        require(READ);
        LocalDate at = date != null ? date : today();
        List<ReceivableView> rows = new ArrayList<>();
        for (BillingDocument document : documents.findOverdueWithBalance(5000)) {
            if (StringUtils.hasText(segment) && !segment.equalsIgnoreCase(document.getCustomerType())) {
                continue;
            }
            rows.add(view(document, statuses.findByDocumentNumber(document.getDocumentNumber()).orElse(null), at));
        }
        for (AccReceivableStatus status : statuses.findAllByStatusIn(AT_RISK)) {
            if (rows.stream().noneMatch(row -> row.documentNumber().equals(status.getDocumentNumber()))) {
                documents.findByDocumentNumber(status.getDocumentNumber())
                        .filter(document -> !StringUtils.hasText(segment) || segment.equalsIgnoreCase(document.getCustomerType()))
                        .ifPresent(document -> rows.add(view(document, status, at)));
            }
        }
        rows.sort(Comparator.comparing(ReceivableView::daysOverdue).reversed());
        return rows;
    }

    @Transactional(readOnly = true)
    public ReceivableView receivable(String documentNumber) {
        require(READ);
        BillingDocument document = document(documentNumber);
        return view(document, statuses.findByDocumentNumber(document.getDocumentNumber()).orElse(null), today());
    }

    // ================================================================== file de revue

    @Transactional(readOnly = true)
    public List<AccReceivableProposal> queue() {
        require(READ);
        return proposals.findAllByStatusOrderByCreatedAtAsc("PROPOSED");
    }

    /**
     * Traitement de nuit : propose le classement des factures a risque (retard au-dela du seuil,
     * relance {@code HANDOVER}), classe d'office au-dela du delai automatique.
     */
    @Transactional
    public int propose(LocalDate today) {
        int created = 0;
        long triggerDays = number("accounting.receivable.trigger-days", 90);
        long autoDays = number("accounting.receivable.auto-days", 365);
        for (BillingDocument document : documents.findOverdueWithBalance(5000)) {
            if (!isInvoice(document) || document.getDueDate() == null) {
                continue;
            }
            AccReceivableStatus status = statuses.findByDocumentNumber(document.getDocumentNumber()).orElse(null);
            if (status != null && !NORMAL.equals(status.getStatus())) {
                continue;
            }
            long days = ChronoUnit.DAYS.between(document.getDueDate(), today);
            String trigger = days > triggerDays ? "OVER_" + triggerDays + "_DAYS" : handover(document) ? "HANDOVER" : extraTrigger(document, today);
            if (trigger == null) {
                continue;
            }
            List<AccReceivableProposal> open = proposals.findAllByDocumentNumberAndStatusIn(document.getDocumentNumber(), List.of("PROPOSED", "POSTPONED"));
            if (open.stream().anyMatch(item -> "PROPOSED".equals(item.getStatus())
                    || item.getPostponedUntil() != null && item.getPostponedUntil().isAfter(today))) {
                continue;
            }
            if (days > autoDays) {
                classify(document, DOUBTFUL, "Classement automatique : retard de " + days + " jours", "AUTO_OVER_" + autoDays + "_DAYS", "SYSTEM", today);
                continue;
            }
            BigDecimal due = money(document.getBalanceDue());
            proposals.save(AccReceivableProposal.builder().documentNumber(document.getDocumentNumber()).customerCode(customerCode(document))
                    .triggerCode(trigger).daysOverdue((int) days).balanceDue(due).guarantees(guarantee(customerCode(document)))
                    .suggested(suggested(document, days)).build());
            created++;
        }
        return created;
    }

    public record Decision(String decision, LocalDate postponedUntil, String reason, String targetStatus) {
    }

    @Transactional
    public AccReceivableProposal decide(Long id, Decision decision) {
        require(CLASSIFY);
        AccReceivableProposal proposal = proposals.findById(id).orElseThrow(() -> new ResourceNotFoundException("Proposition introuvable : " + id));
        if (!"PROPOSED".equals(proposal.getStatus())) {
            throw new BadRequestException("La proposition est deja traitee (" + proposal.getStatus() + ")");
        }
        String verdict = decision == null || decision.decision() == null ? "" : decision.decision().trim().toUpperCase(Locale.ROOT);
        switch (verdict) {
            case "ACCEPT" -> {
                String target = decision.targetStatus() == null ? proposal.getTargetStatus() : decision.targetStatus().trim().toUpperCase(Locale.ROOT);
                classify(document(proposal.getDocumentNumber()), target, nz(decision.reason(), "Proposition " + proposal.getTriggerCode() + " acceptee"),
                        proposal.getTriggerCode(), actor(), today());
                proposal.setStatus("ACCEPTED");
            }
            case "POSTPONE" -> {
                if (decision.postponedUntil() == null || !decision.postponedUntil().isAfter(today())) {
                    throw new BadRequestException("Un report donne une date de revue future");
                }
                proposal.setStatus("POSTPONED");
                proposal.setPostponedUntil(decision.postponedUntil());
            }
            case "REJECT" -> {
                if (!StringUtils.hasText(decision.reason())) {
                    throw new BadRequestException("Un rejet donne un motif");
                }
                proposal.setStatus("REJECTED");
            }
            default -> throw new BadRequestException("Decision attendue : ACCEPT, POSTPONE ou REJECT");
        }
        proposal.setReason(decision.reason());
        proposal.setDecidedBy(actor());
        proposal.setDecidedAt(Instant.now());
        return proposals.save(proposal);
    }

    // ================================================================== classement

    public record ClassifyRequest(String status, String reason, LocalDate date, BigDecimal individualRate, String rateReason, Boolean deductible) {
    }

    /** Classement a la main en douteux ({@code DOUBTFUL}) ou litigieux ({@code DISPUTED}). */
    @Transactional
    public ReceivableView classify(String documentNumber, ClassifyRequest request) {
        require(CLASSIFY);
        if (request == null || !StringUtils.hasText(request.reason())) {
            throw new BadRequestException("Un classement donne un motif");
        }
        BillingDocument document = document(documentNumber);
        AccReceivableStatus status = classify(document, request.status() == null ? DOUBTFUL : request.status().trim().toUpperCase(Locale.ROOT),
                request.reason().trim(), "MANUAL", actor(), request.date() != null ? request.date() : today());
        if (request.individualRate() != null) {
            rate(status, request.individualRate(), request.rateReason());
        }
        if (request.deductible() != null) {
            status.setDeductible(request.deductible());
        }
        statuses.save(status);
        return view(document, status, today());
    }

    AccReceivableStatus classify(BillingDocument document, String target, String reason, String trigger, String actor, LocalDate date) {
        if (!AT_RISK.contains(target)) {
            throw new BadRequestException("Classement attendu : DOUBTFUL (douteux) ou DISPUTED (litigieux)");
        }
        if (!isInvoice(document)) {
            throw new BadRequestException("Seule une facture se classe en douteux ou litigieux");
        }
        BigDecimal due = money(document.getBalanceDue());
        if (due.signum() <= 0) {
            throw new BadRequestException("La facture " + document.getDocumentNumber() + " n'a plus de reste du");
        }
        AccReceivableStatus status = statuses.findByDocumentNumber(document.getDocumentNumber()).orElseGet(() -> AccReceivableStatus.builder()
                .documentNumber(document.getDocumentNumber()).customerCode(customerCode(document)).customerType(document.getCustomerType())
                .since(date).createdBy(actor).build());
        if (target.equals(status.getStatus())) {
            return status;
        }
        if (WRITTEN_OFF.equals(status.getStatus()) || RECOVERED_AFTER_LOSS.equals(status.getStatus())) {
            throw new BadRequestException("La facture " + document.getDocumentNumber() + " est passee en perte");
        }
        String from = AT_RISK.contains(status.getStatus()) ? status.getAccountNumber() : clientAccount();
        String to = DISPUTED.equals(target) ? disputedAccount() : doubtfulAccount();
        String label = (DISPUTED.equals(target) ? "Creance litigieuse " : "Creance douteuse ") + document.getDocumentNumber();
        AccEntry entry = book(journal(), date, label, "RECEIVABLE_" + target, document.getDocumentNumber(), List.of(
                line(to, customerCode(document), label, due, null, document.getDocumentNumber()),
                line(from, customerCode(document), label, null, due, document.getDocumentNumber())), actor);
        if (AT_RISK.contains(status.getStatus()) && status.getImpairment().signum() > 0) {
            // Changement entre litigieux et douteux : la depreciation suit son compte.
            String oldAllowance = allowanceAccount(status.getStatus());
            String newAllowance = allowanceAccount(target);
            if (!oldAllowance.equals(newAllowance)) {
                book(journal(), date, "Reclassement de la depreciation " + document.getDocumentNumber(), "RECEIVABLE_IMPAIRMENT_RECLASS",
                        document.getDocumentNumber(), List.of(line(oldAllowance, null, label, status.getImpairment(), null, document.getDocumentNumber()),
                                line(newAllowance, null, label, null, status.getImpairment(), document.getDocumentNumber())), actor);
            }
        }
        boolean first = !AT_RISK.contains(status.getStatus());
        status.setStatus(target);
        status.setAccountNumber(to);
        status.setSince(date);
        status.setReason(reason);
        status.setTriggerCode(trigger);
        status.setReclassEntry(entry.getEntryNumber());
        status.setAmountTtc(due);
        status.setUpdatedAt(Instant.now());
        status.setVoucherNumber(vouchers.issueAndPrint("PCD", "RECEIVABLE_CLASSIFICATION", document.getDocumentNumber(), due,
                label, actor).number());
        statuses.save(status);
        proposals.findAllByDocumentNumberAndStatusIn(document.getDocumentNumber(), List.of("PROPOSED")).forEach(item -> {
            item.setStatus("ACCEPTED");
            item.setDecidedBy(actor);
            item.setDecidedAt(Instant.now());
            proposals.save(item);
        });
        if (first && DOUBTFUL.equals(target) && bool("accounting.receivable.contagion", true)) {
            contagion(document, date);
        }
        return status;
    }

    /** Contagion : les autres factures echues du client sont proposees au classement. */
    private void contagion(BillingDocument origin, LocalDate today) {
        String customer = customerCode(origin);
        for (BillingDocument other : documents.findOverdueWithBalance(5000)) {
            if (other.getDocumentNumber().equals(origin.getDocumentNumber()) || !customer.equals(customerCode(other)) || !isInvoice(other)) {
                continue;
            }
            boolean classified = statuses.findByDocumentNumber(other.getDocumentNumber()).map(item -> !NORMAL.equals(item.getStatus())).orElse(false);
            boolean queued = !proposals.findAllByDocumentNumberAndStatusIn(other.getDocumentNumber(), List.of("PROPOSED")).isEmpty();
            if (classified || queued) {
                continue;
            }
            long days = other.getDueDate() == null ? 0 : ChronoUnit.DAYS.between(other.getDueDate(), today);
            proposals.save(AccReceivableProposal.builder().documentNumber(other.getDocumentNumber()).customerCode(customer)
                    .triggerCode("CONTAGION").daysOverdue((int) Math.max(0, days)).balanceDue(money(other.getBalanceDue()))
                    .suggested(suggested(other, days)).build());
        }
    }

    public record BackToNormalRequest(String reason, LocalDate date) {
    }

    /** Retour en normal : reclassement inverse vers 411 et reprise de la depreciation. */
    @Transactional
    public ReceivableView backToNormal(String documentNumber, BackToNormalRequest request) {
        require(CLASSIFY);
        if (request == null || !StringUtils.hasText(request.reason())) {
            throw new BadRequestException("Un retour en normal donne un motif");
        }
        BillingDocument document = document(documentNumber);
        AccReceivableStatus status = statuses.findByDocumentNumber(document.getDocumentNumber())
                .filter(item -> AT_RISK.contains(item.getStatus()))
                .orElseThrow(() -> new BadRequestException("La facture " + documentNumber + " n'est ni douteuse ni litigieuse"));
        LocalDate date = request.date() != null ? request.date() : today();
        String label = "Retour en normal " + document.getDocumentNumber();
        BigDecimal due = status.getAmountTtc();
        if (due.signum() > 0) {
            book(journal(), date, label, "RECEIVABLE_BACK_TO_NORMAL", document.getDocumentNumber(), List.of(
                    line(clientAccount(), customerCode(document), label, due, null, document.getDocumentNumber()),
                    line(status.getAccountNumber(), customerCode(document), label, null, due, document.getDocumentNumber())), actor());
        }
        release(status, status.getImpairment(), date, "BACK_TO_NORMAL", label);
        status.setStatus(NORMAL);
        status.setAccountNumber(clientAccount());
        status.setReason(request.reason().trim());
        status.setSince(date);
        status.setUpdatedAt(Instant.now());
        statuses.save(status);
        return view(document, status, today());
    }

    /** Taux individuel force, avec motif : il prime sur la grille a l'ajustement suivant. */
    @Transactional
    public ReceivableView individualRate(String documentNumber, BigDecimal rate, String reason) {
        require(IMPAIR);
        AccReceivableStatus status = statuses.findByDocumentNumber(document(documentNumber).getDocumentNumber())
                .filter(item -> AT_RISK.contains(item.getStatus()))
                .orElseThrow(() -> new BadRequestException("La facture " + documentNumber + " n'est ni douteuse ni litigieuse"));
        rate(status, rate, reason);
        statuses.save(status);
        return receivable(documentNumber);
    }

    private void rate(AccReceivableStatus status, BigDecimal rate, String reason) {
        if (rate == null) {
            status.setIndividualRate(null);
            status.setRateReason(null);
            return;
        }
        if (rate.signum() < 0 || rate.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BadRequestException("Le taux de depreciation est un pourcentage de 0 a 100");
        }
        if (!StringUtils.hasText(reason)) {
            throw new BadRequestException("Un taux individuel est justifie par un motif");
        }
        status.setIndividualRate(rate);
        status.setRateReason(reason.trim());
    }

    // ================================================================== ajustement des depreciations

    public record ImpairmentLine(String documentNumber, String customerCode, String status, long daysOverdue, BigDecimal remainingTtc,
                                 BigDecimal baseHt, BigDecimal guarantees, String method, BigDecimal rate, BigDecimal amount, BigDecimal previous,
                                 BigDecimal difference, boolean deductible) {
    }

    public record ImpairmentRun(String period, boolean simulated, BigDecimal allowances, BigDecimal releases, String entryNumber,
                                List<ImpairmentLine> lines, List<String> warnings) {
    }

    @Transactional(readOnly = true)
    public ImpairmentRun previewImpairment(String period) {
        require(READ);
        return impairment(month(period), true, actor());
    }

    @Transactional
    public ImpairmentRun runImpairment(String period) {
        require(IMPAIR);
        return impairment(month(period), false, actor());
    }

    @Transactional
    public ImpairmentRun runImpairmentAutomatically(YearMonth month) {
        return impairment(month, false, "SYSTEM");
    }

    private ImpairmentRun impairment(YearMonth month, boolean simulate, String actor) {
        LocalDate end = month.atEndOfMonth();
        List<String> warnings = new ArrayList<>();
        List<ImpairmentLine> lines = new ArrayList<>();
        Map<String, BigDecimal> guaranteesLeft = new LinkedHashMap<>();
        List<AccReceivableStatus> atRisk = new ArrayList<>(statuses.findAllByStatusIn(AT_RISK));
        // Les garanties s'imputent d'abord sur la creance la plus ancienne du client.
        atRisk.sort(Comparator.comparing((AccReceivableStatus item) -> due(item.getDocumentNumber())));
        BigDecimal threshold = decimal("accounting.receivable.individual-threshold", new BigDecimal("500000"));
        for (AccReceivableStatus status : atRisk) {
            BillingDocument document = documents.findByDocumentNumber(status.getDocumentNumber()).orElse(null);
            if (document == null) {
                continue;
            }
            BigDecimal remaining = status.getAmountTtc();
            BigDecimal ht = ht(document, remaining);
            String customer = status.getCustomerCode();
            BigDecimal available = guaranteesLeft.computeIfAbsent(customer, this::guarantee);
            BigDecimal guaranteeHt = available.min(remaining).multiply(ratio(document)).setScale(0, RoundingMode.HALF_UP).min(ht);
            guaranteesLeft.put(customer, available.subtract(available.min(remaining)));
            BigDecimal base = ht.subtract(guaranteeHt).max(BigDecimal.ZERO);
            long days = document.getDueDate() == null ? 0 : Math.max(0, ChronoUnit.DAYS.between(document.getDueDate(), end));
            String method;
            BigDecimal rate;
            if (status.getIndividualRate() != null) {
                method = "INDIVIDUAL";
                rate = status.getIndividualRate();
            } else {
                method = "STATISTICAL";
                rate = gridRate(days);
                if (base.compareTo(threshold) >= 0) {
                    warnings.add(status.getDocumentNumber() + " : base de " + VoucherService.money(base)
                            + " au-dessus du seuil de la methode individuelle, taux de la grille applique faute de taux individuel");
                }
            }
            BigDecimal amount = base.multiply(rate).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
            BigDecimal difference = amount.subtract(status.getImpairment());
            lines.add(new ImpairmentLine(status.getDocumentNumber(), customer, status.getStatus(), days, remaining, ht, guaranteeHt, method, rate,
                    amount, status.getImpairment(), difference, Boolean.TRUE.equals(status.getDeductible())));
        }
        BigDecimal allowances = lines.stream().map(ImpairmentLine::difference).filter(value -> value.signum() > 0).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal releases = lines.stream().map(ImpairmentLine::difference).filter(value -> value.signum() < 0).map(BigDecimal::negate)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (simulate || allowances.add(releases).signum() == 0) {
            return new ImpairmentRun(month.toString(), simulate, allowances, releases, null, lines, warnings);
        }
        AccPeriod period = periodRepository.findByCode(month.toString())
                .orElseThrow(() -> new BadRequestException("Periode " + month + " inconnue : creez l'exercice"));
        LocalDate date = period.getStatus() == PeriodStatus.OPEN ? end : periods.firstOpenOnOrAfter(end).getStartDate();
        String label = "Ajustement des depreciations de creances " + month;
        List<AccountingRequests.EntryLine> entryLines = new ArrayList<>();
        for (ImpairmentLine line : lines) {
            if (line.difference().signum() == 0) {
                continue;
            }
            String allowance = allowanceAccount(line.status());
            if (line.difference().signum() > 0) {
                entryLines.add(line(expenseAccount(), null, label + " " + line.documentNumber(), line.difference(), null, line.documentNumber()));
                entryLines.add(line(allowance, null, label + " " + line.documentNumber(), null, line.difference(), line.documentNumber()));
            } else {
                entryLines.add(line(allowance, null, label + " " + line.documentNumber(), line.difference().negate(), null, line.documentNumber()));
                entryLines.add(line(reversalAccount(), null, label + " " + line.documentNumber(), null, line.difference().negate(), line.documentNumber()));
            }
        }
        AccEntry entry = book(journal(), date, label, "RECEIVABLE_IMPAIRMENT_ADJUSTED", month.toString(), entryLines, actor);
        for (ImpairmentLine line : lines) {
            if (line.difference().signum() == 0) {
                continue;
            }
            AccReceivableStatus status = statuses.findByDocumentNumber(line.documentNumber()).orElseThrow();
            impairments.save(AccReceivableImpairment.builder().documentNumber(line.documentNumber()).periodCode(month.toString())
                    .baseHt(line.baseHt()).guarantees(line.guarantees()).method(line.method()).rate(line.rate()).amount(line.amount())
                    .previousAmount(line.previous()).difference(line.difference()).deductible(line.deductible())
                    .overrideReason("INDIVIDUAL".equals(line.method()) ? status.getRateReason() : null).kind("ADJUSTMENT")
                    .entryNumber(entry.getEntryNumber()).createdBy(actor).build());
            status.setImpairment(line.amount());
            status.setUpdatedAt(Instant.now());
            statuses.save(status);
        }
        return new ImpairmentRun(month.toString(), false, allowances, releases, entry.getEntryNumber(), lines, warnings);
    }

    // ================================================================== encaissements

    /**
     * Compte a crediter pour un paiement impute a cette facture : 4161 ou 4162 si elle est a risque,
     * le compte de recuperation si elle est passee en perte, sinon {@code null} (le compte client).
     */
    @Transactional(readOnly = true)
    public String paymentAccount(String documentNumber) {
        if (documentNumber == null) {
            return null;
        }
        return statuses.findByDocumentNumber(documentNumber).map(status -> switch (status.getStatus()) {
            case DISPUTED, DOUBTFUL -> status.getAccountNumber();
            case WRITTEN_OFF, RECOVERED_AFTER_LOSS -> recoveryAccount();
            default -> null;
        }).orElse(null);
    }

    /** Apres l'ecriture d'un encaissement : reste du suivi, reprise de la depreciation au prorata. */
    @Transactional
    public void onPayment(String documentNumber, BigDecimal amount, LocalDate date) {
        AccReceivableStatus status = documentNumber == null ? null : statuses.findByDocumentNumber(documentNumber).orElse(null);
        if (status == null || amount == null || amount.signum() <= 0) {
            return;
        }
        if (WRITTEN_OFF.equals(status.getStatus()) || RECOVERED_AFTER_LOSS.equals(status.getStatus())) {
            status.setStatus(RECOVERED_AFTER_LOSS);
            status.setUpdatedAt(Instant.now());
            statuses.save(status);
            return;
        }
        if (!AT_RISK.contains(status.getStatus())) {
            return;
        }
        BigDecimal before = status.getAmountTtc();
        BigDecimal paid = amount.min(before);
        BigDecimal release = before.signum() == 0 ? status.getImpairment()
                : paid.compareTo(before) >= 0 ? status.getImpairment()
                : status.getImpairment().multiply(paid).divide(before, 0, RoundingMode.HALF_UP);
        status.setAmountTtc(before.subtract(paid));
        boolean immediate = !"NEXT_ADJUSTMENT".equalsIgnoreCase(settings.text("accounting.receivable.release", "IMMEDIATE"));
        if (immediate || status.getAmountTtc().signum() == 0) {
            release(status, release, date, "PAYMENT_RELEASE", "Reprise sur encaissement " + documentNumber);
        }
        if (status.getAmountTtc().signum() == 0) {
            status.setStatus(RECOVERED);
            status.setReason("Encaissement total");
        }
        status.setUpdatedAt(Instant.now());
        statuses.save(status);
    }

    private void release(AccReceivableStatus status, BigDecimal amount, LocalDate date, String kind, String label) {
        BigDecimal value = amount == null ? BigDecimal.ZERO : amount.min(status.getImpairment());
        if (value.signum() <= 0) {
            return;
        }
        AccEntry entry = book(journal(), date, label, "RECEIVABLE_IMPAIRMENT_RELEASED", status.getDocumentNumber(), List.of(
                line(allowanceAccount(status.getStatus()), null, label, value, null, status.getDocumentNumber()),
                line(reversalAccount(), null, label, null, value, status.getDocumentNumber())), actor());
        impairments.save(AccReceivableImpairment.builder().documentNumber(status.getDocumentNumber()).periodCode(YearMonth.from(date).toString())
                .method("RELEASE").amount(status.getImpairment().subtract(value)).previousAmount(status.getImpairment()).difference(value.negate())
                .deductible(Boolean.TRUE.equals(status.getDeductible())).kind(kind).entryNumber(entry.getEntryNumber()).createdBy(actor()).build());
        status.setImpairment(status.getImpairment().subtract(value));
    }

    // ================================================================== passage en perte

    public record WriteOffRequest(List<String> documentNumbers, String reason, String comment, String justificationRefs, Boolean vatRecovered) {
    }

    @Transactional(readOnly = true)
    public List<AccReceivableWriteoff> writeOffs() {
        require(READ);
        return writeoffs.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public AccReceivableWriteoff prepareWriteOff(WriteOffRequest request) {
        require(WRITE_OFF_PREPARE);
        if (request == null || request.documentNumbers() == null || request.documentNumbers().isEmpty()) {
            throw new BadRequestException("Choisissez au moins une facture");
        }
        String reason = request.reason() == null ? "" : request.reason().trim().toUpperCase(Locale.ROOT);
        if (!WRITE_OFF_REASONS.contains(reason)) {
            throw new BadRequestException("Motif attendu : " + String.join(", ", WRITE_OFF_REASONS));
        }
        if (!StringUtils.hasText(request.justificationRefs()) && !"SMALL_OLD".equals(reason)) {
            throw new BadRequestException("Une decision de passage en perte s'appuie sur des justificatifs");
        }
        Totals totals = totals(new LinkedHashSet<>(request.documentNumbers()));
        AccReceivableWriteoff writeoff = AccReceivableWriteoff.builder().customerCode(totals.customer())
                .documentNumbers(String.join(",", totals.documents())).amountHt(totals.ht()).amountVat(totals.vat()).amountTtc(totals.ttc())
                .vatRecovered(request.vatRecovered() == null ? bool("accounting.receivable.writeoff-vat-recovery", true) : request.vatRecovered())
                .reason(reason).comment(request.comment()).justificationRefs(request.justificationRefs())
                .approvalLevel(totals.ttc().compareTo(decimal("accounting.receivable.writeoff-direction-threshold", new BigDecimal("100000"))) > 0
                        ? "DIRECTION" : "FINANCE")
                .preparedBy(actor()).build();
        return writeoffs.save(writeoff);
    }

    /** Validation selon la grille, par une autre personne : ecritures, depreciation reprise, facture {@code WRITTEN_OFF}. */
    @Transactional
    public AccReceivableWriteoff approveWriteOff(Long id) {
        AccReceivableWriteoff writeoff = writeoffs.findById(id).orElseThrow(() -> new ResourceNotFoundException("Decision introuvable : " + id));
        require("DIRECTION".equals(writeoff.getApprovalLevel()) ? WRITE_OFF_DIRECTION : WRITE_OFF_APPROVE);
        if (!"PREPARED".equals(writeoff.getStatus())) {
            throw new BadRequestException("La decision est deja traitee (" + writeoff.getStatus() + ")");
        }
        if (actor().equalsIgnoreCase(writeoff.getPreparedBy())) {
            throw new ForbiddenException("Celui qui prepare une decision de passage en perte ne la valide pas");
        }
        post(writeoff, actor(), today());
        return writeoffs.save(writeoff);
    }

    @Transactional
    public AccReceivableWriteoff rejectWriteOff(Long id, String reason) {
        AccReceivableWriteoff writeoff = writeoffs.findById(id).orElseThrow(() -> new ResourceNotFoundException("Decision introuvable : " + id));
        require("DIRECTION".equals(writeoff.getApprovalLevel()) ? WRITE_OFF_DIRECTION : WRITE_OFF_APPROVE);
        if (!"PREPARED".equals(writeoff.getStatus())) {
            throw new BadRequestException("La decision est deja traitee (" + writeoff.getStatus() + ")");
        }
        writeoff.setStatus("REJECTED");
        writeoff.setComment(nz(writeoff.getComment(), "") + (StringUtils.hasText(reason) ? " | Rejet : " + reason.trim() : ""));
        writeoff.setApprovedBy(actor());
        writeoff.setApprovedAt(Instant.now());
        return writeoffs.save(writeoff);
    }

    private void post(AccReceivableWriteoff writeoff, String actor, LocalDate date) {
        String label = "Passage en perte " + writeoff.getDocumentNumbers();
        List<AccountingRequests.EntryLine> lines = new ArrayList<>();
        BigDecimal released = BigDecimal.ZERO;
        boolean vat = Boolean.TRUE.equals(writeoff.getVatRecovered());
        for (String number : writeoff.getDocumentNumbers().split(",")) {
            BillingDocument document = document(number);
            AccReceivableStatus status = statuses.findByDocumentNumber(number).orElse(null);
            BigDecimal ttc = status != null && AT_RISK.contains(status.getStatus()) ? status.getAmountTtc() : money(document.getBalanceDue());
            if (ttc.signum() <= 0) {
                continue;
            }
            BigDecimal ht = ht(document, ttc);
            BigDecimal tax = ttc.subtract(ht);
            String account = status != null && AT_RISK.contains(status.getStatus()) ? status.getAccountNumber() : clientAccount();
            lines.add(line(lossAccount(), null, label + " " + number, vat ? ht : ttc, null, number));
            if (vat && tax.signum() > 0) {
                lines.add(line(vatAccount(), null, "TVA " + label + " " + number, tax, null, number));
            }
            lines.add(line(account, customerCode(document), label + " " + number, null, ttc, number));
            if (status != null && status.getImpairment().signum() > 0) {
                lines.add(line(allowanceAccount(status.getStatus()), null, "Reprise " + label + " " + number, status.getImpairment(), null, number));
                lines.add(line(reversalAccount(), null, "Reprise " + label + " " + number, null, status.getImpairment(), number));
                released = released.add(status.getImpairment());
            }
        }
        if (lines.isEmpty()) {
            throw new BadRequestException("Aucun reste du a passer en perte");
        }
        AccEntry entry = book(journal(), date, label, "INVOICE_WRITTEN_OFF", writeoff.getDocumentNumbers(), lines, actor);
        String voucher = vouchers.issueAndPrint("DPP", "RECEIVABLE_WRITE_OFF", writeoff.getDocumentNumbers(), writeoff.getAmountTtc(), label, actor).number();
        for (String number : writeoff.getDocumentNumbers().split(",")) {
            BillingDocument document = document(number);
            AccReceivableStatus status = statuses.findByDocumentNumber(number).orElseGet(() -> AccReceivableStatus.builder().documentNumber(number)
                    .customerCode(customerCode(document)).customerType(document.getCustomerType()).since(date).createdBy(actor).build());
            if (status.getImpairment().signum() > 0) {
                impairments.save(AccReceivableImpairment.builder().documentNumber(number).periodCode(YearMonth.from(date).toString()).method("RELEASE")
                        .amount(BigDecimal.ZERO).previousAmount(status.getImpairment()).difference(status.getImpairment().negate())
                        .kind("WRITE_OFF_RELEASE").entryNumber(entry.getEntryNumber()).createdBy(actor).build());
            }
            status.setStatus(WRITTEN_OFF);
            status.setImpairment(BigDecimal.ZERO);
            status.setAmountTtc(BigDecimal.ZERO);
            status.setSince(date);
            status.setReason("Decision " + voucher);
            status.setUpdatedAt(Instant.now());
            statuses.save(status);
            if (document.getStatus() != BillingDocumentStatus.WRITTEN_OFF) {
                document.setStatus(BillingDocumentStatus.WRITTEN_OFF);
                documents.save(document);
            }
        }
        writeoff.setImpairmentReleased(released);
        writeoff.setStatus("APPROVED");
        writeoff.setApprovedBy(actor);
        writeoff.setApprovedAt(Instant.now());
        writeoff.setEntryNumber(entry.getEntryNumber());
        writeoff.setVoucherNumber(voucher);
    }

    /**
     * Une facture passee {@code WRITTEN_OFF} dans la facturation sans decision comptable est soldee en
     * comptabilite (traitement de nuit), avec reprise de sa depreciation.
     */
    @Transactional
    public int syncBillingWriteOffs(LocalDate today) {
        entityManager.flush();
        List<String> numbers = new JdbcTemplate(dataSource).queryForList("SELECT d.document_number FROM billing_document d "
                + "WHERE d.status = 'WRITTEN_OFF' AND d.deleted_at IS NULL AND d.balance_due > 0 AND NOT EXISTS (SELECT 1 FROM acc_receivable_status s "
                + "WHERE s.document_number = d.document_number AND s.status IN ('WRITTEN_OFF', 'RECOVERED_AFTER_LOSS'))", String.class);
        int done = 0;
        for (String number : numbers) {
            try {
                Totals totals = totals(Set.of(number));
                AccReceivableWriteoff writeoff = AccReceivableWriteoff.builder().customerCode(totals.customer()).documentNumbers(number)
                        .amountHt(totals.ht()).amountVat(totals.vat()).amountTtc(totals.ttc())
                        .vatRecovered(bool("accounting.receivable.writeoff-vat-recovery", true)).reason("OTHER")
                        .comment("Facture passee en perte dans la facturation").origin("BILLING_SYNC").preparedBy("SYSTEM").build();
                post(writeoff, "SYSTEM", today);
                writeoffs.save(writeoff);
                done++;
            } catch (RuntimeException exception) {
                log.warn("Creances : perte de {} non soldee en comptabilite : {}", number, exception.getMessage());
            }
        }
        return done;
    }

    // ================================================================== politique d'acces

    @Override
    @Transactional(readOnly = true)
    public boolean disputed(String documentNumber) {
        return documentNumber != null && statuses.findByDocumentNumber(documentNumber).map(item -> DISPUTED.equals(item.getStatus())).orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public Policy policyFor(String customerType, String customerCode) {
        if (!StringUtils.hasText(customerCode)) {
            return Policy.NONE;
        }
        boolean atRisk = statuses.findAllByCustomerCode(customerCode).stream()
                .anyMatch(item -> AT_RISK.contains(item.getStatus()) || WRITTEN_OFF.equals(item.getStatus()));
        if (!atRisk) {
            return Policy.NONE;
        }
        String segment = customerType == null ? "" : "." + customerType.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        String value = settings.text("accounting.doubtful.customer-policy" + segment, settings.text("accounting.doubtful.customer-policy", "ALERT"));
        try {
            return Policy.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return Policy.ALERT;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public String guardNewSale(String customerType, String customerCode, String what) {
        Policy policy = policyFor(customerType, customerCode);
        return switch (policy) {
            case NONE -> null;
            case ALERT -> "Client a creance douteuse ou litigieuse : verifiez sa situation avant " + what + ".";
            case PREPAY -> "Client a creance douteuse : " + what + " payable d'avance uniquement.";
            case BLOCK -> {
                if (actors.hasAuthority(OVERRIDE)) {
                    log.warn("Creances : derogation au blocage du client {} par {} ({})", customerCode, actor(), what);
                    yield "Derogation au blocage du client a creance douteuse, tracee au nom de " + actor() + ".";
                }
                throw new ForbiddenException("Client bloque pour creance douteuse : " + what + " impossible sans derogation (droit "
                        + OVERRIDE + ").");
            }
        };
    }

    @Override
    @Transactional(readOnly = true)
    public void guardCreditSale(String customerType, String customerCode, String what) {
        if (policyFor(customerType, customerCode) != Policy.PREPAY) {
            return;
        }
        if (actors.hasAuthority(OVERRIDE)) {
            log.warn("Creances : derogation a la politique PREPAY du client {} par {} ({})", customerCode, actor(), what);
            return;
        }
        throw new ForbiddenException("Client a creance douteuse : " + what + " doit etre payee d'avance (vente a credit refusee sans derogation, droit "
                + OVERRIDE + ").");
    }

    // ================================================================== controles

    public record Finding(String severity, String subject, String message) {
    }

    @Transactional(readOnly = true)
    public List<Finding> controls(LocalDate today) {
        List<Finding> findings = new ArrayList<>();
        entityManager.flush();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        for (String account : List.of(disputedAccount(), doubtfulAccount())) {
            BigDecimal ledger = balance(jdbc, account);
            BigDecimal tracked = statuses.findAllByStatusIn(AT_RISK).stream().filter(item -> account.equals(item.getAccountNumber()))
                    .map(AccReceivableStatus::getAmountTtc).reduce(BigDecimal.ZERO, BigDecimal::add);
            if (ledger.subtract(tracked).abs().compareTo(BigDecimal.ONE) > 0) {
                findings.add(new Finding("BLOCKING", "Compte " + account, "Solde " + VoucherService.money(ledger) + " different du reste du suivi "
                        + VoucherService.money(tracked)));
            }
        }
        for (String status : List.of(DISPUTED, DOUBTFUL)) {
            String account = allowanceAccount(status);
            BigDecimal ledger = balance(jdbc, account).negate();
            BigDecimal tracked = statuses.findAllByStatusIn(Set.of(status)).stream().map(AccReceivableStatus::getImpairment)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (ledger.subtract(tracked).abs().compareTo(BigDecimal.ONE) > 0) {
                findings.add(new Finding("BLOCKING", "Compte " + account, "Depreciation au compte " + VoucherService.money(ledger)
                        + " differente du total par facture " + VoucherService.money(tracked)));
            }
        }
        Integer unsynced = jdbc.queryForObject("SELECT COUNT(*) FROM billing_document d WHERE d.status = 'WRITTEN_OFF' AND d.deleted_at IS NULL "
                + "AND d.balance_due > 0 AND NOT EXISTS (SELECT 1 FROM acc_receivable_status s WHERE s.document_number = d.document_number "
                + "AND s.status IN ('WRITTEN_OFF', 'RECOVERED_AFTER_LOSS'))", Integer.class);
        if (unsynced != null && unsynced > 0) {
            findings.add(new Finding("BLOCKING", "Pertes", unsynced + " facture(s) WRITTEN_OFF en facturation non soldee(s) en comptabilite"));
        }
        long triggerDays = number("accounting.receivable.trigger-days", 90);
        for (BillingDocument document : documents.findOverdueWithBalance(5000)) {
            if (!isInvoice(document) || document.getDueDate() == null || ChronoUnit.DAYS.between(document.getDueDate(), today) <= triggerDays) {
                continue;
            }
            boolean handled = statuses.findByDocumentNumber(document.getDocumentNumber()).map(item -> !NORMAL.equals(item.getStatus())).orElse(false)
                    || !proposals.findAllByDocumentNumberAndStatusIn(document.getDocumentNumber(), List.of("PROPOSED", "POSTPONED")).isEmpty();
            if (!handled) {
                findings.add(new Finding("WARNING", document.getDocumentNumber(), "Echue depuis plus de " + triggerDays + " jours, ni classee ni reportee"));
            }
        }
        statuses.findAllByStatusIn(Set.of(RECOVERED, NORMAL)).stream().filter(item -> item.getImpairment().signum() > 0)
                .forEach(item -> findings.add(new Finding("WARNING", item.getDocumentNumber(), "Depreciation restante sur une facture soldee ou normale")));
        return findings;
    }

    /** Elements de la liste de cloture propres aux creances : ajustement de cloture et file de revue. */
    @Transactional(readOnly = true)
    public List<Finding> closingCheck(int year) {
        List<Finding> findings = new ArrayList<>();
        String closing = YearMonth.of(year, 12).toString();
        boolean adjusted = !impairments.findAllByPeriodCode(closing).isEmpty() || statuses.findAllByStatusIn(AT_RISK).isEmpty()
                || impairment(YearMonth.of(year, 12), true, "SYSTEM").lines().stream().allMatch(line -> line.difference().signum() == 0);
        if (!adjusted) {
            findings.add(new Finding("BLOCKING", "Depreciations", "Ajustement des depreciations de creances de " + closing + " non comptabilise"));
        }
        long pending = proposals.findAllByStatusOrderByCreatedAtAsc("PROPOSED").size();
        if (pending > 0) {
            findings.add(new Finding("BLOCKING", "File de revue", pending + " proposition(s) de classement non traitee(s)"));
        }
        controls(LocalDate.of(year, 12, 31)).stream().filter(item -> "BLOCKING".equals(item.severity())).forEach(findings::add);
        return findings;
    }

    // ================================================================== outils

    private record Totals(String customer, List<String> documents, BigDecimal ht, BigDecimal vat, BigDecimal ttc) {
    }

    private Totals totals(Set<String> numbers) {
        String customer = null;
        List<String> list = new ArrayList<>();
        BigDecimal ht = BigDecimal.ZERO;
        BigDecimal ttc = BigDecimal.ZERO;
        for (String number : numbers) {
            BillingDocument document = document(number);
            if (!isInvoice(document)) {
                throw new BadRequestException(number + " n'est pas une facture");
            }
            String code = customerCode(document);
            if (customer != null && !customer.equals(code)) {
                throw new BadRequestException("Une decision de passage en perte porte sur un seul client");
            }
            customer = code;
            AccReceivableStatus status = statuses.findByDocumentNumber(document.getDocumentNumber()).orElse(null);
            if (status != null && (WRITTEN_OFF.equals(status.getStatus()) || RECOVERED_AFTER_LOSS.equals(status.getStatus()))) {
                throw new BadRequestException(number + " est deja passee en perte");
            }
            BigDecimal remaining = status != null && AT_RISK.contains(status.getStatus()) ? status.getAmountTtc() : money(document.getBalanceDue());
            if (remaining.signum() <= 0) {
                throw new BadRequestException(number + " n'a plus de reste du");
            }
            list.add(document.getDocumentNumber());
            ttc = ttc.add(remaining);
            ht = ht.add(ht(document, remaining));
        }
        return new Totals(customer, list, ht, ttc.subtract(ht), ttc);
    }

    private ReceivableView view(BillingDocument document, AccReceivableStatus status, LocalDate at) {
        long days = document.getDueDate() == null ? 0 : Math.max(0, ChronoUnit.DAYS.between(document.getDueDate(), at));
        BigDecimal due = status != null && AT_RISK.contains(status.getStatus()) ? status.getAmountTtc() : money(document.getBalanceDue());
        BigDecimal impairment = status == null ? BigDecimal.ZERO : status.getImpairment();
        return new ReceivableView(document.getDocumentNumber(), customerCode(document), document.getCustomerType(),
                status == null ? NORMAL : status.getStatus(), status == null ? clientAccount() : status.getAccountNumber(), document.getDueDate(), days,
                due, ht(document, due), impairment, due.subtract(impairment), status == null ? null : status.getReason(),
                status == null ? null : status.getVoucherNumber());
    }

    /** HT du reste du : reste du TTC multiplie par HT / TTC de la facture. */
    private BigDecimal ht(BillingDocument document, BigDecimal remainingTtc) {
        return remainingTtc.multiply(ratio(document)).setScale(0, RoundingMode.HALF_UP);
    }

    private BigDecimal ratio(BillingDocument document) {
        BigDecimal total = money(document.getTotalAmount());
        if (total.signum() <= 0) {
            return BigDecimal.ONE;
        }
        BigDecimal tax = money(document.getTaxAmount());
        return total.subtract(tax).divide(total, 10, RoundingMode.HALF_UP);
    }

    private BigDecimal suggested(BillingDocument document, long days) {
        return ht(document, money(document.getBalanceDue())).multiply(gridRate(days)).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
    }

    /** Grille statistique : « 91-180:25,181-365:50,366+:100 » par defaut. */
    BigDecimal gridRate(long days) {
        String grid = settings.text("accounting.receivable.grid", "91-180:25,181-365:50,366+:100");
        for (String part : grid.split(",")) {
            String[] pair = part.trim().split(":");
            if (pair.length != 2) {
                continue;
            }
            try {
                String range = pair[0].trim();
                BigDecimal rate = new BigDecimal(pair[1].trim());
                if (range.endsWith("+") ? days >= Long.parseLong(range.substring(0, range.length() - 1))
                        : days >= Long.parseLong(range.split("-")[0]) && days <= Long.parseLong(range.split("-")[1])) {
                    return rate;
                }
            } catch (RuntimeException ignored) {
                // tranche illisible : ignoree
            }
        }
        return BigDecimal.ZERO;
    }

    /**
     * Garanties detenues : soldes crediteurs du portefeuille (4191) et de la caution (165) du client,
     * plus ses avoirs emis et non encore imputes.
     */
    private BigDecimal guarantee(String customerCode) {
        if (!StringUtils.hasText(customerCode)) {
            return BigDecimal.ZERO;
        }
        entityManager.flush();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        BigDecimal total = BigDecimal.ZERO;
        for (String account : List.of(settings.text("accounting.receivable.guarantee-account", "4191"),
                settings.text("accounting.receivable.deposit-account", "165"))) {
            BigDecimal value = jdbc.queryForObject("SELECT COALESCE(SUM(l.credit - l.debit), 0) FROM acc_entry_line l "
                    + "JOIN acc_entry e ON e.id = l.entry_id WHERE l.account_number = ? AND l.third_party_code = ? AND e.status IN ('VALIDATED', 'REVERSED')",
                    BigDecimal.class, account, customerCode);
            total = total.add(value == null ? BigDecimal.ZERO : value.max(BigDecimal.ZERO));
        }
        BigDecimal credits = jdbc.queryForObject("SELECT COALESCE(SUM(balance_due), 0) FROM billing_document WHERE document_type = 'CREDIT_NOTE' "
                + "AND deleted_at IS NULL AND status NOT IN ('DRAFT', 'CANCELLED', 'VOIDED') AND balance_due > 0 "
                + "AND COALESCE(customer_code, company_customer_code) = ?", BigDecimal.class, customerCode);
        return total.add(credits == null ? BigDecimal.ZERO : credits);
    }

    /**
     * Autres declencheurs : cheque rejete non regularise apres le delai ; abonnement suspendu pour
     * impaye depuis plus du delai (factures de ses elements facturables).
     */
    private String extraTrigger(BillingDocument document, LocalDate today) {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        long chequeDays = number("accounting.receivable.cheque-rejected-days", 15);
        Integer cheques = jdbc.queryForObject("SELECT COUNT(*) FROM bank_cheque c JOIN payment_transaction t ON t.transaction_number = c.transaction_number "
                + "JOIN payment_allocation a ON a.payment_transaction_id = t.id WHERE c.status = 'REJECTED' AND a.billing_document_number = ? "
                + "AND c.rejected_at < ?", Integer.class, document.getDocumentNumber(),
                java.sql.Timestamp.from(today.minusDays(chequeDays).atStartOfDay(ZONE).toInstant()));
        if (cheques != null && cheques > 0) {
            return "CHEQUE_REJECTED";
        }
        long suspendedDays = number("accounting.receivable.suspended-days", 30);
        Integer suspended = jdbc.queryForObject("SELECT COUNT(*) FROM billing_document_line l JOIN billing_document d ON d.id = l.document_id "
                + "JOIN billable_item b ON b.billable_number = l.item_code JOIN subscription s ON s.subscription_number = b.source_id "
                + "WHERE d.document_number = ? AND s.status = 'SUSPENDED' AND s.suspended_at < ?", Integer.class, document.getDocumentNumber(),
                java.sql.Timestamp.from(today.minusDays(suspendedDays).atStartOfDay(ZONE).toInstant()));
        if (suspended != null && suspended > 0) {
            return "SUBSCRIPTION_SUSPENDED";
        }
        return null;
    }

    private boolean handover(BillingDocument document) {
        for (DunningNotice notice : notices.findByDocumentNumberOrderByExecutedAtDesc(document.getDocumentNumber())) {
            if (notice.getAction() == DunningStep.Action.HANDOVER) {
                return true;
            }
        }
        return false;
    }

    private LocalDate due(String documentNumber) {
        return documents.findByDocumentNumber(documentNumber).map(BillingDocument::getDueDate).orElse(LocalDate.MAX);
    }

    private BigDecimal balance(JdbcTemplate jdbc, String account) {
        BigDecimal value = jdbc.queryForObject("SELECT COALESCE(SUM(l.debit - l.credit), 0) FROM acc_entry_line l JOIN acc_entry e ON e.id = l.entry_id "
                + "WHERE l.account_number = ? AND e.status IN ('VALIDATED', 'REVERSED')", BigDecimal.class, account);
        return value == null ? BigDecimal.ZERO : value;
    }

    AccEntry book(String journal, LocalDate date, String label, String sourceType, String sourceCode, List<AccountingRequests.EntryLine> lines,
                  String actor) {
        LocalDate effective = date;
        LocalDate original = null;
        Optional<AccPeriod> period = periodRepository.findCovering(date);
        if (period.isPresent() && period.get().getStatus() != PeriodStatus.OPEN) {
            effective = periods.firstOpenOnOrAfter(date).getStartDate();
            original = date;
        }
        AccEntry entry = entries.post(new AccountingEntryService.AutoPost(journal, effective, original, label, EntryOrigin.PLATFORM, sourceType,
                sourceCode.length() > 120 ? sourceCode.substring(0, 120) : sourceCode, lines, actor, true, null));
        if (entry.getStatus() != EntryStatus.VALIDATED) {
            throw new BadRequestException("L'ecriture n'a pas pu etre validee : " + entry.getPendingDetail());
        }
        return entry;
    }

    static AccountingRequests.EntryLine line(String account, String thirdParty, String label, BigDecimal debit, BigDecimal credit, String reference) {
        return new AccountingRequests.EntryLine(account, thirdParty, label.length() > 250 ? label.substring(0, 250) : label, debit, credit,
                null, null, null, null, null, null, reference, null);
    }

    private static boolean isInvoice(BillingDocument document) {
        return document.getDocumentType() == BillingDocumentType.INVOICE || document.getDocumentType() == BillingDocumentType.CORRECTIVE_INVOICE
                || document.getDocumentType() == BillingDocumentType.DEBIT_NOTE;
    }

    static String customerCode(BillingDocument document) {
        return StringUtils.hasText(document.getCustomerCode()) ? document.getCustomerCode() : document.getCompanyCustomerCode();
    }

    private BillingDocument document(String number) {
        return documents.findByDocumentNumber(number == null ? "" : number.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Facture introuvable : " + number));
    }

    private static YearMonth month(String period) {
        try {
            return YearMonth.parse(period.trim());
        } catch (RuntimeException ex) {
            throw new BadRequestException("Periode attendue au format AAAA-MM");
        }
    }

    private String clientAccount() {
        return settings.text("accounting.sales.client-account", "411");
    }

    private String disputedAccount() {
        return settings.text("accounting.receivable.disputed-account", "4161");
    }

    private String doubtfulAccount() {
        return settings.text("accounting.receivable.doubtful-account", "4162");
    }

    String allowanceAccount(String status) {
        return DISPUTED.equals(status) ? settings.text("accounting.receivable.disputed-allowance-account", "4911")
                : settings.text("accounting.receivable.doubtful-allowance-account", "4912");
    }

    private String expenseAccount() {
        return settings.text("accounting.receivable.expense-account", "6594");
    }

    private String reversalAccount() {
        return settings.text("accounting.receivable.reversal-account", "7594");
    }

    private String lossAccount() {
        return settings.text("accounting.receivable.loss-account", "6511");
    }

    private String vatAccount() {
        return settings.text("accounting.sales.vat-account", "4431");
    }

    String recoveryAccount() {
        return settings.text("accounting.receivable.recovery-account", "758");
    }

    private String journal() {
        return settings.text("accounting.receivable.journal", "OD");
    }

    private long number(String key, long fallback) {
        try {
            return Long.parseLong(settings.text(key, String.valueOf(fallback)).trim());
        } catch (RuntimeException ex) {
            return fallback;
        }
    }

    private BigDecimal decimal(String key, BigDecimal fallback) {
        try {
            return new BigDecimal(settings.text(key, fallback.toPlainString()).trim());
        } catch (RuntimeException ex) {
            return fallback;
        }
    }

    private boolean bool(String key, boolean fallback) {
        return Boolean.parseBoolean(settings.text(key, String.valueOf(fallback)).trim());
    }

    private static BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(0, RoundingMode.HALF_UP);
    }

    private static String nz(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private static LocalDate today() {
        return LocalDate.now(ZONE);
    }

    void require(String authority) {
        if (!actors.hasAuthority(authority)) {
            throw new ForbiddenException("Action reservee : " + authority);
        }
    }

    String actor() {
        return actors.resolve("SYSTEM");
    }

    @SuppressWarnings("unused")
    private static List<String> split(String value) {
        return value == null ? List.of() : Arrays.stream(value.split(",")).map(String::trim).filter(StringUtils::hasText).toList();
    }
}
