package com.sni.bokaticowork.features.accounting.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ForbiddenException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.accounting.dto.AccountingRequests;
import com.sni.bokaticowork.features.accounting.enums.EntryStatus;
import com.sni.bokaticowork.features.accounting.model.AccEntry;
import com.sni.bokaticowork.features.accounting.model.AccPartnerAccount;
import com.sni.bokaticowork.features.accounting.model.AccPartnerInterest;
import com.sni.bokaticowork.features.accounting.repository.AccPartnerAccountRepository;
import com.sni.bokaticowork.features.accounting.repository.AccPartnerInterestRepository;
import com.sni.bokaticowork.features.accounting.repository.BankAccountRepository;
import com.sni.bokaticowork.features.accounting.service.support.AccountingSettings;
import com.sni.bokaticowork.features.payment.service.support.CashActorResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Comptes courants d'associes (lot F17) : convention (taux, plafond, blocage), apports,
 * remboursements, depenses payees par l'associe, affectation de dividendes, interets au prorata
 * temporis avec la part non deductible, releve et note annexe.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class PartnerAccountService {

    private final AccPartnerAccountRepository partners;
    private final AccPartnerInterestRepository interests;
    private final BankAccountRepository bankAccounts;
    private final LoanService loans;
    private final AccountingSettings settings;
    private final CashActorResolver actors;
    private final DataSource dataSource;
    private final jakarta.persistence.EntityManager entityManager;

    public record PartnerRequest(String code, String partnerName, String thirdPartyCode, String accountNumber, Boolean blocked, LocalDate blockedUntil,
                                 BigDecimal rate, BigDecimal admittedRate, BigDecimal cap, LocalDate openedOn) {
    }

    /** {@code type} : CONTRIBUTION, REPAYMENT, EXPENSE, DIVIDEND. */
    public record Movement(String type, LocalDate date, BigDecimal amount, String counterAccount, BigDecimal vatAmount, String vatAccount,
                           String journalCode, String label, String reference) {
    }

    public record StatementLine(LocalDate date, String entryNumber, String label, String reference, BigDecimal debit, BigDecimal credit, BigDecimal balance) {
    }

    public record Statement(AccPartnerAccount partner, LocalDate from, LocalDate to, BigDecimal opening, List<StatementLine> lines, BigDecimal closing) {
    }

    @Transactional(readOnly = true)
    public List<AccPartnerAccount> list() {
        return partners.findAllByOrderByCodeAsc();
    }

    public AccPartnerAccount save(PartnerRequest request) {
        require();
        if (!StringUtils.hasText(request.partnerName()) || !StringUtils.hasText(request.thirdPartyCode())) {
            throw new BadRequestException("Nom de l'associe et code tiers obligatoires");
        }
        String code = StringUtils.hasText(request.code()) ? request.code().trim().toUpperCase(Locale.ROOT) : "CCA-" + (partners.count() + 1);
        AccPartnerAccount partner = partners.findByCode(code).orElseGet(() -> AccPartnerAccount.builder().code(code)
                .createdBy(actors.resolve("SYSTEM")).build());
        partner.setPartnerName(request.partnerName().trim());
        partner.setThirdPartyCode(request.thirdPartyCode().trim());
        partner.setAccountNumber(StringUtils.hasText(request.accountNumber()) ? request.accountNumber().trim()
                : settings.text("accounting.partner.account", "462"));
        partner.setBlocked(Boolean.TRUE.equals(request.blocked()));
        partner.setBlockedUntil(request.blockedUntil());
        partner.setRate(request.rate() == null ? BigDecimal.ZERO : request.rate());
        partner.setAdmittedRate(request.admittedRate());
        partner.setCap(request.cap());
        partner.setOpenedOn(request.openedOn() == null ? LocalDate.now() : request.openedOn());
        return partners.save(partner);
    }

    /** Mouvement du compte courant, ecrit aussitot. */
    public AccEntry move(String code, Movement movement) {
        require();
        AccPartnerAccount partner = partner(code);
        LocalDate date = movement.date() == null ? LocalDate.now() : movement.date();
        BigDecimal amount = LoanService.money(movement.amount());
        if (amount.signum() <= 0) {
            throw new BadRequestException("Le montant est positif");
        }
        String type = movement.type() == null ? "" : movement.type().trim().toUpperCase(Locale.ROOT);
        BigDecimal balance = balance(partner, date);
        String label = StringUtils.hasText(movement.label()) ? movement.label() : switch (type) {
            case "CONTRIBUTION" -> "Apport en compte courant " + partner.getPartnerName();
            case "REPAYMENT" -> "Remboursement compte courant " + partner.getPartnerName();
            case "EXPENSE" -> "Depense payee par " + partner.getPartnerName();
            case "DIVIDEND" -> "Dividendes affectes au compte courant " + partner.getPartnerName();
            default -> throw new BadRequestException("Mouvement inconnu : " + movement.type());
        };
        String reference = StringUtils.hasText(movement.reference()) ? movement.reference() : partner.getCode();
        String tp = partner.getThirdPartyCode();
        String account = partner.getAccountNumber();
        List<AccountingRequests.EntryLine> lines = new ArrayList<>();
        String journal = journal(movement);
        switch (type) {
            case "CONTRIBUTION" -> {
                if (partner.getCap() != null && balance.add(amount).compareTo(partner.getCap()) > 0) {
                    throw new BadRequestException("L'apport porterait le compte courant au-dela du plafond de la convention (" + LoanService.plain(partner.getCap()) + ")");
                }
                lines.add(loans.line(counter(movement), null, label, amount, null, reference, null));
                lines.add(loans.line(account, tp, label, null, amount, reference, null));
            }
            case "REPAYMENT" -> {
                if (Boolean.TRUE.equals(partner.getBlocked()) && (partner.getBlockedUntil() == null || date.isBefore(partner.getBlockedUntil()))) {
                    throw new BadRequestException("Le compte courant est bloque" + (partner.getBlockedUntil() == null ? "" : " jusqu'au " + partner.getBlockedUntil()));
                }
                if (amount.compareTo(balance) > 0) {
                    throw new BadRequestException("Le remboursement depasse le solde du compte courant (" + LoanService.plain(balance) + ")");
                }
                lines.add(loans.line(account, tp, label, amount, null, reference, null));
                lines.add(loans.line(counter(movement), null, label, null, amount, reference, null));
            }
            case "EXPENSE" -> {
                BigDecimal vat = LoanService.money(movement.vatAmount());
                lines.add(loans.line(counter(movement), null, label, amount.subtract(vat), null, reference, null));
                if (vat.signum() > 0) {
                    lines.add(loans.line(StringUtils.hasText(movement.vatAccount()) ? movement.vatAccount() : "4452", null, label + " (TVA)", vat, null, reference, null));
                }
                lines.add(loans.line(account, tp, label, null, amount, reference, null));
            }
            default -> {
                lines.add(loans.line(settings.text("accounting.partner.dividend-account", "465"), tp, label, amount, null, reference, null));
                lines.add(loans.line(account, tp, label, null, amount, reference, null));
            }
        }
        return loans.post(journal, date, label, "PARTNER_" + type, partner.getCode(), lines);
    }

    /**
     * Interets d'une periode : solde crediteur de chaque jour x taux / 365 ; part non deductible
     * au-dela du taux admis fiscalement. D interets ; C compte courant.
     */
    public AccPartnerInterest interest(String code, LocalDate from, LocalDate to) {
        require();
        AccPartnerAccount partner = partner(code);
        if (from == null || to == null || to.isBefore(from)) {
            throw new BadRequestException("Periode invalide");
        }
        if (interests.findByPartnerCodeAndToDate(partner.getCode(), to).isPresent()) {
            throw new BadRequestException("Les interets au " + to + " sont deja calcules");
        }
        long days = ChronoUnit.DAYS.between(from, to) + 1;
        BigDecimal balance = balance(partner, from.minusDays(1));
        Map<LocalDate, BigDecimal> changes = new TreeMap<>();
        jdbc().query("SELECT e.entry_date, SUM(l.credit - l.debit) AS net FROM acc_entry_line l JOIN acc_entry e ON e.id = l.entry_id "
                + "WHERE l.account_number = ? AND l.third_party_code = ? AND e.status IN ('VALIDATED', 'REVERSED') AND e.entry_date BETWEEN ? AND ? "
                + "AND COALESCE(e.source_type, '') <> 'PARTNER_INTEREST' GROUP BY e.entry_date", rs -> {
            changes.put(rs.getDate(1).toLocalDate(), rs.getBigDecimal(2));
        }, partner.getAccountNumber(), partner.getThirdPartyCode(), Date.valueOf(from), Date.valueOf(to));
        BigDecimal sum = BigDecimal.ZERO;
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            balance = balance.add(changes.getOrDefault(day, BigDecimal.ZERO));
            sum = sum.add(balance.max(BigDecimal.ZERO));
        }
        BigDecimal average = sum.divide(BigDecimal.valueOf(days), 0, RoundingMode.HALF_UP);
        BigDecimal rate = partner.getRate();
        BigDecimal interest = sum.multiply(rate).divide(BigDecimal.valueOf(36500), 0, RoundingMode.HALF_UP);
        BigDecimal nonDeductible = BigDecimal.ZERO;
        if (partner.getAdmittedRate() != null && rate.compareTo(partner.getAdmittedRate()) > 0 && rate.signum() > 0) {
            nonDeductible = interest.multiply(rate.subtract(partner.getAdmittedRate())).divide(rate, 0, RoundingMode.HALF_UP);
        }
        String entryNumber = null;
        if (interest.signum() > 0) {
            String label = "Interets compte courant " + partner.getPartnerName() + " du " + from + " au " + to;
            AccEntry entry = loans.post(settings.text("accounting.partner.journal", "OD"), to, label, "PARTNER_INTEREST", partner.getCode() + "@" + to, List.of(
                    loans.line(settings.text("accounting.partner.interest-account", "671"), null, label, interest, null, partner.getCode(), null),
                    loans.line(partner.getAccountNumber(), partner.getThirdPartyCode(), label, null, interest, partner.getCode(), null)));
            entryNumber = entry.getEntryNumber();
        }
        return interests.save(AccPartnerInterest.builder().partnerCode(partner.getCode()).fromDate(from).toDate(to).averageBalance(average).rate(rate)
                .interest(interest).nonDeductible(nonDeductible).entryNumber(entryNumber).createdBy(actors.resolve("SYSTEM")).build());
    }

    @Transactional(readOnly = true)
    public List<AccPartnerInterest> interests(String code) {
        return interests.findAllByPartnerCodeOrderByToDateDesc(partner(code).getCode());
    }

    /** Releve du compte courant, comme un releve de tiers. */
    @Transactional(readOnly = true)
    public Statement statement(String code, LocalDate from, LocalDate to) {
        AccPartnerAccount partner = partner(code);
        LocalDate start = from == null ? partner.getOpenedOn() : from;
        LocalDate end = to == null ? LocalDate.now() : to;
        BigDecimal opening = balance(partner, start.minusDays(1));
        List<StatementLine> lines = new ArrayList<>();
        BigDecimal[] running = {opening};
        jdbc().query("SELECT e.entry_date, e.entry_number, l.label, l.reference, l.debit, l.credit FROM acc_entry_line l JOIN acc_entry e ON e.id = l.entry_id "
                + "WHERE l.account_number = ? AND l.third_party_code = ? AND e.status IN ('VALIDATED', 'REVERSED') AND e.entry_date BETWEEN ? AND ? "
                + "ORDER BY e.entry_date, e.id, l.line_no", rs -> {
            BigDecimal debit = rs.getBigDecimal("debit");
            BigDecimal credit = rs.getBigDecimal("credit");
            running[0] = running[0].add(credit).subtract(debit);
            lines.add(new StatementLine(rs.getDate("entry_date").toLocalDate(), rs.getString("entry_number"), rs.getString("label"), rs.getString("reference"),
                    debit, credit, running[0]));
        }, partner.getAccountNumber(), partner.getThirdPartyCode(), Date.valueOf(start), Date.valueOf(end));
        return new Statement(partner, start, end, opening, lines, running[0]);
    }

    public record NoteLine(String code, String partnerName, String account, BigDecimal balance, BigDecimal rate, boolean blocked, LocalDate blockedUntil,
                           BigDecimal interestOfYear, BigDecimal nonDeductibleOfYear) {
    }

    /** Note annexe : comptes courants par associe a une date. */
    @Transactional(readOnly = true)
    public List<NoteLine> note(LocalDate date) {
        LocalDate day = date == null ? LocalDate.now() : date;
        List<NoteLine> result = new ArrayList<>();
        for (AccPartnerAccount partner : partners.findAllByOrderByCodeAsc()) {
            List<AccPartnerInterest> year = interests.findAllByPartnerCodeOrderByToDateDesc(partner.getCode()).stream()
                    .filter(item -> item.getToDate().getYear() == day.getYear() && !item.getToDate().isAfter(day)).toList();
            result.add(new NoteLine(partner.getCode(), partner.getPartnerName(), partner.getAccountNumber(), balance(partner, day), partner.getRate(),
                    Boolean.TRUE.equals(partner.getBlocked()), partner.getBlockedUntil(),
                    year.stream().map(AccPartnerInterest::getInterest).reduce(BigDecimal.ZERO, BigDecimal::add),
                    year.stream().map(AccPartnerInterest::getNonDeductible).reduce(BigDecimal.ZERO, BigDecimal::add)));
        }
        return result;
    }

    /** Solde crediteur (du a l'associe) a une date incluse. */
    BigDecimal balance(AccPartnerAccount partner, LocalDate date) {
        BigDecimal value = jdbc().queryForObject("SELECT COALESCE(SUM(l.credit - l.debit), 0) FROM acc_entry_line l JOIN acc_entry e ON e.id = l.entry_id "
                + "WHERE l.account_number = ? AND l.third_party_code = ? AND e.status IN ('VALIDATED', 'REVERSED') AND e.entry_date <= ?", BigDecimal.class,
                partner.getAccountNumber(), partner.getThirdPartyCode(), Date.valueOf(date));
        return value == null ? BigDecimal.ZERO : value;
    }

    private String journal(Movement movement) {
        if (StringUtils.hasText(movement.journalCode())) {
            return movement.journalCode().trim();
        }
        if (StringUtils.hasText(movement.counterAccount())) {
            return bankAccounts.findAllByOrderByCodeAsc().stream().filter(bank -> movement.counterAccount().trim().equals(bank.getAccountNumber()))
                    .map(bank -> bank.getJournalCode()).findFirst().orElse(settings.text("accounting.partner.journal", "OD"));
        }
        return settings.text("accounting.partner.journal", "OD");
    }

    private static String counter(Movement movement) {
        if (!StringUtils.hasText(movement.counterAccount())) {
            throw new BadRequestException("Compte de contrepartie obligatoire (tresorerie, ou charge pour une depense)");
        }
        return movement.counterAccount().trim();
    }

    private AccPartnerAccount partner(String code) {
        return partners.findByCode(code).orElseThrow(() -> new ResourceNotFoundException("Compte courant introuvable : " + code));
    }

    private void require() {
        if (!actors.hasAuthority(LoanService.MANAGE)) {
            throw new ForbiddenException("Action reservee : " + LoanService.MANAGE);
        }
    }

    private JdbcTemplate jdbc() {
        // Les ecritures de la transaction en cours doivent etre visibles des requetes JDBC.
        entityManager.flush();
        return new JdbcTemplate(dataSource);
    }

    static boolean posted(EntryStatus status) {
        return status == EntryStatus.VALIDATED || status == EntryStatus.REVERSED;
    }
}
