package com.sni.bokaticowork.features.document.kyc.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxService;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import com.sni.bokaticowork.features.document.documentMaster.model.DocumentRequirement;
import com.sni.bokaticowork.features.document.documentMaster.model.DocumentType;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentTypeRepository;
import com.sni.bokaticowork.features.document.kyc.KycCaseStatus;
import com.sni.bokaticowork.features.document.kyc.model.KycCase;
import com.sni.bokaticowork.features.document.kyc.repository.KycCaseRepository;
import com.sni.bokaticowork.features.document.kyc.repository.KycDocumentRepository;
import com.sni.bokaticowork.features.document.kyc.service.implementation.KycServiceImpl;
import com.sni.bokaticowork.features.document.kyc.service.interfaces.KycAutomationService;
import com.sni.bokaticowork.features.document.kyc.service.support.KycRequirementResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Lot KG1 : relever le niveau KYC d'un dossier demande les pieces de ce niveau et rouvre le dossier. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class KycLevelChangeKg1Test {

    @Mock KycCaseRepository caseRepository;
    @Mock KycDocumentRepository documentRepository;
    @Mock DocumentTypeRepository documentTypeRepository;
    @Mock OutboxService outboxService;
    @Mock KycAutomationService kycAutomationService;
    @Mock KycRequirementResolver resolver;
    @InjectMocks KycServiceImpl service;

    private KycCase kycCase;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "requirementResolver", resolver);
        kycCase = KycCase.builder().id(1L).code("KYC-1").ownerType(DocumentOwnerType.CUSTOMER).ownerId(9L)
                .status(KycCaseStatus.APPROVED).requestedKycLevel(1).build();
        when(caseRepository.findByCode("KYC-1")).thenReturn(Optional.of(kycCase));
        when(documentRepository.findAllByKycCaseOrderByIdAsc(any())).thenReturn(List.of());
        DocumentRequirement rib = new DocumentRequirement();
        rib.setDocumentTypeCode("RIB");
        rib.setDocumentTypeName("RIB");
        rib.setRequired(true);
        when(resolver.resolve(any())).thenReturn(List.of(rib));
        DocumentType ribType = new DocumentType();
        ribType.setCode("RIB");
        ribType.setName("Relevé d'identité bancaire");
        when(documentTypeRepository.findByCode(anyString())).thenReturn(Optional.empty());
        when(documentTypeRepository.findByCode("RIB")).thenReturn(Optional.of(ribType));
    }

    @Test
    @SuppressWarnings("unchecked")
    void raisingTheLevelRequestsThePiecesAndRecomputesTheCase() {
        try {
            service.changeLevel("KYC-1", 2, "Volume de transactions");
        } catch (RuntimeException ignored) {
            // La reponse (mapper non simule) importe peu ici.
        }

        assertThat(kycCase.getRequestedKycLevel()).isEqualTo(2);
        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(outboxService).publish(eq("KYC_CASE_LEVEL_CHANGED"), eq("KYC_CASE"), eq("KYC-1"), payload.capture());
        Map<String, Object> sent = (Map<String, Object>) payload.getValue();
        assertThat(sent).containsEntry("previousLevel", 1).containsEntry("level", 2)
                .containsEntry("missingDocumentTypeCodes", List.of("RIB"))
                .containsEntry("missingDocumentNames", List.of("Relevé d'identité bancaire"));
        verify(kycAutomationService).recompute(kycCase);
    }

    @Test
    void aClosedCaseMustBeReopenedFirst() {
        kycCase.setStatus(KycCaseStatus.REJECTED);
        assertThatThrownBy(() -> service.changeLevel("KYC-1", 2, null)).isInstanceOf(BadRequestException.class);
        verify(kycAutomationService, never()).recompute(any());
    }

    @Test
    void anOutOfRangeLevelIsRefused() {
        assertThatThrownBy(() -> service.changeLevel("KYC-1", 5, null)).isInstanceOf(BadRequestException.class);
    }
}
