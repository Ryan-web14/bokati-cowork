package com.sni.bokaticowork.features.payment.repository;

import com.sni.bokaticowork.features.payment.model.WalletLedgerEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface WalletLedgerEntryRepository extends JpaRepository<WalletLedgerEntry, Long> {

    Page<WalletLedgerEntry> findByWallet_IdOrderByCreatedAtDesc(Long walletId, Pageable pageable);

    Optional<WalletLedgerEntry> findByIdempotencyKey(String idempotencyKey);

    Optional<WalletLedgerEntry> findByTransactionNumber(String transactionNumber);

    /**
     * Par son numero d'ecriture · celui que le releve et la liste affichent en premier.
     *
     * <p>Une ecriture porte deux identifiants : le sien (WLE) et celui de la transaction (WTX),
     * ce dernier absent sur les ecritures anterieures au chainage. Les deux doivent mener au
     * meme recu.</p>
     */
    Optional<WalletLedgerEntry> findByEntryNumber(String entryNumber);

    /** Les ecritures d'une periode, dans l'ordre d'ecriture · celui d'un releve. */
    @Query(nativeQuery = true, value = """
            SELECT *
            FROM wallet_ledger_entry
            WHERE wallet_id = :walletId
              AND created_at >= :from
              AND created_at < :to
            ORDER BY id ASC
            """)
    java.util.List<WalletLedgerEntry> findBetween(@Param("walletId") Long walletId,
                                                  @Param("from") java.time.Instant from,
                                                  @Param("to") java.time.Instant to);

    /** Solde comptable a un instant · celui de la derniere ecriture anterieure, ou zero. */
    @Query(nativeQuery = true, value = """
            SELECT balance_after
            FROM wallet_ledger_entry
            WHERE wallet_id = :walletId
              AND created_at < :before
            ORDER BY id DESC
            LIMIT 1
            """)
    Optional<BigDecimal> findBalanceBefore(@Param("walletId") Long walletId, @Param("before") java.time.Instant before);

    /**
     * Somme signee des ecritures qui deplacent reellement le solde comptable.
     * <p>
     * Les types HOLD et HOLD_RELEASE sont exclus : ils ne font que transferer entre le
     * solde disponible et le solde bloque sans toucher au solde comptable, donc les
     * inclure fausserait le rapprochement.
     */
    @Query(nativeQuery = true, value = """
            SELECT COALESCE(SUM(CASE WHEN direction = 'CREDIT' THEN amount ELSE -amount END), 0)
            FROM wallet_ledger_entry
            WHERE wallet_id = :walletId
              AND entry_type NOT IN ('HOLD', 'HOLD_RELEASE')
            """)
    BigDecimal sumLedgerImpact(@Param("walletId") Long walletId);

    // ---------------------------------------------------------------------------------------
    // Consommation des plafonds
    // ---------------------------------------------------------------------------------------

    /**
     * Montant deja engage sur une periode, pour un type d'ecriture donne.
     *
     * <p>Seul le type demande est somme. Une contre-passation porte le type {@code REVERSAL} et ne
     * vient donc pas s'en retrancher : un transfert annule pese sur le plafond du jour ou il a ete
     * emis. C'est volontaire · emettre puis annuler en boucle ne doit pas servir a se fabriquer un
     * plafond sans fin.</p>
     */
    @Query(nativeQuery = true, value = """
            SELECT COALESCE(SUM(amount), 0)
            FROM wallet_ledger_entry
            WHERE wallet_id = :walletId
              AND entry_type = CAST(:entryType AS VARCHAR)
              AND created_at >= :from
            """)
    java.math.BigDecimal sumSince(@Param("walletId") Long walletId,
                                  @Param("entryType") String entryType,
                                  @Param("from") java.time.Instant from);

    /**
     * Empreinte de la derniere ecriture du portefeuille · maillon auquel la suivante s'accroche.
     *
     * <p>Interrogee sous le verrou exclusif deja pose sur {@code wallet_account}, donc jamais en
     * concurrence : deux ecritures du meme portefeuille ne peuvent pas lire le meme maillon.</p>
     */
    @Query(nativeQuery = true, value = """
            SELECT current_hash
            FROM wallet_ledger_entry
            WHERE wallet_id = :walletId
            ORDER BY id DESC
            LIMIT 1
            """)
    Optional<String> findLastHash(@Param("walletId") Long walletId);

    /** Le journal d'un portefeuille dans l'ordre ou il a ete ecrit · l'ordre de verification. */
    @Query(nativeQuery = true, value = """
            SELECT *
            FROM wallet_ledger_entry
            WHERE wallet_id = :walletId
            ORDER BY id ASC
            """)
    java.util.List<WalletLedgerEntry> findChain(@Param("walletId") Long walletId);

    // ---------------------------------------------------------------------------------------
    // Centre de controle
    // ---------------------------------------------------------------------------------------

    /** Volume par nature et sens sur une periode · la matiere du tableau de bord. */
    @Query(nativeQuery = true, value = """
            SELECT entry_type, direction, COUNT(*), COALESCE(SUM(amount), 0)
            FROM wallet_ledger_entry
            WHERE created_at >= :from AND created_at < :to
            GROUP BY entry_type, direction
            ORDER BY entry_type, direction
            """)
    java.util.List<Object[]> volumeBetween(@Param("from") java.time.Instant from, @Param("to") java.time.Instant to);

    /** Toutes les ecritures d'une periode, tous portefeuilles · l'export comptable. */
    @Query(nativeQuery = true, value = """
            SELECT e.*
            FROM wallet_ledger_entry e
            WHERE e.created_at >= :from AND e.created_at < :to
            ORDER BY e.id ASC
            """)
    java.util.List<WalletLedgerEntry> findAllBetween(@Param("from") java.time.Instant from, @Param("to") java.time.Instant to);

    /** Nombre d'operations sortantes sur la periode · un plafond en nombre, pas en montant. */
    @Query(nativeQuery = true, value = """
            SELECT COUNT(*)
            FROM wallet_ledger_entry
            WHERE wallet_id = :walletId
              AND direction = 'DEBIT'
              AND created_at >= :from
            """)
    long countDebitsSince(@Param("walletId") Long walletId, @Param("from") java.time.Instant from);
}
