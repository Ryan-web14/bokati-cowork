package com.sni.bokaticowork.features.reporting.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class DebtRecoveryReportRepository {

    @PersistenceContext
    private EntityManager em;

    @SuppressWarnings("unchecked")
    public List<Object[]> unpaidInvoices(String sortBy) {
        String orderClause = switch (sortBy) {
            case "amount" -> "bd.balance_due DESC";
            case "customer" -> "bd.customer_name ASC, bd.balance_due DESC";
            default -> "(CURRENT_DATE - bd.due_date) DESC NULLS LAST";
        };
        Query q = em.createNativeQuery("""
                SELECT
                    bd.document_number,
                    bd.customer_name,
                    bd.customer_code,
                    bd.customer_type,
                    bd.customer_email,
                    bd.customer_phone,
                    bd.issue_date,
                    bd.due_date,
                    bd.total_amount,
                    bd.paid_amount,
                    bd.balance_due,
                    CASE WHEN bd.due_date IS NULL THEN 0
                         ELSE GREATEST(CURRENT_DATE - bd.due_date, 0) END AS aging_days,
                    CASE
                        WHEN bd.due_date IS NULL OR bd.due_date >= CURRENT_DATE       THEN 'CURRENT'
                        WHEN bd.due_date >= CURRENT_DATE - 30  THEN '1_30'
                        WHEN bd.due_date >= CURRENT_DATE - 60  THEN '31_60'
                        WHEN bd.due_date >= CURRENT_DATE - 90  THEN '61_90'
                        ELSE '90_PLUS'
                    END AS aging_bucket
                FROM billing_document bd
                WHERE bd.document_type = 'INVOICE'
                  AND bd.status NOT IN ('DRAFT','CANCELLED','VOIDED','PAID')
                  AND bd.balance_due > 0
                ORDER BY """ + orderClause);
        return q.getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> agingSummary() {
        Query q = em.createNativeQuery("""
                SELECT
                    CASE
                        WHEN due_date IS NULL OR due_date >= CURRENT_DATE       THEN 'CURRENT'
                        WHEN due_date >= CURRENT_DATE - INTERVAL '30 days'      THEN '1_30'
                        WHEN due_date >= CURRENT_DATE - INTERVAL '60 days'      THEN '31_60'
                        WHEN due_date >= CURRENT_DATE - INTERVAL '90 days'      THEN '61_90'
                        ELSE '90_PLUS'
                    END AS bucket,
                    count(*)                        AS invoice_count,
                    coalesce(sum(balance_due), 0)   AS total_amount,
                    coalesce(avg(balance_due), 0)   AS avg_amount
                FROM billing_document
                WHERE document_type = 'INVOICE'
                  AND status NOT IN ('DRAFT','CANCELLED','VOIDED','PAID')
                  AND balance_due > 0
                GROUP BY bucket
                """);
        return q.getResultList();
    }
}
