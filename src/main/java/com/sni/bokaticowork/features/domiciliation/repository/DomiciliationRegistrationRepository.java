package com.sni.bokaticowork.features.domiciliation.repository;

import com.sni.bokaticowork.features.domiciliation.model.DomiciliationRegistration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface DomiciliationRegistrationRepository extends JpaRepository<DomiciliationRegistration, Long> {

    Optional<DomiciliationRegistration> findByRegistrationNumber(String registrationNumber);

    List<DomiciliationRegistration> findByContract_IdOrderByCreatedAtDesc(Long contractId);

    Optional<DomiciliationRegistration> findFirstByContract_IdAndStatusInOrderByCreatedAtDesc(
            Long contractId, Collection<DomiciliationRegistration.Status> statuses);

    /** Le tableau de suivi · la ou les dossiers s'enlisent. */
    Page<DomiciliationRegistration> findByStatusInOrderBySubmittedAtAsc(Collection<DomiciliationRegistration.Status> statuses, Pageable pageable);
}
