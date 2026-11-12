package com.sni.bokaticowork.features.domiciliation.repository;

import com.sni.bokaticowork.features.domiciliation.model.SubscriptionService;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionServiceRepository extends JpaRepository<SubscriptionService, Long> {

    /** Abonnement et definition lus avec le service : SubscriptionServiceView les lit hors transaction. */
    @EntityGraph(attributePaths = {"subscription", "serviceDefinition"})
    Optional<SubscriptionService> findByServiceNumber(String serviceNumber);

    @EntityGraph(attributePaths = {"subscription", "serviceDefinition"})
    List<SubscriptionService> findBySubscription_IdOrderByCreatedAtAsc(Long subscriptionId);

    Optional<SubscriptionService> findFirstBySubscription_IdAndServiceDefinition_CodeAndStatusNot(
            Long subscriptionId, String serviceCode, SubscriptionService.Status status);
}
