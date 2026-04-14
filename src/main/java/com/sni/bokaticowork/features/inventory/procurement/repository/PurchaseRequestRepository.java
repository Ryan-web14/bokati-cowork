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

    @Query("""
            SELECT request FROM PurchaseRequest request
            LEFT JOIN request.location location
            WHERE (:status IS NULL OR request.status = :status)
              AND (:locationCode IS NULL OR location.locationCode = :locationCode)
              AND (:query IS NULL OR UPPER(request.requestCode) LIKE CONCAT('%', :query, '%')
                   OR UPPER(request.requestedBy) LIKE CONCAT('%', :query, '%'))
            """)
    Page<PurchaseRequest> search(@Param("status") PurchaseRequestStatus status,
                                 @Param("locationCode") String locationCode,
                                 @Param("query") String query,
                                 Pageable pageable);
}
