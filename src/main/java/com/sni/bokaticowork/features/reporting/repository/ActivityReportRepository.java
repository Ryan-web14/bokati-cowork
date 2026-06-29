package com.sni.bokaticowork.features.reporting.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public class ActivityReportRepository {

    @PersistenceContext
    private EntityManager em;

    public Object[] bookingStats(Instant from, Instant to) {
        return single("""
                SELECT
                    count(*) AS total_count,
                    count(*) FILTER (WHERE status = 'CONFIRMED') AS confirmed,
                    count(*) FILTER (WHERE status = 'COMPLETED') AS completed,
                    count(*) FILTER (WHERE status = 'CANCELLED') AS cancelled,
                    coalesce(sum(total_amount), 0) AS revenue,
                    coalesce(sum(duration_minutes), 0) AS booked_minutes
                FROM booking
                WHERE deleted = false
                  AND (CAST(:from AS timestamp) IS NULL OR created_at >= CAST(:from AS timestamp))
                  AND (CAST(:to   AS timestamp) IS NULL OR created_at <= CAST(:to   AS timestamp))
                """, from, to);
    }

    public Object[] invoiceStats(Instant from, Instant to) {
        return single("""
                SELECT
                    count(*) AS total_count,
                    coalesce(sum(total_amount), 0) AS total_invoiced,
                    coalesce(sum(paid_amount), 0) AS total_paid,
                    coalesce(sum(balance_due), 0) AS total_outstanding,
                    count(*) FILTER (WHERE status = 'OVERDUE') AS overdue_count
                FROM billing_document
                WHERE document_type = 'INVOICE'
                  AND status NOT IN ('DRAFT','CANCELLED','VOIDED')
                  AND (CAST(:from AS timestamptz) IS NULL OR created_at >= CAST(:from AS timestamptz))
                  AND (CAST(:to   AS timestamptz) IS NULL OR created_at <= CAST(:to   AS timestamptz))
                """, from, to);
    }

    public Object[] paymentStats(Instant from, Instant to) {
        return single("""
                SELECT
                    count(*) FILTER (WHERE status = 'SUCCEEDED') AS transaction_count,
                    coalesce(sum(amount) FILTER (WHERE status = 'SUCCEEDED'), 0) AS total_received,
                    coalesce(sum(amount) FILTER (WHERE status = 'SUCCEEDED' AND payment_method = 'CASH'), 0) AS cash_amount,
                    coalesce(sum(amount) FILTER (WHERE status = 'SUCCEEDED' AND payment_method = 'WALLET'), 0) AS wallet_amount,
                    coalesce(sum(amount) FILTER (WHERE status = 'SUCCEEDED' AND payment_method = 'MOBILE_MONEY'), 0) AS mobile_money_amount,
                    coalesce(sum(amount) FILTER (WHERE status = 'SUCCEEDED' AND payment_method = 'BANK_TRANSFER'), 0) AS bank_transfer_amount,
                    coalesce(sum(amount) FILTER (WHERE status = 'SUCCEEDED' AND payment_method = 'CARD'), 0) AS card_amount
                FROM payment_transaction
                WHERE (CAST(:from AS timestamptz) IS NULL OR paid_at >= CAST(:from AS timestamptz))
                  AND (CAST(:to   AS timestamptz) IS NULL OR paid_at <= CAST(:to   AS timestamptz))
                """, from, to);
    }

    public Object[] subscriptionStats(Instant from, Instant to) {
        return single("""
                SELECT
                    count(*) FILTER (WHERE status = 'ACTIVE') AS active_count,
                    count(*) FILTER (WHERE
                        (CAST(:from AS timestamptz) IS NULL OR created_at >= CAST(:from AS timestamptz))
                        AND (CAST(:to AS timestamptz) IS NULL OR created_at <= CAST(:to AS timestamptz))
                    ) AS new_count,
                    count(*) FILTER (WHERE cancelled_at IS NOT NULL
                        AND (CAST(:from AS timestamptz) IS NULL OR cancelled_at >= CAST(:from AS timestamptz))
                        AND (CAST(:to AS timestamptz) IS NULL OR cancelled_at <= CAST(:to AS timestamptz))
                    ) AS cancelled_count,
                    count(*) FILTER (WHERE status = 'ACTIVE' AND auto_renew = true
                        AND current_period_start IS NOT NULL
                        AND (CAST(:from AS timestamptz) IS NULL OR current_period_start >= CAST(:from AS date))
                        AND (CAST(:to AS timestamptz) IS NULL OR current_period_start <= CAST(:to AS date))
                    ) AS renewed_count
                FROM subscription
                """, from, to);
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> stockMovementStats(Instant from, Instant to) {
        String sql = """
                SELECT
                    movement_type,
                    count(*) AS mv_count,
                    coalesce(sum(total_cost), 0) AS total_value
                FROM stock_movement
                WHERE (CAST(:from AS timestamptz) IS NULL OR performed_at >= CAST(:from AS timestamptz))
                  AND (CAST(:to   AS timestamptz) IS NULL OR performed_at <= CAST(:to   AS timestamptz))
                GROUP BY movement_type
                """;
        Query q = em.createNativeQuery(sql);
        q.setParameter("from", from);
        q.setParameter("to", to);
        return q.getResultList();
    }

    public Object[] contractStats(Instant from, Instant to) {
        return single("""
                SELECT
                    count(*) FILTER (WHERE signed_at IS NOT NULL
                        AND (CAST(:from AS timestamp) IS NULL OR signed_at >= CAST(:from AS timestamp))
                        AND (CAST(:to AS timestamp) IS NULL OR signed_at <= CAST(:to AS timestamp))
                    ) AS signed_count,
                    count(*) FILTER (WHERE status = 'EXPIRED'
                        AND end_date IS NOT NULL
                        AND (CAST(:from AS timestamp) IS NULL OR end_date >= CAST(:from AS date))
                        AND (CAST(:to AS timestamp) IS NULL OR end_date <= CAST(:to AS date))
                    ) AS expired_count,
                    count(*) FILTER (WHERE status = 'ACTIVE') AS active_count
                FROM contract_record
                WHERE deleted = false
                """, from, to);
    }

    public Object[] newMemberStats(Instant from, Instant to) {
        return single("""
                SELECT
                    count(*) FILTER (WHERE member_status = 'ACTIVE') AS total_active,
                    count(*) FILTER (WHERE
                        (CAST(:from AS timestamp) IS NULL OR created_at >= CAST(:from AS timestamp))
                        AND (CAST(:to AS timestamp) IS NULL OR created_at <= CAST(:to AS timestamp))
                    ) AS new_count
                FROM member
                WHERE deleted = false
                """, from, to);
    }

    public Object[] supportTicketStats(Instant from, Instant to) {
        return single("""
                SELECT
                    count(*) FILTER (WHERE
                        (CAST(:from AS timestamp) IS NULL OR created_at >= CAST(:from AS timestamp))
                        AND (CAST(:to AS timestamp) IS NULL OR created_at <= CAST(:to AS timestamp))
                    ) AS opened_count,
                    count(*) FILTER (WHERE resolved_at IS NOT NULL
                        AND (CAST(:from AS timestamp) IS NULL OR resolved_at >= CAST(:from AS timestamp))
                        AND (CAST(:to AS timestamp) IS NULL OR resolved_at <= CAST(:to AS timestamp))
                    ) AS resolved_count,
                    count(*) FILTER (WHERE closed_at IS NOT NULL
                        AND (CAST(:from AS timestamp) IS NULL OR closed_at >= CAST(:from AS timestamp))
                        AND (CAST(:to AS timestamp) IS NULL OR closed_at <= CAST(:to AS timestamp))
                    ) AS closed_count,
                    count(*) FILTER (WHERE priority IN ('HIGH','URGENT','CRITICAL')
                        AND (CAST(:from AS timestamp) IS NULL OR created_at >= CAST(:from AS timestamp))
                        AND (CAST(:to AS timestamp) IS NULL OR created_at <= CAST(:to AS timestamp))
                    ) AS high_priority_count
                FROM support_ticket
                """, from, to);
    }

    private Object[] single(String sql, Instant from, Instant to) {
        Query q = em.createNativeQuery(sql);
        if (sql.contains(":from")) q.setParameter("from", from);
        if (sql.contains(":to"))   q.setParameter("to", to);
        Object result = q.getSingleResult();
        return result instanceof Object[] arr ? arr : new Object[]{result};
    }
}
