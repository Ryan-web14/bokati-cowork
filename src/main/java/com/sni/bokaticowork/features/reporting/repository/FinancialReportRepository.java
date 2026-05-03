package com.sni.bokaticowork.features.reporting.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public class FinancialReportRepository {

    @PersistenceContext
    private EntityManager em;

    public Object[] financialKpis(Instant from, Instant to) {
        return single("""
                SELECT
                    count(*) FILTER (WHERE document_type = 'INVOICE' AND status NOT IN ('DRAFT','CANCELLED','VOIDED')) AS invoice_count,
                    coalesce(sum(total_amount) FILTER (WHERE document_type = 'INVOICE' AND status NOT IN ('DRAFT','CANCELLED','VOIDED')), 0) AS total_invoiced,
                    coalesce(sum(paid_amount)  FILTER (WHERE document_type = 'INVOICE'), 0) AS total_paid,
                    coalesce(sum(balance_due)  FILTER (WHERE document_type = 'INVOICE' AND status NOT IN ('DRAFT','CANCELLED','VOIDED','PAID')), 0) AS total_outstanding,
                    count(*) FILTER (WHERE document_type = 'INVOICE' AND status = 'OVERDUE') AS overdue_count,
                    coalesce(sum(balance_due)  FILTER (WHERE document_type = 'INVOICE' AND status = 'OVERDUE'), 0) AS overdue_amount,
                    coalesce(sum(vat_amount)   FILTER (WHERE document_type = 'INVOICE'), 0) AS total_vat,
                    coalesce(sum(additional_cent_amount) FILTER (WHERE document_type = 'INVOICE'), 0) AS total_additional_cent,
                    coalesce(sum(discount_amount) FILTER (WHERE document_type = 'INVOICE'), 0) AS total_discounts,
                    count(*) FILTER (WHERE document_type = 'INVOICE' AND status IN ('CANCELLED','VOIDED')) AS cancelled_count
                FROM billing_document
                WHERE (CAST(:from AS timestamptz) IS NULL OR created_at >= CAST(:from AS timestamptz))
                  AND (CAST(:to   AS timestamptz) IS NULL OR created_at <= CAST(:to   AS timestamptz))
                """, from, to);
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> revenueBySource(Instant from, Instant to) {
        return list("""
                SELECT
                    coalesce(source_type, 'MANUAL') AS source_type,
                    count(*) AS invoice_count,
                    coalesce(sum(total_amount), 0) AS total_invoiced,
                    coalesce(sum(paid_amount),  0) AS total_paid,
                    coalesce(sum(balance_due),  0) AS outstanding
                FROM billing_document
                WHERE document_type = 'INVOICE'
                  AND status NOT IN ('DRAFT','CANCELLED','VOIDED')
                  AND (CAST(:from AS timestamptz) IS NULL OR created_at >= CAST(:from AS timestamptz))
                  AND (CAST(:to   AS timestamptz) IS NULL OR created_at <= CAST(:to   AS timestamptz))
                GROUP BY source_type
                ORDER BY total_invoiced DESC
                """, from, to);
    }

    public Object[] paymentSummary(Instant from, Instant to) {
        return single("""
                SELECT
                    count(*) AS transaction_count,
                    coalesce(sum(amount) FILTER (WHERE status = 'SUCCEEDED'), 0) AS total_received,
                    coalesce(sum(amount) FILTER (WHERE status = 'SUCCEEDED' AND payment_method = 'CASH'), 0) AS cash_amount,
                    coalesce(sum(amount) FILTER (WHERE status = 'SUCCEEDED' AND payment_method = 'WALLET'), 0) AS wallet_amount,
                    coalesce(sum(amount) FILTER (WHERE status = 'SUCCEEDED' AND payment_method = 'MOBILE_MONEY'), 0) AS mobile_money_amount,
                    coalesce(sum(amount) FILTER (WHERE status = 'SUCCEEDED' AND payment_method = 'BANK_TRANSFER'), 0) AS bank_transfer_amount,
                    coalesce(sum(amount) FILTER (WHERE status = 'SUCCEEDED' AND payment_method = 'CARD'), 0) AS card_amount,
                    coalesce(sum(amount) FILTER (WHERE status = 'SUCCEEDED' AND payment_method = 'CHEQUE'), 0) AS cheque_amount,
                    count(*) FILTER (WHERE status = 'FAILED') AS failed_count,
                    count(*) FILTER (WHERE status IN ('PENDING','PROCESSING')) AS pending_count
                FROM payment_transaction
                WHERE (CAST(:from AS timestamptz) IS NULL OR paid_at >= CAST(:from AS timestamptz))
                  AND (CAST(:to   AS timestamptz) IS NULL OR paid_at <= CAST(:to   AS timestamptz))
                """, from, to);
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> paymentByMethod(Instant from, Instant to) {
        return list("""
                SELECT
                    payment_method,
                    count(*) AS tx_count,
                    coalesce(sum(amount), 0) AS amount
                FROM payment_transaction
                WHERE status = 'SUCCEEDED'
                  AND (CAST(:from AS timestamptz) IS NULL OR paid_at >= CAST(:from AS timestamptz))
                  AND (CAST(:to   AS timestamptz) IS NULL OR paid_at <= CAST(:to   AS timestamptz))
                GROUP BY payment_method
                ORDER BY amount DESC
                """, from, to);
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> paymentDailySeries(Instant from, Instant to) {
        return list("""
                SELECT
                    DATE(paid_at AT TIME ZONE 'UTC') AS day,
                    coalesce(sum(amount) FILTER (WHERE payment_method = 'CASH'), 0)          AS cash,
                    coalesce(sum(amount) FILTER (WHERE payment_method = 'WALLET'), 0)        AS wallet,
                    coalesce(sum(amount) FILTER (WHERE payment_method = 'MOBILE_MONEY'), 0)  AS mobile_money,
                    coalesce(sum(amount) FILTER (WHERE payment_method = 'BANK_TRANSFER'), 0) AS bank_transfer,
                    coalesce(sum(amount) FILTER (WHERE payment_method = 'CARD'), 0)          AS card,
                    coalesce(sum(amount) FILTER (WHERE payment_method = 'CHEQUE'), 0)        AS cheque,
                    coalesce(sum(amount), 0)                                                  AS total
                FROM payment_transaction
                WHERE status = 'SUCCEEDED'
                  AND (CAST(:from AS timestamptz) IS NULL OR paid_at >= CAST(:from AS timestamptz))
                  AND (CAST(:to   AS timestamptz) IS NULL OR paid_at <= CAST(:to   AS timestamptz))
                GROUP BY DATE(paid_at AT TIME ZONE 'UTC')
                ORDER BY day
                """, from, to);
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> billingAging() {
        String sql = """
                SELECT
                    CASE
                        WHEN due_date IS NULL OR due_date >= CURRENT_DATE       THEN 'CURRENT'
                        WHEN due_date >= CURRENT_DATE - INTERVAL '30 days'      THEN '1_30'
                        WHEN due_date >= CURRENT_DATE - INTERVAL '60 days'      THEN '31_60'
                        WHEN due_date >= CURRENT_DATE - INTERVAL '90 days'      THEN '61_90'
                        ELSE '90_PLUS'
                    END AS bucket,
                    count(*)                        AS invoice_count,
                    coalesce(sum(balance_due), 0)   AS outstanding_amount
                FROM billing_document
                WHERE document_type = 'INVOICE'
                  AND status NOT IN ('DRAFT','CANCELLED','VOIDED','PAID')
                  AND balance_due > 0
                GROUP BY bucket
                """;
        Query q = em.createNativeQuery(sql);
        return q.getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> cashRegisterReport(Instant from, Instant to) {
        return list("""
                SELECT
                    cr.register_code,
                    cr.name                                                              AS register_name,
                    count(DISTINCT cs.id)                                               AS sessions_count,
                    coalesce(sum(cs.opening_amount), 0)                                 AS total_opening,
                    coalesce(sum(cm.amount) FILTER (WHERE cm.movement_type IN (
                        'PAYMENT','CASH_IN','TRANSFER_IN','OPENING_FLOAT')), 0)         AS cash_in,
                    coalesce(sum(cm.amount) FILTER (WHERE cm.movement_type IN (
                        'REFUND','CASH_OUT','TRANSFER_OUT','SAFE_DEPOSIT')), 0)         AS cash_out,
                    coalesce(sum(cs.variance_amount), 0)                                AS total_variance,
                    count(cm.id)                                                        AS movements_count
                FROM cash_register cr
                LEFT JOIN cash_session cs ON cs.cash_register_id = cr.id
                    AND (CAST(:from AS timestamptz) IS NULL OR cs.opened_at >= CAST(:from AS timestamptz))
                    AND (CAST(:to   AS timestamptz) IS NULL OR cs.opened_at <= CAST(:to   AS timestamptz))
                LEFT JOIN cash_movement cm ON cm.cash_session_id = cs.id
                    AND cm.movement_type NOT IN ('OPENING_FLOAT','CLOSING_COUNT')
                WHERE cr.active = true
                GROUP BY cr.id, cr.register_code, cr.name
                ORDER BY cash_in DESC
                """, from, to);
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> cashFlowByType(Instant from, Instant to) {
        return list("""
                SELECT
                    cm.movement_type,
                    CASE WHEN cm.movement_type IN ('PAYMENT','CASH_IN','TRANSFER_IN','OPENING_FLOAT')
                         THEN 'IN' ELSE 'OUT' END AS direction,
                    count(*) AS mv_count,
                    coalesce(sum(cm.amount), 0) AS total_amount
                FROM cash_movement cm
                JOIN cash_session cs ON cs.id = cm.cash_session_id
                WHERE cm.movement_type NOT IN ('OPENING_FLOAT','CLOSING_COUNT')
                  AND (CAST(:from AS timestamptz) IS NULL OR cm.created_at >= CAST(:from AS timestamptz))
                  AND (CAST(:to   AS timestamptz) IS NULL OR cm.created_at <= CAST(:to   AS timestamptz))
                GROUP BY cm.movement_type
                ORDER BY total_amount DESC
                """, from, to);
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> cashFlowDailySeries(Instant from, Instant to) {
        return list("""
                SELECT
                    DATE(cm.created_at AT TIME ZONE 'UTC') AS day,
                    coalesce(sum(cm.amount) FILTER (WHERE cm.movement_type IN (
                        'PAYMENT','CASH_IN','TRANSFER_IN')), 0) AS inflows,
                    coalesce(sum(cm.amount) FILTER (WHERE cm.movement_type IN (
                        'REFUND','CASH_OUT','TRANSFER_OUT','SAFE_DEPOSIT')), 0) AS outflows
                FROM cash_movement cm
                JOIN cash_session cs ON cs.id = cm.cash_session_id
                WHERE cm.movement_type NOT IN ('OPENING_FLOAT','CLOSING_COUNT','ADJUSTMENT')
                  AND (CAST(:from AS timestamptz) IS NULL OR cm.created_at >= CAST(:from AS timestamptz))
                  AND (CAST(:to   AS timestamptz) IS NULL OR cm.created_at <= CAST(:to   AS timestamptz))
                GROUP BY DATE(cm.created_at AT TIME ZONE 'UTC')
                ORDER BY day
                """, from, to);
    }

    private Object[] single(String sql, Instant from, Instant to) {
        Query q = em.createNativeQuery(sql);
        if (sql.contains(":from")) q.setParameter("from", from);
        if (sql.contains(":to"))   q.setParameter("to", to);
        Object result = q.getSingleResult();
        return result instanceof Object[] arr ? arr : new Object[]{result};
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> list(String sql, Instant from, Instant to) {
        Query q = em.createNativeQuery(sql);
        if (sql.contains(":from")) q.setParameter("from", from);
        if (sql.contains(":to"))   q.setParameter("to", to);
        return q.getResultList();
    }
}