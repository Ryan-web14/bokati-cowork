package com.sni.bokaticowork.features.ressource.repository.repo;

import com.sni.bokaticowork.features.ressource.model.ResourceGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResourceGroupRepository extends JpaRepository<ResourceGroup, Long>, JpaSpecificationExecutor<ResourceGroup> {

    @Query(nativeQuery = true, value = "SELECT EXISTS(SELECT 1 FROM resource_group WHERE code = :code)")
    Boolean existsByCode(@Param("code") String code);

    @Query(nativeQuery = true, value = "SELECT EXISTS(SELECT 1 FROM resource_group WHERE lower(name) = lower(:name))")
    Boolean existsByNameIgnoreCase(@Param("name") String name);

    @Query(nativeQuery = true, value = "SELECT * FROM resource_group WHERE code = :code")
    Optional<ResourceGroup> findByCode(@Param("code") String code);

    @Query(nativeQuery = true, value = "SELECT * FROM resource_group WHERE lower(name) = lower(:name)")
    Optional<ResourceGroup> findByNameIgnoreCase(@Param("name") String name);

    @Query(
            nativeQuery = true,
            value = """
                    SELECT rg.*
                    FROM search_resource_group(:query) srg
                    JOIN resource_group rg ON rg.id = srg.id
                    ORDER BY srg.score DESC
                    """
    )
    List<ResourceGroup> basicSearch(@Param("query") String query);
}
