package com.sni.bokaticowork.features.document.documentMaster.service.implementation;

import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxService;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentReviewStatus;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentStatus;
import com.sni.bokaticowork.features.document.documentMaster.model.Document;
import com.sni.bokaticowork.features.document.documentMaster.model.DocumentReview;
import com.sni.bokaticowork.features.document.documentMaster.model.DocumentType;
import com.sni.bokaticowork.features.document.documentMaster.model.DocumentVersion;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentRepository;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentReviewRepository;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentTypeRepository;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentVersionRepository;
import com.sni.bokaticowork.features.document.documentMaster.service.interfaces.DocumentService;
import com.sni.bokaticowork.features.document.kyc.service.interfaces.KycAutomationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Lot KG3 : une seule regle d'approbation automatique (delai sur la version courante), par
 * l'approbation normale, jamais sur un document refuse ou renvoye en correction.
 */
class DocumentAutoApproveKg3Test {

    private final DocumentRepository documents = mock(DocumentRepository.class);
    private final DocumentTypeRepository types = mock(DocumentTypeRepository.class);
    private final DocumentService documentService = mock(DocumentService.class);
    private final DocumentVersionRepository versions = mock(DocumentVersionRepository.class);
    private final DocumentReviewRepository reviews = mock(DocumentReviewRepository.class);
    private final DocumentLifecycleAutomationServiceImpl service = new DocumentLifecycleAutomationServiceImpl(
            documents, types, mock(KycAutomationService.class), mock(OutboxService.class));

    private final DocumentType type = DocumentType.builder().code("CNI").autoApproveAfterDays(3).autoApprove(false).build();
    private Document doc;

    @BeforeEach
    void setUp() {
        service.setAutoApproveCollaborators(documentService, versions, reviews);
        doc = Document.builder().id(1L).code("DOC-1").status(DocumentStatus.PENDING_REVIEW).documentType(type).build();
        when(types.findAllByAutoApproveAfterDaysIsNotNull()).thenReturn(List.of(type));
        when(documents.findAllByStatusAndDocumentTypeAndUploadedAtBefore(eq(DocumentStatus.PENDING_REVIEW), eq(type), any()))
                .thenReturn(List.of(doc));
        when(versions.findByDocumentAndCurrentTrue(doc)).thenReturn(Optional.of(
                DocumentVersion.builder().uploadedAt(Instant.now().minus(5, ChronoUnit.DAYS)).build()));
        when(reviews.findAllByDocumentOrderByReviewedAtDesc(doc)).thenReturn(List.of());
    }

    @Test
    void aDelayWithoutTheAutoApproveFlagIsEnoughAndGoesThroughTheNormalApproval() {
        assertThat(service.autoApproveDocuments()).isEqualTo(1);
        verify(documentService).approve(eq("DOC-1"), any());
    }

    @Test
    void aRecentlyReplacedVersionWaitsAgain() {
        when(versions.findByDocumentAndCurrentTrue(doc)).thenReturn(Optional.of(
                DocumentVersion.builder().uploadedAt(Instant.now().minus(1, ChronoUnit.DAYS)).build()));
        assertThat(service.autoApproveDocuments()).isZero();
        verify(documentService, never()).approve(any(), any());
    }

    @Test
    void aDocumentSentBackForCorrectionIsNeverAutoApproved() {
        when(reviews.findAllByDocumentOrderByReviewedAtDesc(doc)).thenReturn(List.of(
                DocumentReview.builder().reviewStatus(DocumentReviewStatus.NEEDS_CORRECTION).build()));
        assertThat(service.autoApproveDocuments()).isZero();
        verify(documentService, never()).approve(any(), any());
    }

    @Test
    void oneFailureDoesNotStopTheOthers() {
        Document other = Document.builder().id(2L).code("DOC-2").status(DocumentStatus.PENDING_REVIEW).documentType(type).build();
        when(documents.findAllByStatusAndDocumentTypeAndUploadedAtBefore(eq(DocumentStatus.PENDING_REVIEW), eq(type), any()))
                .thenReturn(List.of(doc, other));
        when(versions.findByDocumentAndCurrentTrue(other)).thenReturn(Optional.empty());
        when(reviews.findAllByDocumentOrderByReviewedAtDesc(other)).thenReturn(List.of());
        doThrow(new IllegalStateException("verrou")).when(documentService).approve(eq("DOC-1"), any());

        assertThat(service.autoApproveDocuments()).isEqualTo(1);
        verify(documentService).approve(eq("DOC-2"), any());
    }
}
