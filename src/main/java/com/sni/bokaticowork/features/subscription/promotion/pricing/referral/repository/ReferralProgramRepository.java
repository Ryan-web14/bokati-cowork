package com.sni.bokaticowork.features.subscription.promotion.pricing.referral.repository;

import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.model.ReferralProgram;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ReferralProgramRepository extends JpaRepository<ReferralProgram, Long> {

    @Query("SELECT p FROM ReferralProgram p WHERE lower(p.code) = lower(:code)")
    Optional<ReferralProgram> findByCode(@Param("code") String code);

    @Query(nativeQuery = true, value = """
            SELECT *
            FROM referral_program
            WHERE status = 'ACTIVE'
              AND (valid_from IS NULL OR valid_from <= :now)
              AND (valid_until IS NULL OR valid_until >= :now)
            ORDER BY id
            """)
    List<ReferralProgram> findOpen(@Param("now") Instant now);
}
