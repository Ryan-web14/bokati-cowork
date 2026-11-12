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

public interface AccEntryLineRepository extends JpaRepository<com.sni.bokaticowork.features.accounting.model.AccEntryLine, Long> {

    List<com.sni.bokaticowork.features.accounting.model.AccEntryLine> findByEntryIdOrderByLineNoAsc(Long entryId);

    List<com.sni.bokaticowork.features.accounting.model.AccEntryLine> findByEntryIdInOrderByEntryIdAscLineNoAsc(Collection<Long> entryIds);

    void deleteByEntryId(Long entryId);

    /** Lignes comptabilisees (pieces validees ou contre-passees) d'un compte, dans l'ordre chronologique. */
    @Query("SELECT l FROM AccEntryLine l, AccEntry e WHERE l.entryId = e.id AND e.status IN :statuses AND l.accountNumber LIKE CONCAT(:account, '%') AND (:thirdParty IS NULL OR l.thirdPartyCode = :thirdParty) AND e.entryDate >= :from AND e.entryDate <= :to ORDER BY e.entryDate ASC, e.id ASC, l.lineNo ASC")
    List<com.sni.bokaticowork.features.accounting.model.AccEntryLine> findPosted(@Param("account") String account, @Param("thirdParty") String thirdParty,
                                              @Param("from") LocalDate from, @Param("to") LocalDate to,
                                              @Param("statuses") Collection<com.sni.bokaticowork.features.accounting.enums.EntryStatus> statuses);

    /** Totaux debit et credit par compte des pieces comptabilisees d'une periode. */
    @Query("SELECT l.accountNumber, SUM(l.debit), SUM(l.credit) FROM AccEntryLine l, AccEntry e WHERE l.entryId = e.id AND e.status IN :statuses AND e.entryDate >= :from AND e.entryDate <= :to GROUP BY l.accountNumber")
    List<Object[]> totalsByAccount(@Param("from") LocalDate from, @Param("to") LocalDate to,
                                   @Param("statuses") Collection<com.sni.bokaticowork.features.accounting.enums.EntryStatus> statuses);

    @Query("SELECT l.accountNumber, SUM(l.debit), SUM(l.credit) FROM AccEntryLine l, AccEntry e WHERE l.entryId = e.id AND e.status IN :statuses AND e.fiscalYearCode = :year AND e.journalCode = :journal GROUP BY l.accountNumber")
    List<Object[]> totalsByAccountForJournal(@Param("year") String year, @Param("journal") String journal,
                                             @Param("statuses") Collection<com.sni.bokaticowork.features.accounting.enums.EntryStatus> statuses);

    @Query("SELECT COUNT(l) FROM AccEntryLine l, AccEntry e WHERE l.entryId = e.id AND e.status IN :statuses AND l.accountNumber = :account")
    long countPostedOnAccount(@Param("account") String account, @Param("statuses") Collection<com.sni.bokaticowork.features.accounting.enums.EntryStatus> statuses);
}
