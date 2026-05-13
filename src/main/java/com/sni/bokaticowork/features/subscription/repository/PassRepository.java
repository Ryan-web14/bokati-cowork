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
