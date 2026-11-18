package com.sni.bokaticowork.features.ressource.repository.repo;

import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.ressource.model.ResourceCalendarDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Lot R5a : jours particuliers du calendrier des ressources. */
@Repository
public interface ResourceCalendarDayRepository extends JpaRepository<ResourceCalendarDay, Long> {

    Optional<ResourceCalendarDay> findFirstByDayDateAndResource(LocalDate dayDate, Resource resource);

    Optional<ResourceCalendarDay> findFirstByDayDateAndResourceIsNull(LocalDate dayDate);

    @Query("""
            select d from ResourceCalendarDay d left join fetch d.resource r
            where d.dayDate between :from and :to
              and (:resourceId is null or r is null or r.id = :resourceId)
            order by d.dayDate
            """)
    List<ResourceCalendarDay> findBetween(@Param("from") LocalDate from, @Param("to") LocalDate to,
                                          @Param("resourceId") Long resourceId);
}
