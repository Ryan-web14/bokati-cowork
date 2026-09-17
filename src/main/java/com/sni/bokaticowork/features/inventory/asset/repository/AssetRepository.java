package com.sni.bokaticowork.features.inventory.asset.repository;

import com.sni.bokaticowork.features.inventory.asset.enums.AssetAssigneeType;
import com.sni.bokaticowork.features.inventory.asset.enums.AssetStatus;
import com.sni.bokaticowork.features.inventory.asset.model.Asset;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AssetRepository extends JpaRepository<Asset, Long>, JpaSpecificationExecutor<Asset> {

    boolean existsByAssetCode(String assetCode);

    boolean existsBySerialNumber(String serialNumber);

    boolean existsByAssetTag(String assetTag);

    Optional<Asset> findFirstBySerialNumber(String serialNumber);

    Optional<Asset> findFirstByAssetTag(String assetTag);

    Optional<Asset> findByAssetCode(String assetCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT asset FROM Asset asset WHERE asset.assetCode = :assetCode")
    Optional<Asset> findByAssetCodeForUpdate(@Param("assetCode") String assetCode);

    @Query("""
            SELECT COUNT(asset) > 0 FROM Asset asset
            WHERE (:excludedId IS NULL OR asset.id <> :excludedId)
              AND (
                    (:serialNumber IS NOT NULL AND asset.serialNumber = :serialNumber)
                    OR (:assetTag IS NOT NULL AND asset.assetTag = :assetTag)
              )
            """)
    boolean existsConflictingCodes(@Param("excludedId") Long excludedId,
                                   @Param("serialNumber") String serialNumber,
                                   @Param("assetTag") String assetTag);

    @Query(value = """
            SELECT asset.*
            FROM asset asset
            JOIN inventory_item item ON item.id = asset.item_id
            LEFT JOIN inventory_location location ON location.id = asset.location_id
            WHERE (:query IS NULL
                    OR inventory_search_match(asset.asset_code, :query)
                    OR inventory_search_match(asset.serial_number, :query)
                    OR inventory_search_match(asset.asset_tag, :query)
                    OR inventory_search_match(item.name, :query)
                    OR inventory_search_match(item.item_code, :query))
              AND (:itemCode IS NULL OR item.item_code = :itemCode)
              AND (:status IS NULL OR asset.status = :status)
              AND (:locationCode IS NULL OR location.location_code = :locationCode)
              AND (:assignedToType IS NULL OR asset.assigned_to_type = :assignedToType)
              AND (:assignedToCode IS NULL OR asset.assigned_to_code = :assignedToCode)
            ORDER BY
              CASE WHEN :query IS NULL THEN 0 ELSE GREATEST(
                inventory_search_score(asset.asset_code, :query),
                inventory_search_score(asset.serial_number, :query),
                inventory_search_score(asset.asset_tag, :query),
                inventory_search_score(item.name, :query),
                inventory_search_score(item.item_code, :query)
              ) END DESC,
              asset.asset_code ASC
            """,
            countQuery = """
            SELECT COUNT(*)
            FROM asset asset
            JOIN inventory_item item ON item.id = asset.item_id
            LEFT JOIN inventory_location location ON location.id = asset.location_id
            WHERE (:query IS NULL
                    OR inventory_search_match(asset.asset_code, :query)
                    OR inventory_search_match(asset.serial_number, :query)
                    OR inventory_search_match(asset.asset_tag, :query)
                    OR inventory_search_match(item.name, :query)
                    OR inventory_search_match(item.item_code, :query))
              AND (:itemCode IS NULL OR item.item_code = :itemCode)
              AND (:status IS NULL OR asset.status = :status)
              AND (:locationCode IS NULL OR location.location_code = :locationCode)
              AND (:assignedToType IS NULL OR asset.assigned_to_type = :assignedToType)
              AND (:assignedToCode IS NULL OR asset.assigned_to_code = :assignedToCode)
            """,
            nativeQuery = true)
    Page<Asset> nativeSearch(@Param("query") String query,
                             @Param("itemCode") String itemCode,
                             @Param("status") String status,
                             @Param("locationCode") String locationCode,
                             @Param("assignedToType") String assignedToType,
                             @Param("assignedToCode") String assignedToCode,
                             Pageable pageable);
}
