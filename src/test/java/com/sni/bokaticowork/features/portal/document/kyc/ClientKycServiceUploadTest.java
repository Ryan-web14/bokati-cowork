package com.sni.bokaticowork.features.portal.document.kyc;

import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentResponse;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Un envoi depuis le portail donne une seule piece · et un dossier complet ne reclame plus rien.
 *
 * <p>L'envoi d'un document KYC creait deja sa ligne de dossier ; le portail en ajoutait une seconde
 * pour le meme fichier, et chaque piece apparaissait deux fois.</p>
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
    private KycDocument autoCreated;
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

        // La ligne que documentService.upload cree lui-meme pour un document de categorie KYC.
        autoCreated = KycDocument.builder().id(20L).kycCase(kycCase).document(uploaded).documentType("CNI")
                .status(KycDocumentVerificationStatus.PENDING).build();
        when(kycDocumentRepository.findByDocument(uploaded)).thenReturn(Optional.of(autoCreated));
    }

    @Test
    @DisplayName("Premier envoi : la ligne creee par l'envoi est reprise, aucune seconde")
    void firstUploadReusesTheAutoCreatedLine() {
        when(kycDocumentRepository.findAllByKycCaseAndDocumentTypeOrderByIdAsc(kycCase, "CNI")).thenReturn(List.of(autoCreated));

        service.uploadDocument(member, "CNI", "123", null, null, file, "FRONT");

        verify(kycDocumentRepository).save(autoCreated);
        verify(kycDocumentRepository, never()).delete(any());
        verify(kycDocumentRepository, never()).save(org.mockito.ArgumentMatchers.argThat(doc -> doc != autoCreated));
    }

    @Test
    @DisplayName("Nouvel envoi du meme type : la piece existante est remplacee, le doublon retire")
    void reuploadReplacesTheExistingPiece() {
        Document previous = new Document();
        previous.setCode("DOC-1");
        KycDocument existing = KycDocument.builder().id(10L).kycCase(kycCase).document(previous).documentType("CNI")
                .status(KycDocumentVerificationStatus.REJECTED).build();
        when(kycDocumentRepository.findAllByKycCaseAndDocumentTypeOrderByIdAsc(kycCase, "CNI"))
                .thenReturn(List.of(existing, autoCreated));

        service.uploadDocument(member, "CNI", "123", null, null, file, "FRONT");

        assertThat(existing.getDocument()).isSameAs(uploaded);
        assertThat(existing.getStatus()).isEqualTo(KycDocumentVerificationStatus.PENDING);
        verify(kycDocumentRepository).delete(autoCreated);
        verify(kycDocumentRepository).save(existing);
    }

    @Test
    @DisplayName("Verso : rattache au recto, sans devenir une piece a part")
    void backSideAttachesToTheFront() {
        Document frontFile = new Document();
        frontFile.setCode("DOC-1");
        KycDocument front = KycDocument.builder().id(10L).kycCase(kycCase).document(frontFile).documentType("CNI")
                .status(KycDocumentVerificationStatus.PENDING).build();
        when(kycDocumentRepository.findAllByKycCaseAndDocumentTypeOrderByIdAsc(kycCase, "CNI"))
                .thenReturn(List.of(front, autoCreated));

        service.uploadDocument(member, "CNI", null, null, null, file, "BACK");

        assertThat(front.getBackDocument()).isSameAs(uploaded);
        verify(kycDocumentRepository).delete(autoCreated);
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
