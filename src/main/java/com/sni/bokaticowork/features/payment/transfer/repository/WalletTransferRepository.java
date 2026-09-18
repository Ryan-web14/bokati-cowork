package com.sni.bokaticowork.features.payment.transfer.repository;

import com.sni.bokaticowork.features.payment.transfer.model.WalletTransfer;
import com.sni.bokaticowork.features.payment.transfer.model.WalletTransferStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface WalletTransferRepository extends JpaRepository<WalletTransfer, Long> {

    Optional<WalletTransfer> findByTransferNumber(String transferNumber);

    /** Les transferts ou ce portefeuille est d'un cote ou de l'autre, les plus recents d'abord. */
    @Query("""
            SELECT t FROM WalletTransfer t
            WHERE t.sourceWallet.id = :walletId OR t.targetWallet.id = :walletId
            ORDER BY t.createdAt DESC
            """)
    Page<WalletTransfer> findInvolving(@Param("walletId") Long walletId, Pageable pageable);

    @Query("""
            SELECT t FROM WalletTransfer t
            WHERE t.status = :status AND t.expiresAt < :now
            """)
    List<WalletTransfer> findExpired(@Param("status") WalletTransferStatus status, @Param("now") Instant now);

    /** Combien de transferts un emetteur a deja faits vers un destinataire · nul ou zero, c'est un inconnu. */
    @Query("""
            SELECT COUNT(t) FROM WalletTransfer t
            WHERE t.sourceWallet.id = :sourceId AND t.targetWallet.id = :targetId
              AND t.status = com.sni.bokaticowork.features.payment.transfer.model.WalletTransferStatus.COMPLETED
            """)
    long countCompletedBetween(@Param("sourceId") Long sourceId, @Param("targetId") Long targetId);
}
