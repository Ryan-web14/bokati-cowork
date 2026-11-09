package com.sni.bokaticowork.features.subscription.promotion.pricing.referral.repository;

import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.model.ReferralLink;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ReferralLinkRepository extends JpaRepository<ReferralLink, Long> {

    @Query("SELECT l FROM ReferralLink l WHERE lower(l.code) = lower(:code)")
    Optional<ReferralLink> findByCode(@Param("code") String code);

    @Query("""
            SELECT l FROM ReferralLink l
            WHERE l.program.id = :programId
              AND lower(l.referrerType) = lower(:referrerType)
              AND lower(l.referrerCode) = lower(:referrerCode)
            """)
    Optional<ReferralLink> findForReferrer(@Param("programId") Long programId,
                                           @Param("referrerType") String referrerType,
                                           @Param("referrerCode") String referrerCode);

    @Query(nativeQuery = true, value = "SELECT EXISTS(SELECT 1 FROM referral_link WHERE lower(code) = lower(:code))")
    boolean existsByCode(@Param("code") String code);

    Page<ReferralLink> findByProgram_IdOrderByCreatedAtDesc(Long programId, Pageable pageable);

    /** Les liens d'un parrain, tous programmes confondus. */
    @Query("""
            SELECT l FROM ReferralLink l
            WHERE lower(l.referrerType) = lower(:referrerType)
              AND lower(l.referrerCode) = lower(:referrerCode)
            ORDER BY l.createdAt DESC
            """)
    List<ReferralLink> findAllForReferrer(@Param("referrerType") String referrerType,
                                         @Param("referrerCode") String referrerCode);

    long countByProgram_Id(Long programId);
}
