package com.sni.bokaticowork.features.ressource.repository.repo;

import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.ressource.model.ResourceAudience;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/** Lot B10 : audiences des ressources. */
@Repository
public interface ResourceAudienceRepository extends JpaRepository<ResourceAudience, Long> {

    List<ResourceAudience> findAllByResourceOrderByIdAsc(Resource resource);

    @Modifying
    @Query("delete from ResourceAudience a where a.resource = :resource")
    void deleteAllForResource(@Param("resource") Resource resource);

    /** Ressources dont l'audience contient l'une des cles « TYPE:CODE ». */
    @Query("select distinct a.resource.id from ResourceAudience a where concat(a.audienceType, ':', a.audienceCode) in :keys")
    List<Long> resourceIdsFor(@Param("keys") Collection<String> keys);
}
