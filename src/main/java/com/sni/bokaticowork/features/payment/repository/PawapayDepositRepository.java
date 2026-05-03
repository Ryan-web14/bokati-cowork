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
}
