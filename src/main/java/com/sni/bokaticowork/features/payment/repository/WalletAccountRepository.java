package com.sni.bokaticowork.features.payment.repository;

import com.sni.bokaticowork.features.payment.model.WalletAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WalletAccountRepository extends JpaRepository<WalletAccount, Long> {

    /**
     * Verrou exclusif sur un compte, pour qui doit en tenir deux a la fois.
     *
     * <p>Le grand livre verrouille deja chaque compte qu'il ecrit, mais un a un. Un transfert qui
     * en touche deux doit les prendre tous les deux <em>avant</em> la premiere ecriture, et dans un
     * ordre fixe · sinon deux transferts croises entre les memes comptes s'interbloquent.</p>
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM WalletAccount w WHERE w.id = :id")
    Optional<WalletAccount> findByIdForUpdate(@Param("id") Long id);

    @Query(nativeQuery = true, value = "SELECT * FROM wallet_account WHERE wallet_number = :walletNumber")
    Optional<WalletAccount> findByWalletNumber(@Param("walletNumber") String walletNumber);

    /**
     * Identifiants des portefeuilles a rapprocher, pour le worker de reconciliation.
     * Requete sur la cle seule : le worker recharge chaque compte dans sa propre transaction
     * afin qu'un ecart isole ne fasse pas echouer le lot entier.
     */
    @Query(nativeQuery = true, value = "SELECT id FROM wallet_account WHERE status <> 'CLOSED' ORDER BY id")
    java.util.List<Long> findAllOpenIds();

    @Query(nativeQuery = true, value = """
            SELECT *
            FROM wallet_account
            WHERE owner_type = :ownerType
              AND owner_code = :ownerCode
              AND currency = :currency
            """)
    Optional<WalletAccount> findByOwnerAndCurrency(@Param("ownerType") String ownerType,
                                                   @Param("ownerCode") String ownerCode,
                                                   @Param("currency") String currency);

    // ---------------------------------------------------------------------------------------
    // Centre de controle
    // ---------------------------------------------------------------------------------------

    /** Encours, retenues et nombre de portefeuilles ouverts, par devise. */
    @Query(nativeQuery = true, value = """
            SELECT COALESCE(SUM(ledger_balance), 0), COALESCE(SUM(held_balance), 0), COUNT(*)
            FROM wallet_account
            WHERE status <> 'CLOSED' AND currency = :currency
            """)
    Object[] aggregateOpen(@Param("currency") String currency);

    @Query(nativeQuery = true, value = """
            SELECT status, COUNT(*)
            FROM wallet_account
            GROUP BY status
            """)
    java.util.List<Object[]> countByStatus();

    @Query(nativeQuery = true, value = "SELECT COUNT(*) FROM wallet_account WHERE frozen_at IS NOT NULL")
    long countFrozen();

    @Query(nativeQuery = true, value = "SELECT COUNT(*) FROM wallet_account WHERE locked_by_owner_at IS NOT NULL")
    long countLockedByOwner();

    @Query(nativeQuery = true, value = "SELECT COUNT(*) FROM wallet_account WHERE dormant_since IS NOT NULL")
    long countDormant();

    @Query(nativeQuery = true, value = "SELECT COUNT(*) FROM wallet_account WHERE ledger_balance < 0 OR available_balance < 0")
    long countNegative();

    /**
     * Portefeuilles sans mouvement depuis la date, pas encore marques dormants.
     *
     * <p>Un portefeuille sans aucune activite enregistree est juge sur sa date d'ouverture · un
     * compte ouvert et jamais utilise dort aussi.</p>
     */
    @Query(nativeQuery = true, value = """
            SELECT id
            FROM wallet_account
            WHERE status <> 'CLOSED'
              AND dormant_since IS NULL
              AND COALESCE(last_activity_at, opened_at) < :before
            """)
    java.util.List<Long> findDormantCandidates(@Param("before") java.time.Instant before);

    @Query(value = """
            SELECT *
            FROM wallet_account
            WHERE (:ownerType IS NULL OR owner_type = :ownerType)
              AND (:ownerCode IS NULL OR owner_code = :ownerCode)
            ORDER BY created_at DESC
            """,
           countQuery = """
            SELECT COUNT(*)
            FROM wallet_account
            WHERE (:ownerType IS NULL OR owner_type = :ownerType)
              AND (:ownerCode IS NULL OR owner_code = :ownerCode)
            """,
           nativeQuery = true)
    Page<WalletAccount> list(@Param("ownerType") String ownerType,
                             @Param("ownerCode") String ownerCode,
                             Pageable pageable);
}
