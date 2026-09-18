package com.sni.bokaticowork.features.subscription.subscription.pass.repository;

import com.sni.bokaticowork.features.subscription.subscription.pass.model.PassUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface PassUsageRepository extends JpaRepository<PassUsage, Long> {

    @Query("SELECT u FROM PassUsage u WHERE u.usageNumber = :usageNumber")
    Optional<PassUsage> findByNumber(@Param("usageNumber") String usageNumber);

    /** Usage deja enregistre pour ce declencheur · un double scan retrouve le sien au lieu d'en creer un. */
    @Query(nativeQuery = true, value = """
            SELECT *
            FROM pass_usage
            WHERE pass_id = :passId
              AND reference_type = CAST(:referenceType AS VARCHAR)
              AND reference_code = CAST(:referenceCode AS VARCHAR)
              AND reversed_at IS NULL
            LIMIT 1
            """)
    Optional<PassUsage> findByReference(@Param("passId") Long passId,
                                        @Param("referenceType") String referenceType,
                                        @Param("referenceCode") String referenceCode);

    /** Usages du jour, pour la regle MAX_PER_DAY. */
    @Query(nativeQuery = true, value = """
            SELECT COUNT(*)
            FROM pass_usage
            WHERE pass_id = :passId
              AND reversed_at IS NULL
              AND status <> 'CANCELLED'
              AND started_at >= :dayStart
              AND started_at < :dayEnd
            """)
    long countOnDay(@Param("passId") Long passId,
                    @Param("dayStart") Instant dayStart,
                    @Param("dayEnd") Instant dayEnd);

    /** Usages en cours, pour la regle MAX_CONCURRENT. */
    @Query(nativeQuery = true, value = """
            SELECT COUNT(*)
            FROM pass_usage
            WHERE pass_id = :passId
              AND reversed_at IS NULL
              AND status IN ('RESERVED', 'ACTIVE')
            """)
    long countInFlight(@Param("passId") Long passId);
}
