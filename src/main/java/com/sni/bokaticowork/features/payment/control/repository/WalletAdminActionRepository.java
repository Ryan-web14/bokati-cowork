package com.sni.bokaticowork.features.payment.control.repository;

import com.sni.bokaticowork.features.payment.control.model.WalletAdminAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WalletAdminActionRepository extends JpaRepository<WalletAdminAction, Long> {

    @EntityGraph(attributePaths = {"wallet"})
    Optional<WalletAdminAction> findByActionNumber(String actionNumber);

    @EntityGraph(attributePaths = {"wallet"})
    Page<WalletAdminAction> findByWallet_IdOrderByCreatedAtDesc(Long walletId, Pageable pageable);

    @EntityGraph(attributePaths = {"wallet"})
    Page<WalletAdminAction> findByStatusOrderByCreatedAtDesc(WalletAdminAction.Status status, Pageable pageable);

    long countByStatus(WalletAdminAction.Status status);
}
