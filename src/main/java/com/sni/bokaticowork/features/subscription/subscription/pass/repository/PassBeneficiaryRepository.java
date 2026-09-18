package com.sni.bokaticowork.features.subscription.subscription.pass.repository;

import com.sni.bokaticowork.features.subscription.subscription.pass.model.PassBeneficiary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PassBeneficiaryRepository extends JpaRepository<PassBeneficiary, Long> {

    @Query("""
            SELECT b FROM PassBeneficiary b
            WHERE b.pass.id = :passId
              AND lower(b.beneficiaryType) = lower(:beneficiaryType)
              AND lower(b.beneficiaryCode) = lower(:beneficiaryCode)
            """)
    Optional<PassBeneficiary> find(@Param("passId") Long passId,
                                   @Param("beneficiaryType") String beneficiaryType,
                                   @Param("beneficiaryCode") String beneficiaryCode);

    @Query("SELECT b FROM PassBeneficiary b WHERE b.pass.id = :passId")
    List<PassBeneficiary> findAllByPassId(@Param("passId") Long passId);

    @Query("SELECT COUNT(b) FROM PassBeneficiary b WHERE b.pass.id = :passId AND b.revokedAt IS NULL")
    long countActive(@Param("passId") Long passId);
}
