package com.sni.bokaticowork.features.accounting.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AccEntryRepository extends JpaRepository<com.sni.bokaticowork.features.accounting.model.AccEntry, Long> {

    Optional<com.sni.bokaticowork.features.accounting.model.AccEntry> findByEntryNumber(String entryNumber);

    Optional<com.sni.bokaticowork.features.accounting.model.AccEntry> findByProvisionalNumber(String provisionalNumber);

    List<com.sni.bokaticowork.features.accounting.model.AccEntry> findByStatusOrderByCreatedAtAsc(com.sni.bokaticowork.features.accounting.enums.EntryStatus status);

    @Query("SELECT e FROM AccEntry e WHERE e.journalCode = :journal AND e.fiscalYearCode = :year AND e.status IN :statuses ORDER BY e.sequenceNumber ASC")
    List<com.sni.bokaticowork.features.accounting.model.AccEntry> findNumbered(@Param("journal") String journal, @Param("year") String year,
                                            @Param("statuses") Collection<com.sni.bokaticowork.features.accounting.enums.EntryStatus> statuses);

    @Query("SELECT MAX(e.entryDate) FROM AccEntry e WHERE e.journalCode = :journal AND e.fiscalYearCode = :year AND e.status IN :statuses")
    Optional<LocalDate> lastValidatedDate(@Param("journal") String journal, @Param("year") String year,
                                          @Param("statuses") Collection<com.sni.bokaticowork.features.accounting.enums.EntryStatus> statuses);

    @Query("SELECT e FROM AccEntry e WHERE (:journal IS NULL OR e.journalCode = :journal) AND (:status IS NULL OR e.status = :status) AND e.entryDate >= :from AND e.entryDate <= :to ORDER BY e.entryDate ASC, e.id ASC")
    List<com.sni.bokaticowork.features.accounting.model.AccEntry> search(@Param("journal") String journal, @Param("status") com.sni.bokaticowork.features.accounting.enums.EntryStatus status,
                                      @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT COUNT(e) FROM AccEntry e WHERE e.periodCode = :period AND e.status IN :statuses")
    long countByPeriodAndStatuses(@Param("period") String period, @Param("statuses") Collection<com.sni.bokaticowork.features.accounting.enums.EntryStatus> statuses);

    @Query("SELECT e FROM AccEntry e WHERE e.acceptedUnbalanced = true AND e.status IN :statuses")
    List<com.sni.bokaticowork.features.accounting.model.AccEntry> findAcceptedUnbalanced(@Param("statuses") Collection<com.sni.bokaticowork.features.accounting.enums.EntryStatus> statuses);

    List<com.sni.bokaticowork.features.accounting.model.AccEntry> findBySourceTypeAndSourceCode(String sourceType, String sourceCode);
}
