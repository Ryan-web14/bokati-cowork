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
