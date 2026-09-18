package com.sni.bokaticowork.core.communication.mailService.repository;

import com.sni.bokaticowork.core.communication.mailService.model.EmailDeliveryLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.Optional;

public interface EmailDeliveryLogRepository extends JpaRepository<EmailDeliveryLog, Long>, JpaSpecificationExecutor<EmailDeliveryLog> {

    Optional<EmailDeliveryLog> findByEmailNumber(String emailNumber);

    /**
     * Courriels enregistres mais jamais pris par un consommateur · un consommateur qui prend un
     * courriel le passe SENDING aussitot, donc un QUEUED qui vieillit n'est jamais arrive au broker.
     */
    java.util.List<EmailDeliveryLog> findTop50ByStatusAndCreatedAtBeforeOrderByCreatedAtAsc(
            com.sni.bokaticowork.core.communication.mailService.enums.EmailDeliveryStatus status,
            java.time.Instant before);

    Optional<EmailDeliveryLog> findFirstByDedupKeyInOrderByCreatedAtDesc(Collection<String> dedupKeys);
}
