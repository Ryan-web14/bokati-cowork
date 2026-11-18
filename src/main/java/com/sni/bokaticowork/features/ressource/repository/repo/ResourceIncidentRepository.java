package com.sni.bokaticowork.features.ressource.repository.repo;

import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.ressource.model.ResourceIncident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Lot R5d : incidents des ressources. */
@Repository
public interface ResourceIncidentRepository extends JpaRepository<ResourceIncident, Long> {

    Optional<ResourceIncident> findByCodeIgnoreCase(String code);

    List<ResourceIncident> findAllByResourceOrderByCreatedAtDesc(Resource resource);

    List<ResourceIncident> findAllByStatusInOrderByCreatedAtDesc(List<String> statuses);

    long countByResourceAndBlockedTrueAndStatusNot(Resource resource, String status);
}
