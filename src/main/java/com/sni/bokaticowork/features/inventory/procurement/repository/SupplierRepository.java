package com.sni.bokaticowork.features.inventory.procurement.repository;

import com.sni.bokaticowork.features.inventory.procurement.model.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    boolean existsBySupplierCode(String supplierCode);

    Optional<Supplier> findBySupplierCode(String supplierCode);

    @Query("""
            SELECT supplier FROM Supplier supplier
            WHERE (:query IS NULL
                OR UPPER(supplier.name) LIKE CONCAT('%', :query, '%')
                OR UPPER(supplier.supplierCode) LIKE CONCAT('%', :query, '%')
                OR UPPER(supplier.email) LIKE CONCAT('%', :query, '%')
                OR UPPER(supplier.phone) LIKE CONCAT('%', :query, '%')
                OR UPPER(supplier.taxId) LIKE CONCAT('%', :query, '%'))
            """)
    Page<Supplier> search(@Param("query") String query, Pageable pageable);
}
