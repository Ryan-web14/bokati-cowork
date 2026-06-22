package com.sni.bokaticowork.features.reporting.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class SubscriptionReportRepository {

    @PersistenceContext
    private EntityManager em;

    @SuppressWarnings("unchecked")
    public List<Object[]> mrrTrend(int months) {
        Query q = em.createNativeQuery("""
                SELECT
                    m.month::date AS month,
                    coalesce(sum(s.total_amount) FILTER (WHERE s.status = 'ACTIVE' AND s.billing_cycle = 'MONTHLY'), 0) AS mrr,
                    count(*) FILTER (WHERE s.status = 'ACTIVE') AS active_count
                FROM generate_series(
                    date_trunc('month', NOW()) - (CAST(:months AS integer) - 1) * INTERVAL '1 month',
                    date_trunc('month', NOW()),
                    '1 month'
                ) AS m(month)
                LEFT JOIN subscription s ON s.start_date <= m.month + INTERVAL '1 month' - INTERVAL '1 day'
                    AND (s.cancelled_at IS NULL OR s.cancelled_at >= m.month)
                    AND s.status NOT IN ('DRAFT','CANCELLED')
                GROUP BY m.month
                ORDER BY m.month
                """);
        q.setParameter("months", months);
        return q.getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> activeByPlan() {
        Query q = em.createNativeQuery("""
                SELECT
                    sp.name AS plan_name,
                    count(*) AS subscription_count,
                    coalesce(sum(s.total_amount), 0) AS total_amount
                FROM subscription s
                JOIN subscription_plan_version spv ON spv.id = s.plan_version_id
                JOIN subscription_plan sp ON sp.id = spv.plan_id
                WHERE s.status = 'ACTIVE'
                GROUP BY sp.name
                ORDER BY subscription_count DESC
                """);
        return q.getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> churnTrend(int months) {
        Query q = em.createNativeQuery("""
                SELECT
                    m.month::date AS month,
                    count(*) FILTER (WHERE s.cancelled_at >= m.month
                        AND s.cancelled_at < m.month + INTERVAL '1 month') AS cancelled_count,
                    count(*) AS total_count
                FROM generate_series(
                    date_trunc('month', NOW()) - (CAST(:months AS integer) - 1) * INTERVAL '1 month',
                    date_trunc('month', NOW()),
                    '1 month'
                ) AS m(month)
                LEFT JOIN subscription s ON s.start_date <= m.month + INTERVAL '1 month' - INTERVAL '1 day'
                    AND (s.cancelled_at IS NULL OR s.cancelled_at >= m.month)
                    AND s.status NOT IN ('DRAFT')
                GROUP BY m.month
                ORDER BY m.month
                """);
        q.setParameter("months", months);
        return q.getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> upcomingRenewals(int days) {
        Query q = em.createNativeQuery("""
                SELECT
                    s.subscription_number,
                    s.subscriber_code,
                    s.subscriber_type,
                    sp.name AS plan_name,
                    s.total_amount,
                    s.currency,
                    s.current_period_end,
                    (s.current_period_end - CURRENT_DATE) AS days_until
                FROM subscription s
                JOIN subscription_plan_version spv ON spv.id = s.plan_version_id
                JOIN subscription_plan sp ON sp.id = spv.plan_id
                WHERE s.status = 'ACTIVE'
                  AND s.auto_renew = true
                  AND s.current_period_end IS NOT NULL
                  AND s.current_period_end <= CURRENT_DATE + CAST(:days AS integer) * INTERVAL '1 day'
                  AND s.current_period_end >= CURRENT_DATE
                ORDER BY s.current_period_end
                """);
        q.setParameter("days", days);
        return q.getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> revenueByPlan() {
        Query q = em.createNativeQuery("""
                SELECT
                    sp.name AS plan_name,
                    count(*) AS subscription_count,
                    coalesce(sum(s.total_amount), 0) AS total_revenue
                FROM subscription s
                JOIN subscription_plan_version spv ON spv.id = s.plan_version_id
                JOIN subscription_plan sp ON sp.id = spv.plan_id
                WHERE s.status = 'ACTIVE'
                GROUP BY sp.name
                ORDER BY total_revenue DESC
                """);
        return q.getResultList();
    }
}
