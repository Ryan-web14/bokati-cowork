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
}
