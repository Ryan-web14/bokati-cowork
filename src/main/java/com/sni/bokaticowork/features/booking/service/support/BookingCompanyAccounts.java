package com.sni.bokaticowork.features.booking.service.support;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.core.reference.service.SessionActor;
import com.sni.bokaticowork.core.utils.time.AppTime;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentResponse;
import com.sni.bokaticowork.features.booking.model.Booking;
import com.sni.bokaticowork.features.booking.model.BookingCompanyTerms;
import com.sni.bokaticowork.features.booking.model.BookingMemberBudget;
import com.sni.bokaticowork.features.booking.repository.BookingCompanyTermsRepository;
import com.sni.bokaticowork.features.booking.repository.BookingMemberBudgetRepository;
import com.sni.bokaticowork.features.subscription.repository.BillableItemRepository;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.subscription.model.BillableItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Lot B9e : reservations facturees a une entreprise (billingCompanyCode).
 *
 * <ul>
 *   <li><b>Budget</b> mensuel de l'entreprise et, s'il est pose, de chaque collaborateur : une
 *   reservation qui le depasse est refusee ({@code 409 COMPANY_BUDGET_EXCEEDED}).</li>
 *   <li><b>Forfait d'heures</b> mensuel partage : les minutes restantes couvrent la reservation, le
 *   reste est facture au prorata (meme mecanique que la couverture partielle du lot B2).</li>
 *   <li><b>Releve mensuel groupe</b> : la reservation est confirmee sans attendre de paiement, son
 *   element a facturer reste en attente, et une seule facture par entreprise reprend le mois.</li>
 * </ul>
 */
@Slf4j
@Component
public class BookingCompanyAccounts {

    private static final String ACTIVE = "status NOT IN ('CANCELLED', 'REJECTED', 'EXPIRED', 'DRAFT') AND deleted = false";

    public record Terms(String businessCode, boolean monthlyStatement, BigDecimal monthlyBudget, Integer monthlyHours, String updatedBy) {
    }

    public record MemberBudget(String businessCode, String memberId, BigDecimal monthlyBudget) {
    }

    public record Usage(String businessCode, YearMonth month, BigDecimal spent, BigDecimal monthlyBudget, long coveredMinutes,
                        Integer monthlyHours, long remainingMinutes, List<MemberUsage> members) {
    }

    public record MemberUsage(String memberId, BigDecimal spent, BigDecimal monthlyBudget) {
    }

    public record Statement(String businessCode, YearMonth month, int items, String documentNumber) {
    }

    private final BookingCompanyTermsRepository terms;
    private final BookingMemberBudgetRepository memberBudgets;
    private final JdbcTemplate jdbc;
    private final ObjectProvider<BillableItemRepository> billableItems;
    private final ObjectProvider<BookingBillableBridge> billableBridge;

    private SessionActor actor;

    public BookingCompanyAccounts(BookingCompanyTermsRepository terms, BookingMemberBudgetRepository memberBudgets, JdbcTemplate jdbc,
                                  ObjectProvider<BillableItemRepository> billableItems, ObjectProvider<BookingBillableBridge> billableBridge) {
        this.terms = terms;
        this.memberBudgets = memberBudgets;
        this.jdbc = jdbc;
        this.billableItems = billableItems;
        this.billableBridge = billableBridge;
    }

    @Autowired(required = false)
    void setActor(SessionActor actor) {
        this.actor = actor;
    }

    // ------------------------------------------------------------------ administration

    @Transactional(readOnly = true)
    public Terms terms(String businessCode) {
        return terms.findByBusinessCode(code(businessCode)).map(BookingCompanyAccounts::view)
                .orElse(new Terms(code(businessCode), false, null, null, null));
    }

    @Transactional
    public Terms saveTerms(String businessCode, Boolean monthlyStatement, BigDecimal monthlyBudget, Integer monthlyHours) {
        if (monthlyBudget != null && monthlyBudget.signum() < 0) {
            throw new BadRequestException("Le budget ne peut pas être négatif");
        }
        if (monthlyHours != null && monthlyHours < 0) {
            throw new BadRequestException("Le forfait d'heures ne peut pas être négatif");
        }
        BookingCompanyTerms entity = terms.findByBusinessCode(code(businessCode))
                .orElseGet(() -> BookingCompanyTerms.builder().businessCode(code(businessCode)).build());
        entity.setMonthlyStatement(Boolean.TRUE.equals(monthlyStatement));
        entity.setMonthlyBudget(monthlyBudget == null || monthlyBudget.signum() == 0 ? null : monthlyBudget);
        entity.setMonthlyHours(monthlyHours == null || monthlyHours == 0 ? null : monthlyHours);
        entity.setUpdatedBy(actor == null ? null : actor.resolve(null));
        return view(terms.save(entity));
    }

    @Transactional(readOnly = true)
    public List<MemberBudget> memberBudgets(String businessCode) {
        return memberBudgets.findByBusinessCodeOrderByMemberIdAsc(code(businessCode)).stream()
                .map(budget -> new MemberBudget(budget.getBusinessCode(), budget.getMemberId(), budget.getMonthlyBudget())).toList();
    }

