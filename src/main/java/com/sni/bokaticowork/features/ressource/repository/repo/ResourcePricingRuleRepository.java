package com.sni.bokaticowork.features.ressource.repository.repo;

import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.ressource.model.ResourcePricingRule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}
