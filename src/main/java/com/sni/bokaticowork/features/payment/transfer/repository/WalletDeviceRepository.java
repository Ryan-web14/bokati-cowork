package com.sni.bokaticowork.features.payment.transfer.repository;

import com.sni.bokaticowork.features.payment.transfer.model.WalletDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WalletDeviceRepository extends JpaRepository<WalletDevice, Long> {

    Optional<WalletDevice> findByWallet_IdAndDeviceId(Long walletId, String deviceId);

    List<WalletDevice> findByWallet_IdOrderByLastSeenAtDesc(Long walletId);

    Optional<WalletDevice> findByIdAndWallet_Id(Long id, Long walletId);

    /** Combien de portefeuilles distincts un appareil a pilotes depuis un instant · un seul, c'est normal. */
    @org.springframework.data.jpa.repository.Query("""
            SELECT COUNT(DISTINCT d.wallet.id) FROM WalletDevice d
            WHERE d.deviceId = :deviceId AND d.lastSeenAt >= :from AND d.revokedAt IS NULL
            """)
    long countDistinctWalletsUsingDevice(@org.springframework.data.repository.query.Param("deviceId") String deviceId,
                                         @org.springframework.data.repository.query.Param("from") java.time.Instant from);
}
