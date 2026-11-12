package com.sni.bokaticowork.features.ressource.repository.repo;

import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.ressource.model.ResourceOpeningHours;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResourceOpeningHoursRepository extends JpaRepository<ResourceOpeningHours, Long> {

    List<ResourceOpeningHours> findAllByResourceOrderByDayOfWeekAscOpenTimeAsc(Resource resource);

    void deleteAllByResource(Resource resource);
}
