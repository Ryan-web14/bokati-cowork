package com.sni.bokaticowork.features.accounting.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.accounting.dto.AccountingRequests;
import com.sni.bokaticowork.features.accounting.enums.EntryOrigin;
import com.sni.bokaticowork.features.accounting.enums.EntryStatus;
import com.sni.bokaticowork.features.accounting.model.*;
import com.sni.bokaticowork.features.accounting.repository.*;
import com.sni.bokaticowork.features.accounting.service.support.AccountingNotifier;
import com.sni.bokaticowork.features.accounting.service.support.AccountingSettings;
import com.sni.bokaticowork.features.accounting.service.support.LoanSchedule;
import com.sni.bokaticowork.features.fund.voucher.VoucherService;
import com.sni.bokaticowork.features.payment.service.support.CashActorResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Emprunts (lot F17) : fiche, tableau d'amortissement versionne, deblocage, echeances ecrites
 * seules a leur date, remboursement anticipe, changement de taux, report d'echeance, interets
 * courus en cloture et leur extourne, note annexe des dettes financieres.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class LoanService {

    public static final String MANAGE = "FINANCING:MANAGE";

    private final AccLoanRepository loans;
    private final AccLoanInstallmentRepository installments;
    private final AccLoanEventRepository events;
    private final AccCommitmentRepository commitments;
    private final BankAccountRepository bankAccounts;
    private final AccEntryRepository entryRepository;
    private final AccountingEntryService entries;
    private final AccountingSettings settings;
    private final AccountingNotifier notifier;
    private final VoucherService vouchers;
    private final CashActorResolver actors;

    // ------------------------------------------------------------------ fiche et tableau

    public record LoanRequest(String code, String lenderType, String lenderCode, String lenderName, String purpose, BigDecimal principal,
                              LocalDate disbursedOn, LocalDate firstDueDate, BigDecimal rate, String rateType, String dayCount, Integer frequencyMonths,
                              Integer termCount, String deferralType, Integer deferralCount, String method, BigDecimal fees, BigDecimal insurance,
                              String loanAccount, String interestAccount, String insuranceAccount, String feesAccount, String accruedAccount,
                              String bankAccountCode, String sectionCode, String budgetItemCode, String postingMode) {
    }

    public record LoanView(AccLoan loan, List<AccLoanInstallment> schedule, List<AccLoanEvent> events, BigDecimal outstanding) {
    }

    @Transactional(readOnly = true)
    public List<AccLoan> list() {
        return loans.findAllByOrderByCodeAsc();
    }

    @Transactional(readOnly = true)
    public LoanView view(String code) {
        AccLoan loan = loan(code);
        List<AccLoanInstallment> schedule = current(loan);
        return new LoanView(loan, schedule, events.findAllByLoanIdOrderByCreatedAtAsc(loan.getId()), outstanding(loan, LocalDate.now()));
    }

    /** Toutes les versions du tableau : l'ancien tableau est conserve a chaque recalcul. */
    @Transactional(readOnly = true)
    public List<AccLoanInstallment> history(String code) {
        return installments.findAllByLoanIdOrderByVersionAscNumberAsc(loan(code).getId());
    }

    public LoanView create(LoanRequest request) {
        require(MANAGE);
        if (request.principal() == null || request.principal().signum() <= 0) {
            throw new BadRequestException("Le montant de l'emprunt est positif");
        }
        if (request.disbursedOn() == null || request.firstDueDate() == null || !request.firstDueDate().isAfter(request.disbursedOn())) {
            throw new BadRequestException("La premiere echeance suit la date de deblocage");
        }
        if (request.termCount() == null || request.termCount() <= 0) {
            throw new BadRequestException("Le nombre d'echeances est positif");
        }
        bankAccounts.findByCode(request.bankAccountCode()).orElseThrow(() -> new BadRequestException("Compte bancaire inconnu : " + request.bankAccountCode()));
        String code = StringUtils.hasText(request.code()) ? request.code().trim().toUpperCase(Locale.ROOT) : "EMP-" + (loans.count() + 1);
        if (loans.findByCode(code).isPresent()) {
            throw new BadRequestException("L'emprunt " + code + " existe deja");
        }
        String actor = actors.resolve("SYSTEM");
        AccLoan loan = AccLoan.builder().code(code).lenderType(or(request.lenderType(), "BANK")).lenderCode(request.lenderCode())
                .lenderName(request.lenderName()).purpose(request.purpose()).principal(request.principal()).disbursedOn(request.disbursedOn())
                .firstDueDate(request.firstDueDate()).rate(request.rate() == null ? BigDecimal.ZERO : request.rate()).rateType(or(request.rateType(), "FIXED"))
                .dayCount(or(request.dayCount(), "30_360")).frequencyMonths(request.frequencyMonths() == null ? 1 : request.frequencyMonths())
                .termCount(request.termCount()).deferralType(or(request.deferralType(), "NONE"))
                .deferralCount(request.deferralCount() == null ? 0 : request.deferralCount()).method(or(request.method(), "ANNUITY"))
                .fees(nz(request.fees())).insurance(nz(request.insurance())).loanAccount(or(request.loanAccount(), "162"))
                .interestAccount(or(request.interestAccount(), "671")).insuranceAccount(or(request.insuranceAccount(), "625"))
                .feesAccount(or(request.feesAccount(), "631")).accruedAccount(or(request.accruedAccount(), "166"))
                .bankAccountCode(request.bankAccountCode()).sectionCode(request.sectionCode()).budgetItemCode(request.budgetItemCode())
                .postingMode(or(request.postingMode(), "AT_DATE")).createdBy(actor).build();
        loan = loans.save(loan);
        save(loan, 1, LoanSchedule.compute(new LoanSchedule.Terms(loan.getPrincipal(), loan.getRate(), loan.getDayCount(), loan.getFrequencyMonths(),
                loan.getTermCount(), loan.getDeferralType(), loan.getDeferralCount(), loan.getMethod(), loan.getInsurance(), loan.getDisbursedOn(),
                loan.getFirstDueDate(), 1)), List.of());
        event(loan, "CREATED", loan.getDisbursedOn(), loan.getPrincipal(), loan.getRate(), "Tableau calcule", null, 1, null);
        return view(code);
    }

    /** Deblocage : D banque (net des frais) ; D frais ; C emprunt. */
    public LoanView disburse(String code) {
        require(MANAGE);
        AccLoan loan = loan(code);
        if (!"DRAFT".equals(loan.getStatus())) {
            throw new BadRequestException("L'emprunt " + code + " est deja debloque");
        }
        BankAccount bank = bank(loan);
        String label = "Deblocage emprunt " + loan.getCode() + " " + loan.getLenderName();
        List<AccountingRequests.EntryLine> lines = new ArrayList<>();
        lines.add(line(bank.getAccountNumber(), null, label, loan.getPrincipal().subtract(loan.getFees()), null, loan.getCode(), null));
        if (loan.getFees().signum() > 0) {
            lines.add(line(loan.getFeesAccount(), null, "Frais de dossier " + loan.getCode(), loan.getFees(), null, loan.getCode(), loan.getSectionCode()));
        }
        lines.add(line(loan.getLoanAccount(), lenderThirdParty(loan), label, null, loan.getPrincipal(), loan.getCode(), null));
        AccEntry entry = post(bank.getJournalCode(), loan.getDisbursedOn(), label, "LOAN_DISBURSEMENT", loan.getCode(), lines);
        loan.setStatus("ACTIVE");
        loan.setDisbursementEntry(entry.getEntryNumber());
        loans.save(loan);
        event(loan, "DISBURSED", loan.getDisbursedOn(), loan.getPrincipal(), null, null, entry.getEntryNumber(), null, null);
        return view(code);
    }

    // ------------------------------------------------------------------ echeances

    /** Ecrit les echeances arrivees a leur date (emprunts actifs, mode {@code AT_DATE}). Appele chaque jour. */
    public int postDue(LocalDate today) {
        int posted = 0;
        for (AccLoanInstallment installment : installments.findAllByStatusAndDueDateLessThanEqualOrderByDueDateAsc("PLANNED", today)) {
            AccLoan loan = loans.findById(installment.getLoanId()).orElse(null);
            if (loan == null || !"ACTIVE".equals(loan.getStatus()) || !"AT_DATE".equals(loan.getPostingMode())
                    || !Objects.equals(installment.getVersion(), loan.getScheduleVersion())) {
                continue;
            }
            postInstallment(loan, installment);
            posted++;
        }
        return posted;
    }

    /** Ecriture d'une echeance a la demande (mode au releve bancaire, ou avance). */
    public AccLoanInstallment postInstallment(String code, int number) {
        require(MANAGE);
        AccLoan loan = loan(code);
        if (!"ACTIVE".equals(loan.getStatus())) {
            throw new BadRequestException("L'emprunt " + code + " n'est pas actif");
        }
        AccLoanInstallment installment = current(loan).stream().filter(item -> item.getNumber() == number).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Echeance " + number + " introuvable"));
        if (!"PLANNED".equals(installment.getStatus())) {
            throw new BadRequestException("L'echeance " + number + " est deja ecrite");
        }
        boolean earlierOpen = current(loan).stream().anyMatch(item -> item.getNumber() < number && "PLANNED".equals(item.getStatus()));
        if (earlierOpen) {
            throw new BadRequestException("Les echeances precedentes ne sont pas encore ecrites");
        }
        return postInstallment(loan, installment);
    }

    /** Echeance : D emprunt (capital) ; D interets ; D assurance ; C banque. Differe total : D interets ; C emprunt. */
    private AccLoanInstallment postInstallment(AccLoan loan, AccLoanInstallment installment) {
        BankAccount bank = bank(loan);
        String label = "Echeance " + installment.getNumber() + "/" + loan.getTermCount() + " emprunt " + loan.getCode();
        List<AccountingRequests.EntryLine> lines = new ArrayList<>();
        String tp = lenderThirdParty(loan);
        if (installment.getPrincipal().signum() > 0) {
            lines.add(line(loan.getLoanAccount(), tp, label + " (capital)", installment.getPrincipal(), null, loan.getCode(), null));
        } else if (installment.getPrincipal().signum() < 0) {
            lines.add(line(loan.getLoanAccount(), tp, label + " (interets capitalises)", null, installment.getPrincipal().negate(), loan.getCode(), null));
        }
        if (installment.getInterest().signum() > 0) {
            lines.add(line(loan.getInterestAccount(), null, label + " (interets)", installment.getInterest(), null, loan.getCode(), loan.getSectionCode()));
        }
        if (installment.getInsurance().signum() > 0) {
            lines.add(line(loan.getInsuranceAccount(), null, label + " (assurance)", installment.getInsurance(), null, loan.getCode(), loan.getSectionCode()));
        }
        if (installment.getPayment().signum() > 0) {
            lines.add(line(bank.getAccountNumber(), null, label, null, installment.getPayment(), loan.getCode(), null));
        }
        if (!lines.isEmpty()) {
            AccEntry entry = post(bank.getJournalCode(), installment.getDueDate(), label, "LOAN_INSTALLMENT",
                    loan.getCode() + "#" + installment.getVersion() + "-" + installment.getNumber(), lines);
            installment.setEntryNumber(entry.getEntryNumber());
        }
        installment.setStatus("POSTED");
        installment.setPostedAt(Instant.now());
        installments.save(installment);
        boolean finished = current(loan).stream().allMatch(item -> "POSTED".equals(item.getStatus()));
        if (finished) {
            loan.setStatus("CLOSED");
            loan.setClosedAt(Instant.now());
            loans.save(loan);
            event(loan, "CLOSED", installment.getDueDate(), null, null, "Derniere echeance ecrite", null, null, null);
        }
        return installment;
    }

    // ------------------------------------------------------------------ recalculs

    public record Prepayment(LocalDate date, BigDecimal amount, BigDecimal penalty, String mode) {
    }

    /**
     * Remboursement anticipe, partiel ou total, apres la derniere echeance ecrite : D emprunt ;
     * D interets (indemnite) ; C banque. Le tableau est recalcule (duree reduite ou echeance reduite).
     */
    public LoanView prepay(String code, Prepayment request) {
        require(MANAGE);
        AccLoan loan = active(code);
        List<AccLoanInstallment> schedule = current(loan);
        LocalDate date = request.date() == null ? LocalDate.now() : request.date();
        requireUpTo(schedule, date);
        BigDecimal balance = balance(loan, schedule);
        BigDecimal amount = money(request.amount());
        if (amount.signum() <= 0 || amount.compareTo(balance) > 0) {
            throw new BadRequestException("Le remboursement anticipe est positif et au plus le capital restant du (" + plain(balance) + ")");
        }
        BigDecimal penalty = money(request.penalty());
        BankAccount bank = bank(loan);
        String label = "Remboursement anticipe emprunt " + loan.getCode();
        List<AccountingRequests.EntryLine> lines = new ArrayList<>();
        lines.add(line(loan.getLoanAccount(), lenderThirdParty(loan), label, amount, null, loan.getCode(), null));
        if (penalty.signum() > 0) {
            lines.add(line(loan.getInterestAccount(), null, label + " (indemnite)", penalty, null, loan.getCode(), loan.getSectionCode()));
        }
        lines.add(line(bank.getAccountNumber(), null, label, null, amount.add(penalty), loan.getCode(), null));
        AccEntry entry = post(bank.getJournalCode(), date, label, "LOAN_PREPAYMENT", loan.getCode() + "@" + date, lines);
        BigDecimal remainingBalance = balance.subtract(amount);
        List<AccLoanInstallment> planned = schedule.stream().filter(item -> "PLANNED".equals(item.getStatus())).toList();
        int count = planned.size();
        if (remainingBalance.signum() > 0 && "REDUCE_TERM".equalsIgnoreCase(request.mode()) && !planned.isEmpty()) {
            if ("ANNUITY".equals(loan.getMethod())) {
                BigDecimal payment = planned.get(planned.size() - 1).getPayment().subtract(planned.get(0).getInsurance());
                payment = planned.stream().map(item -> item.getPayment().subtract(item.getInsurance())).max(BigDecimal::compareTo).orElse(payment);
                count = Math.min(count, LoanSchedule.termsFor(remainingBalance, loan.getRate(), loan.getFrequencyMonths(), payment));
            } else if ("CONSTANT_CAPITAL".equals(loan.getMethod())) {
                BigDecimal principal = planned.get(0).getPrincipal();
                count = Math.min(count, remainingBalance.divide(principal, 0, RoundingMode.CEILING).intValue());
            }
        }
        int version = regenerate(loan, schedule, remainingBalance, count, loan.getRate(), 0);
        event(loan, "PREPAYMENT", date, amount, null, (request.mode() == null ? "REDUCE_PAYMENT" : request.mode()) + ", indemnite " + plain(penalty),
                entry.getEntryNumber(), version - 1, version);
        if (remainingBalance.signum() == 0) {
            loan.setStatus("CLOSED");
            loan.setClosedAt(Instant.now());
            loans.save(loan);
        }
        return view(code);
    }

    /** Changement de taux (taux variable, renegociation) a partir de la prochaine echeance. */
    public LoanView changeRate(String code, BigDecimal rate, LocalDate date) {
        require(MANAGE);
        AccLoan loan = active(code);
        if (rate == null || rate.signum() < 0) {
            throw new BadRequestException("Le taux est positif ou nul");
        }
        List<AccLoanInstallment> schedule = current(loan);
        long planned = schedule.stream().filter(item -> "PLANNED".equals(item.getStatus())).count();
        BigDecimal previous = loan.getRate();
        loan.setRate(rate);
        loans.save(loan);
        int version = regenerate(loan, schedule, balance(loan, schedule), (int) planned, rate, 0);
        event(loan, "RATE_CHANGE", date == null ? LocalDate.now() : date, null, rate, "Ancien taux " + previous.stripTrailingZeros().toPlainString() + " %",
                null, version - 1, version);
        return view(code);
    }

    /** Report de la prochaine echeance d'une periode ; les interets de la periode reportee s'ajoutent au capital. */
    public LoanView postpone(String code) {
        require(MANAGE);
        AccLoan loan = active(code);
        List<AccLoanInstallment> schedule = current(loan);
        long planned = schedule.stream().filter(item -> "PLANNED".equals(item.getStatus())).count();
        if (planned == 0) {
            throw new BadRequestException("Aucune echeance a reporter");
        }
        BigDecimal balance = balance(loan, schedule);
        BigDecimal interest = balance.multiply(LoanSchedule.periodicRate(loan.getRate(), loan.getFrequencyMonths()), MathContext.DECIMAL64)
                .setScale(0, RoundingMode.HALF_UP);
        int version = regenerate(loan, schedule, balance.add(interest), (int) planned, loan.getRate(), loan.getFrequencyMonths());
        event(loan, "POSTPONEMENT", LocalDate.now(), interest, null, "Echeance reportee d'une periode, interets capitalises", null, version - 1, version);
        return view(code);
    }

    /** Nouvelle version : echeances deja ecrites recopiees, puis tableau recalcule. */
    private int regenerate(AccLoan loan, List<AccLoanInstallment> schedule, BigDecimal balance, int count, BigDecimal rate, int shiftMonths) {
        List<AccLoanInstallment> posted = schedule.stream().filter(item -> "POSTED".equals(item.getStatus())).toList();
        AccLoanInstallment next = schedule.stream().filter(item -> "PLANNED".equals(item.getStatus())).findFirst().orElse(null);
        int version = loan.getScheduleVersion() + 1;
        List<LoanSchedule.Line> lines = List.of();
        if (next != null && balance.signum() > 0 && count > 0) {
            LocalDate previous = posted.isEmpty() ? loan.getDisbursedOn() : posted.get(posted.size() - 1).getDueDate();
            int deferralLeft = Math.max(0, loan.getDeferralCount() - (next.getNumber() - 1));
            lines = LoanSchedule.compute(new LoanSchedule.Terms(balance, rate, loan.getDayCount(), loan.getFrequencyMonths(), count,
                    deferralLeft > 0 ? loan.getDeferralType() : "NONE", deferralLeft, loan.getMethod(), loan.getInsurance(), previous,
                    next.getDueDate().plusMonths(shiftMonths), next.getNumber()));
        }
        save(loan, version, lines, posted);
        loan.setScheduleVersion(version);
        loans.save(loan);
        return version;
    }

    private void save(AccLoan loan, int version, List<LoanSchedule.Line> lines, List<AccLoanInstallment> posted) {
        for (AccLoanInstallment item : posted) {
            installments.save(AccLoanInstallment.builder().loanId(loan.getId()).version(version).number(item.getNumber()).dueDate(item.getDueDate())
                    .openingBalance(item.getOpeningBalance()).interest(item.getInterest()).principal(item.getPrincipal()).insurance(item.getInsurance())
                    .payment(item.getPayment()).closingBalance(item.getClosingBalance()).status("POSTED").entryNumber(item.getEntryNumber())
                    .postedAt(item.getPostedAt()).build());
        }
        for (LoanSchedule.Line line : lines) {
            installments.save(AccLoanInstallment.builder().loanId(loan.getId()).version(version).number(line.number()).dueDate(line.dueDate())
                    .openingBalance(line.openingBalance()).interest(line.interest()).principal(line.principal()).insurance(line.insurance())
                    .payment(line.payment()).closingBalance(line.closingBalance()).build());
        }
    }

    // ------------------------------------------------------------------ cloture

    public record Accrual(String loanCode, BigDecimal interest, String entryNumber, String reversalNumber) {
    }

    /**
     * Interets courus a une date (cloture) : D interets ; C interets courus, au prorata des jours
     * ecoules de l'echeance en cours ; extourne datee du lendemain. Une seule fois par date.
     */
    public List<Accrual> accrue(LocalDate date) {
        require(MANAGE);
        List<Accrual> result = new ArrayList<>();
        String journal = settings.text("accounting.loan.accrual-journal", "OD");
        for (AccLoan loan : loans.findAllByStatus("ACTIVE")) {
            String source = loan.getCode() + "@" + date;
            if (!entryRepository.findBySourceTypeAndSourceCode("LOAN_ACCRUAL", source).isEmpty()) {
                continue;
            }
            List<AccLoanInstallment> schedule = current(loan);
            AccLoanInstallment next = schedule.stream().filter(item -> item.getDueDate().isAfter(date)).findFirst().orElse(null);
            if (next == null || next.getInterest().signum() <= 0) {
                continue;
            }
            LocalDate previous = schedule.stream().filter(item -> !item.getDueDate().isAfter(date)).map(AccLoanInstallment::getDueDate)
                    .reduce((a, b) -> b).orElse(loan.getDisbursedOn());
            if (!date.isAfter(previous)) {
                continue;
            }
            long period = ChronoUnit.DAYS.between(previous, next.getDueDate());
            BigDecimal interest = next.getInterest().multiply(BigDecimal.valueOf(ChronoUnit.DAYS.between(previous, date)))
                    .divide(BigDecimal.valueOf(Math.max(1, period)), 0, RoundingMode.HALF_UP);
            if (interest.signum() <= 0) {
                continue;
            }
            String label = "Interets courus emprunt " + loan.getCode() + " au " + date;
            AccEntry entry = post(journal, date, label, "LOAN_ACCRUAL", source, List.of(
                    line(loan.getInterestAccount(), null, label, interest, null, loan.getCode(), loan.getSectionCode()),
                    line(loan.getAccruedAccount(), null, label, null, interest, loan.getCode(), null)));
            String reversalLabel = "Extourne " + label;
            AccEntry reversal = post(journal, date.plusDays(1), reversalLabel, "LOAN_ACCRUAL_REVERSAL", source, List.of(
                    line(loan.getAccruedAccount(), null, reversalLabel, interest, null, loan.getCode(), null),
                    line(loan.getInterestAccount(), null, reversalLabel, null, interest, loan.getCode(), loan.getSectionCode())));
            result.add(new Accrual(loan.getCode(), interest, entry.getEntryNumber(), reversal.getEntryNumber()));
        }
        return result;
    }

    // ------------------------------------------------------------------ suivi

    public record NoteLine(String code, String lender, String account, BigDecimal outstanding, BigDecimal withinOneYear, BigDecimal beyondOneYear,
                           BigDecimal rate, LocalDate lastDueDate, List<String> guarantees) {
    }

    /** Note annexe des dettes financieres : capital restant du, part a moins et a plus d'un an, garanties. */
    @Transactional(readOnly = true)
    public List<NoteLine> note(LocalDate date) {
        LocalDate day = date == null ? LocalDate.now() : date;
        List<NoteLine> result = new ArrayList<>();
        for (AccLoan loan : loans.findAllByOrderByCodeAsc()) {
            if ("DRAFT".equals(loan.getStatus()) || loan.getDisbursedOn().isAfter(day)) {
                continue;
            }
            List<AccLoanInstallment> schedule = current(loan);
            BigDecimal outstanding = outstanding(loan, day);
            if (outstanding.signum() == 0) {
                continue;
            }
            BigDecimal within = schedule.stream().filter(item -> item.getDueDate().isAfter(day) && !item.getDueDate().isAfter(day.plusYears(1)))
                    .map(AccLoanInstallment::getPrincipal).reduce(BigDecimal.ZERO, BigDecimal::add).max(BigDecimal.ZERO).min(outstanding);
            List<String> guarantees = commitments.findAllByLinkedTypeAndLinkedCode("LOAN", loan.getCode()).stream()
                    .filter(item -> "ACTIVE".equals(item.getStatus())).map(item -> item.getCode() + " " + item.getTypeCode() + " " + plain(item.getAmount())).toList();
            result.add(new NoteLine(loan.getCode(), loan.getLenderName(), loan.getLoanAccount(), outstanding, within, outstanding.subtract(within),
                    loan.getRate(), schedule.isEmpty() ? null : schedule.get(schedule.size() - 1).getDueDate(), guarantees));
        }
        return result;
    }

    /** Echeancier de tous les emprunts actifs entre deux dates (previsionnel de tresorerie). */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> upcoming(LocalDate from, LocalDate to) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (AccLoan loan : loans.findAllByStatus("ACTIVE")) {
            for (AccLoanInstallment item : current(loan)) {
                if ("PLANNED".equals(item.getStatus()) && !item.getDueDate().isBefore(from) && !item.getDueDate().isAfter(to)) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("loanCode", loan.getCode());
                    row.put("lender", loan.getLenderName());
                    row.put("number", item.getNumber());
                    row.put("dueDate", item.getDueDate());
                    row.put("principal", item.getPrincipal());
                    row.put("interest", item.getInterest());
                    row.put("insurance", item.getInsurance());
                    row.put("payment", item.getPayment());
                    result.add(row);
                }
            }
        }
        result.sort(Comparator.comparing(row -> (LocalDate) row.get("dueDate")));
        return result;
    }

    /** Alerte des comptables quelques jours avant une echeance ({@code accounting.loan.alert-days}). */
    public int alerts(LocalDate today) {
        int days = (int) Math.max(1, parse(settings.text("accounting.loan.alert-days", "7")));
        int sent = 0;
        for (Map<String, Object> row : upcoming(today.plusDays(days), today.plusDays(days))) {
            notifier.notifyAccountants("Echeance d'emprunt dans " + days + " jours", "L'echeance " + row.get("number") + " de l'emprunt " + row.get("loanCode")
                    + " (" + row.get("lender") + ") tombe le " + row.get("dueDate") + " : " + plain((BigDecimal) row.get("payment")) + " XAF.",
                    (String) row.get("loanCode"), "/accounting/loans/" + row.get("loanCode"), null);
            sent++;
        }
        return sent;
    }

    // ------------------------------------------------------------------ editions

    @Transactional(readOnly = true)
    public byte[] csv(String code) {
        AccLoan loan = loan(code);
        StringBuilder out = new StringBuilder("﻿Emprunt;Version;Numero;Echeance;Capital restant du;Interets;Capital rembourse;Assurance;Echeance totale;Capital restant apres;Etat;Piece\n");
        for (AccLoanInstallment item : current(loan)) {
            out.append(String.join(";", loan.getCode(), String.valueOf(item.getVersion()), String.valueOf(item.getNumber()), String.valueOf(item.getDueDate()),
                    plain(item.getOpeningBalance()), plain(item.getInterest()), plain(item.getPrincipal()), plain(item.getInsurance()), plain(item.getPayment()),
                    plain(item.getClosingBalance()), item.getStatus(), item.getEntryNumber() == null ? "" : item.getEntryNumber())).append("\n");
        }
        return out.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Transactional(readOnly = true)
    public byte[] pdf(String code) {
        AccLoan loan = loan(code);
        VoucherService.Issued issued = new VoucherService.Issued(loan.getCode() + "-V" + loan.getScheduleVersion(), "EMP", loan.getCode(), loan.getPrincipal(),
                null, loan.getCreatedBy(), loan.getCreatedAt(), 0, null, null);
        List<VoucherService.Section> sections = new ArrayList<>();
        sections.add(new VoucherService.Section("Emprunt").pair("Preteur", loan.getLenderName()).pair("Objet", loan.getPurpose())
                .pair("Montant", VoucherService.money(loan.getPrincipal())).pair("Deblocage", loan.getDisbursedOn())
                .pair("Taux annuel", loan.getRate().stripTrailingZeros().toPlainString() + " % (" + loan.getRateType() + ", base " + loan.getDayCount() + ")")
                .pair("Echeances", loan.getTermCount() + " tous les " + loan.getFrequencyMonths() + " mois, " + loan.getMethod()
                        + ("NONE".equals(loan.getDeferralType()) ? "" : ", differe " + loan.getDeferralType() + " " + loan.getDeferralCount()))
                .pair("Compte", loan.getLoanAccount()).pair("Version du tableau", loan.getScheduleVersion()));
        VoucherService.Section table = new VoucherService.Section("Tableau d'amortissement").columns(
                List.of("N", "Echeance", "Capital du", "Interets", "Capital", "Assurance", "Total", "Reste", "Etat"),
                List.of(true, false, true, true, true, true, true, true, false));
        current(loan).forEach(item -> table.row(item.getNumber(), item.getDueDate(), VoucherService.money(item.getOpeningBalance()),
                VoucherService.money(item.getInterest()), VoucherService.money(item.getPrincipal()), VoucherService.money(item.getInsurance()),
                VoucherService.money(item.getPayment()), VoucherService.money(item.getClosingBalance()), "POSTED".equals(item.getStatus()) ? "Ecrite" : "Prevue"));
        sections.add(table);
        return vouchers.render("Tableau d'amortissement " + loan.getCode(), issued, sections, null, null, List.of());
    }

    // ------------------------------------------------------------------ outils

    private void require(String authority) {
        if (!actors.hasAuthority(authority)) {
            throw new com.sni.bokaticowork.core.exception.customs.ForbiddenException("Action reservee : " + authority);
        }
    }

    List<AccLoanInstallment> current(AccLoan loan) {
        return installments.findAllByLoanIdAndVersionOrderByNumberAsc(loan.getId(), loan.getScheduleVersion());
    }

    /** Capital restant du a une date : reste apres la derniere echeance echue, sinon le capital emprunte. */
    BigDecimal outstanding(AccLoan loan, LocalDate date) {
        if ("DRAFT".equals(loan.getStatus())) {
            return BigDecimal.ZERO;
        }
        List<AccLoanInstallment> schedule = current(loan);
        return schedule.stream().filter(item -> !item.getDueDate().isAfter(date)).map(AccLoanInstallment::getClosingBalance)
                .reduce((a, b) -> b).orElse(schedule.isEmpty() ? BigDecimal.ZERO : schedule.get(0).getOpeningBalance());
    }

    private BigDecimal balance(AccLoan loan, List<AccLoanInstallment> schedule) {
        return schedule.stream().filter(item -> "PLANNED".equals(item.getStatus())).map(AccLoanInstallment::getOpeningBalance).findFirst()
                .orElse(BigDecimal.ZERO);
    }

    private static void requireUpTo(List<AccLoanInstallment> schedule, LocalDate date) {
        if (schedule.stream().anyMatch(item -> "PLANNED".equals(item.getStatus()) && !item.getDueDate().isAfter(date))) {
            throw new BadRequestException("Ecrivez d'abord les echeances arrivees au " + date);
        }
    }

    private AccLoan loan(String code) {
        return loans.findByCode(code).orElseThrow(() -> new ResourceNotFoundException("Emprunt introuvable : " + code));
    }

    private AccLoan active(String code) {
        AccLoan loan = loan(code);
        if (!"ACTIVE".equals(loan.getStatus())) {
            throw new BadRequestException("L'emprunt " + code + " n'est pas actif");
        }
        return loan;
    }

    private BankAccount bank(AccLoan loan) {
        return bankAccounts.findByCode(loan.getBankAccountCode()).orElseThrow(() -> new BadRequestException("Compte bancaire inconnu : " + loan.getBankAccountCode()));
    }

    private static String lenderThirdParty(AccLoan loan) {
        return "PARTNER".equals(loan.getLenderType()) || loan.getLoanAccount().startsWith("46") ? loan.getLenderCode() : null;
    }

    private void event(AccLoan loan, String kind, LocalDate date, BigDecimal amount, BigDecimal rate, String detail, String entry, Integer from, Integer to) {
        events.save(AccLoanEvent.builder().loanId(loan.getId()).kind(kind).eventDate(date).amount(amount).rate(rate).detail(detail).entryNumber(entry)
                .fromVersion(from).toVersion(to).createdBy(actors.resolve("SYSTEM")).build());
    }

    AccEntry post(String journal, LocalDate date, String label, String sourceType, String sourceCode, List<AccountingRequests.EntryLine> lines) {
        AccEntry entry = entries.post(journal, date, label, EntryOrigin.PLATFORM, sourceType, sourceCode, lines, actors.resolve("SYSTEM"));
        if (entry.getStatus() != EntryStatus.VALIDATED) {
            throw new BadRequestException("L'ecriture n'a pas pu etre validee : " + entry.getPendingDetail());
        }
        return entry;
    }

    AccountingRequests.EntryLine line(String account, String thirdParty, String label, BigDecimal debit, BigDecimal credit, String reference, String section) {
        Map<String, String> analytic = null;
        String axis = settings.text("accounting.financing.analytic-axis", "");
        if (StringUtils.hasText(section) && StringUtils.hasText(axis)) {
            analytic = Map.of(axis, section);
        }
        return new AccountingRequests.EntryLine(account, thirdParty, label, debit, credit, null, null, null, null, null, null, reference, analytic);
    }

    private static String or(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : fallback;
    }

    private static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    static BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(0, RoundingMode.HALF_UP).setScale(4, RoundingMode.HALF_UP);
    }

    static String plain(BigDecimal value) {
        return value == null ? "" : value.setScale(0, RoundingMode.HALF_UP).toPlainString();
    }

    private static long parse(String value) {
        try {
            return Long.parseLong(value.trim());
        } catch (RuntimeException exception) {
            return 7;
        }
    }
}
