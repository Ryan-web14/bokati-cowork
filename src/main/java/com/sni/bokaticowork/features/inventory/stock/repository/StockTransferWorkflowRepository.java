package com.sni.bokaticowork.features.inventory.stock.repository;

import com.sni.bokaticowork.features.inventory.stock.enums.StockTransferWorkflowStatus;
import com.sni.bokaticowork.features.inventory.stock.model.StockTransferWorkflow;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface StockTransferWorkflowRepository extends JpaRepository<StockTransferWorkflow, Long> {
    boolean existsByTransferCode(String transferCode);

    Optional<StockTransferWorkflow> findByTransferCode(String transferCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT transfer FROM StockTransferWorkflow transfer WHERE transfer.transferCode = :transferCode")
    Optional<StockTransferWorkflow> findByTransferCodeForUpdate(@Param("transferCode") String transferCode);

    Page<StockTransferWorkflow> findAllByStatus(StockTransferWorkflowStatus status, Pageable pageable);
}
