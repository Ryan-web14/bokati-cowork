package com.sni.bokaticowork.features.subscription.repository;

import com.sni.bokaticowork.features.subscription.subscription.model.Pass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface PassRepository extends JpaRepository<Pass, Long>, JpaSpecificationExecutor<Pass> {

    @Query(nativeQuery = true, value = "SELECT * FROM subscription_pass WHERE pass_number = :passNumber")
    Optional<Pass> findByPassNumber(@Param("passNumber") String passNumber);

    @Query(nativeQuery = true, value = "SELECT * FROM subscription_pass WHERE idempotency_key = :idempotencyKey")
    Optional<Pass> findByIdempotencyKey(@Param("idempotencyKey") String idempotencyKey);

    // ---------------------------------------------------------------------------------------
    // Detection des alertes
    // ---------------------------------------------------------------------------------------

    /** Pass utilisables dont l'echeance tombe dans la fenetre. */
    @Query(nativeQuery = true, value = """
            SELECT *
            FROM subscription_pass
            WHERE status IN ('ACTIVE', 'PARTIALLY_USED')
              AND valid_until IS NOT NULL
              AND valid_until > :now
              AND valid_until <= :until
            ORDER BY valid_until
            LIMIT :limit
            """)
    List<Pass> findExpiringSoon(@Param("now") Instant now,
                                @Param("until") Instant until,
                                @Param("limit") int limit);

    /**
     * Pass dont le solde restant tombe sous le seuil, sans etre nul.
     *
     * <p>Un pass a zero n'est pas une alerte de solde bas · il est consomme, et son statut le dit
     * deja. Prevenir la est trop tard, ce qui est exactement le defaut qu'on corrige.</p>
     */
    @Query(nativeQuery = true, value = """
            SELECT *
            FROM subscription_pass
            WHERE status IN ('ACTIVE', 'PARTIALLY_USED')
              AND max_uses IS NOT NULL
              AND max_uses > 0
              AND (max_uses - COALESCE(used_count, 0)) > 0
              AND (max_uses - COALESCE(used_count, 0))::numeric / max_uses <= :ratio
              AND (valid_until IS NULL OR valid_until > :now)
            ORDER BY (max_uses - COALESCE(used_count, 0))
            LIMIT :limit
            """)
    List<Pass> findLowBalance(@Param("ratio") java.math.BigDecimal ratio,
                              @Param("now") Instant now,
                              @Param("limit") int limit);

    /** Pass actifs, acquis depuis un moment, et jamais utilises. */
    @Query(nativeQuery = true, value = """
            SELECT p.*
            FROM subscription_pass p
            WHERE p.status = 'ACTIVE'
              AND COALESCE(p.used_count, 0) = 0
              AND p.valid_from IS NOT NULL
              AND p.valid_from <= :since
              AND (p.valid_until IS NULL OR p.valid_until > :now)
            ORDER BY p.valid_from
            LIMIT :limit
            """)
    List<Pass> findUnused(@Param("since") Instant since,
                          @Param("now") Instant now,
                          @Param("limit") int limit);

    @Query(nativeQuery = true, value = """
            SELECT *
            FROM subscription_pass
            WHERE owner_type = :ownerType
              AND owner_code = :ownerCode
              AND status = :status
            ORDER BY created_at DESC
            """)
    List<Pass> findAllByOwnerTypeAndOwnerCodeAndStatus(@Param("ownerType") String ownerType,
                                                       @Param("ownerCode") String ownerCode,
                                                       @Param("status") String status);

    @Query(nativeQuery = true, value = "SELECT * FROM subscription_pass WHERE status = :status AND valid_until < :validUntil ORDER BY valid_until ASC")
    List<Pass> findAllByStatusAndValidUntilBefore(@Param("status") String status, @Param("validUntil") Instant validUntil);

    @Query(nativeQuery = true, value = """
            SELECT * FROM subscription_pass
            WHERE status IN (:statuses) AND valid_until < :validUntil
            ORDER BY valid_until ASC
            """)
    List<Pass> findAllByStatusInAndValidUntilBefore(@Param("statuses") List<String> statuses,
                                                     @Param("validUntil") Instant validUntil);

    List<Pass> findAllByContractCode(String contractCode);

    @Query(nativeQuery = true, value = """
            SELECT id
            FROM subscription_pass
            WHERE contract_code IS NULL
              AND created_at <= :createdBefore
              AND status NOT IN ('DRAFT', 'CANCELLED', 'EXPIRED')
            ORDER BY created_at ASC
            LIMIT :limit
            """)
    List<Long> findIdsMissingContract(@Param("createdBefore") Instant createdBefore,
                                      @Param("limit") int limit);
}
