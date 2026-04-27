package com.sni.bokaticowork.features.subscription.metrics.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

@Repository
public class SubscriptionMetricsRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public Object[] overview() {
        return (Object[]) entityManager.createNativeQuery("""
                SELECT
                    (SELECT count(*) FROM subscription WHERE status = 'ACTIVE') AS active_subscriptions,
                    (SELECT count(*) FROM subscription WHERE status = 'PENDING_ACTIVATION') AS pending_subscriptions,
                    (SELECT count(*) FROM subscription WHERE status = 'SUSPENDED') AS suspended_subscriptions,
                    (SELECT count(*) FROM subscription WHERE status = 'CANCELLED') AS cancelled_subscriptions,
                    (SELECT count(*) FROM subscription_pass WHERE status = 'ACTIVE') AS active_passes,
                    (SELECT count(*) FROM subscription_addon WHERE status = 'ACTIVE') AS active_addons,
                    (SELECT count(*) FROM promotion WHERE status = 'ACTIVE') AS active_promotions,
                    (SELECT count(*) FROM billable_item WHERE status = 'PENDING') AS pending_billable_items,
                    (SELECT coalesce(sum(amount), 0) FROM billable_item WHERE status = 'PENDING') AS pending_billable_amount,
                    (SELECT coalesce(sum(total_amount), 0) FROM subscription WHERE status = 'ACTIVE' AND billing_cycle = 'MONTHLY') AS active_subscription_mrr,
                    (SELECT coalesce(sum(quantity), 0) FROM usage_record WHERE status IN ('RECORDED','BILLED')) AS recorded_usage_quantity,
                    (SELECT coalesce(sum(quantity), 0) FROM usage_record WHERE billable = true) AS billable_usage_quantity,
                    (SELECT count(*) FROM subscription_overage_charge) AS overage_charges,
                    (SELECT coalesce(sum(amount), 0) FROM subscription_overage_charge) AS overage_amount,
                    (SELECT count(*) FROM subscription_rollover_record) AS rollover_records,
                    (SELECT coalesce(sum(quantity), 0) FROM subscription_rollover_record) AS rollover_quantity
                """).getSingleResult();
    }
}
