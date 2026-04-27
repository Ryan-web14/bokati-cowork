package com.sni.bokaticowork.features.billing.repository;

import com.sni.bokaticowork.features.billing.model.TaxRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TaxRuleRepository extends JpaRepository<TaxRule, Long> {

    @Query(nativeQuery = true, value = """
            SELECT *
            FROM billing_tax_rule
            WHERE code = :code
              AND active = TRUE
            """)
    Optional<TaxRule> findActiveByCode(@Param("code") String code);

    @Query(nativeQuery = true, value = """
            SELECT *
            FROM billing_tax_rule
            WHERE active = TRUE
              AND (:countryCode IS NULL OR country_code = CAST(:countryCode AS VARCHAR))
              AND (valid_from IS NULL OR valid_from <= CAST(:date AS DATE))
              AND (valid_until IS NULL OR valid_until >= CAST(:date AS DATE))
            ORDER BY id ASC
            """)
    List<TaxRule> findActiveRules(@Param("countryCode") String countryCode, @Param("date") LocalDate date);
}
