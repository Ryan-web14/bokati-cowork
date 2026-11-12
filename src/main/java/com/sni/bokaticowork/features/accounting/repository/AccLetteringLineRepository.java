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

public interface AccLetteringLineRepository extends JpaRepository<com.sni.bokaticowork.features.accounting.model.AccLetteringLine, Long> {

    List<com.sni.bokaticowork.features.accounting.model.AccLetteringLine> findByLetteringId(Long letteringId);

    List<com.sni.bokaticowork.features.accounting.model.AccLetteringLine> findByEntryLineId(Long entryLineId);

    /** Montant deja rapproche de chaque ligne par des lettrages encore actifs. */
    @Query("SELECT ll.entryLineId, SUM(ll.matchedAmount) FROM AccLetteringLine ll, AccLettering l WHERE ll.letteringId = l.id AND l.status <> com.sni.bokaticowork.features.accounting.enums.LetteringStatus.UNLETTERED AND ll.entryLineId IN :lineIds GROUP BY ll.entryLineId")
    List<Object[]> matchedByLine(@Param("lineIds") Collection<Long> lineIds);
}
