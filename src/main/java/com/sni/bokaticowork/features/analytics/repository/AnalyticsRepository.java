package com.sni.bokaticowork.features.analytics.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public class AnalyticsRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public Object[] financial(Instant fromDate, Instant toDate) {
        return single("""
                SELECT
                    count(*) FILTER (WHERE document_type = 'INVOICE') AS invoice_count,
                    coalesce(sum(total_amount) FILTER (
                        WHERE document_type = 'INVOICE'
                          AND status NOT IN ('DRAFT','REJECTED','EXPIRED','CONVERTED')), 0) AS invoiced_amount,
                    coalesce(sum(paid_amount) FILTER (WHERE document_type = 'INVOICE'), 0) AS paid_amount,
                    -- Ce qui reste a encaisser · une facture en brouillon n'a jamais ete reclamee,
                    -- et une facture reglee ou passee en perte n'est plus une creance.
                    coalesce(sum(balance_due) FILTER (
                        WHERE document_type = 'INVOICE'
                          AND status NOT IN ('DRAFT','REJECTED','EXPIRED','CONVERTED','PAID','REFUNDED','WRITTEN_OFF')), 0) AS outstanding_amount,
                    count(*) FILTER (WHERE document_type = 'INVOICE' AND status = 'OVERDUE') AS overdue_invoice_count,
                    count(*) FILTER (WHERE document_type = 'QUOTE') AS quotation_count,
                    coalesce(sum(total_amount) FILTER (WHERE document_type = 'QUOTE'), 0) AS quoted_amount,
                    coalesce(sum(vat_amount), 0) AS vat_amount,
                    coalesce(sum(additional_cent_amount), 0) AS additional_cent_amount
                FROM billing_document
                WHERE status NOT IN ('CANCELLED','VOIDED')
                  AND (CAST(:fromDate AS timestamptz) IS NULL OR created_at >= CAST(:fromDate AS timestamptz))
                  AND (CAST(:toDate AS timestamptz) IS NULL OR created_at <= CAST(:toDate AS timestamptz))
                """, fromDate, toDate);
    }

    public Object[] payment(Instant fromDate, Instant toDate) {
        return single("""
                SELECT
                    count(*) AS transaction_count,
                    coalesce(sum(amount) FILTER (WHERE status = 'SUCCEEDED'), 0) AS succeeded_amount,
                    coalesce(sum(amount) FILTER (WHERE status = 'SUCCEEDED' AND payment_method = 'CASH'), 0) AS cash_amount,
                    coalesce(sum(amount) FILTER (WHERE status = 'SUCCEEDED' AND payment_method = 'WALLET'), 0) AS wallet_amount,
                    coalesce(sum(amount) FILTER (WHERE status = 'SUCCEEDED' AND payment_method = 'MOBILE_MONEY'), 0) AS mobile_money_amount,
                    coalesce(sum(amount) FILTER (WHERE status = 'SUCCEEDED' AND payment_method = 'BANK_TRANSFER'), 0) AS bank_transfer_amount,
                    coalesce(sum(amount) FILTER (WHERE status = 'SUCCEEDED' AND payment_method = 'CARD'), 0) AS card_amount,
                    count(*) FILTER (WHERE status = 'FAILED') AS failed_transaction_count,
                    count(*) FILTER (WHERE status IN ('PENDING', 'PROCESSING')) AS pending_transaction_count
                FROM payment_transaction
                WHERE (CAST(:fromDate AS timestamptz) IS NULL OR created_at >= CAST(:fromDate AS timestamptz))
                  AND (CAST(:toDate AS timestamptz) IS NULL OR created_at <= CAST(:toDate AS timestamptz))
                """, fromDate, toDate);
    }

    public Object[] wallet(Instant fromDate, Instant toDate) {
        return single("""
                SELECT
                    (SELECT count(*) FROM wallet_account) AS wallet_count,
                    (SELECT coalesce(sum(available_balance), 0) FROM wallet_account) AS available_balance,
                    (SELECT coalesce(sum(held_balance), 0) FROM wallet_account) AS held_balance,
                    coalesce(sum(amount) FILTER (WHERE direction = 'CREDIT'), 0) AS credits,
                    coalesce(sum(amount) FILTER (WHERE direction = 'DEBIT'), 0) AS debits
                FROM wallet_ledger_entry
                WHERE (CAST(:fromDate AS timestamptz) IS NULL OR created_at >= CAST(:fromDate AS timestamptz))
                  AND (CAST(:toDate AS timestamptz) IS NULL OR created_at <= CAST(:toDate AS timestamptz))
                """, fromDate, toDate);
    }

    public Object[] booking(Instant fromDate, Instant toDate) {
        return single("""
                SELECT
                    count(*) AS booking_count,
                    count(*) FILTER (WHERE status = 'CONFIRMED') AS confirmed_bookings,
                    count(*) FILTER (WHERE status = 'COMPLETED') AS completed_bookings,
                    count(*) FILTER (WHERE status = 'CANCELLED') AS cancelled_bookings,
                    count(*) FILTER (WHERE status = 'NO_SHOW') AS no_show_bookings,
                    coalesce(sum(total_amount), 0) AS booking_revenue,
                    coalesce(sum(duration_minutes), 0) AS booked_minutes,
                    (SELECT count(*) FROM booking_participant participant
                     JOIN booking booking_filter ON booking_filter.id = participant.booking_id
                     WHERE (CAST(:fromDate AS timestamp) IS NULL OR booking_filter.created_at >= CAST(:fromDate AS timestamp))
                       AND (CAST(:toDate AS timestamp) IS NULL OR booking_filter.created_at <= CAST(:toDate AS timestamp))) AS participant_count
                FROM booking
                WHERE deleted = false
                  AND (CAST(:fromDate AS timestamp) IS NULL OR created_at >= CAST(:fromDate AS timestamp))
                  AND (CAST(:toDate AS timestamp) IS NULL OR created_at <= CAST(:toDate AS timestamp))
                """, fromDate, toDate);
    }

    public List<Object[]> bookingTrend(Instant fromDate, Instant toDate, String groupBy) {
        Query query = entityManager.createNativeQuery("""
                SELECT
                    date_trunc(:groupBy, created_at)::date AS date,
                    count(*) AS booking_count,
                    count(*) FILTER (WHERE status = 'CONFIRMED') AS confirmed_count,
                    count(*) FILTER (WHERE status = 'CANCELLED') AS cancelled_count,
                    coalesce(sum(total_amount), 0) AS revenue
                FROM booking
                WHERE deleted = false
                  AND (CAST(:fromDate AS timestamp) IS NULL OR created_at >= CAST(:fromDate AS timestamp))
                  AND (CAST(:toDate AS timestamp) IS NULL OR created_at <= CAST(:toDate AS timestamp))
                GROUP BY 1
                ORDER BY 1
                """);
        query.setParameter("groupBy", groupBy);
        query.setParameter("fromDate", fromDate);
        query.setParameter("toDate", toDate);
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();
        return results;
    }

    public List<Object[]> topOwners(Instant fromDate, Instant toDate, int limit) {
        Query query = entityManager.createNativeQuery("""
                SELECT
                    owner_type,
                    owner_code,
                    coalesce(max(nullif(contact_name, '')), owner_code) AS owner_name,
                    count(*) AS booking_count,
                    count(*) FILTER (WHERE status = 'CONFIRMED') AS confirmed_count,
                    count(*) FILTER (WHERE status = 'COMPLETED') AS completed_count,
                    count(*) FILTER (WHERE status = 'CANCELLED') AS cancelled_count,
                    coalesce(sum(total_amount), 0) AS revenue,
                    coalesce(sum(duration_minutes), 0) AS booked_minutes
                FROM booking
                WHERE deleted = false
                  AND (CAST(:fromDate AS timestamp) IS NULL OR created_at >= CAST(:fromDate AS timestamp))
                  AND (CAST(:toDate AS timestamp) IS NULL OR created_at <= CAST(:toDate AS timestamp))
                GROUP BY owner_type, owner_code
                ORDER BY booking_count DESC, revenue DESC, owner_code ASC
                LIMIT :limit
                """);
        query.setParameter("fromDate", fromDate);
        query.setParameter("toDate", toDate);
        query.setParameter("limit", limit);
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();
        return results;
    }

    public List<Object[]> topResources(Instant fromDate, Instant toDate, int limit) {
        Query query = entityManager.createNativeQuery("""
                SELECT
                    r.code AS resource_code,
                    r.name AS resource_name,
                    rt.code AS resource_type_code,
                    rt.name AS resource_type_name,
                    rg.code AS resource_group_code,
                    rg.name AS resource_group_name,
                    count(*) AS booking_count,
                    count(*) FILTER (WHERE b.status = 'CONFIRMED') AS confirmed_count,
                    count(*) FILTER (WHERE b.status = 'COMPLETED') AS completed_count,
                    count(*) FILTER (WHERE b.status = 'CANCELLED') AS cancelled_count,
                    coalesce(sum(b.total_amount), 0) AS revenue,
                    coalesce(sum(b.duration_minutes), 0) AS booked_minutes
                FROM booking b
                JOIN resource r ON r.id = b.resource_id AND r.deleted = false
                LEFT JOIN resource_type rt ON rt.id = r.type_id
                LEFT JOIN resource_group rg ON rg.id = r.group_id
                WHERE b.deleted = false
                  AND (CAST(:fromDate AS timestamp) IS NULL OR b.created_at >= CAST(:fromDate AS timestamp))
                  AND (CAST(:toDate AS timestamp) IS NULL OR b.created_at <= CAST(:toDate AS timestamp))
                GROUP BY r.code, r.name, rt.code, rt.name, rg.code, rg.name
                ORDER BY booking_count DESC, revenue DESC, r.code ASC
                LIMIT :limit
                """);
        query.setParameter("fromDate", fromDate);
        query.setParameter("toDate", toDate);
        query.setParameter("limit", limit);
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();
        return results;
    }

    public Object[] subscription() {
        return single("""
                SELECT
                    count(*) FILTER (WHERE status = 'ACTIVE') AS active_subscriptions,
                    count(*) FILTER (WHERE status = 'PENDING_ACTIVATION') AS pending_subscriptions,
                    count(*) FILTER (WHERE status = 'SUSPENDED') AS suspended_subscriptions,
                    (SELECT count(*) FROM subscription_pass WHERE status IN ('ACTIVE', 'PARTIALLY_USED')) AS active_passes,
                    coalesce(sum(total_amount) FILTER (WHERE status = 'ACTIVE' AND billing_cycle = 'MONTHLY'), 0) AS monthly_recurring_revenue,
                    (SELECT coalesce(sum(quantity), 0) FROM usage_record WHERE billable = true) AS billable_usage_quantity
                FROM subscription
                """, null, null);
    }

    public Object[] customer(Instant fromDate, Instant toDate) {
        return single("""
                SELECT
                    (SELECT count(*) FROM customer WHERE deleted = false) AS customer_count,
                    (SELECT count(*) FROM member WHERE deleted = false) AS member_count,
                    (SELECT count(*) FROM member WHERE deleted = false AND member_status = 'ACTIVE') AS active_members,
                    (SELECT count(*) FROM booking
                     WHERE deleted = false AND owner_type = 'GUEST'
                       AND (CAST(:fromDate AS timestamp) IS NULL OR created_at >= CAST(:fromDate AS timestamp))
                       AND (CAST(:toDate AS timestamp) IS NULL OR created_at <= CAST(:toDate AS timestamp))) AS guest_booking_count
                """, fromDate, toDate);
    }

    public Object[] inventory() {
        return single("""
                SELECT
                    (SELECT count(*) FROM inventory_item WHERE active = true) AS item_count,
                    (SELECT coalesce(sum(quantity_on_hand * average_cost), 0) FROM stock_level) AS stock_value,
                    (SELECT count(*) FROM inventory_alert WHERE status = 'OPEN' AND alert_type IN ('LOW_STOCK', 'OUT_OF_STOCK')) AS low_stock_alerts,
                    (SELECT count(*) FROM inventory_purchase_order WHERE status IN ('APPROVED', 'ORDERED', 'PARTIALLY_RECEIVED')) AS open_purchase_orders
                """, null, null);
    }

    public Object[] live() {
        Object result = entityManager.createNativeQuery("""
                SELECT
                    -- ── Instantané ─────────────────────────────────────────────
                    (SELECT COUNT(*) FROM booking
                     WHERE deleted = false AND status IN ('CONFIRMED', 'IN_PROGRESS')
                       AND started_at <= NOW() AND ended_at >= NOW()) AS active_bookings,

                    (SELECT COUNT(*) FROM booking
                     WHERE deleted = false AND status = 'IN_PROGRESS'
                       AND checked_in_at IS NOT NULL) AS checked_in_bookings,

                    (SELECT COUNT(DISTINCT resource_id) FROM booking
                     WHERE deleted = false AND status IN ('CONFIRMED', 'IN_PROGRESS')
                       AND started_at <= NOW() AND ended_at >= NOW()) AS occupied_resources,

                    (SELECT COUNT(*) FROM resource
                     WHERE deleted = false AND active = true AND booking_enabled = true) AS total_bookable_resources,

                    (SELECT COUNT(*) FROM booking
                     WHERE deleted = false AND status = 'PENDING_APPROVAL') AS pending_approval,

                    (SELECT COUNT(*) FROM booking
                     WHERE deleted = false AND status IN ('CONFIRMED', 'PENDING_APPROVAL')
                       AND started_at > NOW() AND started_at <= NOW() + INTERVAL '1 hour') AS upcoming_next_hour,

                    (SELECT COUNT(*) FROM booking_hold
                     WHERE status = 'ACTIVE' AND expires_at > NOW()) AS active_holds,

                    -- ── Aujourd'hui ─────────────────────────────────────────────
                    (SELECT COUNT(*) FROM booking
                     WHERE deleted = false AND started_at::date = CURRENT_DATE) AS bookings_today,

                    (SELECT COALESCE(SUM(total_amount), 0) FROM booking
                     WHERE deleted = false AND started_at::date = CURRENT_DATE
                       AND status IN ('CONFIRMED', 'IN_PROGRESS', 'COMPLETED')) AS revenue_today,

                    (SELECT COALESCE(AVG(total_amount), 0) FROM booking
                     WHERE deleted = false AND started_at::date = CURRENT_DATE
                       AND status IN ('CONFIRMED', 'IN_PROGRESS', 'COMPLETED')) AS avg_booking_amount,

                    (SELECT COALESCE(SUM(duration_minutes), 0) FROM booking
                     WHERE deleted = false AND started_at::date = CURRENT_DATE
                       AND status IN ('CONFIRMED', 'IN_PROGRESS', 'COMPLETED')) AS booked_minutes_today,

                    (SELECT COUNT(*) FROM booking
                     WHERE deleted = false AND started_at::date = CURRENT_DATE
                       AND status = 'CANCELLED') AS cancelled_today,

                    (SELECT COUNT(*) FROM booking
                     WHERE deleted = false AND started_at::date = CURRENT_DATE
                       AND status = 'NO_SHOW') AS no_show_today,

                    (SELECT COUNT(*) FROM booking
                     WHERE deleted = false AND checked_in_at IS NOT NULL
                       AND checked_in_at::date = CURRENT_DATE) AS check_ins_today,

                    (SELECT COUNT(*) FROM member
                     WHERE deleted = false AND created_at::date = CURRENT_DATE) AS new_members_today
                """).getSingleResult();
        return result instanceof Object[] row ? row : new Object[]{result};
    }

    private Object[] single(String sql, Instant fromDate, Instant toDate) {
        Query query = entityManager.createNativeQuery(sql);
        if (sql.contains(":fromDate")) {
            query.setParameter("fromDate", fromDate);
        }
        if (sql.contains(":toDate")) {
            query.setParameter("toDate", toDate);
        }
        Object result = query.getSingleResult();
        return result instanceof Object[] values ? values : new Object[]{result};
    }
}
