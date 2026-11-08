package com.sni.bokaticowork.features.document.explorer;

import com.sni.bokaticowork.features.document.explorer.repository.GedTextQueries;
import com.sni.bokaticowork.support.PostgresIntegrationTestBase;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * La recherche plein texte de la GED sur un PostgreSQL reel : configuration francaise sans
 * accents, nom de fichier, restriction a un dossier, corbeille, et file d'attente de lecture.
 */
@Import(GedTextQueries.class)
class GedTextQueriesIT extends PostgresIntegrationTestBase {

    private static final AtomicLong IDS = new AtomicLong(991_000_000L);

    @Autowired private EntityManager entityManager;
    @Autowired private GedTextQueries queries;

    private long folder(String parentPath, String name) {
        long id = IDS.incrementAndGet();
        entityManager.createNativeQuery("""
                INSERT INTO document_folder (id, code, name, space, path, depth, created_at, updated_at, deleted)
                VALUES (:id, :code, :name, 'GENERIC', :path, 0, now(), now(), FALSE)
                """).setParameter("id", id).setParameter("code", "FLD-" + id).setParameter("name", name)
                .setParameter("path", (parentPath == null ? "/" : parentPath) + id + "/").executeUpdate();
        return id;
    }

    private String path(long folderId) {
        return (String) entityManager.createNativeQuery("SELECT path FROM document_folder WHERE id = :id")
                .setParameter("id", folderId).getSingleResult();
    }

    private long file(Long folderId, String title, String content) {
        long id = IDS.incrementAndGet();
        entityManager.createNativeQuery("""
                INSERT INTO document (id, code, owner_type, owner_id, category, document_type_id, title, file_name, file_url,
                                      status, uploaded_at, updated_at, correction_count, space, deleted, folder_id,
                                      current_version_number)
                SELECT :id, :code, 'INTERNAL', NULL, 'OTHER', t.id, :title, :title, 'internal/x', 'APPROVED',
                       now(), now(), 0, 'GENERIC', FALSE, :folder, 1
                FROM document_type t WHERE t.code = 'GENERAL'
                """).setParameter("id", id).setParameter("code", "DOC-" + id).setParameter("title", title)
                .setParameter("folder", folderId).executeUpdate();
        if (content != null) {
            queries.save(id, 1, "DONE", "OCR", 1, 1, content, false, null);
        }
        return id;
    }

    private List<Long> found(String text, String folderPath) {
        return queries.search(text, folderPath, 0, 50).stream().map(row -> ((Number) row[0]).longValue()).toList();
    }

    @Test
    @DisplayName("Le contenu se cherche sans accents, avec les variantes du francais, et un nom de fichier passe devant")
    void searchContentAndNames() {
        long bail = file(null, "scan-0042.pdf", "Contrat de bail commercial. L'Échéance du loyer est fixée au 5 du mois.");
        long factures = file(null, "releve.pdf", "Relevé des factures impayées du trimestre");
        long named = file(null, "Echeancier fournisseurs.xlsx", "Tableau sans rapport");
        long other = file(null, "divers.txt", "Note interne sur le ménage des bureaux");
        entityManager.flush();

        assertThat(found("echeance loyer", null)).contains(bail).doesNotContain(factures, other);
        assertThat(found("facture impayee", null)).containsExactly(factures);
        assertThat(found("echeancier", null).getFirst()).isEqualTo(named);
        assertThat(found("\"bail commercial\"", null)).containsExactly(bail);
        assertThat(found("contrat -bail", null)).doesNotContain(bail);
        assertThat(queries.countSearch("menage", null)).isEqualTo(1);
    }

    @Test
    @DisplayName("Sous un dossier, la recherche couvre ses sous-dossiers et rien d'autre ; la corbeille n'apparait pas")
    void searchUnderFolderSkipsTrash() {
        long root = folder(null, "Juridique GED");
        long sub = folder(path(root), "Baux");
        long inside = file(sub, "bail-a.pdf", "Bail du local B12, clause de résiliation anticipée");
        long outside = file(null, "bail-b.pdf", "Bail du local C3, clause de résiliation anticipée");
        long trashed = file(root, "bail-c.pdf", "Bail ancien, résiliation anticipée");
        entityManager.createNativeQuery("UPDATE document SET deleted = TRUE WHERE id = :id").setParameter("id", trashed).executeUpdate();
        entityManager.flush();

        assertThat(found("resiliation", path(root))).containsExactly(inside);
        assertThat(found("resiliation", null)).contains(inside, outside).doesNotContain(trashed);
    }

    @Test
    @DisplayName("Un fichier attend sa lecture tant qu'il n'a pas de texte pour sa version courante ; un echec compte ses tentatives")
    void pendingQueue() {
        long fresh = file(null, "nouveau.pdf", null);
        long read = file(null, "lu.pdf", "deja lu");
        long newVersion = file(null, "v2.pdf", "ancienne version");
        entityManager.createNativeQuery("UPDATE document SET current_version_number = 2 WHERE id = :id")
                .setParameter("id", newVersion).executeUpdate();
        entityManager.flush();

        List<Long> pending = queries.pendingDocumentIds(10_000);
        assertThat(pending).contains(fresh, newVersion).doesNotContain(read);
        assertThat(pending.indexOf(fresh)).isLessThan(pending.indexOf(newVersion));

        queries.save(fresh, 1, "FAILED", null, null, 0, null, false, "Tesseract absent");
        queries.save(fresh, 1, "FAILED", null, null, 0, null, false, "Tesseract absent");
        Object[] row = queries.textOf(fresh).orElseThrow();
        assertThat(row[0]).isEqualTo("FAILED");
        Number attempts = (Number) entityManager.createNativeQuery("SELECT attempts FROM document_text WHERE document_id = :id")
                .setParameter("id", fresh).getSingleResult();
        assertThat(attempts.intValue()).isEqualTo(2);
        // Un echec recent n'est pas retente tout de suite
        assertThat(queries.pendingDocumentIds(10_000)).doesNotContain(fresh);
        assertThat(queries.countByStatus()).containsKeys("FAILED", "DONE");
    }
}
