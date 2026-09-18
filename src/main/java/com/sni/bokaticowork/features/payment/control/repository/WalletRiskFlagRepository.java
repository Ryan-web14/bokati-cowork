package com.sni.bokaticowork.features.payment.control.repository;

import com.sni.bokaticowork.features.payment.control.model.WalletRiskFlag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;

@Repository
public interface WalletRiskFlagRepository extends JpaRepository<WalletRiskFlag, Long> {

    Optional<WalletRiskFlag> findByFlagNumber(String flagNumber);

    Optional<WalletRiskFlag> findFirstByWallet_IdAndFlagTypeAndStatus(Long walletId, WalletRiskFlag.Type type,
                                                                      WalletRiskFlag.Status status);

    // Deux methodes plutot qu'un « :statuses IS NULL OR ... » : une collection nulle liee a un IN
    // n'est pas portable, et le test de nullite d'une collection en JPQL ne l'est pas davantage.
    Page<WalletRiskFlag> findByStatusInOrderBySeverityDescDetectedAtDesc(Collection<WalletRiskFlag.Status> statuses, Pageable pageable);

    Page<WalletRiskFlag> findAllByOrderBySeverityDescDetectedAtDesc(Pageable pageable);

    Page<WalletRiskFlag> findByWallet_IdOrderByDetectedAtDesc(Long walletId, Pageable pageable);

    long countByStatusIn(Collection<WalletRiskFlag.Status> statuses);
}
