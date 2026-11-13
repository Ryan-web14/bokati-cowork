package com.sni.bokaticowork.features.billing.repository;

import com.sni.bokaticowork.features.billing.model.ServiceCatalogItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ServiceCatalogItemRepository extends JpaRepository<ServiceCatalogItem, Long> {

    Optional<ServiceCatalogItem> findByItemCode(String itemCode);

    boolean existsByItemCode(String itemCode);

    @Query(nativeQuery = true,
            value = """
                    SELECT *
                    FROM service_catalog_item
                    WHERE (:active IS NULL OR active = CAST(:active AS BOOLEAN))
                      AND (:category IS NULL OR UPPER(TRIM(category)) = UPPER(TRIM(CAST(:category AS VARCHAR))))
                      AND (:searchText IS NULL OR (
                          name ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                          OR COALESCE(description, '') ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                          OR item_code ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                      ))
                    ORDER BY display_order ASC, name ASC
                    """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM service_catalog_item
                    WHERE (:active IS NULL OR active = CAST(:active AS BOOLEAN))
                      AND (:category IS NULL OR UPPER(TRIM(category)) = UPPER(TRIM(CAST(:category AS VARCHAR))))
                      AND (:searchText IS NULL OR (
                          name ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                          OR COALESCE(description, '') ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                          OR item_code ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                      ))
                    """)
    Page<ServiceCatalogItem> search(@Param("active") Boolean active,
                                    @Param("category") String category,
                                    @Param("searchText") String searchText,
                                    Pageable pageable);

    // ------------------------------------------------------------ lot C0 : listes de reference

    @Query(nativeQuery = true, value = """
            SELECT COUNT(*) FROM service_catalog_item WHERE lower(TRIM(category)) = lower(:code)
            """)
    long countUsingCategory(@Param("code") String code);

    @Query(nativeQuery = true, value = """
            SELECT COUNT(*) FROM service_catalog_item
            WHERE lower(TRIM(subcategory)) = lower(:code)
              AND (lower(TRIM(COALESCE(category, ''))) = lower(:parent) OR (category IS NULL OR TRIM(category) = '') AND lower(:parent) = 'autres')
            """)
    long countUsingSubcategory(@Param("parent") String parent, @Param("code") String code);

    @Query(nativeQuery = true, value = "SELECT COUNT(*) FROM service_catalog_item WHERE lower(TRIM(unit)) = lower(:code)")
    long countUsingUnit(@Param("code") String code);

    @Query(nativeQuery = true, value = "SELECT COUNT(*) FROM service_catalog_item WHERE lower(TRIM(tax_rule_code)) = lower(:code)")
    long countUsingTaxRule(@Param("code") String code);

    @org.springframework.data.jpa.repository.Modifying
    @Query(nativeQuery = true, value = """
            UPDATE service_catalog_item SET category = :target, updated_at = now() WHERE lower(TRIM(category)) = lower(:source)
            """)
    int replaceCategory(@Param("source") String source, @Param("target") String target);

    @org.springframework.data.jpa.repository.Modifying
    @Query(nativeQuery = true, value = """
            UPDATE service_catalog_item SET subcategory = :target, updated_at = now()
            WHERE lower(TRIM(subcategory)) = lower(:source)
              AND (lower(TRIM(COALESCE(category, ''))) = lower(:parent) OR (category IS NULL OR TRIM(category) = '') AND lower(:parent) = 'autres')
            """)
    int replaceSubcategory(@Param("parent") String parent, @Param("source") String source, @Param("target") String target);

    @org.springframework.data.jpa.repository.Modifying
    @Query(nativeQuery = true, value = """
            UPDATE service_catalog_item SET unit = :target, updated_at = now() WHERE lower(TRIM(unit)) = lower(:source)
            """)
    int replaceUnit(@Param("source") String source, @Param("target") String target);

    /** Recherche par famille et sous-famille (codes), pour la liste du catalogue. */
    @Query(nativeQuery = true,
            value = """
                    SELECT *
                    FROM service_catalog_item
                    WHERE (:active IS NULL OR active = CAST(:active AS BOOLEAN))
                      AND (:category IS NULL OR UPPER(TRIM(category)) = UPPER(TRIM(CAST(:category AS VARCHAR))))
                      AND (:subcategory IS NULL OR UPPER(TRIM(subcategory)) = UPPER(TRIM(CAST(:subcategory AS VARCHAR))))
                      AND (:searchText IS NULL OR (
                          name ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                          OR COALESCE(description, '') ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                          OR item_code ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                      ))
                    ORDER BY display_order ASC, name ASC
                    """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM service_catalog_item
                    WHERE (:active IS NULL OR active = CAST(:active AS BOOLEAN))
                      AND (:category IS NULL OR UPPER(TRIM(category)) = UPPER(TRIM(CAST(:category AS VARCHAR))))
                      AND (:subcategory IS NULL OR UPPER(TRIM(subcategory)) = UPPER(TRIM(CAST(:subcategory AS VARCHAR))))
                      AND (:searchText IS NULL OR (
                          name ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                          OR COALESCE(description, '') ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                          OR item_code ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                      ))
                    """)
    Page<ServiceCatalogItem> searchBySubcategory(@Param("active") Boolean active,
                                                 @Param("category") String category,
                                                 @Param("subcategory") String subcategory,
                                                 @Param("searchText") String searchText,
                                                 Pageable pageable);

    /** Nombre d'articles par famille (code en minuscules), en une requete. */
    @Query(nativeQuery = true, value = """
            SELECT lower(TRIM(category)) AS code, COUNT(*) AS total FROM service_catalog_item
            WHERE category IS NOT NULL AND TRIM(category) <> '' GROUP BY lower(TRIM(category))
            """)
    java.util.List<Object[]> countGroupedByCategory();

    /** Nombre d'articles par couple famille et sous-famille ; famille vide rendue « autres ». */
    @Query(nativeQuery = true, value = """
            SELECT COALESCE(NULLIF(lower(TRIM(category)), ''), 'autres') AS parent, lower(TRIM(subcategory)) AS code, COUNT(*) AS total
            FROM service_catalog_item
            WHERE subcategory IS NOT NULL AND TRIM(subcategory) <> ''
            GROUP BY COALESCE(NULLIF(lower(TRIM(category)), ''), 'autres'), lower(TRIM(subcategory))
            """)
    java.util.List<Object[]> countGroupedBySubcategory();

    @Query(nativeQuery = true, value = """
            SELECT lower(TRIM(unit)) AS code, COUNT(*) AS total FROM service_catalog_item
            WHERE unit IS NOT NULL AND TRIM(unit) <> '' GROUP BY lower(TRIM(unit))
            """)
    java.util.List<Object[]> countGroupedByUnit();
}