    /** Pose ou retire (montant nul ou zero) le budget mensuel d'un collaborateur. */
    @Transactional
    public Optional<MemberBudget> setMemberBudget(String businessCode, String memberId, BigDecimal monthlyBudget) {
        Optional<BookingMemberBudget> existing = memberBudgets.findByBusinessCodeAndMemberId(code(businessCode), memberId.trim());
        if (monthlyBudget == null || monthlyBudget.signum() <= 0) {
            existing.ifPresent(memberBudgets::delete);
            return Optional.empty();
        }
        BookingMemberBudget entity = existing.orElseGet(() -> BookingMemberBudget.builder().businessCode(code(businessCode)).memberId(memberId.trim()).build());
        entity.setMonthlyBudget(monthlyBudget);
        entity.setUpdatedBy(actor == null ? null : actor.resolve(null));
        BookingMemberBudget saved = memberBudgets.save(entity);
        return Optional.of(new MemberBudget(saved.getBusinessCode(), saved.getMemberId(), saved.getMonthlyBudget()));
    }

    @Transactional(readOnly = true)
    public Usage usage(String businessCode, YearMonth month) {
        String code = code(businessCode);
        YearMonth m = month == null ? YearMonth.from(AppTime.today()) : month;
        Terms t = terms(code);
        BigDecimal spent = spent(code, null, m);
        long covered = coveredMinutes(code, m);
        long remaining = t.monthlyHours() == null ? 0 : Math.max(0, t.monthlyHours() * 60L - covered);
        List<MemberUsage> members = new ArrayList<>();
        for (MemberBudget budget : memberBudgets(code)) {
            members.add(new MemberUsage(budget.memberId(), spent(code, budget.memberId(), m), budget.monthlyBudget()));
        }
        return new Usage(code, m, spent, t.monthlyBudget(), covered, t.monthlyHours(), remaining, members);
    }

    // ------------------------------------------------------------------ a la reservation

    /**
     * Applique les conditions de l'entreprise a une reservation qui lui est facturee : forfait
     * d'heures, budgets, releve mensuel. Sans conditions, rien ne change.
     */
    public void applyAtCreation(Booking booking) {
        if (!StringUtils.hasText(booking.getBillingCompanyCode()) || "NONE".equalsIgnoreCase(booking.getBillingCompanyCode())) {
            return;
        }
        Optional<BookingCompanyTerms> found = terms.findByBusinessCode(booking.getBillingCompanyCode());
        String code = booking.getBillingCompanyCode();
        YearMonth month = YearMonth.from(booking.getStartedAt());
        BigDecimal total = booking.getTotalAmount() == null ? BigDecimal.ZERO : booking.getTotalAmount();
        if (found.isPresent() && found.get().getMonthlyHours() != null && found.get().getMonthlyHours() > 0
                && booking.getDurationMinutes() != null && booking.getDurationMinutes() > 0 && total.signum() > 0) {
            long remaining = Math.max(0, found.get().getMonthlyHours() * 60L - coveredMinutes(code, month));
            int covered = (int) Math.min(remaining, booking.getDurationMinutes());
            if (covered > 0) {
                booking.setCompanyCoveredMinutes(covered);
                BigDecimal left = total.multiply(BigDecimal.valueOf(booking.getDurationMinutes() - covered))
                        .divide(BigDecimal.valueOf(booking.getDurationMinutes()), 2, RoundingMode.HALF_UP);
                booking.setTotalAmount(left);
                total = left;
            }
        }
        if (found.isPresent() && found.get().getMonthlyBudget() != null) {
            BigDecimal after = spent(code, null, month).add(total);
            if (after.compareTo(found.get().getMonthlyBudget()) > 0) {
                throw ConflictException.coded("COMPANY_BUDGET_EXCEEDED", "Budget mensuel de l'entreprise dépassé : "
                        + plain(after) + " pour " + plain(found.get().getMonthlyBudget()) + " " + currency(booking));
            }
        }
        if (booking.getOwnerType() == SubscriberType.MEMBER) {
            Optional<BookingMemberBudget> memberBudget = memberBudgets.findByBusinessCodeAndMemberId(code, booking.getOwnerCode());
            if (memberBudget.isPresent()) {
                BigDecimal after = spent(code, booking.getOwnerCode(), month).add(total);
                if (after.compareTo(memberBudget.get().getMonthlyBudget()) > 0) {
                    throw ConflictException.coded("COMPANY_BUDGET_EXCEEDED", "Budget mensuel du collaborateur dépassé : "
                            + plain(after) + " pour " + plain(memberBudget.get().getMonthlyBudget()) + " " + currency(booking));
                }
            }
        }
        booking.setMonthlyStatement(found.isPresent() && Boolean.TRUE.equals(found.get().getMonthlyStatement()));
    }

    // ------------------------------------------------------------------ releve mensuel

