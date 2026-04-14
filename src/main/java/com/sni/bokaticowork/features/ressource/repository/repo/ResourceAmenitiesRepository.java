package com.sni.bokaticowork.features.ressource.repository.repo;

import com.sni.bokaticowork.features.ressource.model.ResourceAmenities;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResourceAmenitiesRepository extends JpaRepository<ResourceAmenities, Long> {

    @Query(nativeQuery = true, value = "SELECT EXISTS(SELECT 1 FROM resource_amenity WHERE code = :code)")
    Boolean existsByCode(@Param("code") String code);

    @Query(nativeQuery = true, value = "SELECT EXISTS(SELECT 1 FROM resource_amenity WHERE lower(name) = lower(:name))")
    Boolean existsByNameIgnoreCase(@Param("name") String name);

    @Query(nativeQuery = true, value = "SELECT * FROM resource_amenity WHERE code = :code")
    Optional<ResourceAmenities> findByCode(@Param("code") String code);

    @Query(nativeQuery = true, value = "SELECT * FROM resource_amenity WHERE lower(name) = lower(:name)")
    Optional<ResourceAmenities> findByNameIgnoreCase(@Param("name") String name);

    @Query(
            nativeQuery = true,
            value = """
                    SELECT *
                    FROM resource_amenity ra
                    WHERE lower(ra.code) LIKE lower(concat('%', :query, '%'))
                       OR lower(ra.name) LIKE lower(concat('%', :query, '%'))
                       OR lower(coalesce(ra.description, '')) LIKE lower(concat('%', :query, '%'))
                    ORDER BY ra.name ASC
                    """
    )
    List<ResourceAmenities> basicSearch(@Param("query") String query);

    @Query(nativeQuery = true, value = "SELECT EXISTS(SELECT 1 FROM resource_amenity_link WHERE amenity_id = :amenityId)")
    Boolean existsLinkedResources(@Param("amenityId") Long amenityId);

    Page<ResourceAmenities> findAll(Pageable pageable);
}
