package com.sni.bokaticowork.features.payment.repository;

import com.sni.bokaticowork.features.payment.model.PaymentReconciliationBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PaymentReconciliationBatchRepository extends JpaRepository<PaymentReconciliationBatch, Long> {

    @Query(nativeQuery = true, value = "SELECT * FROM payment_reconciliation_batch WHERE batch_number = :batchNumber")
    Optional<PaymentReconciliationBatch> findByBatchNumber(@Param("batchNumber") String batchNumber);
}
