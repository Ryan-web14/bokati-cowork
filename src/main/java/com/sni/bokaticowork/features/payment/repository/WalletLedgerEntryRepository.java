package com.sni.bokaticowork.features.payment.repository;

import com.sni.bokaticowork.features.payment.model.WalletLedgerEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface WalletLedgerEntryRepository extends JpaRepository<WalletLedgerEntry, Long> {

    @Query(
            nativeQuery = true,
            value = """
                    SELECT *
                    FROM wallet_ledger_entry
                    WHERE wallet_id = :walletId
                    ORDER BY created_at DESC
                    """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM wallet_ledger_entry
                    WHERE wallet_id = :walletId
                    """
    )
    Page<WalletLedgerEntry> findAllByWalletIdOrderByCreatedAtDesc(@Param("walletId") Long walletId, Pageable pageable);
}
