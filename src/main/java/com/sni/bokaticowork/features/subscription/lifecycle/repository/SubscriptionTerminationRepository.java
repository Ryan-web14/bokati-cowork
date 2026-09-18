package com.sni.bokaticowork.features.subscription.lifecycle.repository;

import com.sni.bokaticowork.features.subscription.lifecycle.model.SubscriptionTermination;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionTerminationRepository extends JpaRepository<SubscriptionTermination, Long> {

    Optional<SubscriptionTermination> findByTerminationCode(String terminationCode);

    @Query(nativeQuery = true, value = "SELECT * FROM subscription_termination WHERE subscription_id = :subscriptionId AND status IN ('REQUESTED', 'ACCEPTED') LIMIT 1")
    Optional<SubscriptionTermination> findOpenBySubscription(@Param("subscriptionId") Long subscriptionId);

    List<SubscriptionTermination> findBySubscription_IdOrderByRequestedAtDesc(Long subscriptionId);

    /** Les preavis acceptes dont la date d'effet est atteinte · a achever. */
    @Query(nativeQuery = true, value = "SELECT * FROM subscription_termination WHERE status = 'ACCEPTED' AND effective_date <= :day ORDER BY effective_date ASC")
    List<SubscriptionTermination> findDue(@Param("day") LocalDate day);

    List<SubscriptionTermination> findByStatusOrderByEffectiveDateAsc(SubscriptionTermination.Status status);
}
