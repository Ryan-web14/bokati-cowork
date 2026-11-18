package com.sni.bokaticowork.features.ressource.repository.repo;

import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.ressource.model.ResourceGroup;
import com.sni.bokaticowork.features.ressource.model.ResourcePolicy;
import com.sni.bokaticowork.features.ressource.model.ResourceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResourceRepository extends JpaRepository<Resource, Long>, JpaSpecificationExecutor<Resource> {

    /** Lot R4 : type, groupe et politique charges avec la page (fin du N+1 des listes). */
    @Override
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"resourceType", "resourceGroup", "resourcePolicy"})
    Page<Resource> findAll(Pageable pageable);

    @Override
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"resourceType", "resourceGroup", "resourcePolicy"})
    Page<Resource> findAll(org.springframework.data.jpa.domain.Specification<Resource> spec, Pageable pageable);


    @Query(nativeQuery = true, value = "SELECT EXISTS(SELECT 1 FROM resource WHERE code = :code)")
    Boolean existsByCode(@Param("code") String code);

    @Query(nativeQuery = true, value = "SELECT * FROM resource WHERE code = :code AND deleted = false")
    Optional<Resource> findByCode(@Param("code") String code);

    @Query(
            nativeQuery = true,
            value = """
                    SELECT r.*
                    FROM search_resource(:query) sr
                    JOIN resource r ON r.id = sr.id
                    WHERE r.deleted = false
                    ORDER BY sr.score DESC
                    """
    )
    List<Resource> basicSearch(@Param("query") String query);

    List<Resource> findAllByResourceType(ResourceType resourceType);
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"resourceType", "resourceGroup", "resourcePolicy"})
    Page<Resource> findAllByResourceType(ResourceType resourceType, Pageable pageable);

    List<Resource> findAllByResourceGroup(ResourceGroup resourceGroup);

    /** Lot R2 : ressources rattachees, supprimees en douce comprises (elles retiennent la cle etrangere). */
    @org.springframework.data.jpa.repository.Query(nativeQuery = true, value = "SELECT count(*) FROM resource WHERE group_id = :id")
    long countAttachedToGroup(@org.springframework.data.repository.query.Param("id") Long id);

    @org.springframework.data.jpa.repository.Query(nativeQuery = true, value = "SELECT count(*) FROM resource WHERE type_id = :id")
    long countAttachedToType(@org.springframework.data.repository.query.Param("id") Long id);

    @org.springframework.data.jpa.repository.Query(nativeQuery = true, value = "SELECT count(*) FROM resource WHERE policy_id = :id")
    long countAttachedToPolicy(@org.springframework.data.repository.query.Param("id") Long id);
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"resourceType", "resourceGroup", "resourcePolicy"})
    Page<Resource> findAllByResourceGroup(ResourceGroup resourceGroup, Pageable pageable);

    List<Resource> findAllByResourcePolicy(ResourcePolicy resourcePolicy);
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"resourceType", "resourceGroup", "resourcePolicy"})
    Page<Resource> findAllByResourcePolicy(ResourcePolicy resourcePolicy, Pageable pageable);
}
