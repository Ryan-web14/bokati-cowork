package com.sni.bokaticowork.features.company.wallet.repository;

import com.sni.bokaticowork.features.company.wallet.model.CompanyWalletSpend;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CompanyWalletSpendRepository extends JpaRepository<CompanyWalletSpend, Long> {

    Optional<CompanyWalletSpend> findByShortId(Long shortId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM CompanyWalletSpend s WHERE s.shortId = :shortId")
    Optional<CompanyWalletSpend> lockByShortId(@Param("shortId") Long shortId);

    /**
     * Ce qu'un collaborateur a engage sur le portefeuille depuis un instant · les depenses executees
     * et celles qui attendent, pour qu'une serie de demandes ne contourne pas les plafonds.
     */
    @Query("""
            SELECT COALESCE(SUM(s.amount), 0) FROM CompanyWalletSpend s
            WHERE s.walletId = :walletId AND lower(s.spentByMember) = lower(:memberCode)
              AND s.createdAt >= :since
              AND s.status IN (com.sni.bokaticowork.features.company.wallet.model.CompanyWalletSpend.Status.EXECUTED,
                               com.sni.bokaticowork.features.company.wallet.model.CompanyWalletSpend.Status.PENDING_APPROVAL)
            """)
    BigDecimal sumEngagedSince(@Param("walletId") Long walletId, @Param("memberCode") String memberCode,
                               @Param("since") Instant since);

    @Query("""
            SELECT s FROM CompanyWalletSpend s
            WHERE s.walletId = :walletId
              AND (:status IS NULL OR s.status = :status)
            ORDER BY s.createdAt DESC
            """)
    List<CompanyWalletSpend> findOfWallet(@Param("walletId") Long walletId,
                                          @Param("status") CompanyWalletSpend.Status status,
                                          org.springframework.data.domain.Pageable pageable);

    @Query("""
            SELECT s FROM CompanyWalletSpend s
            WHERE s.walletId = :walletId AND lower(s.spentByMember) = lower(:memberCode)
            ORDER BY s.createdAt DESC
            """)
    List<CompanyWalletSpend> findOfMember(@Param("walletId") Long walletId, @Param("memberCode") String memberCode,
                                          org.springframework.data.domain.Pageable pageable);

    @Query("""
            SELECT s FROM CompanyWalletSpend s
            WHERE s.status = com.sni.bokaticowork.features.company.wallet.model.CompanyWalletSpend.Status.PENDING_APPROVAL
              AND s.expiresAt <= :now
            ORDER BY s.expiresAt ASC
            """)
    List<CompanyWalletSpend> findExpiredPending(@Param("now") Instant now, org.springframework.data.domain.Pageable pageable);

    @Query("""
            SELECT s FROM CompanyWalletSpend s
            WHERE lower(s.companyCode) = lower(:companyCode) AND lower(s.spentByMember) = lower(:memberCode)
              AND s.status = com.sni.bokaticowork.features.company.wallet.model.CompanyWalletSpend.Status.PENDING_APPROVAL
            """)
    List<CompanyWalletSpend> findPendingOfMemberInCompany(@Param("companyCode") String companyCode,
                                                          @Param("memberCode") String memberCode);

    @Query("""
            SELECT COUNT(s) > 0 FROM CompanyWalletSpend s
            WHERE lower(s.documentNumber) = lower(:documentNumber)
              AND s.status = com.sni.bokaticowork.features.company.wallet.model.CompanyWalletSpend.Status.PENDING_APPROVAL
            """)
    boolean existsPendingForDocument(@Param("documentNumber") String documentNumber);

    @Query("SELECT s FROM CompanyWalletSpend s WHERE s.intentNumber IN :numbers")
    List<CompanyWalletSpend> findByIntentNumbers(@Param("numbers") Collection<String> numbers);
}
