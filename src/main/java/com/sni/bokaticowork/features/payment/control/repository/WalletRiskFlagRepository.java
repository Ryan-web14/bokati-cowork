package com.sni.bokaticowork.features.payment.control.repository;

import com.sni.bokaticowork.features.payment.control.model.WalletRiskFlag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;

@Repository
public interface WalletRiskFlagRepository extends JpaRepository<WalletRiskFlag, Long> {

    @EntityGraph(attributePaths = {"wallet"})
    Optional<WalletRiskFlag> findByFlagNumber(String flagNumber);

    Optional<WalletRiskFlag> findFirstByWallet_IdAndFlagTypeAndStatus(Long walletId, WalletRiskFlag.Type type,
                                                                      WalletRiskFlag.Status status);

    // Deux methodes plutot qu'un « :statuses IS NULL OR ... » : une collection nulle liee a un IN
    // n'est pas portable, et le test de nullite d'une collection en JPQL ne l'est pas davantage.
    @EntityGraph(attributePaths = {"wallet"})
    Page<WalletRiskFlag> findByStatusInOrderBySeverityDescDetectedAtDesc(Collection<WalletRiskFlag.Status> statuses, Pageable pageable);

    @EntityGraph(attributePaths = {"wallet"})
    Page<WalletRiskFlag> findAllByOrderBySeverityDescDetectedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"wallet"})
    Page<WalletRiskFlag> findByWallet_IdOrderByDetectedAtDesc(Long walletId, Pageable pageable);

    long countByStatusIn(Collection<WalletRiskFlag.Status> statuses);

    long countByWallet_IdAndFlagTypeAndDetectedAtAfter(Long walletId, WalletRiskFlag.Type type, java.time.Instant after);

    Optional<WalletRiskFlag> findFirstByWallet_IdAndFlagTypeOrderByDetectedAtDesc(Long walletId, WalletRiskFlag.Type type);

    /** Les regles qui declenchent le plus · pour le tableau de bord de la surveillance. */
    @org.springframework.data.jpa.repository.Query(nativeQuery = true, value = """
            SELECT rule_code, COUNT(*)
            FROM wallet_risk_flag
            WHERE rule_code IS NOT NULL AND detected_at >= :since
            GROUP BY rule_code
            ORDER BY COUNT(*) DESC
            """)
    java.util.List<Object[]> countByRuleSince(@org.springframework.data.repository.query.Param("since") java.time.Instant since);
}
