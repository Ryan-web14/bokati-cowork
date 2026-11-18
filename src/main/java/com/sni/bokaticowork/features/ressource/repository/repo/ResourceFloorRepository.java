package com.sni.bokaticowork.features.ressource.repository.repo;

import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.ressource.model.ResourceFloor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Lot R5b : etages. */
@Repository
public interface ResourceFloorRepository extends JpaRepository<ResourceFloor, Long> {

    Optional<ResourceFloor> findByCodeIgnoreCase(String code);

    @Query("select count(r) from Resource r where r.floor = :floor")
    long countResources(@Param("floor") ResourceFloor floor);

    @Query("select r from Resource r where r.floor = :floor and r.deleted = false order by r.displayOrder asc nulls last, r.name asc")
    List<Resource> resources(@Param("floor") ResourceFloor floor);
}
