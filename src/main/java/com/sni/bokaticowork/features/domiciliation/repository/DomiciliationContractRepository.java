package com.sni.bokaticowork.features.domiciliation.repository;

import com.sni.bokaticowork.features.domiciliation.model.DomiciliationContract;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface DomiciliationContractRepository extends JpaRepository<DomiciliationContract, Long> {

    Optional<DomiciliationContract> findByContractNumber(String contractNumber);

    Optional<DomiciliationContract> findFirstBySubscription_IdAndStatusNotInOrderByCreatedAtDesc(
            Long subscriptionId, Collection<DomiciliationContract.Status> statuses);

    Page<DomiciliationContract> findByStatusInOrderByCreatedAtDesc(Collection<DomiciliationContract.Status> statuses, Pageable pageable);

    Page<DomiciliationContract> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<DomiciliationContract> findBySubscription_Id(Long subscriptionId);

    /** Les contrats d'un titulaire · ce que l'espace client montre. */
    List<DomiciliationContract> findBySubscription_SubscriberTypeAndSubscription_SubscriberCodeOrderByCreatedAtDesc(
            com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType subscriberType, String subscriberCode);

    /** Domicilies en cours a une adresse · le registre par adresse. */
    long countByAssignedAddress_IdAndStatusNotIn(Long addressId, Collection<DomiciliationContract.Status> statuses);

    /** Attestations qui expirent dans la fenetre et n'ont pas ete revoquees · la relance. */
    @Query("""
            SELECT c FROM DomiciliationContract c
            WHERE c.certificateDocumentCode IS NOT NULL AND c.certificateRevokedAt IS NULL
              AND c.certificateValidUntil IS NOT NULL
              AND c.certificateValidUntil BETWEEN :from AND :to
              AND c.status = com.sni.bokaticowork.features.domiciliation.model.DomiciliationContract.Status.ACTIVE
            """)
    List<DomiciliationContract> findCertificatesExpiringBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /** Le registre des domicilies · tout, dans l'ordre des entrees. */
    @Query("SELECT c FROM DomiciliationContract c ORDER BY c.startDate ASC NULLS LAST, c.createdAt ASC")
    List<DomiciliationContract> registry();

    long countByStatus(DomiciliationContract.Status status);
}
