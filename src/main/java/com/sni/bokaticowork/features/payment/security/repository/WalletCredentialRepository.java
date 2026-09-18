package com.sni.bokaticowork.features.payment.security.repository;

import com.sni.bokaticowork.features.payment.security.model.WalletCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface WalletCredentialRepository extends JpaRepository<WalletCredential, Long> {

    @Query("SELECT c FROM WalletCredential c WHERE c.wallet.id = :walletId")
    Optional<WalletCredential> findByWalletId(@Param("walletId") Long walletId);
}
