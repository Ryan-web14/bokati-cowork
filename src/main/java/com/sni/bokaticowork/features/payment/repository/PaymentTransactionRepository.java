package com.sni.bokaticowork.features.payment.repository;

import com.sni.bokaticowork.features.payment.model.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    @Query(nativeQuery = true, value = "SELECT * FROM payment_transaction WHERE transaction_number = :transactionNumber")
    Optional<PaymentTransaction> findByTransactionNumber(@Param("transactionNumber") String transactionNumber);

    @Query(nativeQuery = true, value = """
            SELECT *
            FROM payment_transaction
            WHERE payment_intent_id = :paymentIntentId
            ORDER BY created_at DESC
            """)
    List<PaymentTransaction> findAllByPaymentIntentIdOrderByCreatedAtDesc(@Param("paymentIntentId") Long paymentIntentId);
}
