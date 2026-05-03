package com.sni.bokaticowork.features.payment.repository;

import com.sni.bokaticowork.features.payment.model.PaymentAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentAllocationRepository extends JpaRepository<PaymentAllocation, Long> {

    @Query(nativeQuery = true, value = """
            SELECT *
            FROM payment_allocation
            WHERE payment_transaction_id = :paymentTransactionId
            ORDER BY allocated_at ASC
            """)
    List<PaymentAllocation> findAllByPaymentTransactionId(@Param("paymentTransactionId") Long paymentTransactionId);

    List<PaymentAllocation> findAllByBillingDocumentNumber(String billingDocumentNumber);
}
