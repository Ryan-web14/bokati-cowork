package com.sni.bokaticowork.features.subscription.timeline.repository;

import com.sni.bokaticowork.features.subscription.timeline.model.SubscriptionTimelineEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface SubscriptionTimelineEventRepository extends JpaRepository<SubscriptionTimelineEvent, Long> {

    @Query(nativeQuery = true, value = "SELECT * FROM subscription_timeline_event WHERE event_number = :eventNumber")
    Optional<SubscriptionTimelineEvent> findByEventNumber(@Param("eventNumber") String eventNumber);

    @Query(nativeQuery = true, value = """
            SELECT ste.*
            FROM subscription_timeline_event ste
            LEFT JOIN subscription s ON s.id = ste.subscription_id
            WHERE (:subscriptionNumber IS NULL OR s.subscription_number = :subscriptionNumber)
              AND (:ownerType IS NULL OR ste.owner_type = :ownerType)
              AND (:ownerCode IS NULL OR ste.owner_code = :ownerCode)
              AND (:eventType IS NULL OR ste.event_type = :eventType)
              AND (:occurredFrom IS NULL OR ste.occurred_at >= :occurredFrom)
              AND (:occurredTo IS NULL OR ste.occurred_at <= :occurredTo)
            """,
            countQuery = """
            SELECT count(*)
            FROM subscription_timeline_event ste
            LEFT JOIN subscription s ON s.id = ste.subscription_id
            WHERE (:subscriptionNumber IS NULL OR s.subscription_number = :subscriptionNumber)
              AND (:ownerType IS NULL OR ste.owner_type = :ownerType)
              AND (:ownerCode IS NULL OR ste.owner_code = :ownerCode)
              AND (:eventType IS NULL OR ste.event_type = :eventType)
              AND (:occurredFrom IS NULL OR ste.occurred_at >= :occurredFrom)
              AND (:occurredTo IS NULL OR ste.occurred_at <= :occurredTo)
            """)
    Page<SubscriptionTimelineEvent> search(@Param("subscriptionNumber") String subscriptionNumber,
                                           @Param("ownerType") String ownerType,
                                           @Param("ownerCode") String ownerCode,
                                           @Param("eventType") String eventType,
                                           @Param("occurredFrom") Instant occurredFrom,
                                           @Param("occurredTo") Instant occurredTo,
                                           Pageable pageable);
}
