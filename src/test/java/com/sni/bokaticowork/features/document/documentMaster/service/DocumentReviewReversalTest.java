package com.sni.bokaticowork.features.document.documentMaster.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentStatus;
import com.sni.bokaticowork.features.document.documentMaster.model.Document;
import com.sni.bokaticowork.features.document.documentMaster.service.implementation.DocumentServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Un refus vient d'une personne · une personne se trompe.
 *
 * <p>Tant qu'une pièce refusée ne pouvait plus être revue, la seule issue était de demander au
 * client de la redéposer · pour une erreur qui n'était pas la sienne. Un agent peut désormais
 * approuver ou renvoyer en correction une pièce refusée ; il ne peut pas la refuser deux fois, et
 * ce qui est approuvé, signé, expiré, archivé ou remplacé reste hors de portée d'une revue.</p>
 */
class DocumentReviewReversalTest {

    /** Les statuts sur lesquels une revue n'a plus de sens · quelle que soit la décision. */
    private static final Set<DocumentStatus> CLOSED = Set.of(
            DocumentStatus.DRAFT, DocumentStatus.APPROVED, DocumentStatus.SIGNED,
            DocumentStatus.EXPIRED, DocumentStatus.ARCHIVED, DocumentStatus.SUPERSEDED);

    private final DocumentServiceImpl service = new DocumentServiceImpl(
            null, null, null, null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null);

    private void review(DocumentStatus current, DocumentStatus decision) {
        Document document = Document.builder().code("DOC-202607-000035").status(current).build();
        ReflectionTestUtils.invokeMethod(service, "assertReviewable", document, decision);
    }

    @Test
    void aRejectedDocumentCanBeApprovedOrSentBackForCorrection() {
        assertDoesNotThrow(() -> review(DocumentStatus.REJECTED, DocumentStatus.APPROVED));
        assertDoesNotThrow(() -> review(DocumentStatus.REJECTED, DocumentStatus.NEEDS_CORRECTION));
    }

    @Test
    void aRejectedDocumentIsNotRejectedTwice() {
        BadRequestException refusal = assertThrows(BadRequestException.class,
                () -> review(DocumentStatus.REJECTED, DocumentStatus.REJECTED));
        assertTrue(refusal.getMessage().contains("déjà refusée"), refusal.getMessage());
    }

    @ParameterizedTest
    @EnumSource(value = DocumentStatus.class, names = {"PENDING_REVIEW", "UPLOADED", "NEEDS_CORRECTION"})
    void aDocumentAwaitingReviewTakesAnyDecision(DocumentStatus current) {
        for (DocumentStatus decision : Set.of(DocumentStatus.APPROVED, DocumentStatus.REJECTED, DocumentStatus.NEEDS_CORRECTION)) {
            assertDoesNotThrow(() -> review(current, decision));
        }
    }

    @ParameterizedTest
    @EnumSource(DocumentStatus.class)
    void aSettledDocumentStaysOutOfReach(DocumentStatus current) {
        if (!CLOSED.contains(current)) {
            return;
        }
        BadRequestException refusal = assertThrows(BadRequestException.class,
                () -> review(current, DocumentStatus.APPROVED));
        assertTrue(refusal.getMessage().contains("déposer une nouvelle"), refusal.getMessage());
    }
}
