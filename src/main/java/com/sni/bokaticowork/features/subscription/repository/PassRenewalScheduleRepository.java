package com.sni.bokaticowork.features.subscription.repository;

import com.sni.bokaticowork.features.subscription.subscription.enums.BillingScheduleStatus;
import com.sni.bokaticowork.features.subscription.subscription.model.PassRenewalSchedule;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface PassRenewalScheduleRepository extends JpaRepository<PassRenewalSchedule, Long> {

    /** Le pass est lu avec l'echeancier : le controleur le rend tel quel, hors transaction. */
    @EntityGraph(attributePaths = "pass")
    Optional<PassRenewalSchedule> findByPassId(Long passId);

    List<PassRenewalSchedule> findAllByStatusAndNextRenewalDateBefore(BillingScheduleStatus status, Instant before);
}
