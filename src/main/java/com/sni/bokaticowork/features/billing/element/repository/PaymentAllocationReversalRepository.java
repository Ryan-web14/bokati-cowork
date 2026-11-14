package com.sni.bokaticowork.features.billing.element.repository;

import com.sni.bokaticowork.features.billing.element.model.PaymentAllocationReversal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentAllocationReversalRepository extends JpaRepository<PaymentAllocationReversal, Long> {

    List<PaymentAllocationReversal> findByTransactionNumberOrderByReversedAtDesc(String transactionNumber);

    List<PaymentAllocationReversal> findByDocumentNumberOrderByReversedAtDesc(String documentNumber);
}
