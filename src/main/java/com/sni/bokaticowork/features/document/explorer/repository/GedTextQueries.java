package com.sni.bokaticowork.features.document.explorer.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Le texte des documents et la recherche plein texte de la GED.
 *
 * <p>{@code document_text} n'a pas d'entite : une ligne par document, ecrite par le travailleur
 * d'extraction, lue par la recherche. {@code search_vector} est calcule par la base.</p>
 */
@Repository
public class GedTextQueries {

    /** Un echec se retente apres ce delai, au plus trois fois ; un nouvel envoi le retente aussitot. */
    private static final String RETRY_AFTER = "6 hours";
    private static final int MAX_ATTEMPTS = 3;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Les documents dont le texte manque, date d'une ancienne version, ou a echoue et peut etre
     * retente. Les documents a la corbeille attendent leur restauration.
     */
    @SuppressWarnings("unchecked")
    public List<Long> pendingDocumentIds(int limit) {
        return ((List<Number>) entityManager.createNativeQuery("""
                SELECT d.id FROM document d
                LEFT JOIN document_text t ON t.document_id = d.id
                WHERE d.deleted = FALSE
                  AND (t.document_id IS NULL
                       OR t.version_number IS DISTINCT FROM d.current_version_number
                       OR (t.status = 'FAILED' AND t.attempts < :maxAttempts
                           AND t.extracted_at < now() - CAST(:retryAfter AS interval)))
                ORDER BY (t.document_id IS NOT NULL), d.id DESC
                LIMIT :limit
                """)
                .setParameter("maxAttempts", MAX_ATTEMPTS)
                .setParameter("retryAfter", RETRY_AFTER)
                .setParameter("limit", limit)
                .getResultList()).stream().map(Number::longValue).toList();
    }

    public long countPending() {
        return ((Number) entityManager.createNativeQuery("""
                SELECT COUNT(*) FROM document d
                LEFT JOIN document_text t ON t.document_id = d.id
                WHERE d.deleted = FALSE
                  AND (t.document_id IS NULL OR t.version_number IS DISTINCT FROM d.current_version_number)
                """).getSingleResult()).longValue();
    }

    /** Le nombre de documents par etat d'extraction (DONE, EMPTY, UNSUPPORTED, FAILED). */
    @SuppressWarnings("unchecked")
    public Map<String, Long> countByStatus() {
        Map<String, Long> counts = new HashMap<>();
        for (Object[] row : (List<Object[]>) entityManager.createNativeQuery("""
                SELECT t.status, COUNT(*) FROM document_text t
                JOIN document d ON d.id = t.document_id AND d.deleted = FALSE
                GROUP BY t.status
                """).getResultList()) {
            counts.put((String) row[0], ((Number) row[1]).longValue());
        }
        return counts;
    }

    /** Ecrit le texte d'un document ; un echec repete compte ses tentatives. */
    public void save(long documentId, Integer versionNumber, String status, String method, Integer pageCount,
                     int ocrPages, String content, boolean truncated, String error) {
        entityManager.createNativeQuery("""
                INSERT INTO document_text (document_id, version_number, status, method, page_count, ocr_pages,
                                           content, content_length, truncated, error, attempts, extracted_at)
                VALUES (:id, :version, :status, :method, :pages, :ocrPages, :content, :length, :truncated, :error, 1, now())
                ON CONFLICT (document_id) DO UPDATE SET
                    attempts = CASE WHEN EXCLUDED.status = 'FAILED' AND document_text.status = 'FAILED'
                                     AND document_text.version_number IS NOT DISTINCT FROM EXCLUDED.version_number
                                    THEN document_text.attempts + 1 ELSE 1 END,
                    version_number = EXCLUDED.version_number, status = EXCLUDED.status, method = EXCLUDED.method,
                    page_count = EXCLUDED.page_count, ocr_pages = EXCLUDED.ocr_pages, content = EXCLUDED.content,
                    content_length = EXCLUDED.content_length, truncated = EXCLUDED.truncated, error = EXCLUDED.error,
                    extracted_at = now()
                """)
                .setParameter("id", documentId)
                .setParameter("version", versionNumber)
                .setParameter("status", status)
                .setParameter("method", method)
                .setParameter("pages", pageCount)
                .setParameter("ocrPages", ocrPages)
                .setParameter("content", content)
                .setParameter("length", content == null ? 0 : content.length())
                .setParameter("truncated", truncated)
                .setParameter("error", error)
                .executeUpdate();
    }

    /** status, method, page_count, ocr_pages, content, content_length, truncated, error, extracted_at, version_number */
    @SuppressWarnings("unchecked")
    public Optional<Object[]> textOf(long documentId) {
        List<Object[]> rows = entityManager.createNativeQuery("""
                SELECT status, method, page_count, ocr_pages, content, content_length, truncated, error,
                       extracted_at, version_number
                FROM document_text WHERE document_id = :id
                """).setParameter("id", documentId).getResultList();
        return rows.stream().findFirst();
    }

