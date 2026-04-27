package com.sni.bokaticowork.features.inventory.procurement.repository;

import com.sni.bokaticowork.features.inventory.procurement.enums.PurchaseOrderStatus;
import com.sni.bokaticowork.features.inventory.procurement.model.PurchaseOrder;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;
import com.sni.bokaticowork.features.inventory.procurement.model.Supplier;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
    boolean existsByOrderCode(String orderCode);

    Optional<PurchaseOrder> findByOrderCode(String orderCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT po FROM PurchaseOrder po WHERE po.orderCode = :orderCode")
    Optional<PurchaseOrder> findByOrderCodeForUpdate(@Param("orderCode") String orderCode);

    Page<PurchaseOrder> findAllByStatus(PurchaseOrderStatus status, Pageable pageable);

    @Query(value = """
            SELECT po.*
            FROM inventory_purchase_order po
            JOIN inventory_supplier supplier ON supplier.id = po.supplier_id
            LEFT JOIN inventory_location location ON location.id = po.location_id
            WHERE (CAST(:status AS varchar) IS NULL OR po.status = :status)
              AND (CAST(:supplierCode AS varchar) IS NULL OR supplier.supplier_code = :supplierCode)
              AND (CAST(:locationCode AS varchar) IS NULL OR location.location_code = :locationCode)
              AND (CAST(:query AS varchar) IS NULL OR UPPER(po.order_code) LIKE CONCAT('%', :query, '%')
                   OR UPPER(po.source_request_code) LIKE CONCAT('%', :query, '%')
                   OR UPPER(supplier.name) LIKE CONCAT('%', :query, '%'))
            ORDER BY po.created_at DESC
            """,
            countQuery = """
            SELECT COUNT(*)
            FROM inventory_purchase_order po
            JOIN inventory_supplier supplier ON supplier.id = po.supplier_id
            LEFT JOIN inventory_location location ON location.id = po.location_id
            WHERE (CAST(:status AS varchar) IS NULL OR po.status = :status)
              AND (CAST(:supplierCode AS varchar) IS NULL OR supplier.supplier_code = :supplierCode)
              AND (CAST(:locationCode AS varchar) IS NULL OR location.location_code = :locationCode)
              AND (CAST(:query AS varchar) IS NULL OR UPPER(po.order_code) LIKE CONCAT('%', :query, '%')
                   OR UPPER(po.source_request_code) LIKE CONCAT('%', :query, '%')
                   OR UPPER(supplier.name) LIKE CONCAT('%', :query, '%'))
            """,
            nativeQuery = true)
    Page<PurchaseOrder> search(@Param("status") String status,
                               @Param("supplierCode") String supplierCode,
                               @Param("locationCode") String locationCode,
                               @Param("query") String query,
                               Pageable pageable);

    long countByStatusIn(java.util.Collection<PurchaseOrderStatus> statuses);

    List<PurchaseOrder> findAllBySupplier(Supplier supplier);
}
