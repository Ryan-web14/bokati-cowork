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

    @Query("""
            SELECT po FROM PurchaseOrder po
            JOIN po.supplier supplier
            LEFT JOIN po.location location
            WHERE (:status IS NULL OR po.status = :status)
              AND (:supplierCode IS NULL OR supplier.supplierCode = :supplierCode)
              AND (:locationCode IS NULL OR location.locationCode = :locationCode)
              AND (:query IS NULL OR UPPER(po.orderCode) LIKE CONCAT('%', :query, '%')
                   OR UPPER(po.sourceRequestCode) LIKE CONCAT('%', :query, '%')
                   OR UPPER(supplier.name) LIKE CONCAT('%', :query, '%'))
            """)
    Page<PurchaseOrder> search(@Param("status") PurchaseOrderStatus status,
                               @Param("supplierCode") String supplierCode,
                               @Param("locationCode") String locationCode,
                               @Param("query") String query,
                               Pageable pageable);

    long countByStatusIn(java.util.Collection<PurchaseOrderStatus> statuses);

    List<PurchaseOrder> findAllBySupplier(Supplier supplier);
}
