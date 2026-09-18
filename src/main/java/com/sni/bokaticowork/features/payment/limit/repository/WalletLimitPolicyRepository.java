package com.sni.bokaticowork.features.payment.limit.repository;

import com.sni.bokaticowork.features.payment.limit.model.WalletLimitPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface WalletLimitPolicyRepository extends JpaRepository<WalletLimitPolicy, Long> {

    @Query("SELECT p FROM WalletLimitPolicy p WHERE lower(p.code) = lower(:code) AND p.active = true")
    Optional<WalletLimitPolicy> findByCode(@Param("code") String code);

    /**
     * Politique du niveau atteint, ou du plus proche en dessous.
     *
     * <p>Un titulaire de niveau 2 sur un systeme qui n'a de politique qu'aux niveaux 1 et 3 doit
     * relever du niveau 1 · lui appliquer le niveau 3 lui accorderait des plafonds qu'il n'a pas
     * merites, et n'en appliquer aucun reviendrait au meme.</p>
     */
    @Query(nativeQuery = true, value = """
            SELECT *
            FROM wallet_limit_policy
            WHERE active = true
              AND kyc_level <= :kycLevel
            ORDER BY kyc_level DESC
            LIMIT 1
            """)
    Optional<WalletLimitPolicy> findForKycLevel(@Param("kycLevel") int kycLevel);
}
