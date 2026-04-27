package com.sni.bokaticowork.features.analytics.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public class AnalyticsRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public Object[] financial(Instant fromDate, Instant toDate) {
        return single("""
                SELECT
                    count(*) FILTER (WHERE document_type = 'INVOICE') AS invoice_count,
                    coalesce(sum(total_amount) FILTER (WHERE document_type = 'INVOICE'), 0) AS invoiced_amount,
                    coalesce(sum(paid_amount) FILTER (WHERE document_type = 'INVOICE'), 0) AS paid_amount,
                    coalesce(sum(balance_due) FILTER (WHERE document_type = 'INVOICE'), 0) AS outstanding_amount,
                    count(*) FILTER (WHERE document_type = 'INVOICE' AND status = 'OVERDUE') AS overdue_invoice_count,
                    count(*) FILTER (WHERE document_type = 'QUOTE') AS quotation_count,
                    coalesce(sum(total_amount) FILTER (WHERE document_type = 'QUOTE'), 0) AS quoted_amount,
                    coalesce(sum(vat_amount), 0) AS vat_amount,
                    coalesce(sum(additional_cent_amount), 0) AS additional_cent_amount
                FROM billing_document
                WHERE (CAST(:fromDate AS timestamptz) IS NULL OR created_at >= CAST(:fromDate AS timestamptz))
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
