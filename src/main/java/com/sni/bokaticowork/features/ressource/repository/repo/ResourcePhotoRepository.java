package com.sni.bokaticowork.features.ressource.repository.repo;

import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.ressource.model.ResourcePhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResourcePhotoRepository extends JpaRepository<ResourcePhoto, Long> {
    List<ResourcePhoto> findAllByResourceAndActiveTrueOrderByDisplayOrderAscIdAsc(Resource resource);
}
