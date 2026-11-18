package com.sni.bokaticowork.features.ressource.repository.repo;

import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.ressource.model.ResourceTaskTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/** Lot R5d : modeles de taches. */
@Repository
public interface ResourceTaskTemplateRepository extends JpaRepository<ResourceTaskTemplate, Long> {

    /** Modeles de la reservation : ceux de la ressource et ceux de toutes les ressources (hors services). */
    @Query("""
            select t from ResourceTaskTemplate t
            where t.active = true and t.serviceItemCode is null
              and (t.resource is null or t.resource = :resource)
            order by t.id
            """)
    List<ResourceTaskTemplate> forBooking(@Param("resource") Resource resource);

    /** Modeles d'un service vendu (toutes ressources, ou cette ressource). */
    @Query("""
            select t from ResourceTaskTemplate t
            where t.active = true and upper(t.serviceItemCode) = upper(:itemCode)
              and (t.resource is null or t.resource = :resource)
            order by t.id
            """)
    List<ResourceTaskTemplate> forService(@Param("resource") Resource resource, @Param("itemCode") String itemCode);

    List<ResourceTaskTemplate> findAllByOrderByIdAsc();
}
