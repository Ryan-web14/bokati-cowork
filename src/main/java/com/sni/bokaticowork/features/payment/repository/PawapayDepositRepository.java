package com.sni.bokaticowork.features.payment.repository;

import com.sni.bokaticowork.features.payment.model.PawapayDeposit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PawapayDepositRepository extends JpaRepository<PawapayDeposit, Long>,
        org.springframework.data.jpa.repository.JpaSpecificationExecutor<PawapayDeposit> {

    /** Nombre et montant des depots par statut, sur une periode · la synthese de l'outil admin. */
    @org.springframework.data.jpa.repository.Query("""
            select d.status, d.currency, count(d), coalesce(sum(d.amount), 0)
            from PawapayDeposit d
            where d.createdAt >= :from and d.createdAt < :to
            group by d.status, d.currency
            """)
    List<Object[]> summarize(@org.springframework.data.repository.query.Param("from") java.time.Instant from,
                             @org.springframework.data.repository.query.Param("to") java.time.Instant to);

    List<PawapayDeposit> findByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
            java.time.Instant from, java.time.Instant to, org.springframework.data.domain.Pageable pageable);

    Optional<PawapayDeposit> findByDepositId(String depositId);

    Optional<PawapayDeposit> findByTransactionNumber(String transactionNumber);

    Optional<PawapayDeposit> findByClientReferenceId(String clientReferenceId);

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
