package com.sni.bokaticowork.features.payment.security.repository;

import com.sni.bokaticowork.features.payment.security.model.WalletSecurityPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WalletSecurityPolicyRepository extends JpaRepository<WalletSecurityPolicy, Long> {

    /**
     * Politiques susceptibles de s'appliquer a ce portefeuille · la globale, celle de son type de
     * titulaire, la sienne. Le choix de celle qui gagne appartient au service, qui connait la regle
     * de precision.
     */
    @Query(nativeQuery = true, value = """
            SELECT *
            FROM wallet_security_policy
            WHERE scope = 'GLOBAL'
               OR (scope = 'OWNER_TYPE' AND scope_code = CAST(:ownerType AS VARCHAR))
               OR (scope = 'WALLET' AND scope_code = CAST(:walletNumber AS VARCHAR))
            """)
    List<WalletSecurityPolicy> findCandidates(@Param("ownerType") String ownerType,
                                              @Param("walletNumber") String walletNumber);
}
