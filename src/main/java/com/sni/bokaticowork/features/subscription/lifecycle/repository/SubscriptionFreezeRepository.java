package com.sni.bokaticowork.features.subscription.lifecycle.repository;

import com.sni.bokaticowork.features.subscription.lifecycle.model.SubscriptionFreeze;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionFreezeRepository extends JpaRepository<SubscriptionFreeze, Long> {

    List<SubscriptionFreeze> findBySubscription_IdOrderByStartedOnDesc(Long subscriptionId);

    @Query(nativeQuery = true, value = "SELECT * FROM subscription_freeze WHERE subscription_id = :subscriptionId AND started_on >= :since ORDER BY started_on DESC")
    List<SubscriptionFreeze> findBySubscriptionSince(@Param("subscriptionId") Long subscriptionId, @Param("since") LocalDate since);

    @Query(nativeQuery = true, value = "SELECT * FROM subscription_freeze WHERE subscription_id = :subscriptionId AND resumed_on IS NULL ORDER BY started_on DESC LIMIT 1")
    Optional<SubscriptionFreeze> findOpenBySubscription(@Param("subscriptionId") Long subscriptionId);
}
