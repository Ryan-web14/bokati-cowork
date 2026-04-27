package com.sni.bokaticowork.features.subscription.repository;

import com.sni.bokaticowork.features.subscription.subscription.model.EntitlementDefinition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EntitlementDefinitionRepository extends JpaRepository<EntitlementDefinition, Long>, JpaSpecificationExecutor<EntitlementDefinition> {

    boolean existsByCodeIgnoreCase(String code);

    Optional<EntitlementDefinition> findByCodeIgnoreCase(String code);

    @Query(value = """
            SELECT ed.*
            FROM entitlement_definition ed
            LEFT JOIN resource_type rt ON rt.id = ed.resource_type_id
            LEFT JOIN resource_group rg ON rg.id = ed.resource_group_id
            WHERE ed.deleted = false
              AND (:code IS NULL OR ed.code ILIKE CONCAT('%', CAST(:code AS VARCHAR), '%'))
              AND (:name IS NULL OR ed.name ILIKE CONCAT('%', CAST(:name AS VARCHAR), '%'))
              AND (:entitlementType IS NULL OR ed.entitlement_type = :entitlementType)
              AND (:unit IS NULL OR ed.unit = :unit)
              AND (:consumptionMode IS NULL OR ed.consumption_mode = :consumptionMode)
              AND (:resetPolicy IS NULL OR ed.reset_policy = :resetPolicy)
              AND (:resourceTypeCode IS NULL OR COALESCE(rt.code, '') ILIKE CONCAT('%', CAST(:resourceTypeCode AS VARCHAR), '%'))
              AND (:resourceGroupCode IS NULL OR COALESCE(rg.code, '') ILIKE CONCAT('%', CAST(:resourceGroupCode AS VARCHAR), '%'))
              AND (:stackable IS NULL OR ed.stackable = :stackable)
              AND (:transferable IS NULL OR ed.transferable = :transferable)
              AND (:active IS NULL OR ed.active = :active)
              AND (
                    :query IS NULL
                    OR normalize_text(ed.code) % normalize_text(:query)
                    OR normalize_text(ed.name) % normalize_text(:query)
                    OR normalize_text(COALESCE(ed.description, '')) % normalize_text(:query)
                    OR normalize_text(COALESCE(rt.code, '')) % normalize_text(:query)
                    OR normalize_text(COALESCE(rg.code, '')) % normalize_text(:query)
                    OR normalize_text(ed.code) LIKE CONCAT('%', normalize_text(:query), '%')
                    OR normalize_text(ed.name) LIKE CONCAT('%', normalize_text(:query), '%')
              )
            ORDER BY
              CASE WHEN :query IS NULL THEN 0 ELSE GREATEST(
                    similarity(normalize_text(ed.code), normalize_text(:query)),
                    similarity(normalize_text(ed.name), normalize_text(:query)),
                    similarity(normalize_text(COALESCE(ed.description, '')), normalize_text(:query)),
                    similarity(normalize_text(COALESCE(rt.code, '')), normalize_text(:query)),
                    similarity(normalize_text(COALESCE(rg.code, '')), normalize_text(:query))
              ) END DESC,
              ed.created_at DESC
            """,
            countQuery = """
            SELECT COUNT(*)
            FROM entitlement_definition ed
            LEFT JOIN resource_type rt ON rt.id = ed.resource_type_id
            LEFT JOIN resource_group rg ON rg.id = ed.resource_group_id
            WHERE ed.deleted = false
              AND (:code IS NULL OR ed.code ILIKE CONCAT('%', CAST(:code AS VARCHAR), '%'))
              AND (:name IS NULL OR ed.name ILIKE CONCAT('%', CAST(:name AS VARCHAR), '%'))
              AND (:entitlementType IS NULL OR ed.entitlement_type = :entitlementType)
              AND (:unit IS NULL OR ed.unit = :unit)
              AND (:consumptionMode IS NULL OR ed.consumption_mode = :consumptionMode)
              AND (:resetPolicy IS NULL OR ed.reset_policy = :resetPolicy)
              AND (:resourceTypeCode IS NULL OR COALESCE(rt.code, '') ILIKE CONCAT('%', CAST(:resourceTypeCode AS VARCHAR), '%'))
              AND (:resourceGroupCode IS NULL OR COALESCE(rg.code, '') ILIKE CONCAT('%', CAST(:resourceGroupCode AS VARCHAR), '%'))
              AND (:stackable IS NULL OR ed.stackable = :stackable)
              AND (:transferable IS NULL OR ed.transferable = :transferable)
              AND (:active IS NULL OR ed.active = :active)
              AND (
                    :query IS NULL
                    OR normalize_text(ed.code) % normalize_text(:query)
                    OR normalize_text(ed.name) % normalize_text(:query)
                    OR normalize_text(COALESCE(ed.description, '')) % normalize_text(:query)
                    OR normalize_text(COALESCE(rt.code, '')) % normalize_text(:query)
                    OR normalize_text(COALESCE(rg.code, '')) % normalize_text(:query)
                    OR normalize_text(ed.code) LIKE CONCAT('%', normalize_text(:query), '%')
                    OR normalize_text(ed.name) LIKE CONCAT('%', normalize_text(:query), '%')
              )
            """,
            nativeQuery = true)
    Page<EntitlementDefinition> nativeSearch(@Param("query") String query,
                                             @Param("code") String code,
                                             @Param("name") String name,
                                             @Param("entitlementType") String entitlementType,
                                             @Param("unit") String unit,
                                             @Param("consumptionMode") String consumptionMode,
                                             @Param("resetPolicy") String resetPolicy,
                                             @Param("resourceTypeCode") String resourceTypeCode,
                                             @Param("resourceGroupCode") String resourceGroupCode,
                                             @Param("stackable") Boolean stackable,
                                             @Param("transferable") Boolean transferable,
                                             @Param("active") Boolean active,
                                             Pageable pageable);

    @Query(nativeQuery = true, value = """
            SELECT ed.*
            FROM entitlement_definition ed
            LEFT JOIN resource_type rt ON rt.id = ed.resource_type_id
            LEFT JOIN resource_group rg ON rg.id = ed.resource_group_id
            WHERE ed.deleted = false
              AND (
                    normalize_text(ed.code) % normalize_text(:query)
                    OR normalize_text(ed.name) % normalize_text(:query)
                    OR normalize_text(COALESCE(ed.description, '')) % normalize_text(:query)
                    OR normalize_text(COALESCE(rt.code, '')) % normalize_text(:query)
                    OR normalize_text(COALESCE(rg.code, '')) % normalize_text(:query)
                    OR normalize_text(ed.code) LIKE CONCAT('%', normalize_text(:query), '%')
                    OR normalize_text(ed.name) LIKE CONCAT('%', normalize_text(:query), '%')
              )
            ORDER BY GREATEST(
                    similarity(normalize_text(ed.code), normalize_text(:query)),
                    similarity(normalize_text(ed.name), normalize_text(:query)),
                    similarity(normalize_text(COALESCE(ed.description, '')), normalize_text(:query)),
                    similarity(normalize_text(COALESCE(rt.code, '')), normalize_text(:query)),
                    similarity(normalize_text(COALESCE(rg.code, '')), normalize_text(:query))
              ) DESC,
              ed.created_at DESC
            LIMIT 20
            """)
    java.util.List<EntitlementDefinition> basicSearch(@Param("query") String query);
}
