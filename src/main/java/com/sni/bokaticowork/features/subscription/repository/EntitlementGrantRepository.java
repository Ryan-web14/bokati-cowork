package com.sni.bokaticowork.features.subscription.repository;

import com.sni.bokaticowork.features.subscription.subscription.model.EntitlementGrant;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface EntitlementGrantRepository extends JpaRepository<EntitlementGrant, Long>, JpaSpecificationExecutor<EntitlementGrant> {

    @Query(nativeQuery = true, value = "SELECT * FROM entitlement_grant WHERE grant_number = :grantNumber")
    Optional<EntitlementGrant> findByGrantNumber(@Param("grantNumber") String grantNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(nativeQuery = true, value = "SELECT * FROM entitlement_grant WHERE id = :id FOR UPDATE")
    Optional<EntitlementGrant> findLockedById(@Param("id") Long id);

    @Query(nativeQuery = true, value = """
            SELECT eg.*
            FROM entitlement_grant eg
            JOIN entitlement_definition ed ON ed.id = eg.entitlement_definition_id
            WHERE eg.owner_type = :ownerType
              AND eg.owner_code = :ownerCode
              AND lower(ed.code) = lower(:entitlementCode)
              AND eg.status = 'ACTIVE'
              AND eg.valid_from <= :now
              AND (eg.valid_until IS NULL OR eg.valid_until >= :now)
            ORDER BY eg.priority ASC, eg.valid_until ASC NULLS LAST, eg.created_at ASC
            """)
    List<EntitlementGrant> findUsableGrants(@Param("ownerType") String ownerType,
                                            @Param("ownerCode") String ownerCode,
                                            @Param("entitlementCode") String entitlementCode,
                                            @Param("now") Instant now);

    @Query(nativeQuery = true, value = "SELECT * FROM entitlement_grant WHERE subscription_id = :subscriptionId ORDER BY created_at ASC")
    List<EntitlementGrant> findAllBySubscription(@Param("subscriptionId") Long subscriptionId);

    @Query(nativeQuery = true, value = "SELECT * FROM entitlement_grant WHERE status = :status AND valid_until < :validUntil ORDER BY valid_until ASC")
    List<EntitlementGrant> findAllByStatusAndValidUntilBefore(@Param("status") String status, @Param("validUntil") Instant validUntil);

    @Query(nativeQuery = true, value = """
            SELECT *
            FROM entitlement_grant
            WHERE owner_type = :ownerType
              AND owner_code = :ownerCode
              AND status = :status
            ORDER BY priority ASC, created_at ASC
            """)
    List<EntitlementGrant> findAllByOwnerTypeAndOwnerCodeAndStatus(@Param("ownerType") String ownerType,
                                                                    @Param("ownerCode") String ownerCode,
                                                                    @Param("status") String status);

    @Query(nativeQuery = true, value = """
            SELECT *
            FROM entitlement_grant
            WHERE owner_type = :ownerType
              AND owner_code = :ownerCode
              AND status = 'ACTIVE'
              AND valid_from <= :now
              AND (valid_until IS NULL OR valid_until >= :now)
            ORDER BY priority ASC, valid_until ASC NULLS LAST, created_at ASC
            """)
    List<EntitlementGrant> findActiveBalances(@Param("ownerType") String ownerType,
                                              @Param("ownerCode") String ownerCode,
                                              @Param("now") Instant now);

    @Query(nativeQuery = true, value = """
            SELECT eg.*
            FROM entitlement_grant eg
            JOIN entitlement_definition ed ON ed.id = eg.entitlement_definition_id
            LEFT JOIN resource_type rt ON rt.id = ed.resource_type_id
            LEFT JOIN resource_group rg ON rg.id = ed.resource_group_id
            WHERE eg.owner_type = :ownerType
              AND eg.owner_code = :ownerCode
              AND eg.subscription_id = :subscriptionId
              AND eg.status = 'ACTIVE'
              AND eg.valid_from <= :now
              AND (eg.valid_until IS NULL OR eg.valid_until >= :now)
              AND ed.active = true
              AND ed.deleted = false
              AND (ed.resource_type_id IS NULL OR rt.code = :resourceTypeCode)
              AND (ed.resource_group_id IS NULL OR rg.code = :resourceGroupCode)
            ORDER BY
              CASE WHEN ed.resource_group_id IS NULL THEN 0 ELSE 2 END DESC,
              CASE WHEN ed.resource_type_id IS NULL THEN 0 ELSE 1 END DESC,
              eg.priority ASC,
              eg.valid_until ASC NULLS LAST,
              eg.created_at ASC
            LIMIT 1
            """)
    Optional<EntitlementGrant> findUsableSubscriptionGrantForResource(@Param("ownerType") String ownerType,
                                                                      @Param("ownerCode") String ownerCode,
                                                                      @Param("subscriptionId") Long subscriptionId,
                                                                      @Param("resourceTypeCode") String resourceTypeCode,
                                                                      @Param("resourceGroupCode") String resourceGroupCode,
                                                                      @Param("now") Instant now);

    @Query(nativeQuery = true, value = """
            SELECT eg.*
            FROM entitlement_grant eg
            JOIN entitlement_definition ed ON ed.id = eg.entitlement_definition_id
            JOIN subscription_pass sp ON sp.id = eg.pass_id
            LEFT JOIN resource_type rt ON rt.id = ed.resource_type_id
            LEFT JOIN resource_group rg ON rg.id = ed.resource_group_id
            WHERE eg.owner_type = :ownerType
              AND eg.owner_code = :ownerCode
              AND eg.pass_id IS NOT NULL
              AND eg.status = 'ACTIVE'
              AND eg.valid_from <= :now
              AND (eg.valid_until IS NULL OR eg.valid_until >= :now)
              AND sp.status = 'ACTIVE'
              AND sp.valid_from <= :now
              AND (sp.valid_until IS NULL OR sp.valid_until >= :now)
              AND (sp.max_uses IS NULL OR sp.used_count < sp.max_uses)
              AND ed.active = true
              AND ed.deleted = false
              AND (ed.resource_type_id IS NULL OR rt.code = :resourceTypeCode)
              AND (ed.resource_group_id IS NULL OR rg.code = :resourceGroupCode)
            ORDER BY
              CASE WHEN ed.resource_group_id IS NULL THEN 0 ELSE 2 END DESC,
              CASE WHEN ed.resource_type_id IS NULL THEN 0 ELSE 1 END DESC,
              eg.priority ASC,
              eg.valid_until ASC NULLS LAST,
              eg.created_at ASC
            LIMIT 1
            """)
    Optional<EntitlementGrant> findUsablePassGrantForResource(@Param("ownerType") String ownerType,
                                                              @Param("ownerCode") String ownerCode,
                                                              @Param("resourceTypeCode") String resourceTypeCode,
                                                              @Param("resourceGroupCode") String resourceGroupCode,
                                                              @Param("now") Instant now);

    @Query(nativeQuery = true, value = """
            SELECT eg.*
            FROM entitlement_grant eg
            JOIN subscription s ON s.id = eg.subscription_id
            JOIN subscription_plan_entitlement pe
              ON pe.plan_version_id = s.plan_version_id
             AND pe.entitlement_definition_id = eg.entitlement_definition_id
            WHERE eg.unlimited = false
              AND eg.quantity_remaining > 0
              AND eg.valid_until IS NOT NULL
              AND eg.valid_until < :now
              AND eg.status IN ('ACTIVE', 'EXPIRED')
              AND eg.source_type <> 'ROLLOVER'
              AND s.status = 'ACTIVE'
              AND pe.rollover_allowed = true
              AND NOT EXISTS (
                  SELECT 1
                  FROM subscription_rollover_record rr
                  WHERE rr.source_grant_id = eg.id
              )
            ORDER BY eg.valid_until ASC, eg.created_at ASC
            """)
    List<EntitlementGrant> findRolloverCandidates(@Param("now") Instant now);
}
