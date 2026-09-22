package com.sni.bokaticowork.features.payment.transfer.repository;

import com.sni.bokaticowork.features.payment.transfer.model.WalletTransfer;
import com.sni.bokaticowork.features.payment.transfer.model.WalletTransferStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface WalletTransferRepository extends JpaRepository<WalletTransfer, Long> {

    /**
     * Un transfert et les deux portefeuilles qu'il relie.
     *
     * <p>La vue rendue au titulaire nomme le portefeuille d'en face · sans ce chargement, elle le
     * demande a une session deja fermee et le client recoit une erreur interne la ou il attend son
     * recapitulatif.</p>
     */
    @EntityGraph(attributePaths = {"sourceWallet", "targetWallet"})
    Optional<WalletTransfer> findByTransferNumber(String transferNumber);

    /** Les transferts ou ce portefeuille est d'un cote ou de l'autre, les plus recents d'abord. */
    @EntityGraph(attributePaths = {"sourceWallet", "targetWallet"})
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

    // ---------------------------------------------------------------------------------------
    // Surveillance
    // ---------------------------------------------------------------------------------------

    /** Transferts aboutis emis par un portefeuille depuis un instant · pour les detecteurs. */
    @Query("""
            SELECT t FROM WalletTransfer t
            WHERE t.sourceWallet.id = :walletId
              AND t.status = com.sni.bokaticowork.features.payment.transfer.model.WalletTransferStatus.COMPLETED
              AND t.completedAt >= :from
            ORDER BY t.completedAt ASC
            """)
    List<WalletTransfer> findCompletedFromSince(@Param("walletId") Long walletId, @Param("from") Instant from);

    /** Transferts aboutis recus par un portefeuille depuis un instant. */
    @Query("""
            SELECT t FROM WalletTransfer t
            WHERE t.targetWallet.id = :walletId
              AND t.status = com.sni.bokaticowork.features.payment.transfer.model.WalletTransferStatus.COMPLETED
              AND t.completedAt >= :from
            ORDER BY t.completedAt ASC
            """)
    List<WalletTransfer> findCompletedToSince(@Param("walletId") Long walletId, @Param("from") Instant from);

    /** Combien de transferts un emetteur a deja faits vers un destinataire · nul ou zero, c'est un inconnu. */
    @Query("""
            SELECT COUNT(t) FROM WalletTransfer t
            WHERE t.sourceWallet.id = :sourceId AND t.targetWallet.id = :targetId
              AND t.status = com.sni.bokaticowork.features.payment.transfer.model.WalletTransferStatus.COMPLETED
            """)
    long countCompletedBetween(@Param("sourceId") Long sourceId, @Param("targetId") Long targetId);
}
