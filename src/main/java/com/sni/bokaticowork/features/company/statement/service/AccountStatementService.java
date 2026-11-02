package com.sni.bokaticowork.features.company.statement.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.billing.service.support.BillingReceivables;
import com.sni.bokaticowork.features.client.customer.enums.CustomerStatus;
import com.sni.bokaticowork.features.client.customer.enums.CustomerType;
import com.sni.bokaticowork.features.client.customer.model.Customer;
import com.sni.bokaticowork.features.client.customer.repository.CustomerRepository;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.client.member.repository.repo.MemberRepository;
import com.sni.bokaticowork.features.company.statement.dto.StatementDtos;
import com.sni.bokaticowork.features.company.statement.dto.StatementDtos.AccountType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Les etats de compte · d'une organisation (ses documents et ceux de ses membres qui lui sont
 * relies) et d'un membre (ses factures, separees entre son compte et chaque organisation).
 *
 * <p>La periode choisit ce qui a ete facture ; ce qui reste du se lit a la date de l'etat, quelle
 * que soit la date d'emission. Un etat de septembre montre donc aussi la facture de mars encore
 * impayee, marquee hors periode, sans quoi le solde affiche mentirait.</p>
 */
@Service
@Transactional(readOnly = true)
public class AccountStatementService {

    /** Au-dela, l'etat devient un export comptable · ce n'est pas son role. */
    static final long MAX_PERIOD_DAYS = 3L * 366;

    private static final Set<BillingDocumentType> STATEMENT_TYPES = EnumSet.of(
            BillingDocumentType.INVOICE, BillingDocumentType.PROFORMA_INVOICE, BillingDocumentType.CREDIT_NOTE);

    private final BillingDocumentRepository documentRepository;
    private final CustomerRepository customerRepository;
    private final MemberRepository memberRepository;
    private final Clock clock;

    @Autowired
    public AccountStatementService(BillingDocumentRepository documentRepository, CustomerRepository customerRepository,
                                   MemberRepository memberRepository) {
        this(documentRepository, customerRepository, memberRepository, Clock.systemDefaultZone());
    }

    AccountStatementService(BillingDocumentRepository documentRepository, CustomerRepository customerRepository,
                            MemberRepository memberRepository, Clock clock) {
        this.documentRepository = documentRepository;
        this.customerRepository = customerRepository;
        this.memberRepository = memberRepository;
        this.clock = clock;
    }

