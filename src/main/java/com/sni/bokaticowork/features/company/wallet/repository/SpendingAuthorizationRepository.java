package com.sni.bokaticowork.features.company.wallet.repository;

import com.sni.bokaticowork.features.company.wallet.model.SpendingAuthorization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SpendingAuthorizationRepository extends JpaRepository<SpendingAuthorization, Long> {

    Optional<SpendingAuthorization> findByShortId(Long shortId);

    @Query("""
            SELECT a FROM SpendingAuthorization a
            WHERE lower(a.companyCode) = lower(:companyCode)
            ORDER BY a.status ASC, a.createdAt DESC
            """)
    List<SpendingAuthorization> findAllOfCompany(@Param("companyCode") String companyCode);

    /** L'autorisation en vigueur ou suspendue d'un collaborateur sur un portefeuille · pas les revoquees. */
    @Query("""
            SELECT a FROM SpendingAuthorization a
            WHERE a.walletId = :walletId AND lower(a.memberCode) = lower(:memberCode)
              AND a.status <> com.sni.bokaticowork.features.company.wallet.model.SpendingAuthorization.Status.REVOKED
            """)
    Optional<SpendingAuthorization> findOpen(@Param("walletId") Long walletId, @Param("memberCode") String memberCode);

    /** Les autorisations d'un collaborateur, toutes organisations · celles qui ne sont pas revoquees. */
    @Query("""
            SELECT a FROM SpendingAuthorization a
            WHERE lower(a.memberCode) = lower(:memberCode)
              AND a.status <> com.sni.bokaticowork.features.company.wallet.model.SpendingAuthorization.Status.REVOKED
            ORDER BY a.createdAt DESC
            """)
    List<SpendingAuthorization> findOpenOfMember(@Param("memberCode") String memberCode);

    @Query("""
            SELECT a FROM SpendingAuthorization a
            WHERE lower(a.memberCode) = lower(:memberCode) AND lower(a.companyCode) = lower(:companyCode)
              AND a.status = com.sni.bokaticowork.features.company.wallet.model.SpendingAuthorization.Status.ACTIVE
            """)
    List<SpendingAuthorization> findActiveOfMemberInCompany(@Param("memberCode") String memberCode,
                                                            @Param("companyCode") String companyCode);
}
