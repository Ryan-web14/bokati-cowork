package com.sni.bokaticowork.features.payment.repository;

import com.sni.bokaticowork.features.payment.model.WalletLedgerEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WalletLedgerEntryRepository extends JpaRepository<WalletLedgerEntry, Long> {

    Page<WalletLedgerEntry> findByWallet_IdOrderByCreatedAtDesc(Long walletId, Pageable pageable);
}
