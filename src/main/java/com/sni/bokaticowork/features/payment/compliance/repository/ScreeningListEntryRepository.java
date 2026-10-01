package com.sni.bokaticowork.features.payment.compliance.repository;

import com.sni.bokaticowork.features.payment.compliance.model.ScreeningListEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScreeningListEntryRepository extends JpaRepository<ScreeningListEntry, Long> {

    List<ScreeningListEntry> findByActiveTrue();

    /** Les listes en vigueur et leur version · ce qui entre dans la preuve d'un controle. */
    @Query(nativeQuery = true, value = """
            SELECT list_code, list_version, COUNT(*)
            FROM screening_list_entry
            WHERE active = TRUE
            GROUP BY list_code, list_version
            ORDER BY list_code
            """)
    List<Object[]> activeLists();

    @Query("SELECT e FROM ScreeningListEntry e WHERE e.active = true AND e.listCode = :listCode")
    List<ScreeningListEntry> findActiveByList(@Param("listCode") String listCode);
}
