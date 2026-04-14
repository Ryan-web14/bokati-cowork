package com.sni.bokaticowork.features.ressource.repository.repo;

import com.sni.bokaticowork.features.ressource.model.ResourcePolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResourcePolicyRepository extends JpaRepository<ResourcePolicy, Long>, JpaSpecificationExecutor<ResourcePolicy> {

    @Query(nativeQuery = true, value = "SELECT EXISTS(SELECT 1 FROM resource_policy WHERE code = :code)")
    Boolean existsByCode(@Param("code") String code);

    @Query(nativeQuery = true, value = "SELECT EXISTS(SELECT 1 FROM resource_policy WHERE lower(name) = lower(:name))")
    Boolean existsByNameIgnoreCase(@Param("name") String name);

    @Query(nativeQuery = true, value = "SELECT * FROM resource_policy WHERE code = :code")
    Optional<ResourcePolicy> findByCode(@Param("code") String code);

    @Query(nativeQuery = true, value = "SELECT * FROM resource_policy WHERE lower(name) = lower(:name)")
    Optional<ResourcePolicy> findByNameIgnoreCase(@Param("name") String name);

    @Query(
            nativeQuery = true,
            value = """
                    SELECT rp.*
                    FROM search_resource_policy(:query) srp
                    JOIN resource_policy rp ON rp.id = srp.id
                    ORDER BY srp.score DESC
                    """
    )
    List<ResourcePolicy> basicSearch(@Param("query") String query);
}
