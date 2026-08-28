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

    /**
     * Garde d'idempotence de {@code PaymentAllocationService.allocateOne} : une transaction n'est
     * imputee qu'une fois a une facture donnee. Indispensable pour que le rattrapage
     * PAYMENT_ALLOCATION_RETRY puisse rejouer une imputation partiellement reussie sans
     * doubler les lignes deja creees, et adosse a l'index unique ux_payment_allocation_txn_document.
     */
    boolean existsByPaymentTransaction_IdAndBillingDocumentNumber(Long paymentTransactionId, String billingDocumentNumber);

    @Query("SELECT pa FROM PaymentAllocation pa JOIN FETCH pa.paymentTransaction WHERE pa.billingDocumentNumber = :documentNumber ORDER BY pa.allocatedAt ASC")
    List<PaymentAllocation> findAllByBillingDocumentNumberFetchTransaction(@Param("documentNumber") String documentNumber);
}
