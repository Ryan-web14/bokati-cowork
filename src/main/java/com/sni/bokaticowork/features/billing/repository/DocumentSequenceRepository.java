package com.sni.bokaticowork.features.billing.repository;

import com.sni.bokaticowork.features.billing.model.DocumentSequence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DocumentSequenceRepository extends JpaRepository<DocumentSequence, Long> {

    @Query(nativeQuery = true, value = """
            SELECT * FROM document_sequence
            WHERE document_type = :type
              AND year = :year
            FOR UPDATE
            """)
    Optional<DocumentSequence> findLockedByTypeAndYear(
            @Param("type") String type,
            @Param("year") int year);
}
