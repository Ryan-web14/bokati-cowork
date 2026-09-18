package com.sni.bokaticowork.features.subscription.lifecycle.repository;

import com.sni.bokaticowork.features.subscription.lifecycle.model.SubscriptionCommitment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionCommitmentRepository extends JpaRepository<SubscriptionCommitment, Long> {

    Optional<SubscriptionCommitment> findBySubscription_Id(Long subscriptionId);

    /** Les engagements arrives a terme · a reconduire ou a laisser tomber. */
    @Query(nativeQuery = true, value = "SELECT * FROM subscription_commitment WHERE commitment_end < :day")
    List<SubscriptionCommitment> findAllEndedBefore(@Param("day") LocalDate day);
}
