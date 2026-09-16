package com.sni.bokaticowork.features.inventory.stock.repository;

import com.sni.bokaticowork.features.inventory.stock.model.StockValuationSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface StockValuationSnapshotRepository extends JpaRepository<StockValuationSnapshot, Long> {

    List<StockValuationSnapshot> findAllBySnapshotDateOrderByItemCodeAsc(LocalDate snapshotDate);

    List<StockValuationSnapshot> findAllByPeriodCodeOrderByItemCodeAsc(String periodCode);

    void deleteBySnapshotDate(LocalDate snapshotDate);

    @Query("SELECT COALESCE(SUM(snapshot.totalValue), 0) FROM StockValuationSnapshot snapshot WHERE snapshot.snapshotDate = :date")
    Long totalValueAt(@Param("date") LocalDate date);
}
