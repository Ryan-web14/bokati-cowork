package com.sni.bokaticowork.features.payment.repository;

import com.sni.bokaticowork.features.payment.model.WalletHold;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface WalletHoldRepository extends JpaRepository<WalletHold, Long> {

    @Query(nativeQuery = true, value = "SELECT * FROM wallet_hold WHERE hold_number = :holdNumber")
    Optional<WalletHold> findByHoldNumber(@Param("holdNumber") String holdNumber);

    @Query(nativeQuery = true, value = """
            SELECT *
            FROM wallet_hold
            WHERE status = CAST(:status AS VARCHAR)
              AND expires_at IS NOT NULL
              AND expires_at <= :expiresAt
            """)
    List<WalletHold> findAllByStatusAndExpiresAtLessThanEqual(@Param("status") String status,
                                                              @Param("expiresAt") Instant expiresAt);

    @Modifying
    @Query(nativeQuery = true, value = """
            UPDATE wallet_hold
            SET status = 'EXPIRED', updated_at = NOW()
            WHERE status = 'ACTIVE'
              AND expires_at IS NOT NULL
              AND expires_at <= :now
            """)
    int expireDueHolds(@Param("now") Instant now);
}
