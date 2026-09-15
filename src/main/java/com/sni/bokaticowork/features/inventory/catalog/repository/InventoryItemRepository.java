package com.sni.bokaticowork.features.inventory.catalog.repository;

import com.sni.bokaticowork.features.inventory.catalog.enums.InventoryItemType;
import com.sni.bokaticowork.features.inventory.catalog.model.InventoryItem;
import com.sni.bokaticowork.features.inventory.catalog.model.InventoryItemTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long>, JpaSpecificationExecutor<InventoryItem> {

    boolean existsByItemCode(String itemCode);

    boolean existsByPsku(String psku);

    boolean existsByShortCode(String shortCode);

    boolean existsByDisplayCode(String displayCode);

    boolean existsByIdentificationCode(String identificationCode);

    Optional<InventoryItem> findByItemCode(String itemCode);

    Optional<InventoryItem> findByTemplateAndVariantSignature(InventoryItemTemplate template, String variantSignature);

    long countByTemplate(InventoryItemTemplate template);

    /**
     * Retrouve un article par n importe lequel de ses codes metier. Utilise par la resolution de scan.
     */
    @Query("""
            SELECT item FROM InventoryItem item
            WHERE item.itemCode = :code
               OR item.psku = :code
               OR item.shortCode = :code
               OR item.displayCode = :code
               OR item.identificationCode = :code
            """)
    Optional<InventoryItem> findByAnyCode(@Param("code") String code);

    @Query("""
            SELECT COUNT(item) > 0 FROM InventoryItem item
            WHERE (:excludedId IS NULL OR item.id <> :excludedId)
              AND (
                    (:psku IS NOT NULL AND item.psku = :psku)
                    OR (:shortCode IS NOT NULL AND item.shortCode = :shortCode)
                    OR (:displayCode IS NOT NULL AND item.displayCode = :displayCode)
                    OR (:identificationCode IS NOT NULL AND item.identificationCode = :identificationCode)
              )
            """)
    boolean existsConflictingIdentification(@Param("excludedId") Long excludedId,
                                            @Param("psku") String psku,
                                            @Param("shortCode") String shortCode,
                                            @Param("displayCode") String displayCode,
                                            @Param("identificationCode") String identificationCode);

    @Query(value = """
            SELECT item.*
            FROM inventory_item item
            LEFT JOIN inventory_category category ON category.id = item.category_id
            WHERE (:active IS NULL OR item.active = :active)
              AND (:itemType IS NULL OR item.item_type = :itemType)
              AND (:categoryCode IS NULL OR category.code = :categoryCode)
              AND (
                    :query IS NULL
                    OR inventory_search_match(item.search_text, :query)
                    OR inventory_search_match(item.name, :query)
                    OR inventory_search_match(item.item_code, :query)
                    OR inventory_search_match(item.psku, :query)
                    OR inventory_search_match(item.short_code, :query)
                    OR inventory_search_match(item.display_code, :query)
                    OR inventory_search_match(item.identification_code, :query)
                    OR inventory_search_match(item.specification, :query)
              )
            ORDER BY
              CASE WHEN :query IS NULL THEN 0 ELSE inventory_search_score(item.search_text, :query) END DESC,
              item.name ASC
            """,
            countQuery = """
            SELECT COUNT(*)
            FROM inventory_item item
            LEFT JOIN inventory_category category ON category.id = item.category_id
            WHERE (:active IS NULL OR item.active = :active)
              AND (:itemType IS NULL OR item.item_type = :itemType)
              AND (:categoryCode IS NULL OR category.code = :categoryCode)
              AND (
                    :query IS NULL
                    OR inventory_search_match(item.search_text, :query)
                    OR inventory_search_match(item.name, :query)
                    OR inventory_search_match(item.item_code, :query)
                    OR inventory_search_match(item.psku, :query)
                    OR inventory_search_match(item.short_code, :query)
                    OR inventory_search_match(item.display_code, :query)
                    OR inventory_search_match(item.identification_code, :query)
                    OR inventory_search_match(item.specification, :query)
              )
            """,
            nativeQuery = true)
    Page<InventoryItem> nativeSearch(@Param("query") String query,
                                     @Param("categoryCode") String categoryCode,
                                     @Param("itemType") String itemType,
                                     @Param("active") Boolean active,
                                     Pageable pageable);
}
