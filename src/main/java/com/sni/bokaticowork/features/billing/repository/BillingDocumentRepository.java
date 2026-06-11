package com.sni.bokaticowork.features.billing.repository;

import com.sni.bokaticowork.features.billing.model.BillingDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BillingDocumentRepository extends JpaRepository<BillingDocument, Long> {

    @Query(nativeQuery = true, value = "SELECT * FROM billing_document WHERE document_number = :documentNumber")
    Optional<BillingDocument> findByDocumentNumber(@Param("documentNumber") String documentNumber);

    @Query(nativeQuery = true, value = """
            SELECT *
            FROM billing_document
            WHERE source_type = CAST(:sourceType AS VARCHAR)
              AND source_code = CAST(:sourceCode AS VARCHAR)
              AND document_type = CAST(:documentType AS VARCHAR)
            ORDER BY created_at DESC
            LIMIT 1
            """)
    Optional<BillingDocument> findFirstBySourceAndType(@Param("sourceType") String sourceType,
                                                       @Param("sourceCode") String sourceCode,
                                                       @Param("documentType") String documentType);

    @Query(
            nativeQuery = true,
            value = """
                    SELECT *
                    FROM billing_document
                    WHERE (:documentType IS NULL OR document_type = CAST(:documentType AS VARCHAR))
                      AND (:status IS NULL OR status = CAST(:status AS VARCHAR))
                      AND (:customerType IS NULL OR UPPER(TRIM(customer_type)) = UPPER(TRIM(CAST(:customerType AS VARCHAR))))
                      AND (:customerCode IS NULL OR UPPER(TRIM(customer_code)) = UPPER(TRIM(CAST(:customerCode AS VARCHAR))))
                      AND (:sourceType IS NULL OR source_type = CAST(:sourceType AS VARCHAR))
                      AND (:sourceCode IS NULL OR source_code = CAST(:sourceCode AS VARCHAR))
                      AND (:fromDate IS NULL OR issue_date >= CAST(:fromDate AS DATE))
                      AND (:toDate IS NULL OR issue_date <= CAST(:toDate AS DATE))
                      AND (:searchText IS NULL OR (
                          document_number ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                          OR customer_name ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                          OR customer_code ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                          OR COALESCE(customer_email, '') ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                          OR COALESCE(title, '') ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                          OR COALESCE(description, '') ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                          OR COALESCE(source_code, '') ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                      ))
                    ORDER BY created_at DESC
                    """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM billing_document
                    WHERE (:documentType IS NULL OR document_type = CAST(:documentType AS VARCHAR))
                      AND (:status IS NULL OR status = CAST(:status AS VARCHAR))
                      AND (:customerType IS NULL OR UPPER(TRIM(customer_type)) = UPPER(TRIM(CAST(:customerType AS VARCHAR))))
                      AND (:customerCode IS NULL OR UPPER(TRIM(customer_code)) = UPPER(TRIM(CAST(:customerCode AS VARCHAR))))
                      AND (:sourceType IS NULL OR source_type = CAST(:sourceType AS VARCHAR))
                      AND (:sourceCode IS NULL OR source_code = CAST(:sourceCode AS VARCHAR))
                      AND (:fromDate IS NULL OR issue_date >= CAST(:fromDate AS DATE))
                      AND (:toDate IS NULL OR issue_date <= CAST(:toDate AS DATE))
                      AND (:searchText IS NULL OR (
                          document_number ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                          OR customer_name ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                          OR customer_code ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                          OR COALESCE(customer_email, '') ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                          OR COALESCE(title, '') ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                          OR COALESCE(description, '') ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                          OR COALESCE(source_code, '') ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                      ))
                    """
    )
    Page<BillingDocument> search(@Param("documentType") String documentType,
                                 @Param("status") String status,
                                 @Param("customerType") String customerType,
                                 @Param("customerCode") String customerCode,
                                 @Param("sourceType") String sourceType,
                                 @Param("sourceCode") String sourceCode,
                                 @Param("fromDate") LocalDate fromDate,
                                 @Param("toDate") LocalDate toDate,
                                 @Param("searchText") String searchText,
                                 Pageable pageable);

    @Query(nativeQuery = true,
            value = """
                    SELECT *
                    FROM billing_document
                    WHERE UPPER(TRIM(customer_type)) = UPPER(TRIM(CAST(:customerType AS VARCHAR)))
                      AND UPPER(TRIM(customer_code)) = UPPER(TRIM(CAST(:customerCode AS VARCHAR)))
                    ORDER BY created_at DESC
                    """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM billing_document
                    WHERE UPPER(TRIM(customer_type)) = UPPER(TRIM(CAST(:customerType AS VARCHAR)))
                      AND UPPER(TRIM(customer_code)) = UPPER(TRIM(CAST(:customerCode AS VARCHAR)))
                    """)
    Page<BillingDocument> statementDocuments(@Param("customerType") String customerType,
                                             @Param("customerCode") String customerCode,
                                             Pageable pageable);

    @Query(nativeQuery = true, value = """
            SELECT COALESCE(SUM(total_amount), 0), COALESCE(SUM(paid_amount), 0), COALESCE(SUM(balance_due), 0)
            FROM billing_document
            WHERE UPPER(TRIM(customer_type)) = UPPER(TRIM(CAST(:customerType AS VARCHAR)))
              AND UPPER(TRIM(customer_code)) = UPPER(TRIM(CAST(:customerCode AS VARCHAR)))
            """)
    List<Object[]> statementTotals(@Param("customerType") String customerType,
                                   @Param("customerCode") String customerCode);

    @Query(nativeQuery = true, value = """
            SELECT *
            FROM billing_document
            WHERE UPPER(TRIM(customer_type)) = UPPER(TRIM(CAST(:customerType AS VARCHAR)))
              AND UPPER(TRIM(customer_code)) = UPPER(TRIM(CAST(:customerCode AS VARCHAR)))
              AND document_type IN ('INVOICE', 'PROFORMA_INVOICE')
              AND status IN ('ISSUED', 'SENT', 'PARTIALLY_PAID', 'OVERDUE')
              AND balance_due > 0
            ORDER BY created_at ASC
            """)
    List<BillingDocument> findRecoverableDocuments(@Param("customerType") String customerType,
                                                   @Param("customerCode") String customerCode);

    @Query(
            nativeQuery = true,
            value = """
                    SELECT DISTINCT bd.*
                    FROM billing_document bd
                    LEFT JOIN billing_document_line bdl ON bdl.document_id = bd.id
                    WHERE bd.document_type IN ('INVOICE', 'PROFORMA_INVOICE')
                      AND bd.status IN ('ISSUED', 'SENT', 'PARTIALLY_PAID', 'OVERDUE')
                      AND bd.balance_due > 0
                      AND (:documentType IS NULL OR bd.document_type = CAST(:documentType AS VARCHAR))
                      AND (:customerType IS NULL OR UPPER(TRIM(bd.customer_type)) = UPPER(TRIM(CAST(:customerType AS VARCHAR))))
                      AND (:customerCode IS NULL OR UPPER(TRIM(bd.customer_code)) = UPPER(TRIM(CAST(:customerCode AS VARCHAR))))
                      AND (:lineSourceType IS NULL OR UPPER(COALESCE(bdl.source_type, '')) = UPPER(CAST(:lineSourceType AS VARCHAR)))
                      AND (:lineSourceCode IS NULL OR UPPER(COALESCE(bdl.source_code, '')) = UPPER(CAST(:lineSourceCode AS VARCHAR)))
                      AND (:searchText IS NULL OR (
                          bd.document_number ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                          OR bd.customer_name ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                          OR bd.customer_code ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                          OR COALESCE(bd.customer_email, '') ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                          OR COALESCE(bd.title, '') ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                          OR COALESCE(bd.description, '') ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                          OR COALESCE(bd.source_code, '') ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                          OR COALESCE(bdl.description, '') ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                          OR COALESCE(bdl.source_code, '') ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                      ))
                    ORDER BY bd.created_at DESC
                    """,
            countQuery = """
                    SELECT COUNT(DISTINCT bd.id)
                    FROM billing_document bd
                    LEFT JOIN billing_document_line bdl ON bdl.document_id = bd.id
                    WHERE bd.document_type IN ('INVOICE', 'PROFORMA_INVOICE')
                      AND bd.status IN ('ISSUED', 'SENT', 'PARTIALLY_PAID', 'OVERDUE')
                      AND bd.balance_due > 0
                      AND (:documentType IS NULL OR bd.document_type = CAST(:documentType AS VARCHAR))
                      AND (:customerType IS NULL OR UPPER(TRIM(bd.customer_type)) = UPPER(TRIM(CAST(:customerType AS VARCHAR))))
                      AND (:customerCode IS NULL OR UPPER(TRIM(bd.customer_code)) = UPPER(TRIM(CAST(:customerCode AS VARCHAR))))
                      AND (:lineSourceType IS NULL OR UPPER(COALESCE(bdl.source_type, '')) = UPPER(CAST(:lineSourceType AS VARCHAR)))
                      AND (:lineSourceCode IS NULL OR UPPER(COALESCE(bdl.source_code, '')) = UPPER(CAST(:lineSourceCode AS VARCHAR)))
                      AND (:searchText IS NULL OR (
                          bd.document_number ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                          OR bd.customer_name ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                          OR bd.customer_code ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                          OR COALESCE(bd.customer_email, '') ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                          OR COALESCE(bd.title, '') ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                          OR COALESCE(bd.description, '') ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                          OR COALESCE(bd.source_code, '') ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                          OR COALESCE(bdl.description, '') ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                          OR COALESCE(bdl.source_code, '') ILIKE CONCAT('%', CAST(:searchText AS VARCHAR), '%')
                      ))
                    """
    )
    Page<BillingDocument> searchPayable(@Param("documentType") String documentType,
                                        @Param("customerType") String customerType,
                                        @Param("customerCode") String customerCode,
                                        @Param("lineSourceType") String lineSourceType,
                                        @Param("lineSourceCode") String lineSourceCode,
                                        @Param("searchText") String searchText,
                                        Pageable pageable);

    @Query(nativeQuery = true, value = """
            SELECT *
            FROM billing_document
            WHERE document_type IN ('INVOICE', 'PROFORMA_INVOICE')
              AND status IN ('ISSUED', 'SENT', 'VIEWED', 'PARTIALLY_PAID', 'OVERDUE')
              AND balance_due > 0
              AND (:customerType IS NULL OR UPPER(TRIM(customer_type)) = UPPER(TRIM(CAST(:customerType AS VARCHAR))))
              AND (:customerCode IS NULL OR UPPER(TRIM(customer_code)) = UPPER(TRIM(CAST(:customerCode AS VARCHAR))))
            ORDER BY due_date ASC NULLS LAST
            """)
    List<BillingDocument> findForAgingReport(@Param("customerType") String customerType,
                                             @Param("customerCode") String customerCode);

    @Query(nativeQuery = true, value = """
            UPDATE billing_document
            SET status = 'OVERDUE', updated_at = NOW()
            WHERE document_type IN ('INVOICE', 'PROFORMA_INVOICE')
              AND status IN ('ISSUED', 'SENT', 'PARTIALLY_PAID')
              AND due_date IS NOT NULL
              AND due_date < CURRENT_DATE
              AND balance_due > 0
            """)
    @org.springframework.data.jpa.repository.Modifying
    int markOverdueDocuments();

    @Query(nativeQuery = true, value = """
            SELECT *
            FROM billing_document
            WHERE document_type IN ('INVOICE', 'PROFORMA_INVOICE')
              AND status = 'OVERDUE'
              AND balance_due > 0
              AND due_date IS NOT NULL
            ORDER BY due_date ASC
            LIMIT :limit
            """)
    List<BillingDocument> findOverdueReminderCandidates(@Param("limit") int limit);
}
