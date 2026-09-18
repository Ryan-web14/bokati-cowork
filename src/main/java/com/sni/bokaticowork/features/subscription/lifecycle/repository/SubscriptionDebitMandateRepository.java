package com.sni.bokaticowork.features.subscription.lifecycle.repository;

import com.sni.bokaticowork.features.subscription.lifecycle.model.SubscriptionDebitMandate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionDebitMandateRepository extends JpaRepository<SubscriptionDebitMandate, Long> {

    Optional<SubscriptionDebitMandate> findByMandateCode(String mandateCode);

    @Query(nativeQuery = true, value = "SELECT * FROM subscription_debit_mandate WHERE subscription_id = :subscriptionId AND status IN ('ACTIVE', 'SUSPENDED') LIMIT 1")
    Optional<SubscriptionDebitMandate> findCurrentBySubscription(@Param("subscriptionId") Long subscriptionId);

    List<SubscriptionDebitMandate> findBySubscription_IdOrderByCreatedAtDesc(Long subscriptionId);
}
