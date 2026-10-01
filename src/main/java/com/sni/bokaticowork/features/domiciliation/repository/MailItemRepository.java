package com.sni.bokaticowork.features.domiciliation.repository;

import com.sni.bokaticowork.features.domiciliation.model.MailItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface MailItemRepository extends JpaRepository<MailItem, Long> {

    Optional<MailItem> findByItemNumber(String itemNumber);

    Page<MailItem> findByContract_IdOrderByReceivedAtDesc(Long contractId, Pageable pageable);

    Page<MailItem> findByStatusInOrderByReceivedAtAsc(Collection<MailItem.Status> statuses, Pageable pageable);

    /** Plis toujours en garde dont le delai est passe · a relancer, puis retourner ou detruire. */
    List<MailItem> findByStatusInAndStorageDeadlineBefore(Collection<MailItem.Status> statuses, LocalDate before);

    long countByContract_IdAndStatusIn(Long contractId, Collection<MailItem.Status> statuses);
}
