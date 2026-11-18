package com.sni.bokaticowork.features.ressource.repository.repo;

import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.ressource.model.ResourceCost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/** Lot R5e : couts des ressources. */
@Repository
public interface ResourceCostRepository extends JpaRepository<ResourceCost, Long> {

    List<ResourceCost> findAllByResourceOrderByKindAscIdAsc(Resource resource);
}
