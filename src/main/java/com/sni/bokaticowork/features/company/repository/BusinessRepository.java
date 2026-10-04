package com.sni.bokaticowork.features.company.repository;


import com.sni.bokaticowork.features.company.model.BusinessEntity;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BusinessRepository extends JpaRepository<BusinessEntity, Long>, JpaSpecificationExecutor<BusinessEntity> {

    Optional<BusinessEntity> findByCode(String code);

    List<BusinessEntity> findAllByDeletedFalse();

    /**
     * L entite qui exploite l espace · celle qui signe face au client.
     *
     * <p>Elle etait deduite de « la seule ligne active », alors que cette table porte aussi les
     * clients entreprises · des le premier client entreprise enregistre, la deduction cassait et
     * aucun contrat ne se generait plus. Le drapeau la designe, et un index unique partiel
     * garantit qu au plus une ligne vivante le porte.</p>
     */
    Optional<BusinessEntity> findFirstByOperatorTrueAndDeletedFalse();
    Optional<BusinessEntity> findByNiuNumber(String niuNumber);
    Optional<BusinessEntity> findByRccmNumber(String rccmNumber);
    Optional<BusinessEntity> findByNameIgnoreCase(String name);
    @Query(nativeQuery = true, value = "SELECT * FROM business_entity WHERE deleted = false AND lower(email) = lower(:email) LIMIT 1")
    Optional<BusinessEntity> findByEmailAndDeletedFalse(@Param("email") String email);
    @Query(nativeQuery = true, value = "SELECT * FROM business_entity WHERE deleted = false AND phone = :phone LIMIT 1")
    Optional<BusinessEntity> findByPhoneAndDeletedFalse(@Param("phone") String phone);
    boolean existsByCode(String code);
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNiuNumber(String niuNumber);
    boolean existsByRccmNumber(String rccmNumber);

    @Modifying
    @Query(nativeQuery = true, value= "UPDATE business_entity SET deleted = true WHERE entity_code = :businessCode")
    void softDelete(@Param("businessCode") String businessCode);

    @Query("""
            SELECT b
            FROM BusinessEntity b
            WHERE b.deleted = false
              AND (
                   LOWER(b.code) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(b.name) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(b.niuNumber) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(b.rccmNumber) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(COALESCE(b.email, '')) LIKE LOWER(CONCAT('%', :query, '%'))
                OR COALESCE(b.phone, '') LIKE CONCAT('%', :query, '%')
              )
            ORDER BY b.name ASC
            """)
    List<BusinessEntity> basicSearch(@Param("query") String query);
}
