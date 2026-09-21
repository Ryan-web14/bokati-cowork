package com.sni.bokaticowork.features.billing.repository;

import com.sni.bokaticowork.features.billing.model.BillingDocument;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BillingDocumentRepository extends JpaRepository<BillingDocument, Long> {

    @Query(nativeQuery = true, value = "SELECT * FROM billing_document WHERE document_number = :documentNumber")
    Optional<BillingDocument> findByDocumentNumber(@Param("documentNumber") String documentNumber);

    /**
     * Charge le document sous verrou exclusif, pour serialiser les mutations de
     * {@code paid_amount} / {@code balance_due} / {@code status}.
     * <p>
     * Un reglement portefeuille et un callback mobile money visant la meme facture au meme
     * instant lisaient tous deux l'ancien montant paye et le second ecrasait le premier.
     * <p>
     * Requete JPQL et non native : Spring Data ignore {@code @Lock} sur les requetes natives,
     * ce qui rendrait le verrou silencieusement inoperant.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM BillingDocument d WHERE d.documentNumber = :documentNumber")
    Optional<BillingDocument> lockByDocumentNumber(@Param("documentNumber") String documentNumber);

    /**
     * Enregistre le PDF fige, mais seulement si aucun ne l'est deja.
     *
     * <p>Le scellement se declenche au premier acces : deux telechargements simultanes pourraient
     * donc l'entreprendre en meme temps. La clause {@code pdfSha256 IS NULL} fait qu'un seul
     * gagne · l'autre obtient zero ligne modifiee, relit et sert la version retenue. Sans elle,
     * le second ecraserait l'empreinte du premier, et un fichier deja remis au client cesserait
     * de correspondre.
     *
     * @return 1 si ce document vient d'etre scelle, 0 s'il l'etait deja
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE BillingDocument d
               SET d.pdfStorageProvider = :provider,
                   d.pdfStoragePath     = :path,
                   d.pdfSha256          = :sha256,
                   d.pdfSealedAt        = :sealedAt
             WHERE d.documentNumber = :documentNumber
               AND d.pdfSha256 IS NULL
            """)
    int sealPdf(@Param("documentNumber") String documentNumber,
                @Param("provider") String provider,
                @Param("path") String path,
                @Param("sha256") String sha256,
                @Param("sealedAt") java.time.Instant sealedAt);

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

    /**
     * Recherche paginee des documents.
     *
     * <p>Les tests de nullite des dates sont enveloppes dans le meme {@code CAST} que la
     * comparaison. Un parametre nu compare a NULL n'offre a PostgreSQL aucun contexte pour en
     * deduire le type, et la requete echoue en « could not determine data type of parameter ».
     * Les filtres textuels y echappent parce que le pilote leur associe deja VARCHAR ; une date
     * nulle, non. C'est la convention deja suivie par les requetes de reporting.
     */
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
                      AND (CAST(:fromDate AS DATE) IS NULL OR issue_date >= CAST(:fromDate AS DATE))
                      AND (CAST(:toDate AS DATE) IS NULL OR issue_date <= CAST(:toDate AS DATE))
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
                      AND (CAST(:fromDate AS DATE) IS NULL OR issue_date >= CAST(:fromDate AS DATE))
                      AND (CAST(:toDate AS DATE) IS NULL OR issue_date <= CAST(:toDate AS DATE))
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
              AND status NOT IN ('CANCELLED','VOIDED')
              AND document_type IN ('INVOICE', 'PROFORMA_INVOICE')
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

    /**
     * Récupère le hash du dernier document validé pour un type donné (chaînage SHA-256).
     * FOR UPDATE SKIP LOCKED sérialise les validations concurrentes du même type.
     */
    @Query(nativeQuery = true, value = """
            SELECT current_hash
            FROM billing_document
            WHERE document_type = :type
              AND locked = TRUE
              AND current_hash IS NOT NULL
            ORDER BY validated_at DESC
            LIMIT 1
            FOR UPDATE SKIP LOCKED
            """)
    java.util.Optional<String> findLastValidatedHash(@Param("type") String type);

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
              AND payment_reminder_count < :maxReminders
              AND (payment_reminder_sent_at IS NULL OR payment_reminder_sent_at < :notRemindedSince)
            ORDER BY due_date ASC
            LIMIT :limit
            """)
    List<BillingDocument> findOverdueReminderCandidates(@Param("limit") int limit,
                                                        @Param("maxReminders") int maxReminders,
                                                        @Param("notRemindedSince") Instant notRemindedSince);

    @Query(nativeQuery = true, value = """
            SELECT *
            FROM billing_document
            WHERE document_type = :type
              AND locked = TRUE
            ORDER BY validated_at ASC
            """)
    List<BillingDocument> findAllValidatedOrderByValidatedAt(@Param("type") String type);

    /** Les factures echues avec un solde · ce que la relance parametree parcourt chaque jour. */
    @Query(nativeQuery = true, value = """
            SELECT *
            FROM billing_document
            WHERE document_type IN ('INVOICE', 'PROFORMA_INVOICE')
              AND status IN ('ISSUED', 'SENT', 'PARTIALLY_PAID', 'OVERDUE')
              AND balance_due > 0
              AND due_date IS NOT NULL
              AND due_date < CURRENT_DATE
            ORDER BY due_date ASC
            LIMIT :limit
            """)
    List<BillingDocument> findOverdueWithBalance(@Param("limit") int limit);
}
