package com.sni.bokaticowork.features.payment.repository;

import com.sni.bokaticowork.features.payment.model.CashRegister;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CashRegisterRepository extends JpaRepository<CashRegister, Long> {

    @Query(nativeQuery = true, value = "SELECT * FROM cash_register WHERE register_code = :registerCode")
    Optional<CashRegister> findByRegisterCode(@Param("registerCode") String registerCode);

    @Query(nativeQuery = true,
            value = """
                    SELECT cr.*
                    FROM cash_register cr
                    WHERE (:active IS NULL OR cr.active = :active)
                      AND (CAST(:locationCode AS VARCHAR) IS NULL OR cr.location_code = CAST(:locationCode AS VARCHAR))
                      AND (CAST(:businessEntityCode AS VARCHAR) IS NULL OR cr.business_entity_code = CAST(:businessEntityCode AS VARCHAR))
                      AND (
                          CAST(:searchText AS VARCHAR) IS NULL
                          OR cr.register_code ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                          OR cr.name ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                          OR COALESCE(cr.location_code, '') ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                          OR COALESCE(cr.business_entity_code, '') ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                          OR COALESCE(cr.device_code, '') ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                      )
                    ORDER BY cr.created_at DESC
                    """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM cash_register cr
                    WHERE (:active IS NULL OR cr.active = :active)
                      AND (CAST(:locationCode AS VARCHAR) IS NULL OR cr.location_code = CAST(:locationCode AS VARCHAR))
                      AND (CAST(:businessEntityCode AS VARCHAR) IS NULL OR cr.business_entity_code = CAST(:businessEntityCode AS VARCHAR))
                      AND (
                          CAST(:searchText AS VARCHAR) IS NULL
                          OR cr.register_code ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                          OR cr.name ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                          OR COALESCE(cr.location_code, '') ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                          OR COALESCE(cr.business_entity_code, '') ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                          OR COALESCE(cr.device_code, '') ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                      )
                    """)
    Page<CashRegister> search(@Param("active") Boolean active,
                              @Param("locationCode") String locationCode,
                              @Param("businessEntityCode") String businessEntityCode,
                              @Param("searchText") String searchText,
                              Pageable pageable);
}
