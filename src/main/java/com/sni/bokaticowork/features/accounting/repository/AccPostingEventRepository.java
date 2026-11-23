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

    /** Lot K0 (M3) : faits dont la lecture a echoue (donnees vides), les plus anciennement tentes d'abord. */
    List<AccPostingEvent> findByStatusAndPayloadOrderByLastAttemptAtAsc(PostingEventStatus status, String payload, Pageable pageable);

    /**
     * Lot K0 (revue comptable, M2) : file des evenements a traiter, sans les familles coupees, les moins
     * tentes d'abord. Un evenement qui attend (contre-passation dont l'origine n'est pas comptabilisee)
     * prend une tentative a chaque passage et laisse sa place : il ne bloque plus la tete de file.
     */
    @Query("select e from AccPostingEvent e where e.status = :status "
            + "and (e.family is null or upper(e.family) not in :disabled) "
            + "order by e.attempts asc, e.eventDate asc, e.id asc")
    List<AccPostingEvent> queue(@Param("status") PostingEventStatus status, @Param("disabled") Collection<String> disabled, Pageable pageable);

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
