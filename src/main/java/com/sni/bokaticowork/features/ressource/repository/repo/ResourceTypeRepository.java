package com.sni.bokaticowork.features.ressource.repository.repo;

import com.sni.bokaticowork.features.ressource.model.ResourceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResourceTypeRepository extends JpaRepository<ResourceType, Long>, JpaSpecificationExecutor<ResourceType> {

    @Query(nativeQuery = true, value = "SELECT EXISTS(SELECT 1 FROM resource_type WHERE code = :code)")
    Boolean existsByCode(@Param("code") String code);

    @Query(nativeQuery = true, value = "SELECT EXISTS(SELECT 1 FROM resource_type WHERE lower(name) = lower(:name))")
    Boolean existsByNameIgnoreCase(@Param("name") String name);

    @Query(nativeQuery = true, value = "SELECT * FROM resource_type WHERE code = :code")
    Optional<ResourceType> findByCode(@Param("code")String code);

    @Query(nativeQuery = true, value = "SELECT * FROM resource_type WHERE lower(name) = lower(:name)")
    Optional<ResourceType> findByNameIgnoreCase(@Param("name") String name);

    @Query(
            nativeQuery = true,
            value = """
                    SELECT rt.*
                    FROM search_resource_type(:query) srt
                    JOIN resource_type rt ON rt.id = srt.id
                    ORDER BY srt.score DESC
                    """
    )
    List<ResourceType> basicSearch(@Param("query") String query);

    @Query(nativeQuery = true, value = "SELECT * FROM resource_type WHERE active = true ORDER BY name ASC")
    List<ResourceType> findAllActive();

}
