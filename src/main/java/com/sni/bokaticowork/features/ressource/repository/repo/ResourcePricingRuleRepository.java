package com.sni.bokaticowork.features.ressource.repository.repo;

import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.ressource.model.ResourcePricingRule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.List;

@Repository
public interface ResourcePricingRuleRepository extends JpaRepository<ResourcePricingRule, Long> {

    Page<ResourcePricingRule> findAllByResource(Resource resource, Pageable pageable);

    Optional<ResourcePricingRule> findByIdAndResource(Long id, Resource resource);

    @Query(nativeQuery = true, value = """
            SELECT *
            FROM resource_pricing_rule
            WHERE resource_id = :resourceId
              AND active = true
            ORDER BY created_at DESC
            LIMIT 1
            """)
    Optional<ResourcePricingRule> findLatestActiveByResourceId(@Param("resourceId") Long resourceId);

    @Query(nativeQuery = true, value = """
            SELECT *
            FROM resource_pricing_rule
            WHERE resource_id = :resourceId
              AND active = true
            ORDER BY created_at DESC
            """)
    List<ResourcePricingRule> findAllActiveByResourceId(@Param("resourceId") Long resourceId);

    @Query("""
            select rule
            from ResourcePricingRule rule
            where rule.resource = :resource
              and rule.resourceBookingUnit = :unit
              and rule.active = true
              and rule.deleted = false
              and (rule.validFrom is null or rule.validFrom <= :date)
              and (rule.validUntil is null or rule.validUntil >= :date)
              and (rule.dayOfWeek is null or rule.dayOfWeek = :dayOfWeek)
              and (rule.startsAt is null or rule.startsAt <= :time)
              and (rule.endsAt is null or rule.endsAt > :time)
            order by rule.priority desc, rule.id desc
            """)
    List<ResourcePricingRule> findApplicableRules(
            @Param("resource") Resource resource,
            @Param("unit") com.sni.bokaticowork.features.ressource.enums.ResourceBookingUnit unit,
            @Param("date") LocalDate date,
            @Param("dayOfWeek") Integer dayOfWeek,
            @Param("time") LocalTime time
    );
}
