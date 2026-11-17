package com.sni.bokaticowork.features.document.kyc.service;

import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxService;
import com.sni.bokaticowork.features.client.member.enums.MemberStatus;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.client.member.repository.repo.MemberRepository;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import com.sni.bokaticowork.features.document.documentMaster.model.DocumentRequirement;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentRequirementRepository;
import com.sni.bokaticowork.features.document.kyc.KycCaseStatus;
import com.sni.bokaticowork.features.document.kyc.KycDocumentVerificationStatus;
import com.sni.bokaticowork.features.document.kyc.model.KycCase;
import com.sni.bokaticowork.features.document.kyc.model.KycDocument;
import com.sni.bokaticowork.features.document.kyc.repository.KycCaseRepository;
import com.sni.bokaticowork.features.document.kyc.repository.KycDocumentRepository;
import com.sni.bokaticowork.features.document.kyc.service.implementation.KycAutomationServiceImpl;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Creation automatique des dossiers KYC avec des exigences toutes facultatives (17/11/2026). */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class KycAutoCreationTest {

    @Mock KycCaseRepository caseRepository;
    @Mock DocumentRequirementRepository requirementRepository;
    @Mock MemberRepository memberRepository;
    @Mock SequenceGeneratorFacade sequenceGenerator;
    @Mock OutboxService outboxService;
    @Mock KycDocumentRepository documentRepository;
    @Mock KycRequirementResolver resolver;
    @InjectMocks KycAutomationServiceImpl automation;

    private final Member member = Member.builder().id(5L).memberId("MBR-1").status(MemberStatus.PENDING).deleted(false).build();
    private final List<DocumentRequirement> optional = List.of(
            DocumentRequirement.builder().ownerType(DocumentOwnerType.MEMBER).documentTypeCode("MEMBER_NATIONAL_ID").required(false).active(true).build(),
            DocumentRequirement.builder().ownerType(DocumentOwnerType.MEMBER).documentTypeCode("MEMBER_PASSPORT").required(false).active(true).build());

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(automation, "requirementResolver", resolver);
        when(memberRepository.findByMemberIdAndDeletedFalse("MBR-1")).thenReturn(Optional.of(member));
        when(memberRepository.findById(5L)).thenReturn(Optional.of(member));
        when(requirementRepository.findAllByOwnerTypeAndActiveTrueOrderByDocumentTypeNameAsc(DocumentOwnerType.MEMBER)).thenReturn(optional);
        when(resolver.resolve(any())).thenReturn(optional);
        when(sequenceGenerator.next(anyString(), any())).thenReturn("KYC-NEW");
        KycCase[] stored = new KycCase[1];
        when(caseRepository.findFirstByOwnerTypeAndOwnerIdOrderByStartedAtDesc(any(), any()))
                .thenAnswer(invocation -> Optional.ofNullable(stored[0]));
        when(caseRepository.save(any())).thenAnswer(invocation -> stored[0] = invocation.getArgument(0));
    }

    @Test
    void aMemberGetsACaseEvenWhenEveryPieceIsOptional() {
        automation.initializeMemberKyc("MBR-1");

        ArgumentCaptor<KycCase> saved = ArgumentCaptor.forClass(KycCase.class);
        verify(caseRepository, atLeastOnce()).save(saved.capture());
        assertThat(saved.getValue().getOwnerType()).isEqualTo(DocumentOwnerType.MEMBER);
        verify(outboxService).publish(eq("KYC_CASE_AUTO_CREATED"), any(), any(), any());
    }

    @Test
    void optionalPiecesDoNotApproveAnEmptyCase() {
        KycCase kycCase = KycCase.builder().id(1L).code("KYC-1").ownerType(DocumentOwnerType.MEMBER).ownerId(5L)
                .status(KycCaseStatus.IN_PROGRESS).build();
        when(documentRepository.findAllByKycCaseOrderByIdAsc(kycCase)).thenReturn(List.of());

        automation.recompute(kycCase);
        assertThat(kycCase.getStatus()).isEqualTo(KycCaseStatus.IN_PROGRESS);

        KycDocument pending = KycDocument.builder().kycCase(kycCase).documentType("MEMBER_PASSPORT")
                .status(KycDocumentVerificationStatus.PENDING).build();
        when(documentRepository.findAllByKycCaseOrderByIdAsc(kycCase)).thenReturn(List.of(pending));
        automation.recompute(kycCase);
        assertThat(kycCase.getStatus()).isEqualTo(KycCaseStatus.UNDER_REVIEW);

        pending.setStatus(KycDocumentVerificationStatus.VERIFIED);
        automation.recompute(kycCase);
        assertThat(kycCase.getStatus()).isEqualTo(KycCaseStatus.APPROVED);
    }
}
