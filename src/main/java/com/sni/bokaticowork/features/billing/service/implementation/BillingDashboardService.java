package com.sni.bokaticowork.features.billing.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lot C15 : tableau de bord commercial, sur une periode et une devise (jamais de cumul entre
 * devises). Devis emis et transformes, delai moyen de transformation (du devis a sa facture),
 * facture, encaisse et delai moyen de paiement (DSO sur les factures soldees de la periode),
 * prevision d'encaissement par semaine (factures ouvertes et echeances d'echeanciers), affaires
 * par etape (montant attendu et pondere par la probabilite), meilleurs clients.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BillingDashboardService {

    private static final String INVOICE_TYPES = "('INVOICE', 'CORRECTIVE_INVOICE', 'DEBIT_NOTE')";

    private final JdbcTemplate jdbc;

    public record Dashboard(LocalDate from, LocalDate to, String currency, Quotes quotes, Invoicing invoicing, List<WeekForecast> forecast,
                            List<DealStage> deals, List<TopCustomer> topCustomers) {
    }

    public record Quotes(long issued, long converted, BigDecimal conversionRate, BigDecimal issuedAmount, BigDecimal convertedAmount,
                         BigDecimal averageDaysToInvoice) {
    }

    public record Invoicing(long invoices, BigDecimal invoiced, BigDecimal collected, BigDecimal outstanding, BigDecimal overdue,
                            BigDecimal averageDaysToPay) {
    }

    public record WeekForecast(LocalDate weekStart, BigDecimal expected, long documents) {
    }

    public record DealStage(String stage, long deals, BigDecimal expectedAmount, BigDecimal weightedAmount) {
    }

    public record TopCustomer(String customerType, String customerCode, String customerName, BigDecimal invoiced, BigDecimal outstanding) {
    }

    public Dashboard dashboard(LocalDate from, LocalDate to, String currency, int weeks) {
        LocalDate start = from == null ? LocalDate.now().withDayOfMonth(1) : from;
        LocalDate end = to == null ? LocalDate.now() : to;
        if (end.isBefore(start)) {
            throw BadRequestException.coded("PERIODE_INVALIDE", "La fin de la période précède son début.");
        }
        String cur = currency == null || currency.isBlank() ? "XAF" : currency.trim().toUpperCase();
        return new Dashboard(start, end, cur, quotes(start, end, cur), invoicing(start, end, cur), forecast(cur, Math.max(1, Math.min(weeks, 26))),
                deals(cur), topCustomers(start, end, cur));
    }

    Quotes quotes(LocalDate from, LocalDate to, String currency) {
        Map<String, Object> row = jdbc.queryForMap("""
                SELECT COUNT(*) AS issued,
                       COUNT(*) FILTER (WHERE q.status = 'CONVERTED') AS converted,
                       COALESCE(SUM(q.total_amount), 0) AS issued_amount,
                       COALESCE(SUM(q.total_amount) FILTER (WHERE q.status = 'CONVERTED'), 0) AS converted_amount
                FROM billing_document q
                WHERE q.document_type = 'QUOTE' AND q.issued_at IS NOT NULL AND q.deleted_at IS NULL AND q.currency = ?
                  AND q.issue_date BETWEEN ? AND ?
                """, currency, Date.valueOf(from), Date.valueOf(to));
        BigDecimal days = jdbc.queryForObject("""
                SELECT AVG(i.issue_date - q.issue_date)
                FROM billing_document i JOIN billing_document q ON q.document_number = i.source_code AND q.document_type = 'QUOTE'
                WHERE i.document_type = 'INVOICE' AND i.source_type = 'QUOTE' AND i.issued_at IS NOT NULL AND i.currency = ?
                  AND i.issue_date BETWEEN ? AND ?
                """, BigDecimal.class, currency, Date.valueOf(from), Date.valueOf(to));
        long issued = ((Number) row.get("issued")).longValue();
        long converted = ((Number) row.get("converted")).longValue();
        return new Quotes(issued, converted, rate(converted, issued), money(row.get("issued_amount")), money(row.get("converted_amount")),
                days == null ? null : days.setScale(1, RoundingMode.HALF_UP));
    }

    Invoicing invoicing(LocalDate from, LocalDate to, String currency) {
        Map<String, Object> row = jdbc.queryForMap("""
                SELECT COUNT(*) AS invoices, COALESCE(SUM(d.total_amount), 0) AS invoiced, COALESCE(SUM(d.paid_amount), 0) AS collected,
                       COALESCE(SUM(d.balance_due), 0) AS outstanding,
                       COALESCE(SUM(d.balance_due) FILTER (WHERE d.due_date < CURRENT_DATE), 0) AS overdue
                FROM billing_document d
                WHERE d.document_type IN """ + INVOICE_TYPES + """
                   AND d.issued_at IS NOT NULL AND d.deleted_at IS NULL AND d.status <> 'CANCELLED' AND d.currency = ?
                  AND d.issue_date BETWEEN ? AND ?
                """, currency, Date.valueOf(from), Date.valueOf(to));
        BigDecimal dso = jdbc.queryForObject("""
                SELECT AVG(CAST(d.paid_at AS DATE) - d.issue_date)
                FROM billing_document d
                WHERE d.document_type IN """ + INVOICE_TYPES + """
                   AND d.status = 'PAID' AND d.paid_at IS NOT NULL AND d.currency = ? AND CAST(d.paid_at AS DATE) BETWEEN ? AND ?
                """, BigDecimal.class, currency, Date.valueOf(from), Date.valueOf(to));
        return new Invoicing(((Number) row.get("invoices")).longValue(), money(row.get("invoiced")), money(row.get("collected")),
                money(row.get("outstanding")), money(row.get("overdue")), dso == null ? null : dso.setScale(1, RoundingMode.HALF_UP));
    }

    /**
     * Prevision d'encaissement par semaine : reste du des factures ouvertes a leur echeance (une
     * echeance passee compte pour la semaine en cours), echeances d'echeanciers a leur date.
     */
    List<WeekForecast> forecast(String currency, int weeks) {
        LocalDate monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate horizon = monday.plusWeeks(weeks);
        Map<LocalDate, BigDecimal[]> buckets = new LinkedHashMap<>();
        for (int index = 0; index < weeks; index++) {
            buckets.put(monday.plusWeeks(index), new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
        }
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT COALESCE(i.due_date, d.due_date, d.issue_date) AS due, COALESCE(i.amount - COALESCE(i.paid_amount, 0), d.balance_due) AS expected
                FROM billing_document d
                LEFT JOIN payment_schedule s ON s.billing_document_number = d.document_number AND s.status NOT IN ('CANCELLED', 'COMPLETED')
                LEFT JOIN payment_schedule_installment i ON i.schedule_id = s.id AND i.status NOT IN ('PAID', 'CANCELLED')
                WHERE d.document_type IN """ + INVOICE_TYPES + """
                   AND d.balance_due > 0 AND d.status NOT IN ('DRAFT', 'CANCELLED', 'PAID') AND d.deleted_at IS NULL AND d.currency = ?
                """, currency);
        for (Map<String, Object> row : rows) {
            LocalDate due = row.get("due") instanceof Date day ? day.toLocalDate() : monday;
            if (!due.isBefore(horizon)) {
                continue;
            }
            LocalDate week = due.isBefore(monday) ? monday : due.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            BigDecimal[] bucket = buckets.get(week);
            if (bucket != null) {
                bucket[0] = bucket[0].add(money(row.get("expected")));
                bucket[1] = bucket[1].add(BigDecimal.ONE);
            }
        }
        List<WeekForecast> result = new ArrayList<>();
        buckets.forEach((week, bucket) -> result.add(new WeekForecast(week, bucket[0], bucket[1].longValue())));
        return result;
    }

    List<DealStage> deals(String currency) {
        return jdbc.queryForList("""
                SELECT stage, COUNT(*) AS deals, COALESCE(SUM(expected_amount), 0) AS expected,
                       COALESCE(SUM(expected_amount * COALESCE(probability, 0) / 100), 0) AS weighted
                FROM billing_deal WHERE currency = ? GROUP BY stage ORDER BY stage
                """, currency).stream().map(row -> new DealStage((String) row.get("stage"), ((Number) row.get("deals")).longValue(),
                money(row.get("expected")), money(row.get("weighted")).setScale(0, RoundingMode.HALF_UP))).toList();
    }

    List<TopCustomer> topCustomers(LocalDate from, LocalDate to, String currency) {
        return jdbc.queryForList("""
                SELECT d.customer_type, d.customer_code, MAX(d.customer_name) AS customer_name, SUM(d.total_amount) AS invoiced,
                       SUM(d.balance_due) AS outstanding
                FROM billing_document d
                WHERE d.document_type IN """ + INVOICE_TYPES + """
                   AND d.issued_at IS NOT NULL AND d.deleted_at IS NULL AND d.status <> 'CANCELLED' AND d.currency = ?
                  AND d.issue_date BETWEEN ? AND ?
                GROUP BY d.customer_type, d.customer_code ORDER BY SUM(d.total_amount) DESC LIMIT 10
                """, currency, Date.valueOf(from), Date.valueOf(to)).stream()
                .map(row -> new TopCustomer((String) row.get("customer_type"), (String) row.get("customer_code"), (String) row.get("customer_name"),
                        money(row.get("invoiced")), money(row.get("outstanding")))).toList();
    }

    static BigDecimal rate(long part, long whole) {
        return whole == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(part * 100.0 / whole).setScale(1, RoundingMode.HALF_UP);
    }

    private static BigDecimal money(Object value) {
        return value == null ? BigDecimal.ZERO : new BigDecimal(value.toString());
    }
}
