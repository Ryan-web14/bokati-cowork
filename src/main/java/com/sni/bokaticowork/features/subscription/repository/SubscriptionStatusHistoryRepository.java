package com.sni.bokaticowork.features.subscription.repository;

import com.sni.bokaticowork.features.subscription.subscription.model.SubscriptionStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubscriptionStatusHistoryRepository extends JpaRepository<SubscriptionStatusHistory, Long> {

    @Query(nativeQuery = true, value = "SELECT * FROM subscription_status_history WHERE subscription_id = :subscriptionId ORDER BY changed_at ASC")
    List<SubscriptionStatusHistory> findAllBySubscriptionOrderByChangedAtAsc(@Param("subscriptionId") Long subscriptionId);
}
