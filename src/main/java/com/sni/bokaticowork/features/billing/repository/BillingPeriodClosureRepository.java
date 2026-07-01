package com.sni.bokaticowork.features.billing.repository;

import com.sni.bokaticowork.features.billing.model.BillingPeriodClosure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BillingPeriodClosureRepository extends JpaRepository<BillingPeriodClosure, Long> {

    boolean existsByPeriodTypeAndPeriodLabelAndDocumentType(
            String periodType, String periodLabel, String documentType);

    @Query("""
            SELECT c FROM BillingPeriodClosure c
            WHERE c.documentType = :documentType
            ORDER BY c.computedAt DESC
            LIMIT 1
            """)
    Optional<BillingPeriodClosure> findLatestByDocumentType(@Param("documentType") String documentType);

    @Query(nativeQuery = true, value = """
            SELECT
                COUNT(*)                            AS total_documents,
                COALESCE(SUM(total_amount), 0)      AS total_invoiced,
                COALESCE(SUM(paid_amount), 0)       AS total_paid
            FROM billing_document
            WHERE document_type = CAST(:documentType AS VARCHAR)
              AND locked = TRUE
              AND fiscal_date >= CAST(:periodStart AS DATE)
              AND fiscal_date <= CAST(:periodEnd AS DATE)
            """)
    Object[] aggregateForClosure(
            @Param("documentType") String documentType,
            @Param("periodStart") java.time.LocalDate periodStart,
            @Param("periodEnd") java.time.LocalDate periodEnd);
}