    public StatementDtos.Statement company(String customerId, LocalDate from, LocalDate to) {
        Customer company = customerRepository.findByCustomerId(customerId == null ? "" : customerId.trim())
                .filter(c -> !c.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Organisation introuvable : " + customerId));
        if (company.getType() != CustomerType.COMPANY) {
            throw new BadRequestException("Le client " + customerId + " n'est pas une organisation.");
        }
        return company(company, from, to);
    }

    /** Pour qui a deja verifie l'organisation · le portail du gerant. */
    public StatementDtos.Statement company(Customer company, LocalDate from, LocalDate to) {
        Period period = period(from, to);
        String code = company.getCustomerId();
        List<BillingDocument> documents = new ArrayList<>(documentRepository.findCompanyStatementDocuments(code,
                period.from(), period.to(), STATEMENT_TYPES, BillingReceivables.RECEIVABLE_TYPES, closedStatuses()));
        // Un avoir sur une facture de membre est au nom du membre et ne porte pas l'organisation ·
        // on le retrouve par la facture qu'il corrige.
        addMemberCreditNotes(documents, code, period);

        List<StatementDtos.Line> lines = documents.stream().map(d -> line(d, period, accountOfCompanyLine(d, code))).toList();
        return new StatementDtos.Statement(StatementDtos.Subject.COMPANY, code, company.getCompanyName(),
                company.getBillingEmail() != null ? company.getBillingEmail() : company.getEmail(),
                period.from(), period.to(), period.asOf(), company.getCreditLimit(), company.getPaymentTermsDays(),
                company.getStatus() == CustomerStatus.SUSPENDED, company.getBlockedReason(),
                totals(lines), accounts(lines), lines);
    }

    public StatementDtos.Statement member(String memberId, LocalDate from, LocalDate to) {
        Member member = memberRepository.findByMemberIdIncludingArchived(memberId == null ? "" : memberId.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Membre introuvable : " + memberId));
        return member(member, from, to);
    }

    public StatementDtos.Statement member(Member member, LocalDate from, LocalDate to) {
        Period period = period(from, to);
        List<BillingDocument> documents = documentRepository.findMemberStatementDocuments(member.getMemberId(),
                period.from(), period.to(), STATEMENT_TYPES, BillingReceivables.RECEIVABLE_TYPES, closedStatuses());
        // Un avoir prend le compte de la facture qu'il corrige · sinon il tomberait toujours dans
        // le compte personnel, meme pour une facture reliee a une organisation.
        Map<String, BillingDocument> byNumber = new java.util.HashMap<>();
        documents.forEach(d -> byNumber.put(d.getDocumentNumber(), d));
        List<StatementDtos.Line> lines = documents.stream()
                .map(d -> line(d, period, accountOfMemberLine(d, byNumber)))
                .toList();
        return new StatementDtos.Statement(StatementDtos.Subject.MEMBER, member.getMemberId(), fullName(member),
                member.getEmail(), period.from(), period.to(), period.asOf(), null, null, false, null,
                totals(lines), accounts(lines), lines);
    }

    // ── Comptes ──────────────────────────────────────────────────────────────

    private record Account(AccountType type, String code, String name) {
    }

    private static Account accountOfCompanyLine(BillingDocument d, String companyCode) {
        if ("CUSTOMER".equalsIgnoreCase(d.getCustomerType()) && companyCode.equalsIgnoreCase(d.getCustomerCode())) {
            return new Account(AccountType.COMPANY, d.getCustomerCode(), d.getCustomerName());
        }
        return new Account(AccountType.MEMBER, d.getCustomerCode(), d.getCustomerName());
    }

    private static Account accountOfMemberLine(BillingDocument d, Map<String, BillingDocument> byNumber) {
        BillingDocument carrier = d;
        if (d.getDocumentType() == BillingDocumentType.CREDIT_NOTE && "INVOICE".equalsIgnoreCase(d.getSourceType())
                && d.getSourceCode() != null && byNumber.containsKey(d.getSourceCode())) {
            carrier = byNumber.get(d.getSourceCode());
        }
        if (StringUtils.hasText(carrier.getCompanyCustomerCode())) {
            String name = StringUtils.hasText(carrier.getCompanyName()) ? carrier.getCompanyName() : carrier.getCompanyCustomerCode();
            return new Account(AccountType.COMPANY, carrier.getCompanyCustomerCode(), name);
        }
        return new Account(AccountType.PERSONAL, d.getCustomerCode(), d.getCustomerName());
    }

    private void addMemberCreditNotes(List<BillingDocument> documents, String companyCode, Period period) {
        Set<String> memberInvoices = new HashSet<>();
        Set<String> present = new HashSet<>();
        for (BillingDocument d : documents) {
            present.add(d.getDocumentNumber());
            if (companyCode.equalsIgnoreCase(d.getCompanyCustomerCode())
                    && BillingReceivables.RECEIVABLE_TYPES.contains(d.getDocumentType())) {
                memberInvoices.add(d.getDocumentNumber());
            }
        }
        if (memberInvoices.isEmpty()) {
            return;
        }
        for (BillingDocument credit : documentRepository.findCreditNotesForInvoices(memberInvoices)) {
            boolean relevant = inPeriod(credit, period) || BillingReceivables.creditAvailable(credit.getDocumentType(), credit.getStatus());
            if (relevant && present.add(credit.getDocumentNumber())) {
                documents.add(credit);
            }
        }
        documents.sort(Comparator.comparing(BillingDocument::getIssueDate, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(BillingDocument::getId, Comparator.nullsLast(Comparator.naturalOrder())));
    }

    // ── Lignes et totaux ─────────────────────────────────────────────────────

    private static StatementDtos.Line line(BillingDocument d, Period period, Account account) {
        BigDecimal impact = BillingReceivables.customerImpact(d.getDocumentType(), d.getStatus(), d.getBalanceDue(), d.getTotalAmount());
        return new StatementDtos.Line(d.getDocumentNumber(),
                d.getDocumentType() == null ? null : d.getDocumentType().name(),
                d.getStatus() == null ? null : d.getStatus().name(),
                d.getIssueDate(), d.getDueDate(), d.getCurrency(),
                account.type(), account.code(), account.name(),
                d.getCompanyLinkSource(), d.getCompanyCustomerCode(), d.getCompanyName(),
                zero(d.getTotalAmount()), zero(d.getPaidAmount()), zero(d.getBalanceDue()), impact,
                daysOverdue(d, period.asOf()), inPeriod(d, period));
    }

    private static long daysOverdue(BillingDocument d, LocalDate asOf) {
        if (!BillingReceivables.receivable(d.getDocumentType(), d.getStatus()) || d.getDueDate() == null
                || zero(d.getBalanceDue()).signum() <= 0 || !d.getDueDate().isBefore(asOf)) {
            return 0;
        }
        return ChronoUnit.DAYS.between(d.getDueDate(), asOf);
    }

    private static boolean inPeriod(BillingDocument d, Period period) {
        return d.getIssueDate() != null && !d.getIssueDate().isBefore(period.from()) && !d.getIssueDate().isAfter(period.to());
    }

    /** Les totaux par devise · une devise de plus ne s'additionne pas avec la premiere. */
    public static List<StatementDtos.Totals> totals(List<StatementDtos.Line> lines) {
        Map<String, List<StatementDtos.Line>> byCurrency = new TreeMap<>();
        for (StatementDtos.Line line : lines) {
            byCurrency.computeIfAbsent(currency(line), k -> new ArrayList<>()).add(line);
        }
        List<StatementDtos.Totals> totals = new ArrayList<>();
        byCurrency.forEach((currency, group) -> {
            BigDecimal invoiced = BigDecimal.ZERO, paid = BigDecimal.ZERO, due = BigDecimal.ZERO, overdue = BigDecimal.ZERO, credit = BigDecimal.ZERO;
            BigDecimal notDue = BigDecimal.ZERO, d30 = BigDecimal.ZERO, d60 = BigDecimal.ZERO, d90 = BigDecimal.ZERO, over = BigDecimal.ZERO;
            int open = 0, late = 0;
            for (StatementDtos.Line line : group) {
                BillingDocumentType type = BillingDocumentType.valueOf(line.documentType());
                var status = com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus.valueOf(line.status());
                if (line.inPeriod() && BillingReceivables.issued(type, status)) {
                    invoiced = invoiced.add(line.totalAmount());
                    paid = paid.add(line.paidAmount());
                }
                if (BillingReceivables.creditAvailable(type, status)) {
                    credit = credit.add(line.totalAmount());
                }
                if (BillingReceivables.receivable(type, status) && line.balanceDue().signum() > 0) {
                    open++;
                    due = due.add(line.balanceDue());
                    long days = line.daysOverdue();
                    if (days > 0) {
                        late++;
                        overdue = overdue.add(line.balanceDue());
                    }
                    if (days <= 0) notDue = notDue.add(line.balanceDue());
                    else if (days <= 30) d30 = d30.add(line.balanceDue());
                    else if (days <= 60) d60 = d60.add(line.balanceDue());
                    else if (days <= 90) d90 = d90.add(line.balanceDue());
                    else over = over.add(line.balanceDue());
                }
            }
            totals.add(new StatementDtos.Totals(currency, invoiced, paid, due, overdue, credit,
                    due.subtract(credit).max(BigDecimal.ZERO), open, late,
                    new StatementDtos.Aging(notDue, d30, d60, d90, over)));
        });
        return totals;
    }

    /** Un bloc par compte et par devise · l'organisation d'abord, puis par solde du decroissant. */
    public static List<StatementDtos.AccountBreakdown> accounts(List<StatementDtos.Line> lines) {
        Map<String, List<StatementDtos.Line>> groups = new LinkedHashMap<>();
        for (StatementDtos.Line line : lines) {
            groups.computeIfAbsent(line.accountType() + "|" + line.accountCode() + "|" + currency(line), k -> new ArrayList<>()).add(line);
        }
        List<StatementDtos.AccountBreakdown> accounts = new ArrayList<>();
        for (List<StatementDtos.Line> group : groups.values()) {
            StatementDtos.Line first = group.get(0);
            StatementDtos.Totals t = totals(group).get(0);
            accounts.add(new StatementDtos.AccountBreakdown(first.accountType(), first.accountCode(), first.accountName(),
                    t.currency(), t.invoiced(), t.paid(), t.balanceDue(), t.overdue(), group.size()));
        }
        accounts.sort(Comparator.comparing((StatementDtos.AccountBreakdown a) -> a.accountType() == AccountType.MEMBER ? 1 : 0)
                .thenComparing(StatementDtos.AccountBreakdown::balanceDue, Comparator.reverseOrder()));
        return accounts;
    }

    // ── Periode ──────────────────────────────────────────────────────────────

    record Period(LocalDate from, LocalDate to, LocalDate asOf) {
    }

    /** Douze mois jusqu'a aujourd'hui par defaut · une periode a l'envers ou trop longue est refusee. */
    Period period(LocalDate from, LocalDate to) {
        LocalDate today = LocalDate.now(clock);
        LocalDate end = to == null ? today : to;
        LocalDate start = from == null ? end.minusYears(1).plusDays(1) : from;
        if (end.isBefore(start)) {
            throw new BadRequestException("La période se termine avant de commencer.");
        }
        if (ChronoUnit.DAYS.between(start, end) > MAX_PERIOD_DAYS) {
            throw new BadRequestException("Période trop longue : trois ans au plus par état de compte.");
        }
        return new Period(start, end, today);
    }

    private static java.util.Collection<com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus> closedStatuses() {
        Set<com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus> closed = EnumSet.noneOf(
                com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus.class);
        closed.addAll(BillingReceivables.NOT_ISSUED);
        closed.addAll(BillingReceivables.SETTLED);
        return closed;
    }

    private static String currency(StatementDtos.Line line) {
        return StringUtils.hasText(line.currency()) ? line.currency().trim().toUpperCase(java.util.Locale.ROOT) : "XAF";
    }

    private static BigDecimal zero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static String fullName(Member member) {
        String name = ((member.getFirstname() == null ? "" : member.getFirstname()) + " "
                + (member.getLastname() == null ? "" : member.getLastname())).trim();
        return name.isEmpty() ? member.getMemberId() : name;
    }
}
