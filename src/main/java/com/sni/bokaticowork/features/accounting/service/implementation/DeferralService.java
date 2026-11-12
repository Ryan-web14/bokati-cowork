package com.sni.bokaticowork.features.accounting.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ForbiddenException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.accounting.dto.AccountingRequests;
import com.sni.bokaticowork.features.accounting.model.AccAccrual;
import com.sni.bokaticowork.features.accounting.model.AccDeferralRun;
import com.sni.bokaticowork.features.accounting.model.AccDeferralRunLine;
import com.sni.bokaticowork.features.accounting.model.AccDeferralSchedule;
import com.sni.bokaticowork.features.accounting.model.AccDeferralScheduleEvent;
import com.sni.bokaticowork.features.accounting.model.AccEntry;
import com.sni.bokaticowork.features.accounting.model.AccSupplierInvoice;
import com.sni.bokaticowork.features.accounting.model.AccSupplierInvoiceLine;
import com.sni.bokaticowork.features.accounting.model.AccUsageConsumption;
import com.sni.bokaticowork.features.accounting.repository.AccAccrualRepository;
import com.sni.bokaticowork.features.accounting.repository.AccDeferralRunLineRepository;
import com.sni.bokaticowork.features.accounting.repository.AccDeferralRunRepository;
import com.sni.bokaticowork.features.accounting.repository.AccDeferralScheduleEventRepository;
import com.sni.bokaticowork.features.accounting.repository.AccDeferralScheduleRepository;
import com.sni.bokaticowork.features.accounting.repository.AccSupplierInvoiceLineRepository;
import com.sni.bokaticowork.features.accounting.repository.AccSupplierInvoiceRepository;
import com.sni.bokaticowork.features.accounting.repository.AccUsageConsumptionRepository;
import com.sni.bokaticowork.features.accounting.service.support.AccountingSettings;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.model.BillingDocumentLine;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentLineRepository;
import com.sni.bokaticowork.features.booking.repository.BookingRepository;
import com.sni.bokaticowork.features.payment.repository.CashOperationNatureRepository;
import com.sni.bokaticowork.features.payment.service.support.CashActorResolver;
import com.sni.bokaticowork.features.subscription.repository.BillableItemRepository;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Constatations d'avance et rattachement (lot F20) : periode de service des lignes, echeancier
 * par ligne, traitement de fin de periode par ajustement du solde (relancer ne change rien),
 * evenements (gel, resiliation, annulation, report, expiration), consommation des pass a credits,
 * factures a etablir et non parvenues avec extourne.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeferralService implements com.sni.bokaticowork.core.accounting.DeferralEvents {

    public static final String READ = "ACCOUNTING_DEFERRAL:READ";
    public static final String RUN = "ACCOUNTING_DEFERRAL:RUN";
    public static final String EDIT_PERIOD = "ACCOUNTING_DEFERRAL:EDIT_PERIOD";
    public static final String ACCRUAL = "ACCOUNTING_ACCRUAL:MANAGE";
    public static final String REVENUE = "REVENUE";
    public static final String EXPENSE = "EXPENSE";
    public static final String BILLING_LINE = "BILLING_LINE";
    public static final String SUPPLIER_LINE = "SUPPLIER_INVOICE_LINE";
    private static final ZoneId ZONE = ZoneId.of("Africa/Brazzaville");

    private final AccDeferralScheduleRepository schedules;
    private final AccDeferralScheduleEventRepository events;
    private final AccUsageConsumptionRepository consumptions;
    private final AccDeferralRunRepository runs;
    private final AccDeferralRunLineRepository runLines;
    private final AccAccrualRepository accruals;
    private final BillingDocumentLineRepository billingLines;
    private final AccSupplierInvoiceRepository supplierInvoices;
    private final AccSupplierInvoiceLineRepository supplierLines;
    private final CashOperationNatureRepository natures;
    private final BillableItemRepository billables;
    private final BookingRepository bookings;
    private final SalesAccountingService sales;
    private final ReceivableRiskService poster;
    private final AccountingSettings settings;
    private final CashActorResolver actors;
    private final DataSource dataSource;
    private final EntityManager entityManager;

    // ================================================================== echeanciers

    /**
     * Cree les echeanciers des lignes de facture et de charge qui n'en ont pas encore : periode lue
     * sur la ligne, sinon derivee du module d'origine (element facturable, reservation, ligne corrigee
     * pour un avoir). Une ligne sans periode est marquee {@code POINT_IN_TIME} et n'est plus relue.
     */
    @Transactional
    public int sync() {
        entityManager.flush();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        LocalDate start = settings.startDate() == null ? LocalDate.of(2026, 1, 1) : settings.startDate();
        List<Long> ids = jdbc.queryForList("SELECT l.id FROM billing_document_line l JOIN billing_document d ON d.id = l.document_id "
                + "WHERE d.deleted_at IS NULL AND d.status NOT IN ('DRAFT', 'CANCELLED', 'VOIDED') "
                + "AND d.document_type IN ('INVOICE', 'CORRECTIVE_INVOICE', 'DEBIT_NOTE', 'CREDIT_NOTE') "
                + "AND COALESCE(d.issue_date, CURRENT_DATE) >= ? AND (l.recognition_method IS NULL OR (l.recognition_method <> 'POINT_IN_TIME' "
                + "AND NOT EXISTS (SELECT 1 FROM acc_deferral_schedule s WHERE s.kind = 'REVENUE' AND s.source_type = 'BILLING_LINE' AND s.source_line_id = l.id))) "
                + "ORDER BY l.id LIMIT 2000", Long.class, start);
        int created = 0;
        for (Long id : ids) {
            BillingDocumentLine line = billingLines.findById(id).orElse(null);
            if (line == null || Boolean.TRUE.equals(line.getOptional())) {
                continue;
            }
            if (revenueSchedule(line) != null) {
                created++;
            }
        }
        created += expenseSync(jdbc, start);
        return created;
    }

    private AccDeferralSchedule revenueSchedule(BillingDocumentLine line) {
        BillingDocument document = line.getDocument();
        boolean creditNote = document.getDocumentType() == BillingDocumentType.CREDIT_NOTE;
        Period period = period(line, document);
        if (period == null) {
            line.setRecognitionMethod("POINT_IN_TIME");
            if (line.getRecognitionSource() == null) {
                line.setRecognitionSource(requiresPeriod(line) ? "MISSING" : "NONE");
            }
            billingLines.save(line);
            return null;
        }
        line.setServiceStart(period.start());
        line.setServiceEnd(period.end());
        line.setRecognitionMethod(period.method());
        line.setRecognitionSource(period.source());
        billingLines.save(line);
        BigDecimal amount = money(line.getSubtotalAmount() != null ? line.getSubtotalAmount() : line.getTaxableAmount());
        if (amount.signum() == 0) {
            return null;
        }
        // Un avoir suit l'echeancier de la ligne qu'il corrige, gels compris : la ligne retombe a zero.
        AccDeferralSchedule corrected = creditNote && line.getOriginalLineId() != null
                ? schedules.findByKindAndSourceTypeAndSourceLineId(REVENUE, BILLING_LINE, line.getOriginalLineId()).orElse(null) : null;
        AccDeferralSchedule schedule = schedules.save(AccDeferralSchedule.builder().kind(REVENUE).sourceType(BILLING_LINE).sourceLineId(line.getId())
                .documentNumber(document.getDocumentNumber()).thirdParty(ReceivableRiskService.customerCode(document))
                .label(cut(line.getDescription(), 300)).accountNumber(sales.productAccount(line)).activity(period.source())
                .amount(creditNote ? amount.abs().negate() : amount)
                .startDate(corrected != null ? corrected.getStartDate() : period.start())
                .endDate(corrected != null ? corrected.getEndDate() : period.end())
                .method(corrected != null ? corrected.getMethod() : period.method())
                .totalUnits(corrected != null ? corrected.getTotalUnits() : period.units()).recognitionSource(period.source())
                .sourceRef(corrected != null ? corrected.getSourceRef() : period.ref()).build());
        if (corrected != null) {
            for (AccDeferralScheduleEvent event : events.findAllByScheduleIdOrderByCreatedAtAsc(corrected.getId())) {
                if ("FREEZE".equals(event.getType())) {
                    events.save(AccDeferralScheduleEvent.builder().scheduleId(schedule.getId()).type("FREEZE").eventDate(event.getEventDate())
                            .beforeValue(event.getBeforeValue()).afterValue(event.getAfterValue()).reason("Repris de " + corrected.getDocumentNumber())
                            .createdBy(event.getCreatedBy()).build());
                }
            }
        }
        return schedule;
    }

    private record Period(LocalDate start, LocalDate end, String method, String source, BigDecimal units, String ref) {

        Period(LocalDate start, LocalDate end, String method, String source, BigDecimal units) {
            this(start, end, method, source, units, null);
        }
    }

    /** Periode d'une ligne : saisie, element facturable, reservation, ou ligne corrigee d'un avoir. */
    private Period period(BillingDocumentLine line, BillingDocument document) {
        if (line.getServiceStart() != null) {
            LocalDate end = line.getServiceEnd() != null ? line.getServiceEnd() : line.getServiceStart();
            return new Period(line.getServiceStart(), end, method(line.getRecognitionMethod(), end.equals(line.getServiceStart()) ? "EVENT_DATE" : "DAILY"),
                    nz(line.getRecognitionSource(), "MANUAL"), null);
        }
        if (StringUtils.hasText(line.getItemCode())) {
            var billable = billables.findByBillableNumber(line.getItemCode()).orElse(null);
            if (billable != null && billable.getBillingPeriodStart() != null && billable.getBillingPeriodEnd() != null
                    && billable.getBillingPeriodEnd().isAfter(billable.getBillingPeriodStart())) {
                String source = billable.getSourceType() == null ? "SUBSCRIPTION" : billable.getSourceType().toUpperCase(Locale.ROOT);
                if (source.contains("PASS")) {
                    // Pass a credits : reconnaissance a l'usage, credits lus dans le journal des droits.
                    BigDecimal units = passUnits(billable.getSourceId());
                    if (units.signum() > 0) {
                        return new Period(billable.getBillingPeriodStart(), billable.getBillingPeriodEnd(), "USAGE", "PASS", units, billable.getSourceId());
                    }
                }
                return new Period(billable.getBillingPeriodStart(), billable.getBillingPeriodEnd(), "DAILY", source, null, billable.getSourceId());
            }
        }
        String sourceType = line.getSourceType() == null ? "" : line.getSourceType().toUpperCase(Locale.ROOT);
        if (sourceType.contains("BOOKING") && StringUtils.hasText(line.getSourceCode())) {
            var booking = bookings.findByBookingNumber(line.getSourceCode()).orElse(null);
            if (booking != null && booking.getStartedAt() != null) {
                LocalDate day = booking.getStartedAt().toLocalDate();
                return new Period(day, day, "EVENT_DATE", "BOOKING", null);
            }
        }
        if (document.getDocumentType() == BillingDocumentType.CREDIT_NOTE) {
            BillingDocumentLine original = original(line, document);
            if (original != null && original.getServiceStart() != null) {
                line.setOriginalLineId(original.getId());
                return new Period(original.getServiceStart(), original.getServiceEnd() != null ? original.getServiceEnd() : original.getServiceStart(),
                        nz(original.getRecognitionMethod(), "DAILY"), nz(original.getRecognitionSource(), "CREDIT_NOTE"), null);
            }
        }
        return null;
    }

    /** Ligne corrigee par une ligne d'avoir : lien explicite, sinon meme code d'article sur la facture d'origine. */
    private BillingDocumentLine original(BillingDocumentLine line, BillingDocument document) {
        if (line.getOriginalLineId() != null) {
            return billingLines.findById(line.getOriginalLineId()).orElse(null);
        }
        if (!StringUtils.hasText(document.getOriginalDocumentNumber())) {
            return null;
        }
        List<Long> ids = new JdbcTemplate(dataSource).queryForList("SELECT l.id FROM billing_document_line l JOIN billing_document d ON d.id = l.document_id "
                + "WHERE d.document_number = ? ORDER BY l.line_order, l.id", Long.class, document.getOriginalDocumentNumber());
        List<BillingDocumentLine> candidates = ids.stream().map(billingLines::findById).flatMap(Optional::stream).toList();
        return candidates.stream().filter(candidate -> Objects.equals(candidate.getItemCode(), line.getItemCode()) && line.getItemCode() != null)
                .findFirst().orElse(candidates.size() == 1 ? candidates.get(0) : null);
    }

    private boolean requiresPeriod(BillingDocumentLine line) {
        String type = line.getLineType() == null ? "" : line.getLineType().name();
        return "SUBSCRIPTION".equals(type) || "PASS".equals(type);
    }

    /** Charges : lignes de facture fournisseur comptabilisees avec une periode, au-dessus du seuil. */
    private int expenseSync(JdbcTemplate jdbc, LocalDate start) {
        BigDecimal threshold = decimal("accounting.expense.deferral-threshold", new BigDecimal("50000"));
        List<Long> ids = jdbc.queryForList("SELECT l.id FROM acc_supplier_invoice_line l JOIN acc_supplier_invoice i ON i.id = l.invoice_id "
                + "WHERE i.status = 'POSTED' AND i.invoice_date >= ? AND l.amount_ht >= ? AND NOT EXISTS (SELECT 1 FROM acc_deferral_schedule s "
                + "WHERE s.kind = 'EXPENSE' AND s.source_type = 'SUPPLIER_INVOICE_LINE' AND s.source_line_id = l.id) ORDER BY l.id LIMIT 2000",
                Long.class, start, threshold);
        int created = 0;
        for (Long id : ids) {
            AccSupplierInvoiceLine line = supplierLines.findById(id).orElse(null);
            AccSupplierInvoice invoice = line == null ? null : supplierInvoices.findById(line.getInvoiceId()).orElse(null);
            if (line == null || invoice == null || !StringUtils.hasText(line.getAccountNumber()) || !line.getAccountNumber().startsWith("6")) {
                continue;
            }
            LocalDate from = line.getServiceStart();
            LocalDate to = line.getServiceEnd();
            String method = line.getRecognitionMethod();
            if (from == null && StringUtils.hasText(line.getNatureCode())) {
                var nature = natures.findByCode(line.getNatureCode()).orElse(null);
                if (nature != null && Boolean.TRUE.equals(nature.getPeriodic()) && nature.getDefaultDurationMonths() != null) {
                    from = invoice.getInvoiceDate();
                    to = from.plusMonths(nature.getDefaultDurationMonths()).minusDays(1);
                    method = nature.getDefaultMethod();
                }
            }
            if (from == null || to == null || !to.isAfter(from)) {
                continue;
            }
            boolean credit = "CREDIT_NOTE".equals(invoice.getKind());
            BigDecimal amount = money(line.getAmountHt());
            schedules.save(AccDeferralSchedule.builder().kind(EXPENSE).sourceType(SUPPLIER_LINE).sourceLineId(line.getId())
                    .documentNumber(invoice.getNumber()).thirdParty(invoice.getSupplierCode()).label(cut(line.getDesignation(), 300))
                    .accountNumber(line.getAccountNumber()).sectionCode(line.getSectionCode()).activity(line.getNatureCode())
                    .amount(credit ? amount.negate() : amount).startDate(from).endDate(to).method(method(method, "DAILY"))
                    .recognitionSource("SUPPLIER_INVOICE").build());
            created++;
        }
        return created;
    }

    // ================================================================== besoin d'une ligne

    /** Part non encore reconnue d'un echeancier a une date (besoin de 477 ou 476). */
    public BigDecimal need(AccDeferralSchedule schedule, LocalDate date) {
        BigDecimal amount = schedule.getAmount();
        if ("CANCELLED".equals(schedule.getStatus()) || ("ENDED".equals(schedule.getStatus()) && schedule.getEndedOn() != null
                && !date.isBefore(schedule.getEndedOn()))) {
            return BigDecimal.ZERO;
        }
        if (date.isBefore(schedule.getStartDate())) {
            return amount;
        }
        if (!date.isBefore(schedule.getEndDate())) {
            return BigDecimal.ZERO;
        }
        switch (schedule.getMethod()) {
            case "EVENT_DATE" -> {
                return BigDecimal.ZERO;
            }
            case "USAGE" -> {
                BigDecimal total = schedule.getTotalUnits() == null ? BigDecimal.ZERO : schedule.getTotalUnits();
                if (total.signum() <= 0) {
                    return amount;
                }
                BigDecimal used = consumptions.findAllByScheduleIdOrderByUsageDateAsc(schedule.getId()).stream()
                        .filter(item -> !item.getUsageDate().isAfter(date)).map(AccUsageConsumption::getUnits).reduce(BigDecimal.ZERO, BigDecimal::add)
                        .add(passConsumed(schedule, date)).min(total);
                return amount.subtract(amount.multiply(used).divide(total, 0, RoundingMode.HALF_UP));
            }
            case "MONTHLY" -> {
                long months = ChronoUnit.MONTHS.between(YearMonth.from(schedule.getStartDate()), YearMonth.from(schedule.getEndDate())) + 1;
                // Un mois compte quand il est entierement ecoule (date de fin de mois incluse).
                long elapsed = ChronoUnit.MONTHS.between(YearMonth.from(schedule.getStartDate()), YearMonth.from(date))
                        + (date.equals(YearMonth.from(date).atEndOfMonth()) ? 1 : 0);
                elapsed = Math.max(0, Math.min(months, elapsed));
                return amount.subtract(amount.multiply(BigDecimal.valueOf(elapsed)).divide(BigDecimal.valueOf(months), 0, RoundingMode.HALF_UP));
            }
            default -> {
                // DAILY : jours reels de service, hors jours de gel ; dates de debut et de fin incluses.
                long frozenTotal = frozenDays(schedule, schedule.getEndDate());
                long total = ChronoUnit.DAYS.between(schedule.getStartDate(), schedule.getEndDate()) + 1 - frozenTotal;
                long elapsed = ChronoUnit.DAYS.between(schedule.getStartDate(), date) + 1 - frozenDays(schedule, date);
                if (total <= 0) {
                    return BigDecimal.ZERO;
                }
                elapsed = Math.max(0, Math.min(total, elapsed));
                return amount.subtract(amount.multiply(BigDecimal.valueOf(elapsed)).divide(BigDecimal.valueOf(total), 0, RoundingMode.HALF_UP));
            }
        }
    }

    /** Credits achetes d'un pass : somme des droits non illimites qu'il porte. */
    private BigDecimal passUnits(String passNumber) {
        if (!StringUtils.hasText(passNumber)) {
            return BigDecimal.ZERO;
        }
        BigDecimal units = new JdbcTemplate(dataSource).queryForObject("SELECT COALESCE(SUM(g.quantity_granted), 0) FROM entitlement_grant g "
                + "JOIN subscription_pass p ON p.id = g.pass_id WHERE p.pass_number = ? AND COALESCE(g.unlimited, FALSE) = FALSE", BigDecimal.class, passNumber);
        return units == null ? BigDecimal.ZERO : units;
    }

    /** Credits consommes d'un pass jusqu'a une date incluse, lus dans le journal des droits. */
    private BigDecimal passConsumed(AccDeferralSchedule schedule, LocalDate date) {
        if (!"PASS".equals(schedule.getRecognitionSource()) || !StringUtils.hasText(schedule.getSourceRef())) {
            return BigDecimal.ZERO;
        }
        BigDecimal used = new JdbcTemplate(dataSource).queryForObject("SELECT COALESCE(SUM(l.quantity), 0) FROM entitlement_ledger l "
                + "JOIN entitlement_grant g ON g.id = l.grant_id JOIN subscription_pass p ON p.id = g.pass_id WHERE p.pass_number = ? AND l.transaction_type = 'CONSUME' "
                + "AND l.created_at < ?", BigDecimal.class, schedule.getSourceRef(),
                java.sql.Timestamp.from(date.plusDays(1).atStartOfDay(ZONE).toInstant()));
        return used == null ? BigDecimal.ZERO : used.abs();
    }

    /** Jours de gel ecoules jusqu'a une date incluse. */
    private long frozenDays(AccDeferralSchedule schedule, LocalDate until) {
        if (schedule.getId() == null) {
            return 0;
        }
        long days = 0;
        for (AccDeferralScheduleEvent event : events.findAllByScheduleIdOrderByCreatedAtAsc(schedule.getId())) {
            if (!"FREEZE".equals(event.getType()) || event.getEventDate().isAfter(until)) {
                continue;
            }
            long length = Long.parseLong(event.getAfterValue());
            LocalDate last = event.getEventDate().plusDays(length - 1);
            days += ChronoUnit.DAYS.between(event.getEventDate(), (last.isAfter(until) ? until : last)) + 1;
        }
        return days;
    }

    // ================================================================== evenements

    public record EventRequest(String type, LocalDate date, Integer days, LocalDate newDate, String reason) {
    }

    /**
     * Evenement sur un echeancier : {@code FREEZE} (gel de N jours, la fin recule d'autant),
     * {@code TERMINATION} ou {@code CANCELLATION} (le reste est reconnu a la date), {@code RESCHEDULE}
     * (nouvelle date d'un service ponctuel), {@code EXPIRY} (pass expire : solde reconnu).
     */
    @Transactional
    public AccDeferralSchedule event(Long scheduleId, EventRequest request) {
        require(EDIT_PERIOD, RUN);
        return applyEvent(schedule(scheduleId), request, actor());
    }

    private AccDeferralSchedule applyEvent(AccDeferralSchedule schedule, EventRequest request, String actor) {
        if (request == null || !StringUtils.hasText(request.type())) {
            throw new BadRequestException("Type d'evenement attendu : FREEZE, TERMINATION, CANCELLATION, RESCHEDULE ou EXPIRY");
        }
        String type = request.type().trim().toUpperCase(Locale.ROOT);
        LocalDate date = request.date() != null ? request.date() : today();
        String before = schedule.getStartDate() + " / " + schedule.getEndDate() + " / " + schedule.getStatus();
        String after;
        switch (type) {
            case "FREEZE" -> {
                if (request.days() == null || request.days() <= 0) {
                    throw new BadRequestException("Un gel donne un nombre de jours");
                }
                schedule.setEndDate(schedule.getEndDate().plusDays(request.days()));
                after = String.valueOf(request.days());
            }
            case "TERMINATION", "CANCELLATION", "EXPIRY" -> {
                schedule.setStatus("ENDED");
                schedule.setEndedOn(date);
                after = "ENDED " + date;
            }
            case "RESCHEDULE" -> {
                if (request.newDate() == null) {
                    throw new BadRequestException("Un report donne la nouvelle date");
                }
                long length = ChronoUnit.DAYS.between(schedule.getStartDate(), schedule.getEndDate());
                schedule.setStartDate(request.newDate());
                schedule.setEndDate(request.newDate().plusDays(length));
                after = schedule.getStartDate() + " / " + schedule.getEndDate();
            }
            default -> throw new BadRequestException("Type d'evenement attendu : FREEZE, TERMINATION, CANCELLATION, RESCHEDULE ou EXPIRY");
        }
        schedule.setVersion(schedule.getVersion() + 1);
        schedule.setUpdatedAt(Instant.now());
        schedules.save(schedule);
        events.save(AccDeferralScheduleEvent.builder().scheduleId(schedule.getId()).type(type).eventDate(date).beforeValue(cut(before, 200))
                .afterValue(after).reason(cut(request.reason(), 500)).createdBy(actor).build());
        return schedule;
    }

    /** Gel d'un abonnement : tous les echeanciers de ses factures en cours reculent de la duree du gel. */
    @Transactional
    public int freezeDocuments(List<String> documentNumbers, LocalDate from, int days, String reason) {
        int touched = 0;
        for (String number : documentNumbers) {
            for (AccDeferralSchedule schedule : schedules.findAllByDocumentNumber(number)) {
                if ("ACTIVE".equals(schedule.getStatus()) && schedule.getEndDate().isAfter(from) && "DAILY".equals(schedule.getMethod())) {
                    applyEvent(schedule, new EventRequest("FREEZE", from, days, null, reason), "SYSTEM");
                    touched++;
                }
            }
        }
        return touched;
    }

    @Override
    @Transactional
    public void subscriptionFrozen(String subscriptionNumber, LocalDate from, int plannedDays) {
        sync();
        int touched = freezeDocuments(subscriptionDocuments(subscriptionNumber), from, plannedDays, "Gel de l'abonnement " + subscriptionNumber);
        log.info("Constatations d'avance : gel de {} jours de {}, {} echeancier(s) decale(s)", plannedDays, subscriptionNumber, touched);
    }

    /** A la reprise anticipee, la duree reellement gelee remplace la duree prevue. */
    @Override
    @Transactional
    public void subscriptionResumed(String subscriptionNumber, LocalDate from, int effectiveDays) {
        for (String number : subscriptionDocuments(subscriptionNumber)) {
            for (AccDeferralSchedule schedule : schedules.findAllByDocumentNumber(number)) {
                for (AccDeferralScheduleEvent event : events.findAllByScheduleIdOrderByCreatedAtAsc(schedule.getId())) {
                    if (!"FREEZE".equals(event.getType()) || !event.getEventDate().equals(from)) {
                        continue;
                    }
                    int planned = Integer.parseInt(event.getAfterValue());
                    if (effectiveDays >= planned) {
                        continue;
                    }
                    schedule.setEndDate(schedule.getEndDate().minusDays(planned - effectiveDays));
                    schedule.setUpdatedAt(Instant.now());
                    schedules.save(schedule);
                    event.setAfterValue(String.valueOf(effectiveDays));
                    event.setReason(cut(nz(event.getReason(), "") + " (reprise apres " + effectiveDays + " jours)", 500));
                    events.save(event);
                }
            }
        }
    }

    /** Factures des elements facturables d'un abonnement. */
    private List<String> subscriptionDocuments(String subscriptionNumber) {
        return new JdbcTemplate(dataSource).queryForList("SELECT DISTINCT d.document_number FROM billing_document_line l "
                + "JOIN billing_document d ON d.id = l.document_id JOIN billable_item b ON b.billable_number = l.item_code WHERE b.source_id = ?",
                String.class, subscriptionNumber);
    }

    public record PeriodRequest(LocalDate serviceStart, LocalDate serviceEnd, String method, String reason) {
    }

    /** Correction de la periode d'une ligne de facture, avec motif ; l'echeancier suit. */
    @Transactional
    public BillingDocumentLine editPeriod(Long lineId, PeriodRequest request) {
        require(EDIT_PERIOD);
        if (request == null || request.serviceStart() == null || !StringUtils.hasText(request.reason())) {
            throw new BadRequestException("Debut de periode et motif obligatoires");
        }
        LocalDate end = request.serviceEnd() != null ? request.serviceEnd() : request.serviceStart();
        if (end.isBefore(request.serviceStart())) {
            throw new BadRequestException("La fin de la periode suit son debut");
        }
        BillingDocumentLine line = billingLines.findById(lineId).orElseThrow(() -> new ResourceNotFoundException("Ligne introuvable : " + lineId));
        line.setServiceStart(request.serviceStart());
        line.setServiceEnd(end);
        line.setRecognitionMethod(method(request.method(), end.equals(request.serviceStart()) ? "EVENT_DATE" : "DAILY"));
        line.setRecognitionSource("MANUAL");
        billingLines.save(line);
        Optional<AccDeferralSchedule> existing = schedules.findByKindAndSourceTypeAndSourceLineId(REVENUE, BILLING_LINE, lineId);
        if (existing.isPresent()) {
            AccDeferralSchedule schedule = existing.get();
            String before = schedule.getStartDate() + " / " + schedule.getEndDate();
            schedule.setStartDate(request.serviceStart());
            schedule.setEndDate(end);
            schedule.setMethod(line.getRecognitionMethod());
            schedule.setVersion(schedule.getVersion() + 1);
            schedule.setUpdatedAt(Instant.now());
            schedules.save(schedule);
            events.save(AccDeferralScheduleEvent.builder().scheduleId(schedule.getId()).type("PERIOD_EDIT").eventDate(today()).beforeValue(before)
                    .afterValue(request.serviceStart() + " / " + end).reason(cut(request.reason(), 500)).createdBy(actor()).build());
        } else {
            revenueSchedule(line);
        }
        return line;
    }

    public record UsageRequest(LocalDate date, BigDecimal units, String sourceRef, BigDecimal totalUnits) {
    }

    /** Consommation de credits d'un pass : l'echeancier passe a l'usage. */
    @Transactional
    public AccDeferralSchedule usage(Long scheduleId, UsageRequest request) {
        require(EDIT_PERIOD, RUN);
        AccDeferralSchedule schedule = schedule(scheduleId);
        if (request == null || request.units() == null || request.units().signum() <= 0) {
            throw new BadRequestException("Nombre de credits consommes positif attendu");
        }
        if (request.totalUnits() != null) {
            schedule.setTotalUnits(request.totalUnits());
        }
        if (schedule.getTotalUnits() == null || schedule.getTotalUnits().signum() <= 0) {
            throw new BadRequestException("Indiquez le nombre de credits achetes (totalUnits)");
        }
        schedule.setMethod("USAGE");
        schedules.save(schedule);
        consumptions.save(AccUsageConsumption.builder().scheduleId(schedule.getId()).usageDate(request.date() != null ? request.date() : today())
                .units(request.units()).sourceRef(request.sourceRef()).build());
        return schedule;
    }

    // ================================================================== traitement de fin de periode

    public record RunLine(Long scheduleId, String documentNumber, String label, String account, String section, LocalDate start, LocalDate end,
                          BigDecimal amount, BigDecimal need, BigDecimal previous, BigDecimal difference) {
    }

    public record RunResult(String period, String kind, boolean simulated, BigDecimal totalNeed, BigDecimal totalPrevious, BigDecimal difference,
                            String entryNumber, List<RunLine> lines, List<String> warnings) {
    }

    @Transactional
    public RunResult preview(String period, String kind) {
        require(READ, RUN);
        sync();
        return execute(month(period), kind(kind), true, actor());
    }

    @Transactional
    public RunResult run(String period, String kind) {
        require(RUN);
        sync();
        return execute(month(period), kind(kind), false, actor());
    }

    @Transactional
    public RunResult runAutomatically(YearMonth month, String kind) {
        sync();
        return execute(month, kind, false, "SYSTEM");
    }

    private RunResult execute(YearMonth month, String kind, boolean simulate, String actor) {
        List<String> warnings = new ArrayList<>();
        String rhythm = settings.text(REVENUE.equals(kind) ? "accounting.revenue.deferral" : "accounting.expense.deferral", "MONTHLY")
                .trim().toUpperCase(Locale.ROOT);
        if ("NONE".equals(rhythm)) {
            warnings.add("Constatations d'avance desactivees (" + kind + ")");
            return new RunResult(month.toString(), kind, simulate, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, List.of(), warnings);
        }
        if ("YEAR_END".equals(rhythm) && month.getMonthValue() != 12) {
            warnings.add("Rythme YEAR_END : seul le traitement de decembre est comptabilise");
            if (!simulate) {
                return new RunResult(month.toString(), kind, false, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, List.of(), warnings);
            }
        }
        LocalDate end = month.atEndOfMonth();
        List<RunLine> lines = new ArrayList<>();
        for (AccDeferralSchedule schedule : schedules.findAllByKindOrderByIdAsc(kind)) {
            BigDecimal need = need(schedule, end);
            BigDecimal previous = runLines.findAllByScheduleIdOrderByIdDesc(schedule.getId()).stream().findFirst()
                    .map(AccDeferralRunLine::getNeed).orElse(BigDecimal.ZERO);
            BigDecimal difference = need.subtract(previous);
            if (need.signum() == 0 && previous.signum() == 0) {
                continue;
            }
            lines.add(new RunLine(schedule.getId(), schedule.getDocumentNumber(), schedule.getLabel(), schedule.getAccountNumber(),
                    schedule.getSectionCode(), schedule.getStartDate(), schedule.getEndDate(), schedule.getAmount(), need, previous, difference));
        }
        BigDecimal totalNeed = lines.stream().map(RunLine::need).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPrevious = lines.stream().map(RunLine::previous).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal difference = totalNeed.subtract(totalPrevious);
        if (simulate || lines.stream().allMatch(line -> line.difference().signum() == 0)) {
            return new RunResult(month.toString(), kind, simulate, totalNeed, totalPrevious, difference, null, lines, warnings);
        }
        String deferredAccount = REVENUE.equals(kind) ? settings.text("accounting.revenue.deferred-account", "477")
                : settings.text("accounting.expense.deferred-account", "476");
        String label = (REVENUE.equals(kind) ? "Produits constates d'avance " : "Charges constatees d'avance ") + month;
        Map<String, BigDecimal> byAccount = new TreeMap<>();
        for (RunLine line : lines) {
            if (line.difference().signum() != 0) {
                byAccount.merge(line.account() + "|" + nz(line.section(), ""), line.difference(), BigDecimal::add);
            }
        }
        List<AccountingRequests.EntryLine> entryLines = new ArrayList<>();
        byAccount.forEach((key, value) -> {
            if (value.signum() == 0) {
                return;
            }
            String account = key.substring(0, key.indexOf('|'));
            String section = key.substring(key.indexOf('|') + 1);
            boolean up = value.signum() > 0;
            BigDecimal amount = value.abs();
            // La section de la ligne d'origine suit la charge ou le produit reconnu, mois par mois.
            String axis = settings.text("accounting.deferral.analytic-axis", settings.text("accounting.purchase.analytic-axis", ""));
            Map<String, String> analytic = StringUtils.hasText(section) && StringUtils.hasText(axis) ? Map.of(axis, section) : null;
            // Produits : besoin en hausse, D produit C 477. Charges : besoin en hausse, D 476 C charge.
            boolean debitPnl = REVENUE.equals(kind) == up;
            entryLines.add(new AccountingRequests.EntryLine(debitPnl ? account : deferredAccount, null, label, amount, null, null, null, null, null,
                    null, null, month.toString(), debitPnl ? analytic : null));
            entryLines.add(new AccountingRequests.EntryLine(debitPnl ? deferredAccount : account, null, label, null, amount, null, null, null, null,
                    null, null, month.toString(), debitPnl ? null : analytic));
        });
        AccEntry entry = entryLines.isEmpty() ? null : poster.book(settings.text("accounting.deferral.journal", "OD"), end, label,
                REVENUE.equals(kind) ? "REVENUE_DEFERRAL_ADJUSTED" : "EXPENSE_DEFERRAL_ADJUSTED", kind + "-" + month, entryLines, actor);
        AccDeferralRun run = runs.save(AccDeferralRun.builder().periodCode(month.toString()).kind(kind).entryNumber(entry == null ? null : entry.getEntryNumber())
                .totalNeed(totalNeed).totalPrevious(totalPrevious).difference(difference).createdBy(actor).build());
        for (RunLine line : lines) {
            runLines.save(AccDeferralRunLine.builder().runId(run.getId()).scheduleId(line.scheduleId()).need(line.need()).previous(line.previous())
                    .difference(line.difference()).build());
        }
        return new RunResult(month.toString(), kind, false, totalNeed, totalPrevious, difference, entry == null ? null : entry.getEntryNumber(), lines, warnings);
    }

    // ================================================================== detail et echeanciers

    public record DetailLine(Long scheduleId, String kind, String documentNumber, String thirdParty, String label, String activity, String account,
                             LocalDate start, LocalDate end, String method, String status, BigDecimal amount, BigDecimal recognized, BigDecimal remaining) {
    }

    @Transactional(readOnly = true)
    public List<DetailLine> detail(String kind, LocalDate date) {
        require(READ, RUN);
        LocalDate at = date != null ? date : today();
        List<DetailLine> rows = new ArrayList<>();
        for (AccDeferralSchedule schedule : schedules.findAllByKindOrderByIdAsc(kind(kind))) {
            BigDecimal remaining = need(schedule, at);
            if (remaining.signum() == 0) {
                continue;
            }
            rows.add(new DetailLine(schedule.getId(), schedule.getKind(), schedule.getDocumentNumber(), schedule.getThirdParty(), schedule.getLabel(),
                    schedule.getActivity(), schedule.getAccountNumber(), schedule.getStartDate(), schedule.getEndDate(), schedule.getMethod(),
                    schedule.getStatus(), schedule.getAmount(), schedule.getAmount().subtract(remaining), remaining));
        }
        return rows;
    }

    public record ScheduleView(AccDeferralSchedule schedule, List<AccDeferralScheduleEvent> events, List<AccUsageConsumption> usage,
                               Map<String, BigDecimal> monthlyNeed) {
    }

    @Transactional(readOnly = true)
    public ScheduleView scheduleView(Long id) {
        require(READ, RUN);
        AccDeferralSchedule schedule = schedule(id);
        Map<String, BigDecimal> monthly = new LinkedHashMap<>();
        YearMonth cursor = YearMonth.from(schedule.getStartDate());
        YearMonth last = YearMonth.from(schedule.getEndDate());
        for (int guard = 0; !cursor.isAfter(last) && guard < 240; guard++, cursor = cursor.plusMonths(1)) {
            monthly.put(cursor.toString(), need(schedule, cursor.atEndOfMonth()));
        }
        return new ScheduleView(schedule, events.findAllByScheduleIdOrderByCreatedAtAsc(schedule.getId()),
                consumptions.findAllByScheduleIdOrderByUsageDateAsc(schedule.getId()), monthly);
    }

    // ================================================================== factures a etablir et non parvenues

    public record AccrualRequest(String kind, String reference, String thirdParty, String label, String accountNumber, String sectionCode,
                                 String period, BigDecimal amount, BigDecimal vatAmount, String sourceType) {
    }

    @Transactional(readOnly = true)
    public List<AccAccrual> accruals(String period) {
        require(READ, ACCRUAL);
        return StringUtils.hasText(period) ? accruals.findAllByPeriodCode(period.trim()) : accruals.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public AccAccrual proposeAccrual(AccrualRequest request) {
        require(ACCRUAL);
        if (request == null || !StringUtils.hasText(request.reference()) || !StringUtils.hasText(request.accountNumber())
                || request.amount() == null || request.amount().signum() <= 0) {
            throw new BadRequestException("Reference, compte et montant positif obligatoires");
        }
        String kind = request.kind() == null ? "" : request.kind().trim().toUpperCase(Locale.ROOT);
        if (!List.of("ACCRUED_REVENUE", "ACCRUED_EXPENSE").contains(kind)) {
            throw new BadRequestException("Nature attendue : ACCRUED_REVENUE (facture a etablir) ou ACCRUED_EXPENSE (facture non parvenue)");
        }
        String account = request.accountNumber().trim();
        if ("ACCRUED_REVENUE".equals(kind) && !account.startsWith("7") || "ACCRUED_EXPENSE".equals(kind) && !account.startsWith("6")) {
            throw new BadRequestException("Compte attendu : produit (7) pour une facture a etablir, charge (6) pour une facture non parvenue");
        }
        return accruals.save(AccAccrual.builder().kind(kind).sourceType(nz(request.sourceType(), "MANUAL")).reference(request.reference().trim())
                .thirdParty(request.thirdParty()).label(cut(request.label(), 300)).accountNumber(account).sectionCode(request.sectionCode())
                .periodCode(month(request.period()).toString()).estimatedAmount(money(request.amount()))
                .vatAmount(request.vatAmount() == null ? BigDecimal.ZERO : money(request.vatAmount())).createdBy(actor()).build());
    }

    /**
     * Propositions automatiques de fin de periode : depassements d'abonnement consommes non factures
     * (factures a etablir) ; commandes fournisseurs validees, attendues dans la periode et non
     * entierement facturees (factures non parvenues). Une proposition par reference, jamais deux.
     */
    @Transactional
    public int proposeAccruals(YearMonth month) {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        LocalDate end = month.atEndOfMonth();
        int created = 0;
        for (Map<String, Object> row : jdbc.queryForList("SELECT c.charge_number, c.owner_code, c.amount FROM subscription_overage_charge c "
                + "LEFT JOIN billable_item b ON b.id = c.billable_item_id WHERE c.created_at < ? AND (b.id IS NULL OR b.invoice_id IS NULL)",
                java.sql.Timestamp.from(end.plusDays(1).atStartOfDay(ZONE).toInstant()))) {
            String reference = String.valueOf(row.get("charge_number"));
            if (accruals.findAllByPeriodCode(month.toString()).stream().anyMatch(item -> reference.equals(item.getReference()))) {
                continue;
            }
            BigDecimal amount = money((BigDecimal) row.get("amount"));
            if (amount.signum() <= 0) {
                continue;
            }
            accruals.save(AccAccrual.builder().kind("ACCRUED_REVENUE").sourceType("OVERAGE").reference(reference).thirdParty((String) row.get("owner_code"))
                    .label("Depassement non facture " + reference).accountNumber(settings.text("accounting.sales.account.SUBSCRIPTION", "7062"))
                    .periodCode(month.toString()).estimatedAmount(amount).createdBy("SYSTEM").build());
            created++;
        }
        for (Map<String, Object> row : jdbc.queryForList("SELECT o.number, o.supplier_code, l.line_no, l.account_number, l.section_code, l.designation, "
                + "l.amount_ht - COALESCE(l.invoiced_ht, 0) AS remaining FROM acc_purchase_order o JOIN acc_purchase_order_line l ON l.order_id = o.id "
                + "WHERE o.status = 'VALIDATED' AND COALESCE(o.expected_date, o.order_date) <= ? AND l.amount_ht > COALESCE(l.invoiced_ht, 0)", end)) {
            String reference = row.get("number") + "-" + row.get("line_no");
            if (accruals.findAllByPeriodCode(month.toString()).stream().anyMatch(item -> reference.equals(item.getReference()))) {
                continue;
            }
            String account = (String) row.get("account_number");
            if (!StringUtils.hasText(account) || !account.startsWith("6")) {
                continue;
            }
            accruals.save(AccAccrual.builder().kind("ACCRUED_EXPENSE").sourceType("PURCHASE_ORDER").reference(reference)
                    .thirdParty((String) row.get("supplier_code")).label(cut("Commande non facturee : " + row.get("designation"), 300)).accountNumber(account)
                    .sectionCode((String) row.get("section_code")).periodCode(month.toString()).estimatedAmount(money((BigDecimal) row.get("remaining")))
                    .createdBy("SYSTEM").build());
            created++;
        }
        return created;
    }

    /** Comptabilise l'estimation en fin de periode et son extourne au premier jour de la periode suivante. */
    @Transactional
    public AccAccrual postAccrual(Long id) {
        require(ACCRUAL);
        AccAccrual accrual = accruals.findById(id).orElseThrow(() -> new ResourceNotFoundException("Estimation introuvable : " + id));
        if (!"PROPOSED".equals(accrual.getStatus())) {
            throw new BadRequestException("Estimation deja traitee (" + accrual.getStatus() + ")");
        }
        YearMonth month = YearMonth.parse(accrual.getPeriodCode());
        boolean revenue = "ACCRUED_REVENUE".equals(accrual.getKind());
        String label = (revenue ? "Facture a etablir " : "Facture non parvenue ") + accrual.getReference();
        String counter = revenue ? settings.text("accounting.accrual.revenue-account", "4181") : settings.text("accounting.accrual.expense-account", "408");
        List<AccountingRequests.EntryLine> lines = new ArrayList<>();
        BigDecimal vat = revenue ? BigDecimal.ZERO : accrual.getVatAmount();
        if (revenue) {
            lines.add(ReceivableRiskService.line(counter, accrual.getThirdParty(), label, accrual.getEstimatedAmount(), null, accrual.getReference()));
            lines.add(ReceivableRiskService.line(accrual.getAccountNumber(), null, label, null, accrual.getEstimatedAmount(), accrual.getReference()));
        } else {
            lines.add(ReceivableRiskService.line(accrual.getAccountNumber(), null, label, accrual.getEstimatedAmount(), null, accrual.getReference()));
            if (vat.signum() > 0) {
                lines.add(ReceivableRiskService.line(settings.text("accounting.accrual.expense-vat-account", "4454"), null, "TVA " + label, vat, null,
                        accrual.getReference()));
            }
            lines.add(ReceivableRiskService.line(counter, accrual.getThirdParty(), label, null, accrual.getEstimatedAmount().add(vat), accrual.getReference()));
        }
        String journal = settings.text("accounting.deferral.journal", "OD");
        AccEntry entry = poster.book(journal, month.atEndOfMonth(), label, revenue ? "ACCRUED_REVENUE_POSTED" : "ACCRUED_EXPENSE_POSTED",
                accrual.getReference(), lines, actor());
        List<AccountingRequests.EntryLine> reversal = lines.stream().map(line -> new AccountingRequests.EntryLine(line.accountNumber(),
                line.thirdPartyCode(), "Extourne " + line.label(), line.credit(), line.debit(), null, null, null, null, null, null, line.reference(), null))
                .toList();
        AccEntry reversed = poster.book(settings.text("accounting.accrual.reversal-journal", journal), month.plusMonths(1).atDay(1),
                "Extourne " + label, "ACCRUAL_REVERSED", accrual.getReference(), reversal, actor());
        accrual.setEntryNumber(entry.getEntryNumber());
        accrual.setReversalEntryNumber(reversed.getEntryNumber());
        accrual.setStatus("REVERSED");
        return accruals.save(accrual);
    }

    /** Rapprochement avec la facture reelle : ecart d'estimation trace. */
    @Transactional
    public AccAccrual matchAccrual(Long id, String actualReference, BigDecimal actualAmount) {
        require(ACCRUAL);
        AccAccrual accrual = accruals.findById(id).orElseThrow(() -> new ResourceNotFoundException("Estimation introuvable : " + id));
        if (!StringUtils.hasText(actualReference) || actualAmount == null) {
            throw new BadRequestException("Reference et montant de la facture reelle obligatoires");
        }
        accrual.setActualReference(actualReference.trim());
        accrual.setActualAmount(money(actualAmount));
        accrual.setGap(money(actualAmount).subtract(accrual.getEstimatedAmount()));
        accrual.setStatus("MATCHED");
        return accruals.save(accrual);
    }

    // ================================================================== controles et cloture

    public record Finding(String severity, String subject, String message) {
    }

    @Transactional(readOnly = true)
    public List<Finding> controls(LocalDate date) {
        List<Finding> findings = new ArrayList<>();
        entityManager.flush();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        for (String kind : List.of(REVENUE, EXPENSE)) {
            String account = REVENUE.equals(kind) ? settings.text("accounting.revenue.deferred-account", "477")
                    : settings.text("accounting.expense.deferred-account", "476");
            BigDecimal ledger = jdbc.queryForObject("SELECT COALESCE(SUM(l.debit - l.credit), 0) FROM acc_entry_line l JOIN acc_entry e ON e.id = l.entry_id "
                    + "WHERE l.account_number = ? AND e.status IN ('VALIDATED', 'REVERSED')", BigDecimal.class, account);
            BigDecimal booked = REVENUE.equals(kind) ? (ledger == null ? BigDecimal.ZERO : ledger.negate()) : (ledger == null ? BigDecimal.ZERO : ledger);
            BigDecimal schedulesTotal = BigDecimal.ZERO;
            for (AccDeferralSchedule schedule : schedules.findAllByKindOrderByIdAsc(kind)) {
                schedulesTotal = schedulesTotal.add(runLines.findAllByScheduleIdOrderByIdDesc(schedule.getId()).stream().findFirst()
                        .map(AccDeferralRunLine::getNeed).orElse(BigDecimal.ZERO));
            }
            if (booked.subtract(schedulesTotal).abs().compareTo(BigDecimal.ONE) > 0) {
                findings.add(new Finding("BLOCKING", "Compte " + account, "Solde " + booked.toPlainString() + " different du detail des echeanciers "
                        + schedulesTotal.toPlainString()));
            }
        }
        Integer missing = jdbc.queryForObject("SELECT COUNT(*) FROM billing_document_line WHERE recognition_source = 'MISSING'", Integer.class);
        if (missing != null && missing > 0) {
            findings.add(new Finding("WARNING", "Periodes", missing + " ligne(s) d'abonnement ou de pass sans periode de service"));
        }
        long alertDays = number("accounting.accrual.alert-days", 60);
        for (AccAccrual accrual : accruals.findAllByStatus("REVERSED")) {
            if (YearMonth.parse(accrual.getPeriodCode()).atEndOfMonth().plusDays(alertDays).isBefore(date)) {
                findings.add(new Finding("WARNING", accrual.getReference(), "Estimation non rapprochee d'une facture reelle depuis plus de "
                        + alertDays + " jours"));
            }
        }
        return findings;
    }

    /** Liste de cloture : traitement de chaque mois de l'exercice, ecart nul, revue des estimations. */
    @Transactional(readOnly = true)
    public List<Finding> closingCheck(int year) {
        List<Finding> findings = new ArrayList<>();
        for (String kind : List.of(REVENUE, EXPENSE)) {
            String rhythm = settings.text(REVENUE.equals(kind) ? "accounting.revenue.deferral" : "accounting.expense.deferral", "MONTHLY")
                    .trim().toUpperCase(Locale.ROOT);
            if ("NONE".equals(rhythm) || schedules.findAllByKindOrderByIdAsc(kind).isEmpty()) {
                continue;
            }
            List<Integer> months = "YEAR_END".equals(rhythm) ? List.of(12) : List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
            for (int month : months) {
                YearMonth period = YearMonth.of(year, month);
                boolean done = !runs.findAllByKindAndPeriodCodeAndStatus(kind, period.toString(), "POSTED").isEmpty();
                boolean needed = schedules.findAllByKindOrderByIdAsc(kind).stream().anyMatch(item -> !item.getStartDate().isAfter(period.atEndOfMonth())
                        && !YearMonth.from(item.getCreatedAt().atZone(ZONE)).isAfter(period.plusMonths(1)));
                if (needed && !done) {
                    findings.add(new Finding("BLOCKING", kind, "Traitement de fin de periode " + period + " non comptabilise"));
                }
            }
        }
        controls(LocalDate.of(year, 12, 31)).stream().filter(item -> "BLOCKING".equals(item.severity())).forEach(findings::add);
        return findings;
    }

    // ================================================================== outils

    private AccDeferralSchedule schedule(Long id) {
        return schedules.findById(id).orElseThrow(() -> new ResourceNotFoundException("Echeancier introuvable : " + id));
    }

    private static String method(String value, String fallback) {
        String method = StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : fallback;
        return List.of("DAILY", "MONTHLY", "USAGE", "EVENT_DATE").contains(method) ? method : fallback;
    }

    private static String kind(String value) {
        String kind = value == null ? REVENUE : value.trim().toUpperCase(Locale.ROOT);
        if (!List.of(REVENUE, EXPENSE).contains(kind)) {
            throw new BadRequestException("Nature attendue : REVENUE ou EXPENSE");
        }
        return kind;
    }

    private static YearMonth month(String period) {
        try {
            return YearMonth.parse(period.trim());
        } catch (RuntimeException ex) {
            throw new BadRequestException("Periode attendue au format AAAA-MM");
        }
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

    private static BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(0, RoundingMode.HALF_UP);
    }

    private static String cut(String value, int max) {
        return value == null ? null : value.length() > max ? value.substring(0, max) : value;
    }

    private static String nz(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private static LocalDate today() {
        return LocalDate.now(ZONE);
    }

    private void require(String... authorities) {
        for (String authority : authorities) {
            if (actors.hasAuthority(authority)) {
                return;
            }
        }
        throw new ForbiddenException("Action reservee : " + String.join(" ou ", authorities));
    }

    private String actor() {
        return actors.resolve("SYSTEM");
    }
}
