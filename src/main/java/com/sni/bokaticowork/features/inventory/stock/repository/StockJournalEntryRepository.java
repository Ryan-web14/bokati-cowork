package com.sni.bokaticowork.features.inventory.stock.repository;

import com.sni.bokaticowork.features.inventory.stock.model.StockJournalEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface StockJournalEntryRepository extends JpaRepository<StockJournalEntry, Long> {

    boolean existsByMovementCode(String movementCode);

    @Query("""
            SELECT entry FROM StockJournalEntry entry
            WHERE (:fromDate IS NULL OR entry.accountingDate >= :fromDate)
              AND (:toDate IS NULL OR entry.accountingDate <= :toDate)
              AND (:periodCode IS NULL OR entry.periodCode = :periodCode)
            ORDER BY entry.accountingDate ASC, entry.id ASC
            """)
    List<StockJournalEntry> findForExport(@Param("fromDate") LocalDate fromDate,
                                          @Param("toDate") LocalDate toDate,
                                          @Param("periodCode") String periodCode);

    Page<StockJournalEntry> findAllByOrderByAccountingDateDescIdDesc(Pageable pageable);
}
