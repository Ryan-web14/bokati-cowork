package com.sni.bokaticowork.features.payment.repository;

import com.sni.bokaticowork.features.payment.model.WalletHold;
import com.sni.bokaticowork.features.payment.enums.WalletHoldStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @Query("""
            SELECT h
            FROM WalletHold h
            JOIN h.wallet w
            WHERE (:walletNumber IS NULL OR w.walletNumber = :walletNumber)
              AND (:status IS NULL OR h.status = :status)
              AND (:sourceType IS NULL OR h.sourceType = :sourceType)
              AND (:sourceCode IS NULL OR h.sourceCode = :sourceCode)
            ORDER BY h.createdAt DESC
            """)
    Page<WalletHold> list(@Param("walletNumber") String walletNumber,
                          @Param("status") WalletHoldStatus status,
                          @Param("sourceType") String sourceType,
                          @Param("sourceCode") String sourceCode,
                          Pageable pageable);

    @Query(nativeQuery = true, value = """
            SELECT *
            FROM wallet_hold
            WHERE status = CAST(:status AS VARCHAR)
              AND expires_at IS NOT NULL
              AND expires_at <= :expiresAt
            """)
    List<WalletHold> findAllByStatusAndExpiresAtLessThanEqual(@Param("status") String status,
                                                              @Param("expiresAt") Instant expiresAt);

    @Query(nativeQuery = true, value = """
            SELECT *
            FROM wallet_hold
            WHERE status = CAST(:status AS VARCHAR)
              AND source_type = CAST(:sourceType AS VARCHAR)
              AND source_code = CAST(:sourceCode AS VARCHAR)
            """)
    List<WalletHold> findAllByStatusAndSourceTypeAndSourceCode(@Param("status") String status,
                                                               @Param("sourceType") String sourceType,
                                                               @Param("sourceCode") String sourceCode);

    /**
     * Blocages arrives a echeance de reglement : actifs, poses par une source donnee, et assez
     * anciens pour etre encaisses. Le tri par anciennete garantit qu'un lot partiel traite
     * toujours les plus anciens d'abord.
     */
    @Query(nativeQuery = true, value = """
            SELECT *
            FROM wallet_hold
            WHERE status = 'ACTIVE'
              AND source_type = CAST(:sourceType AS VARCHAR)
              AND created_at <= :createdBefore
            ORDER BY created_at
            LIMIT :limit
            """)
    List<WalletHold> findDueForSettlement(@Param("sourceType") String sourceType,
                                          @Param("createdBefore") Instant createdBefore,
                                          @Param("limit") int limit);

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
