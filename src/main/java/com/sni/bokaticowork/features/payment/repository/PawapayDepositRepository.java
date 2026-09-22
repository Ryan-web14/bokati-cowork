package com.sni.bokaticowork.features.payment.repository;

import com.sni.bokaticowork.features.payment.model.PawapayDeposit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PawapayDepositRepository extends JpaRepository<PawapayDeposit, Long> {

    Optional<PawapayDeposit> findByDepositId(String depositId);

    Optional<PawapayDeposit> findByTransactionNumber(String transactionNumber);

    Optional<PawapayDeposit> findByClientReferenceId(String clientReferenceId);

    @Query(nativeQuery = true, value = """
            SELECT *
            FROM pawapay_deposit
            WHERE (:phoneNumber IS NULL OR phone_number = :phoneNumber)
              AND (:provider IS NULL OR provider = :provider)
              AND (:currency IS NULL OR currency = :currency)
              AND (:amount IS NULL OR amount = CAST(:amount AS NUMERIC))
            ORDER BY created_at DESC
            LIMIT 1
            """)
    Optional<PawapayDeposit> findLatestMatchingCallback(
            @Param("phoneNumber") String phoneNumber,
            @Param("provider") String provider,
            @Param("currency") String currency,
            @Param("amount") String amount
    );

    List<PawapayDeposit> findAllByIntentNumberOrderByCreatedAtDesc(String intentNumber);

    /** Les depots dont la verification est due · le worker les prend dans l'ordre. */
    @Query(nativeQuery = true, value = """
            SELECT * FROM pawapay_deposit
            WHERE next_status_check_at IS NOT NULL AND next_status_check_at <= :now
              AND status IN ('PROCESSING', 'SUBMITTED_UNCONFIRMED', 'CREATED', 'ACCEPTED')
            ORDER BY next_status_check_at ASC
            LIMIT :limit
            """)
    List<PawapayDeposit> findDueForStatusCheck(@Param("now") java.time.Instant now, @Param("limit") int limit);

    /** Un depot encore en cours sur cette intention pour ce numero · celui qu'on rend au lieu d'en creer un second. */
    @Query(nativeQuery = true, value = """
            SELECT * FROM pawapay_deposit
            WHERE payment_intent_id = :intentId
              AND phone_number = :phoneNumber
              AND status IN ('PROCESSING', 'SUBMITTED_UNCONFIRMED', 'CREATED', 'ACCEPTED')
              AND created_at >= :since
            ORDER BY created_at DESC
            LIMIT 1
            """)
    Optional<PawapayDeposit> findInFlight(@Param("intentId") Long intentId, @Param("phoneNumber") String phoneNumber,
                                          @Param("since") java.time.Instant since);

    org.springframework.data.domain.Page<PawapayDeposit> findByStatusOrderByCreatedAtDesc(String status, org.springframework.data.domain.Pageable pageable);

    org.springframework.data.domain.Page<PawapayDeposit> findAllByOrderByCreatedAtDesc(org.springframework.data.domain.Pageable pageable);

    List<PawapayDeposit> findByCustomerTypeAndCustomerCodeOrderByCreatedAtDesc(String customerType, String customerCode);

    @Query("SELECT d FROM PawapayDeposit d WHERE d.paymentIntent.id = :intentId ORDER BY d.createdAt DESC LIMIT 1")
    Optional<PawapayDeposit> findLatestByPaymentIntentId(@Param("intentId") Long intentId);
}
