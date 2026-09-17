package com.sni.bokaticowork.features.subscription.promotion.pricing.referral.repository;

import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.model.ReferralLink;
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
}
