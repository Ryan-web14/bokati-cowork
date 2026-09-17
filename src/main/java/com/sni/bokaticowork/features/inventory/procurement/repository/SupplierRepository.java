package com.sni.bokaticowork.features.inventory.procurement.repository;

import com.sni.bokaticowork.features.inventory.procurement.enums.SupplierStatus;
import com.sni.bokaticowork.features.inventory.procurement.model.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    boolean existsBySupplierCode(String supplierCode);

    Optional<Supplier> findBySupplierCode(String supplierCode);

    List<Supplier> findAllByStatusOrderByNameAsc(SupplierStatus status);

    @Query(value = """
            SELECT supplier.*
            FROM inventory_supplier supplier
            WHERE (CAST(:query AS varchar) IS NULL
                OR UPPER(supplier.name) LIKE CONCAT('%', :query, '%')
                OR UPPER(supplier.supplier_code) LIKE CONCAT('%', :query, '%')
                OR UPPER(supplier.email) LIKE CONCAT('%', :query, '%')
                OR UPPER(supplier.phone) LIKE CONCAT('%', :query, '%')
                OR UPPER(supplier.tax_id) LIKE CONCAT('%', :query, '%'))
            ORDER BY supplier.name ASC
            """,
            countQuery = """
            SELECT COUNT(*)
            FROM inventory_supplier supplier
            WHERE (CAST(:query AS varchar) IS NULL
                OR UPPER(supplier.name) LIKE CONCAT('%', :query, '%')
                OR UPPER(supplier.supplier_code) LIKE CONCAT('%', :query, '%')
                OR UPPER(supplier.email) LIKE CONCAT('%', :query, '%')
                OR UPPER(supplier.phone) LIKE CONCAT('%', :query, '%')
                OR UPPER(supplier.tax_id) LIKE CONCAT('%', :query, '%'))
            """,
            nativeQuery = true)
    Page<Supplier> search(@Param("query") String query, Pageable pageable);
}
