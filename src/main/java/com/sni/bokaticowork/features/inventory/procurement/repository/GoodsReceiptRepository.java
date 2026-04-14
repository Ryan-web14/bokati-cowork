package com.sni.bokaticowork.features.inventory.procurement.repository;

import com.sni.bokaticowork.features.inventory.procurement.enums.GoodsReceiptStatus;
import com.sni.bokaticowork.features.inventory.procurement.model.GoodsReceipt;
import com.sni.bokaticowork.features.inventory.procurement.model.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GoodsReceiptRepository extends JpaRepository<GoodsReceipt, Long> {
    boolean existsByReceiptCode(String receiptCode);

    Page<GoodsReceipt> findAllByStatus(GoodsReceiptStatus status, Pageable pageable);

    java.util.List<GoodsReceipt> findAllByPurchaseOrder_Supplier(Supplier supplier);

    @Query("""
            SELECT receipt FROM GoodsReceipt receipt
            LEFT JOIN receipt.purchaseOrder po
            JOIN receipt.location location
            WHERE (:status IS NULL OR receipt.status = :status)
              AND (:locationCode IS NULL OR location.locationCode = :locationCode)
              AND (:orderCode IS NULL OR po.orderCode = :orderCode)
              AND (:query IS NULL OR UPPER(receipt.receiptCode) LIKE CONCAT('%', :query, '%')
                   OR UPPER(po.orderCode) LIKE CONCAT('%', :query, '%')
                   OR UPPER(receipt.receivedBy) LIKE CONCAT('%', :query, '%'))
            """)
    Page<GoodsReceipt> search(@Param("status") GoodsReceiptStatus status,
                              @Param("locationCode") String locationCode,
                              @Param("orderCode") String orderCode,
                              @Param("query") String query,
                              Pageable pageable);
}
