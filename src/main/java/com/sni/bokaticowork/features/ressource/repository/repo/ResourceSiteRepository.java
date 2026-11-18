package com.sni.bokaticowork.features.ressource.repository.repo;

import com.sni.bokaticowork.features.ressource.model.ResourceFloor;
import com.sni.bokaticowork.features.ressource.model.ResourceSite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Lot R5b : sites. */
@Repository
public interface ResourceSiteRepository extends JpaRepository<ResourceSite, Long> {

    Optional<ResourceSite> findByCodeIgnoreCase(String code);

    boolean existsByNameIgnoreCase(String name);

    List<ResourceSite> findAllByOrderByNameAsc();

    @Query("select count(r) from Resource r where r.site = :site")
    long countResources(@Param("site") ResourceSite site);

    @Query("select count(f) from ResourceFloor f where f.site = :site")
    long countFloors(@Param("site") ResourceSite site);

    @Query("select f from ResourceFloor f where f.site = :site order by f.level asc, f.name asc")
    List<ResourceFloor> floors(@Param("site") ResourceSite site);
}
