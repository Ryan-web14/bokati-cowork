package com.sni.bokaticowork.features.payment.repository;

import com.sni.bokaticowork.features.payment.model.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    /**
     * Un paiement de cette facture a-t-il ete lance depuis cet instant, sans etre encore tranche ·
     * un mobile money attend la reponse de l'operateur.
     */
    @Query("""
            SELECT COUNT(t) > 0 FROM PaymentTransaction t
            WHERE t.paymentIntent.sourceType = 'BILLING_DOCUMENT'
              AND t.paymentIntent.sourceCode = :documentNumber
              AND t.status IN (com.sni.bokaticowork.features.payment.enums.PaymentTransactionStatus.PENDING,
                               com.sni.bokaticowork.features.payment.enums.PaymentTransactionStatus.PROCESSING)
              AND t.createdAt >= :since
            """)
    boolean existsInFlightForDocument(@Param("documentNumber") String documentNumber, @Param("since") java.time.Instant since);

    @Query(nativeQuery = true, value = "SELECT * FROM payment_transaction WHERE transaction_number = :transactionNumber")
    Optional<PaymentTransaction> findByTransactionNumber(@Param("transactionNumber") String transactionNumber);

    @Query(nativeQuery = true, value = "SELECT * FROM payment_transaction WHERE receipt_number = :receiptNumber")
    Optional<PaymentTransaction> findByReceiptNumber(@Param("receiptNumber") String receiptNumber);

    @Query(nativeQuery = true, value = """
            SELECT *
            FROM payment_transaction
            WHERE provider_reference = :providerReference
            ORDER BY created_at DESC
            LIMIT 1
            """)
    Optional<PaymentTransaction> findByProviderReference(@Param("providerReference") String providerReference);

    @Query(nativeQuery = true, value = """
            SELECT *
            FROM payment_transaction
            WHERE status = 'PROCESSING'
              AND payment_method = 'MOBILE_MONEY'
              AND provider = 'PAWAYPAY'
              AND provider_reference IS NOT NULL
              AND created_at <= :cutoff
            ORDER BY created_at ASC
            """)
    List<PaymentTransaction> findProcessingMobileMoneyTransactions(@Param("cutoff") java.time.Instant cutoff);

    @Query(nativeQuery = true, value = """
            SELECT *
            FROM payment_transaction
            WHERE payment_intent_id = :paymentIntentId
            ORDER BY created_at DESC
            """)
    List<PaymentTransaction> findAllByPaymentIntentIdOrderByCreatedAtDesc(@Param("paymentIntentId") Long paymentIntentId);

    @Query(nativeQuery = true, value = """
            SELECT COALESCE(SUM(amount), 0)
            FROM payment_transaction
            WHERE payment_intent_id = :paymentIntentId
              AND status = 'SUCCEEDED'
            """)
    BigDecimal sumSucceededAmountByPaymentIntentId(@Param("paymentIntentId") Long paymentIntentId);

    @Query(nativeQuery = true, value = """
            SELECT COALESCE(SUM(amount), 0)
            FROM payment_transaction
            WHERE payment_intent_id = :paymentIntentId
              AND status = 'PROCESSING'
            """)
    BigDecimal sumProcessingAmountByPaymentIntentId(@Param("paymentIntentId") Long paymentIntentId);

    @Query(nativeQuery = true, value = """
            SELECT *
            FROM payment_transaction
            WHERE payment_intent_id = :paymentIntentId
              AND status = 'SUCCEEDED'
            ORDER BY created_at DESC
            LIMIT 1
            """)
    Optional<PaymentTransaction> findSucceededByPaymentIntentId(@Param("paymentIntentId") Long paymentIntentId);

    @Query(nativeQuery = true, value = """
            SELECT *
            FROM payment_transaction
            WHERE metadata_json ->> 'originalTransactionNumber' = :transactionNumber
            ORDER BY created_at ASC
            """)
    List<PaymentTransaction> findAllByOriginalTransactionNumber(@Param("transactionNumber") String transactionNumber);

    /** Les remboursements mobile money encore en cours, les plus anciens d'abord · a relire chez PawaPay. */
    @Query(nativeQuery = true, value = """
            SELECT *
            FROM payment_transaction
            WHERE status = 'PROCESSING'
              AND payment_method = 'MOBILE_MONEY'
              AND provider_reference IS NOT NULL
              AND metadata_json ->> 'originalTransactionNumber' IS NOT NULL
              AND created_at < :before
            ORDER BY created_at ASC
            LIMIT :limit
            """)
    List<PaymentTransaction> findPendingMobileMoneyRefunds(@Param("before") java.time.Instant before, @Param("limit") int limit);
}
