package com.sni.bokaticowork.features.subscription.repository;

import com.sni.bokaticowork.features.subscription.subscription.model.SubscriptionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubscriptionItemRepository extends JpaRepository<SubscriptionItem, Long> {

    @Query(nativeQuery = true, value = "SELECT * FROM subscription_item WHERE subscription_id = :subscriptionId ORDER BY created_at ASC")
    List<SubscriptionItem> findAllBySubscription(@Param("subscriptionId") Long subscriptionId);
}
