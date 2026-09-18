package com.sni.bokaticowork.features.payment.security.repository;

import com.sni.bokaticowork.features.payment.security.model.WalletTransactionConfirmation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface WalletConfirmationRepository extends JpaRepository<WalletTransactionConfirmation, Long> {

    @Query("SELECT c FROM WalletTransactionConfirmation c WHERE c.confirmationCode = :code")
    Optional<WalletTransactionConfirmation> findByCode(@Param("code") String code);

    /** Demandes echues · une confirmation qui traine s'arrache hors de son contexte. */
    @Query("""
            SELECT c FROM WalletTransactionConfirmation c
            WHERE c.status = com.sni.bokaticowork.features.payment.security.enums.WalletConfirmationStatus.PENDING
              AND c.expiresAt <= :now
            ORDER BY c.expiresAt
            """)
    List<WalletTransactionConfirmation> findExpired(@Param("now") Instant now);
}
