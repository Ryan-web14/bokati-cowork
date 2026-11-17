package com.sni.bokaticowork.features.portal.document.kyc;

import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.document.documentMaster.dto.request.DocumentUploadMetadataRequest;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentResponse;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentStatus;
import com.sni.bokaticowork.features.document.documentMaster.model.Document;
import com.sni.bokaticowork.features.document.documentMaster.model.DocumentType;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentRepository;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentRequirementRepository;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentTypeRepository;
import com.sni.bokaticowork.features.document.documentMaster.service.implementation.DocumentUploadValidator;
import com.sni.bokaticowork.features.document.documentMaster.service.interfaces.DocumentService;
import com.sni.bokaticowork.features.document.kyc.KycCaseStatus;
import com.sni.bokaticowork.features.document.kyc.KycDocumentVerificationStatus;
import com.sni.bokaticowork.features.document.kyc.model.KycCase;
import com.sni.bokaticowork.features.document.kyc.model.KycDocument;
import com.sni.bokaticowork.features.document.kyc.repository.KycCaseRepository;
import com.sni.bokaticowork.features.document.kyc.repository.KycDocumentRepository;
import com.sni.bokaticowork.features.document.kyc.service.interfaces.KycAutomationService;
import com.sni.bokaticowork.features.document.kyc.service.interfaces.KycService;
import com.sni.bokaticowork.features.portal.document.kyc.service.ClientKycService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Le portail confie le rattachement recto / verso au depot documentaire (lot KG2) : il indique la
 * face, reprend la piece rattachee, ne cree ni ne supprime de ligne lui-meme.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ClientKycServiceUploadTest {

    @Mock private KycService kycService;
    @Mock private KycCaseRepository kycCaseRepository;
    @Mock private KycDocumentRepository kycDocumentRepository;
    @Mock private KycAutomationService kycAutomationService;
    @Mock private DocumentService documentService;
    @Mock private DocumentRepository documentRepository;
    @Mock private DocumentRequirementRepository requirementRepository;
    @Mock private DocumentTypeRepository documentTypeRepository;
    @Mock private DocumentUploadValidator uploadValidator;
    @InjectMocks private ClientKycService service;

    private Member member;
    private KycCase kycCase;
    private Document uploaded;
    private final MockMultipartFile file = new MockMultipartFile("file", "cni.pdf", "application/pdf", new byte[]{1});

    @BeforeEach
    void setUp() {
        member = new Member();
        member.setId(7L);
        member.setMemberId("MBR-1");
        kycCase = new KycCase();
        kycCase.setId(1L);
        kycCase.setStatus(KycCaseStatus.IN_PROGRESS);
        uploaded = new Document();
        uploaded.setCode("DOC-2");

        when(documentTypeRepository.findByCode("CNI")).thenReturn(Optional.of(new DocumentType()));
        when(kycCaseRepository.findFirstByOwnerTypeAndOwnerIdOrderByStartedAtDesc(DocumentOwnerType.MEMBER, 7L))
                .thenReturn(Optional.of(kycCase));
        DocumentResponse response = mock(DocumentResponse.class);
        when(response.getCode()).thenReturn("DOC-2");
        when(documentService.upload(any(), any())).thenReturn(response);
        when(documentRepository.findByCode("DOC-2")).thenReturn(Optional.of(uploaded));
        when(kycDocumentRepository.findByDocument(any())).thenReturn(Optional.empty());
        when(kycDocumentRepository.findAllByBackDocument(any())).thenReturn(List.of());
    }

    @Test
    @DisplayName("Recto : la face est transmise et la piece rattachee est renvoyee, sans ligne en plus")
    void frontReturnsTheLinkedPiece() {
        KycDocument piece = KycDocument.builder().id(10L).shortId(3L).kycCase(kycCase).document(uploaded).documentType("CNI")
                .status(KycDocumentVerificationStatus.PENDING).build();
        when(kycDocumentRepository.findByDocument(uploaded)).thenReturn(Optional.of(piece));

        var result = service.uploadDocument(member, "CNI", "123", null, null, file, "FRONT");

        ArgumentCaptor<DocumentUploadMetadataRequest> metadata = ArgumentCaptor.forClass(DocumentUploadMetadataRequest.class);
        verify(documentService).upload(metadata.capture(), any());
        assertThat(metadata.getValue().getKycSide()).isEqualTo("FRONT");
        assertThat(result.getId()).isEqualTo(3L);
        verify(kycDocumentRepository, never()).delete(any());
        verify(kycAutomationService).recompute(kycCase);
    }

    @Test
    @DisplayName("Verso : la piece est retrouvee par son verso, aucune piece n'est creee")
    void backReturnsTheFrontPiece() {
        Document frontFile = new Document();
        frontFile.setCode("DOC-1");
        KycDocument front = KycDocument.builder().id(10L).shortId(3L).kycCase(kycCase).document(frontFile)
                .backDocument(uploaded).documentType("CNI").status(KycDocumentVerificationStatus.PENDING).build();
        when(kycDocumentRepository.findAllByBackDocument(uploaded)).thenReturn(List.of(front));

        var result = service.uploadDocument(member, "CNI", null, null, null, file, "BACK");

        ArgumentCaptor<DocumentUploadMetadataRequest> metadata = ArgumentCaptor.forClass(DocumentUploadMetadataRequest.class);
        verify(documentService).upload(metadata.capture(), any());
        assertThat(metadata.getValue().getKycSide()).isEqualTo("BACK");
        assertThat(result.getDocumentCode()).isEqualTo("DOC-1");
        verify(kycDocumentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Retrait d'une piece en attente : fichiers archives, piece retiree, dossier recalcule")
    void deletingArchivesTheFilesSoANewUploadIsPossible() {
        Document frontFile = Document.builder().code("DOC-1").status(DocumentStatus.PENDING_REVIEW).build();
        Document backFile = Document.builder().code("DOC-3").status(DocumentStatus.PENDING_REVIEW).build();
        KycDocument piece = KycDocument.builder().id(10L).shortId(3L).kycCase(kycCase).document(frontFile)
                .backDocument(backFile).documentType("CNI").status(KycDocumentVerificationStatus.PENDING).build();
        when(kycDocumentRepository.findByShortId(3L)).thenReturn(Optional.of(piece));

        service.deleteDocument(member, 3L);

        verify(documentService).archive(org.mockito.ArgumentMatchers.eq("DOC-1"), anyString());
        verify(documentService).archive(org.mockito.ArgumentMatchers.eq("DOC-3"), anyString());
        verify(kycDocumentRepository).delete(piece);
        verify(kycAutomationService).recompute(kycCase);
    }

    @Test
    @DisplayName("Dossier valide ou en examen : plus aucune piece reclamee")
    void closedCaseRequiresNothing() {
        for (KycCaseStatus status : List.of(KycCaseStatus.APPROVED, KycCaseStatus.SUBMITTED, KycCaseStatus.UNDER_REVIEW)) {
            kycCase.setStatus(status);
            assertThat(service.getRequirements(member)).as(status.name()).isEmpty();
        }
        verify(requirementRepository, never()).findAllByOwnerTypeAndActiveTrueOrderByDocumentTypeNameAsc(any());
    }
}
