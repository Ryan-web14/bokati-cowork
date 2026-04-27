package com.sni.bokaticowork.features.subscription.repository;

import com.sni.bokaticowork.features.subscription.subscription.model.EntitlementLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EntitlementLedgerRepository extends JpaRepository<EntitlementLedger, Long> {

    @Query(nativeQuery = true, value = "SELECT EXISTS(SELECT 1 FROM entitlement_ledger WHERE idempotency_key = :idempotencyKey)")
    boolean existsByIdempotencyKey(@Param("idempotencyKey") String idempotencyKey);

    @Query(nativeQuery = true, value = "SELECT * FROM entitlement_ledger WHERE grant_id = :grantId ORDER BY created_at ASC")
    List<EntitlementLedger> findAllByGrantId(@Param("grantId") Long grantId);
}