    /**
     * Une facture par entreprise a releve mensuel, avec les elements en attente des reservations du
     * mois. Une entreprise deja facturee pour ce mois est sautee.
     */
    @Transactional
    public List<Statement> runStatements(YearMonth month) {
        YearMonth m = month == null ? YearMonth.from(AppTime.today()).minusMonths(1) : month;
        List<Statement> done = new ArrayList<>();
        for (BookingCompanyTerms company : terms.findByMonthlyStatementTrue()) {
            try {
                done.add(statement(company.getBusinessCode(), m));
            } catch (RuntimeException ex) {
                log.error("Releve mensuel de {} pour {} en echec : {}", company.getBusinessCode(), m, ex.getMessage(), ex);
            }
        }
        return done;
    }

    @Transactional
    public Statement statement(String businessCode, YearMonth month) {
        String code = code(businessCode);
        Integer existing = jdbc.queryForObject("SELECT COUNT(*) FROM booking_company_statement WHERE business_code = ? AND statement_month = ?",
                Integer.class, code, month.toString());
        if (existing != null && existing > 0) {
            String number = jdbc.queryForObject("SELECT document_number FROM booking_company_statement WHERE business_code = ? AND statement_month = ?",
                    String.class, code, month.toString());
            return new Statement(code, month, 0, number);
        }
        List<String> numbers = jdbc.queryForList("""
                SELECT billable_number FROM billable_item
                WHERE source_type LIKE 'BOOKING%' AND billing_company_code = ? AND status = 'PENDING' AND invoice_id IS NULL
                  AND billing_period_start >= ? AND billing_period_start < ?
                ORDER BY billing_period_start, billable_number
                """, String.class, code, java.sql.Date.valueOf(month.atDay(1)), java.sql.Date.valueOf(month.plusMonths(1).atDay(1)));
        if (numbers.isEmpty()) {
            return new Statement(code, month, 0, null);
        }
        BillableItemRepository repository = billableItems.getObject();
        List<BillableItem> items = numbers.stream().map(number -> repository.findByBillableNumber(number).orElse(null))
                .filter(java.util.Objects::nonNull).toList();
        BillingDocumentResponse invoice = billableBridge.getObject().invoiceGroup(items, "Relevé des réservations " + month,
                "Réservations du mois " + month + " facturées à " + code);
        String documentNumber = invoice == null ? null : invoice.documentNumber();
        jdbc.update("INSERT INTO booking_company_statement (business_code, statement_month, document_number, item_count, created_at) VALUES (?, ?, ?, ?, ?)",
                code, month.toString(), documentNumber, items.size(), Timestamp.from(java.time.Instant.now()));
        return new Statement(code, month, items.size(), documentNumber);
    }

    // ------------------------------------------------------------------ calculs

    private BigDecimal spent(String businessCode, String memberId, YearMonth month) {
        LocalDateTime from = month.atDay(1).atStartOfDay();
        LocalDateTime to = month.plusMonths(1).atDay(1).atStartOfDay();
        BigDecimal value = memberId == null
                ? jdbc.queryForObject("SELECT COALESCE(SUM(total_amount), 0) FROM booking WHERE billing_company_code = ? AND " + ACTIVE
                + " AND started_at >= ? AND started_at < ?", BigDecimal.class, businessCode, Timestamp.valueOf(from), Timestamp.valueOf(to))
                : jdbc.queryForObject("SELECT COALESCE(SUM(total_amount), 0) FROM booking WHERE billing_company_code = ? AND owner_type = 'MEMBER' AND owner_code = ? AND "
                + ACTIVE + " AND started_at >= ? AND started_at < ?", BigDecimal.class, businessCode, memberId, Timestamp.valueOf(from), Timestamp.valueOf(to));
        return value == null ? BigDecimal.ZERO : value;
    }

    private long coveredMinutes(String businessCode, YearMonth month) {
        Long value = jdbc.queryForObject("SELECT COALESCE(SUM(company_covered_minutes), 0) FROM booking WHERE billing_company_code = ? AND " + ACTIVE
                        + " AND started_at >= ? AND started_at < ?", Long.class, businessCode,
                Timestamp.valueOf(month.atDay(1).atStartOfDay()), Timestamp.valueOf(month.plusMonths(1).atDay(1).atStartOfDay()));
        return value == null ? 0 : value;
    }

    private static Terms view(BookingCompanyTerms entity) {
        return new Terms(entity.getBusinessCode(), Boolean.TRUE.equals(entity.getMonthlyStatement()), entity.getMonthlyBudget(),
                entity.getMonthlyHours(), entity.getUpdatedBy());
    }

    private static String code(String businessCode) {
        if (!StringUtils.hasText(businessCode)) {
            throw new BadRequestException("Le code de l'entreprise est requis");
        }
        return businessCode.trim();
    }

    private static String plain(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    private static String currency(Booking booking) {
        return booking.getCurrency() == null ? "XAF" : booking.getCurrency();
    }
}