    /**
     * Cherche dans le contenu et dans le nom des fichiers.
     *
     * <p>{@code websearch_to_tsquery} accepte la syntaxe d'un moteur de recherche : mots,
     * {@code "expression exacte"}, {@code -exclu}, {@code or}. Un nom de fichier qui contient la
     * saisie passe devant. Avec {@code folderPath}, seuls le dossier et ses sous-dossiers.</p>
     *
     * @return id du document et rang, du plus pertinent au moins pertinent
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> search(String text, String folderPath, int offset, int limit) {
        return search(text, folderPath, java.util.Set.of(), offset, limit);
    }

    /** {@code hiddenFolders} : dossiers restreints caches a la personne ; leur contenu est exclu. */
    @SuppressWarnings("unchecked")
    public List<Object[]> search(String text, String folderPath, java.util.Set<Long> hiddenFolders, int offset, int limit) {
        Query query = entityManager.createNativeQuery("""
                SELECT d.id,
                       COALESCE(ts_rank_cd(t.search_vector, q.query), 0)
                         + CASE WHEN normalize_text(d.title) LIKE q.pattern THEN 1 ELSE 0 END AS rank
                FROM document d
                LEFT JOIN document_text t ON t.document_id = d.id
                CROSS JOIN (SELECT websearch_to_tsquery('french', normalize_text(:text)) AS query,
                                   '%' || normalize_text(:text) || '%' AS pattern) q
                WHERE d.deleted = FALSE
                """ + folderFilter(folderPath) + hiddenFilter(hiddenFolders) + """
                  AND (t.search_vector @@ q.query OR normalize_text(d.title) LIKE q.pattern
                       OR normalize_text(d.file_name) LIKE q.pattern)
                ORDER BY rank DESC, d.updated_at DESC, d.id DESC
                OFFSET :offset LIMIT :limit
                """);
        bind(query, text, folderPath);
        bindHidden(query, hiddenFolders);
        return query.setParameter("offset", offset).setParameter("limit", limit).getResultList();
    }

    public long countSearch(String text, String folderPath) {
        return countSearch(text, folderPath, java.util.Set.of());
    }

    public long countSearch(String text, String folderPath, java.util.Set<Long> hiddenFolders) {
        Query query = entityManager.createNativeQuery("""
                SELECT COUNT(*)
                FROM document d
                LEFT JOIN document_text t ON t.document_id = d.id
                CROSS JOIN (SELECT websearch_to_tsquery('french', normalize_text(:text)) AS query,
                                   '%' || normalize_text(:text) || '%' AS pattern) q
                WHERE d.deleted = FALSE
                """ + folderFilter(folderPath) + hiddenFilter(hiddenFolders) + """
                  AND (t.search_vector @@ q.query OR normalize_text(d.title) LIKE q.pattern
                       OR normalize_text(d.file_name) LIKE q.pattern)
                """);
        bind(query, text, folderPath);
        bindHidden(query, hiddenFolders);
        return ((Number) query.getSingleResult()).longValue();
    }

    /** Le texte des documents trouves, pour en tirer l'extrait. */
    @SuppressWarnings("unchecked")
    public Map<Long, String> contents(List<Long> documentIds) {
        Map<Long, String> contents = new HashMap<>();
        if (documentIds.isEmpty()) return contents;
        for (Object[] row : (List<Object[]>) entityManager.createNativeQuery(
                        "SELECT document_id, content FROM document_text WHERE document_id IN (:ids) AND content IS NOT NULL")
                .setParameter("ids", documentIds).getResultList()) {
            contents.put(((Number) row[0]).longValue(), (String) row[1]);
        }
        return contents;
    }

    private static String folderFilter(String folderPath) {
        return folderPath == null ? "" : """
                  AND d.folder_id IN (SELECT f.id FROM document_folder f WHERE f.path LIKE :folderPath AND f.deleted = FALSE)
                """;
    }

    /** Exclut ce qui se trouve sous un dossier cache : son chemin commence par celui du dossier. */
    private static String hiddenFilter(java.util.Set<Long> hiddenFolders) {
        return hiddenFolders == null || hiddenFolders.isEmpty() ? "" : """
                  AND NOT EXISTS (SELECT 1 FROM document_folder df JOIN document_folder hf ON df.path LIKE hf.path || '%'
                                  WHERE df.id = d.folder_id AND hf.id IN (:hiddenFolders))
                """;
    }

    private static void bindHidden(Query query, java.util.Set<Long> hiddenFolders) {
        if (hiddenFolders != null && !hiddenFolders.isEmpty()) {
            query.setParameter("hiddenFolders", hiddenFolders);
        }
    }

    private static void bind(Query query, String text, String folderPath) {
        query.setParameter("text", text);
        if (folderPath != null) {
            query.setParameter("folderPath", folderPath.replace("%", "\\%").replace("_", "\\_") + "%");
        }
    }
}
