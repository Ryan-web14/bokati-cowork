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

    /** Les acces non revoques d'une personne aux passes d'un type de titulaire · l'organisation. */
    @Query("""
            SELECT b FROM PassBeneficiary b JOIN FETCH b.pass p
            WHERE lower(b.beneficiaryType) = lower(:beneficiaryType)
              AND lower(b.beneficiaryCode) = lower(:beneficiaryCode)
              AND b.revokedAt IS NULL
              AND p.ownerType = :ownerType
            ORDER BY b.addedAt ASC
            """)
    List<PassBeneficiary> findOpenAccesses(@Param("beneficiaryType") String beneficiaryType,
                                           @Param("beneficiaryCode") String beneficiaryCode,
                                           @Param("ownerType") com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType ownerType);

    /** Les acces non revoques d'une personne aux passes d'une organisation · a revoquer quand le lien prend fin. */
    @Query("""
            SELECT b FROM PassBeneficiary b JOIN FETCH b.pass p
            WHERE lower(b.beneficiaryType) = lower(:beneficiaryType)
              AND lower(b.beneficiaryCode) = lower(:beneficiaryCode)
              AND b.revokedAt IS NULL
              AND p.ownerType = :ownerType
              AND lower(p.ownerCode) = lower(:ownerCode)
            """)
    List<PassBeneficiary> findOpenAccessesWithOwner(@Param("beneficiaryType") String beneficiaryType,
                                                    @Param("beneficiaryCode") String beneficiaryCode,
                                                    @Param("ownerType") com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType ownerType,
                                                    @Param("ownerCode") String ownerCode);

    @Query("SELECT b FROM PassBeneficiary b JOIN FETCH b.pass WHERE b.shortId = :shortId")
    Optional<PassBeneficiary> findByShortId(@Param("shortId") Long shortId);

    @Query("SELECT COUNT(b) FROM PassBeneficiary b WHERE b.pass.id = :passId AND b.revokedAt IS NULL")
    long countActive(@Param("passId") Long passId);
}
