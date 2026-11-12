package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.enums.PostingEventStatus;
import com.sni.bokaticowork.features.accounting.model.AccPostingEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AccPostingEventRepository extends JpaRepository<AccPostingEvent, Long> {

    Optional<AccPostingEvent> findByEventTypeAndReference(String eventType, String reference);

    List<AccPostingEvent> findByStatusOrderByEventDateAscIdAsc(PostingEventStatus status, Pageable pageable);

    List<AccPostingEvent> findByStatusAndEventType(PostingEventStatus status, String eventType);

    @Query("select e from AccPostingEvent e where (:status is null or e.status = :status) "
            + "and (:eventType is null or e.eventType = :eventType) "
            + "and (:from is null or e.eventDate >= :from) and (:to is null or e.eventDate <= :to) "
            + "order by e.eventDate desc, e.id desc")
    List<AccPostingEvent> search(@Param("status") PostingEventStatus status, @Param("eventType") String eventType,
                                 @Param("from") LocalDate from, @Param("to") LocalDate to, Pageable pageable);

    @Query("select e.status, count(e) from AccPostingEvent e group by e.status")
    List<Object[]> countByStatus();

    long countByStatusIn(Collection<PostingEventStatus> statuses);

    List<AccPostingEvent> findByReferenceOrderByIdAsc(String reference);
}
