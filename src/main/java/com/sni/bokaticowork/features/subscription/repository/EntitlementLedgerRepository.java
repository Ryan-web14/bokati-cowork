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

    /**
     * Ce que chaque beneficiaire a consomme sur les droits d'un pass · par personne et par droit.
     * Lignes : type, code du beneficiaire, code du droit, quantite.
     */
    @org.springframework.data.jpa.repository.Query(nativeQuery = true, value = """
            SELECT el.beneficiary_type, el.beneficiary_code, ed.code, SUM(el.quantity)
            FROM entitlement_ledger el
            JOIN entitlement_grant eg ON eg.id = el.grant_id
            JOIN entitlement_definition ed ON ed.id = eg.entitlement_definition_id
            WHERE eg.pass_id = :passId
              AND el.transaction_type = 'CONSUME'
              AND el.beneficiary_code IS NOT NULL
            GROUP BY el.beneficiary_type, el.beneficiary_code, ed.code
            """)
    java.util.List<Object[]> sumConsumedByBeneficiaryOnPass(@org.springframework.data.repository.query.Param("passId") Long passId);
}
