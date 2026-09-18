package com.sni.bokaticowork.features.payment.transfer.repository;

import com.sni.bokaticowork.features.payment.transfer.model.WalletPaymentRequest;
import com.sni.bokaticowork.features.payment.transfer.model.WalletPaymentRequestStatus;
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
public interface WalletPaymentRequestRepository extends JpaRepository<WalletPaymentRequest, Long> {

    Optional<WalletPaymentRequest> findByRequestNumber(String requestNumber);

    /** Demandes ou ce portefeuille est demandeur ou payeur. */
    @Query("""
            SELECT r FROM WalletPaymentRequest r
            WHERE r.requesterWallet.id = :walletId OR r.payerWallet.id = :walletId
            ORDER BY r.createdAt DESC
            """)
    Page<WalletPaymentRequest> findInvolving(@Param("walletId") Long walletId, Pageable pageable);

    @Query("""
            SELECT r FROM WalletPaymentRequest r
            WHERE r.status = :status AND r.expiresAt < :now
            """)
    List<WalletPaymentRequest> findExpired(@Param("status") WalletPaymentRequestStatus status, @Param("now") Instant now);
}
