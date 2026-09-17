package com.sni.bokaticowork.features.subscription.promotion.pricing.referral.repository;

import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.model.Referral;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ReferralRepository extends JpaRepository<Referral, Long> {

    @Query("SELECT r FROM Referral r WHERE r.referralNumber = :referralNumber")
    Optional<Referral> findByNumber(@Param("referralNumber") String referralNumber);

    /** Un filleul n'est parraine qu'une fois par programme, quel que soit le parrain. */
    @Query("""
            SELECT r FROM Referral r
            WHERE r.program.id = :programId
              AND lower(r.refereeType) = lower(:refereeType)
              AND lower(r.refereeCode) = lower(:refereeCode)
            """)
    Optional<Referral> findByReferee(@Param("programId") Long programId,
                                     @Param("refereeType") String refereeType,
                                     @Param("refereeCode") String refereeCode);

    /**
     * Parrainages deja comptes contre le plafond d'un parrain. Les refuses n'y figurent pas : un
     * parrainage ecarte n'a rien coute et ne doit pas fermer la porte au suivant.
     */
    @Query(nativeQuery = true, value = """
            SELECT COUNT(*)
            FROM referral
            WHERE program_id = :programId
              AND lower(referrer_type) = lower(CAST(:referrerType AS VARCHAR))
              AND lower(referrer_code) = lower(CAST(:referrerCode AS VARCHAR))
              AND status <> 'REJECTED'
            """)
    long countAgainstCap(@Param("programId") Long programId,
                         @Param("referrerType") String referrerType,
                         @Param("referrerCode") String referrerCode);

    /** Parrainages en attente dont l'anciennete exigee est atteinte. */
    @Query(nativeQuery = true, value = """
            SELECT r.*
            FROM referral r
            JOIN referral_program p ON p.id = r.program_id
            WHERE r.status = 'PENDING'
              AND p.qualification_rule = 'TENURE_REACHED'
              AND p.qualification_delay_days IS NOT NULL
              AND r.registered_at + make_interval(days => p.qualification_delay_days) <= :now
            ORDER BY r.registered_at
            LIMIT :limit
            """)
    List<Referral> findDueByTenure(@Param("now") Instant now, @Param("limit") int limit);

    @Query("""
            SELECT r FROM Referral r
            WHERE lower(r.refereeType) = lower(:refereeType)
              AND lower(r.refereeCode) = lower(:refereeCode)
              AND r.status = com.sni.bokaticowork.features.subscription.promotion.pricing.referral.enums.ReferralStatus.PENDING
            """)
    List<Referral> findPendingForReferee(@Param("refereeType") String refereeType,
                                         @Param("refereeCode") String refereeCode);
}
