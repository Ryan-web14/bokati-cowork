package com.sni.bokaticowork.features.inventory.procurement.repository;

import com.sni.bokaticowork.features.inventory.procurement.enums.PurchaseRequestStatus;
import com.sni.bokaticowork.features.inventory.procurement.model.PurchaseRequest;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PurchaseRequestRepository extends JpaRepository<PurchaseRequest, Long> {
    boolean existsByRequestCode(String requestCode);

    Optional<PurchaseRequest> findByRequestCode(String requestCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT request FROM PurchaseRequest request WHERE request.requestCode = :requestCode")
    Optional<PurchaseRequest> findByRequestCodeForUpdate(@Param("requestCode") String requestCode);

    Page<PurchaseRequest> findAllByStatus(PurchaseRequestStatus status, Pageable pageable);

    @Query(value = """
            SELECT request.*
            FROM purchase_request request
            LEFT JOIN inventory_location location ON location.id = request.location_id
            WHERE (CAST(:status AS varchar) IS NULL OR request.status = :status)
              AND (CAST(:locationCode AS varchar) IS NULL OR location.location_code = :locationCode)
              AND (CAST(:query AS varchar) IS NULL OR UPPER(request.request_code) LIKE CONCAT('%', :query, '%')
                   OR UPPER(request.requested_by) LIKE CONCAT('%', :query, '%'))
            ORDER BY request.created_at DESC
            """,
            countQuery = """
            SELECT COUNT(*)
            FROM purchase_request request
            LEFT JOIN inventory_location location ON location.id = request.location_id
            WHERE (CAST(:status AS varchar) IS NULL OR request.status = :status)
              AND (CAST(:locationCode AS varchar) IS NULL OR location.location_code = :locationCode)
              AND (CAST(:query AS varchar) IS NULL OR UPPER(request.request_code) LIKE CONCAT('%', :query, '%')
                   OR UPPER(request.requested_by) LIKE CONCAT('%', :query, '%'))
            """,
            nativeQuery = true)
    Page<PurchaseRequest> search(@Param("status") String status,
                                 @Param("locationCode") String locationCode,
                                 @Param("query") String query,
                                 Pageable pageable);
}
