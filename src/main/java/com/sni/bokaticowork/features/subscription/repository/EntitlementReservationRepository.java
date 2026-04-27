package com.sni.bokaticowork.features.subscription.repository;

import com.sni.bokaticowork.features.subscription.subscription.model.EntitlementReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface EntitlementReservationRepository extends JpaRepository<EntitlementReservation, Long> {

    @Query(nativeQuery = true, value = """
            SELECT *
            FROM entitlement_reservation
            WHERE owner_type = :ownerType
              AND owner_code = :ownerCode
              AND lower(entitlement_code) = lower(:entitlementCode)
              AND reference_type = :referenceType
              AND reference_id = :referenceId
              AND status = :status
            ORDER BY created_at ASC
            """)
    List<EntitlementReservation> findActiveByReference(@Param("ownerType") String ownerType,
                                                       @Param("ownerCode") String ownerCode,
                                                       @Param("entitlementCode") String entitlementCode,
                                                       @Param("referenceType") String referenceType,
                                                       @Param("referenceId") String referenceId,
                                                       @Param("status") String status);

    @Query(nativeQuery = true, value = """
            SELECT *
            FROM entitlement_reservation
            WHERE status = :status
              AND expires_at < :now
            ORDER BY expires_at ASC
            """)
    List<EntitlementReservation> findExpiredActiveReservations(@Param("status") String status, @Param("now") Instant now);
}
