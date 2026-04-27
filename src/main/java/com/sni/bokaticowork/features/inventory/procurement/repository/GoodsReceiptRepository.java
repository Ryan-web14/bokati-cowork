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

    @Query(value = """
            SELECT receipt.*
            FROM goods_receipt receipt
            LEFT JOIN inventory_purchase_order po ON po.id = receipt.purchase_order_id
            JOIN inventory_location location ON location.id = receipt.location_id
            WHERE (CAST(:status AS varchar) IS NULL OR receipt.status = :status)
              AND (CAST(:locationCode AS varchar) IS NULL OR location.location_code = :locationCode)
              AND (CAST(:orderCode AS varchar) IS NULL OR po.order_code = :orderCode)
              AND (CAST(:query AS varchar) IS NULL OR UPPER(receipt.receipt_code) LIKE CONCAT('%', :query, '%')
                   OR UPPER(po.order_code) LIKE CONCAT('%', :query, '%')
                   OR UPPER(receipt.received_by) LIKE CONCAT('%', :query, '%'))
            ORDER BY receipt.received_at DESC
            """,
            countQuery = """
            SELECT COUNT(*)
            FROM goods_receipt receipt
            LEFT JOIN inventory_purchase_order po ON po.id = receipt.purchase_order_id
            JOIN inventory_location location ON location.id = receipt.location_id
            WHERE (CAST(:status AS varchar) IS NULL OR receipt.status = :status)
              AND (CAST(:locationCode AS varchar) IS NULL OR location.location_code = :locationCode)
              AND (CAST(:orderCode AS varchar) IS NULL OR po.order_code = :orderCode)
              AND (CAST(:query AS varchar) IS NULL OR UPPER(receipt.receipt_code) LIKE CONCAT('%', :query, '%')
                   OR UPPER(po.order_code) LIKE CONCAT('%', :query, '%')
                   OR UPPER(receipt.received_by) LIKE CONCAT('%', :query, '%'))
            """,
            nativeQuery = true)
    Page<GoodsReceipt> search(@Param("status") String status,
                              @Param("locationCode") String locationCode,
                              @Param("orderCode") String orderCode,
                              @Param("query") String query,
                              Pageable pageable);
}
