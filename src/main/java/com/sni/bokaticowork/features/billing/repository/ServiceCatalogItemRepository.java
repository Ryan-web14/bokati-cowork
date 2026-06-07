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
}
