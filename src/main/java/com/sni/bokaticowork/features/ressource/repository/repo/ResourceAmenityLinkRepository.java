package com.sni.bokaticowork.features.ressource.repository.repo;

import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.ressource.model.ResourceAmenityLink;
import com.sni.bokaticowork.features.ressource.model.ResourceAmenities;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResourceAmenityLinkRepository extends JpaRepository<ResourceAmenityLink, Long> {

    boolean existsByResourceAndAmenity(Resource resource, ResourceAmenities amenity);

    void deleteByResourceAndAmenity(Resource resource, ResourceAmenities amenity);

    List<ResourceAmenityLink> findAllByResource(Resource resource);
}
