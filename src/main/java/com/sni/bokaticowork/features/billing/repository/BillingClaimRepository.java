package com.sni.bokaticowork.features.billing.repository;

import com.sni.bokaticowork.features.billing.model.BillingClaim;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Lot C11 : reclamations sur facture. */
@Repository
public interface BillingClaimRepository extends JpaRepository<BillingClaim, Long> {

    Optional<BillingClaim> findByClaimNumber(String claimNumber);

    List<BillingClaim> findByDocumentNumberOrderByCreatedAtDesc(String documentNumber);

    boolean existsByDocumentNumberAndStatus(String documentNumber, String status);

    List<BillingClaim> findByCustomerTypeAndCustomerCodeOrderByCreatedAtDesc(String customerType, String customerCode);

    List<BillingClaim> findByStatusOrderByCreatedAtAsc(String status);
}
