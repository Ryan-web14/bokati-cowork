package com.sni.bokaticowork.features.payment.repository;

import com.sni.bokaticowork.features.payment.model.PaymentIntent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface PaymentIntentRepository extends JpaRepository<PaymentIntent, Long> {

    @Query(nativeQuery = true, value = "SELECT * FROM payment_intent WHERE intent_number = :intentNumber")
    Optional<PaymentIntent> findByIntentNumber(@Param("intentNumber") String intentNumber);

    @Query(nativeQuery = true, value = "SELECT * FROM payment_intent WHERE idempotency_key = :idempotencyKey")
    Optional<PaymentIntent> findByIdempotencyKey(@Param("idempotencyKey") String idempotencyKey);

    @Query(nativeQuery = true, value = "SELECT * FROM payment_intent WHERE payment_link_token = :token")
    Optional<PaymentIntent> findByPaymentLinkToken(@Param("token") String token);

    @Query(nativeQuery = true, value = """
            SELECT *
            FROM payment_intent
            WHERE source_type = :sourceType
              AND source_code = :sourceCode
              AND status = 'SUCCEEDED'
            ORDER BY created_at DESC
            LIMIT 1
            """)
    Optional<PaymentIntent> findSucceededBySourceTypeAndSourceCode(@Param("sourceType") String sourceType,
                                                                   @Param("sourceCode") String sourceCode);

    @Query(nativeQuery = true, value = """
            SELECT *
            FROM payment_intent
            WHERE customer_type = :customerType
              AND customer_code = :customerCode
              AND source_type = :sourceType
              AND source_code = :sourceCode
              AND status IN ('PENDING', 'PROCESSING', 'AUTHORIZED')
            ORDER BY created_at DESC
            LIMIT 1
            """)
    Optional<PaymentIntent> findLatestReusable(@Param("customerType") String customerType,
                                               @Param("customerCode") String customerCode,
                                               @Param("sourceType") String sourceType,
                                               @Param("sourceCode") String sourceCode);

    @Query(
            nativeQuery = true,
            value = """
                    SELECT *
                    FROM payment_intent
                    WHERE (:status IS NULL OR status = CAST(:status AS VARCHAR))
                      AND (:customerType IS NULL OR UPPER(TRIM(customer_type)) = UPPER(TRIM(CAST(:customerType AS VARCHAR))))
                      AND (:customerCode IS NULL OR UPPER(TRIM(customer_code)) = UPPER(TRIM(CAST(:customerCode AS VARCHAR))))
                      AND (:sourceType IS NULL OR source_type = CAST(:sourceType AS VARCHAR))
                      AND (:sourceCode IS NULL OR source_code = CAST(:sourceCode AS VARCHAR))
                      AND (:searchText IS NULL OR (
                          intent_number ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                          OR customer_code ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                          OR COALESCE(purpose, '') ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                          OR COALESCE(source_code, '') ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                          OR COALESCE(idempotency_key, '') ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                      ))
                    ORDER BY created_at DESC
                    """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM payment_intent
                    WHERE (:status IS NULL OR status = CAST(:status AS VARCHAR))
                      AND (:customerType IS NULL OR UPPER(TRIM(customer_type)) = UPPER(TRIM(CAST(:customerType AS VARCHAR))))
                      AND (:customerCode IS NULL OR UPPER(TRIM(customer_code)) = UPPER(TRIM(CAST(:customerCode AS VARCHAR))))
                      AND (:sourceType IS NULL OR source_type = CAST(:sourceType AS VARCHAR))
                      AND (:sourceCode IS NULL OR source_code = CAST(:sourceCode AS VARCHAR))
                      AND (:searchText IS NULL OR (
                          intent_number ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                          OR customer_code ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                          OR COALESCE(purpose, '') ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                          OR COALESCE(source_code, '') ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                          OR COALESCE(idempotency_key, '') ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                      ))
                    """
    )
    Page<PaymentIntent> search(@Param("status") String status,
                               @Param("customerType") String customerType,
                               @Param("customerCode") String customerCode,
                               @Param("sourceType") String sourceType,
                               @Param("sourceCode") String sourceCode,
                               @Param("searchText") String searchText,
                               Pageable pageable);

    @Modifying
    @Transactional
    @Query(nativeQuery = true, value = """
            UPDATE payment_intent
            SET status = 'EXPIRED', updated_at = NOW()
            WHERE status IN ('PENDING', 'PROCESSING', 'AUTHORIZED')
              AND expires_at IS NOT NULL
              AND expires_at <= :now
            """)
    int expirePending(@Param("now") Instant now);

    @Query(nativeQuery = true, value = """
            SELECT *
            FROM payment_intent
            WHERE status IN ('PENDING', 'PROCESSING', 'AUTHORIZED')
              AND expires_at IS NOT NULL
              AND expires_at > :now
              AND expires_at <= :alertThreshold
            ORDER BY expires_at ASC
            """)
    java.util.List<PaymentIntent> findExpiringSoon(@Param("now") Instant now,
                                                   @Param("alertThreshold") Instant alertThreshold);
}
