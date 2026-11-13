package com.sni.bokaticowork.features.billing.repository;

import com.sni.bokaticowork.features.billing.model.BillingClaimReason;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Lot C11 : motifs de reclamation. */
@Repository
public interface BillingClaimReasonRepository extends JpaRepository<BillingClaimReason, Long> {

    Optional<BillingClaimReason> findByCode(String code);

    @Query("SELECT r FROM BillingClaimReason r ORDER BY r.displayOrder, r.label")
    List<BillingClaimReason> findAllOrdered();
}
