package com.sni.bokaticowork.features.document.kyc.service.support;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxService;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentStatus;
import com.sni.bokaticowork.features.document.documentMaster.model.Document;
import com.sni.bokaticowork.features.document.documentMaster.model.DocumentType;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentRepository;
import com.sni.bokaticowork.features.document.kyc.KycDocumentVerificationStatus;
import com.sni.bokaticowork.features.document.kyc.model.KycCase;
import com.sni.bokaticowork.features.document.kyc.model.KycDocument;
import com.sni.bokaticowork.features.document.kyc.repository.KycCaseRepository;
import com.sni.bokaticowork.features.document.kyc.repository.KycDocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Lot KG2 : recto, verso et remplacement d'une piece KYC. */
class KycPieceLinkerKg2Test {

    private final KycCaseRepository cases = mock(KycCaseRepository.class);
    private final KycDocumentRepository pieces = mock(KycDocumentRepository.class);
    private final DocumentRepository documents = mock(DocumentRepository.class);
    private final OutboxService outbox = mock(OutboxService.class);
    private final KycPieceLinker linker = new KycPieceLinker(cases, pieces, documents, outbox);

    private final DocumentType cni = DocumentType.builder().code("CNI").name("Carte d'identité").build();
    private final KycCase kycCase = KycCase.builder().id(1L).build();

    private Document file(long id, DocumentStatus status) {
        return Document.builder().id(id).code("DOC-" + id).documentType(cni).status(status)
                .ownerType(DocumentOwnerType.MEMBER).ownerId(7L).build();
    }

    @BeforeEach
    void setUp() {
        when(cases.findFirstByOwnerTypeAndOwnerIdOrderByStartedAtDesc(DocumentOwnerType.MEMBER, 7L)).thenReturn(Optional.of(kycCase));
        when(pieces.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private KycDocument link(Document document, String side) {
        return linker.link(document, DocumentOwnerType.MEMBER, 7L, new KycPieceLinker.Details(side, "123", null, null));
    }

    @Test
    void aFirstFrontCreatesThePiece() {
        when(pieces.findAllByKycCaseAndDocumentTypeOrderByIdAsc(kycCase, "CNI")).thenReturn(List.of());
        Document front = file(1, DocumentStatus.PENDING_REVIEW);

        KycDocument piece = link(front, "FRONT");

        assertThat(piece.getDocument()).isSameAs(front);
        assertThat(piece.getKycCase()).isSameAs(kycCase);
        assertThat(piece.getStatus()).isEqualTo(KycDocumentVerificationStatus.PENDING);
    }

    @Test
    void aBackSideJoinsItsFrontAndSupersedesTheOldBack() {
        Document front = file(1, DocumentStatus.APPROVED);
        Document oldBack = file(2, DocumentStatus.REJECTED);
        KycDocument piece = KycDocument.builder().id(10L).kycCase(kycCase).document(front).backDocument(oldBack)
                .documentType("CNI").status(KycDocumentVerificationStatus.REJECTED).build();
        when(pieces.findAllByKycCaseAndDocumentTypeOrderByIdAsc(kycCase, "CNI")).thenReturn(List.of(piece));
        Document newBack = file(3, DocumentStatus.PENDING_REVIEW);

        KycDocument linked = link(newBack, "BACK");

        assertThat(linked).isSameAs(piece);
        assertThat(piece.getBackDocument()).isSameAs(newBack);
        assertThat(piece.getDocument()).isSameAs(front);
        assertThat(oldBack.getStatus()).isEqualTo(DocumentStatus.SUPERSEDED);
        assertThat(front.getStatus()).isEqualTo(DocumentStatus.APPROVED);
        verify(outbox).publish(eq("DOCUMENT_SUPERSEDED"), eq("DOCUMENT"), eq("DOC-2"), any());
    }

    @Test
    void aBackSideWithoutFrontIsRefused() {
        when(pieces.findAllByKycCaseAndDocumentTypeOrderByIdAsc(kycCase, "CNI")).thenReturn(List.of());
        assertThatThrownBy(() -> link(file(3, DocumentStatus.PENDING_REVIEW), "BACK"))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("recto");
        verify(pieces, never()).save(any());
    }

    @Test
    void aNewFrontReplacesTheFileAndSupersedesBothSides() {
        Document oldFront = file(1, DocumentStatus.APPROVED);
        Document oldBack = file(2, DocumentStatus.APPROVED);
        KycDocument piece = KycDocument.builder().id(10L).kycCase(kycCase).document(oldFront).backDocument(oldBack)
                .documentType("CNI").status(KycDocumentVerificationStatus.VERIFIED).build();
        when(pieces.findAllByKycCaseAndDocumentTypeOrderByIdAsc(kycCase, "CNI")).thenReturn(List.of(piece));
        Document newFront = file(4, DocumentStatus.PENDING_REVIEW);

        KycDocument linked = link(newFront, null);

        assertThat(linked).isSameAs(piece);
        assertThat(piece.getDocument()).isSameAs(newFront);
        assertThat(piece.getBackDocument()).isNull();
        assertThat(piece.getStatus()).isEqualTo(KycDocumentVerificationStatus.PENDING);
        assertThat(oldFront.getStatus()).isEqualTo(DocumentStatus.SUPERSEDED);
        assertThat(oldBack.getStatus()).isEqualTo(DocumentStatus.SUPERSEDED);
        verify(documents, times(2)).save(any());
    }

    @Test
    void anArchivedFileIsNotSupersededAgain() {
        Document archived = file(1, DocumentStatus.ARCHIVED);
        KycDocument piece = KycDocument.builder().id(10L).kycCase(kycCase).document(archived).documentType("CNI")
                .status(KycDocumentVerificationStatus.REJECTED).build();
        when(pieces.findAllByKycCaseAndDocumentTypeOrderByIdAsc(kycCase, "CNI")).thenReturn(List.of(piece));

        link(file(4, DocumentStatus.PENDING_REVIEW), "FRONT");

        assertThat(archived.getStatus()).isEqualTo(DocumentStatus.ARCHIVED);
        verify(documents, never()).save(any());
    }
}
