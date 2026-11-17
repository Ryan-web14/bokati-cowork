package com.sni.bokaticowork.features.document.kyc.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ForbiddenException;
import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxService;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import com.sni.bokaticowork.features.document.kyc.KycCaseStatus;
import com.sni.bokaticowork.features.document.kyc.KycDocumentVerificationStatus;
import com.sni.bokaticowork.features.document.kyc.dto.request.KycDecisionRequest;
import com.sni.bokaticowork.features.document.kyc.model.KycCase;
import com.sni.bokaticowork.features.document.kyc.model.KycDocument;
import com.sni.bokaticowork.features.document.kyc.model.KycReason;
import com.sni.bokaticowork.features.document.kyc.repository.KycCaseRepository;
import com.sni.bokaticowork.features.document.kyc.repository.KycDocumentRepository;
import com.sni.bokaticowork.features.document.kyc.repository.KycReasonRepository;
import com.sni.bokaticowork.features.document.kyc.service.implementation.KycServiceImpl;
import com.sni.bokaticowork.features.document.kyc.service.support.KycCenterService;
import com.sni.bokaticowork.features.document.kyc.service.support.KycReasons;
import com.sni.bokaticowork.security.admin.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Lot KG7 : refus definitif et motifs, tableau de bord, renouvellements, doublons. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class KycCenterKg7Test {

    @Mock KycCaseRepository caseRepository;
    @Mock KycDocumentRepository documentRepository;
    @Mock OutboxService outboxService;
    @InjectMocks KycServiceImpl service;

    private final KycReasonRepository reasonRepository = mock(KycReasonRepository.class);
    private final KycReasons reasons = new KycReasons(reasonRepository);
    private KycCase kycCase;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "reasons", reasons);
        kycCase = KycCase.builder().id(1L).code("KYC-1").ownerType(DocumentOwnerType.CUSTOMER).ownerId(9L)
                .status(KycCaseStatus.UNDER_REVIEW).build();
        when(caseRepository.findByCode("KYC-1")).thenReturn(Optional.of(kycCase));
        when(documentRepository.findAllByKycCaseOrderByIdAsc(any())).thenReturn(List.of());
        when(reasonRepository.findByCode("DOCUMENT_FALSIFIE")).thenReturn(Optional.of(KycReason.builder()
                .code("DOCUMENT_FALSIFIE").label("Document falsifié ou altéré").kind("REJECTION").active(true).build()));
        when(reasonRepository.findByCode("PIECE_ILLISIBLE")).thenReturn(Optional.of(KycReason.builder()
                .code("PIECE_ILLISIBLE").label("Pièce illisible ou floue").kind("CORRECTION").active(true).build()));
        when(reasonRepository.findByCode("AUTRE")).thenReturn(Optional.of(KycReason.builder()
                .code("AUTRE").label("Autre, préciser").kind("BOTH").requiresText(true).active(true).build()));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private static KycDecisionRequest decision(String reasonCode, String comment) {
        KycDecisionRequest request = new KycDecisionRequest();
        request.setReasonCode(reasonCode);
        request.setComment(comment);
        return request;
    }

    @Test
    void aFinalRejectionNeedsAReasonOfTheList() {
        assertThatThrownBy(() -> service.rejectFinal("KYC-1", decision(null, "non"))).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.rejectFinal("KYC-1", decision("PIECE_ILLISIBLE", null)))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("refus définitif");
        assertThatThrownBy(() -> service.rejectFinal("KYC-1", decision("AUTRE", " "))).hasMessageContaining("Précisez");
        assertThat(kycCase.getStatus()).isEqualTo(KycCaseStatus.UNDER_REVIEW);
    }

    @Test
    void aFinalRejectionClosesTheCaseAndTellsTheHolder() {
        try {
            service.rejectFinal("KYC-1", decision("DOCUMENT_FALSIFIE", "Photo retouchée"));
        } catch (RuntimeException ignored) {
            // Reponse (mapper non simule) sans importance ici.
        }

        assertThat(kycCase.getStatus()).isEqualTo(KycCaseStatus.REJECTED);
        assertThat(kycCase.getDecisionReasonCode()).isEqualTo("DOCUMENT_FALSIFIE");
        assertThat(kycCase.getDecisionComment()).isEqualTo("Document falsifié ou altéré : Photo retouchée");
        verify(outboxService).publish(eq("KYC_CASE_REJECTED"), eq("KYC_CASE"), eq("KYC-1"), anyMap());
    }

    @Test
    void onlyAnAdminReopensARejectedCase() {
        kycCase.setStatus(KycCaseStatus.REJECTED);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("agent", null,
                List.of(new SimpleGrantedAuthority("KYC:MANAGE"))));
        assertThatThrownBy(() -> service.reopen("KYC-1", null)).isInstanceOf(ForbiddenException.class);

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("admin", null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        try {
            service.reopen("KYC-1", "Nouvelle pièce fournie");
        } catch (RuntimeException ignored) {
            // Reponse sans importance ici.
        }
        assertThat(kycCase.getStatus()).isEqualTo(KycCaseStatus.IN_PROGRESS);
    }

    @Test
    void aCorrectionOnARejectedCaseIsRefused() {
        kycCase.setStatus(KycCaseStatus.REJECTED);
        assertThatThrownBy(() -> service.reject("KYC-1", decision("PIECE_ILLISIBLE", null))).hasMessageContaining("rouvrir");
        verify(outboxService, never()).publish(any(), any(), any(), anyMap());
    }

    // ---------------------------------------------------------------- centre

    private KycDocument piece(long id, String caseCode, long ownerId, String number, LocalDate expiry) {
        return KycDocument.builder().id(id).kycCase(KycCase.builder().code(caseCode).build()).ownerType(DocumentOwnerType.MEMBER)
                .ownerId(ownerId).documentType("MEMBER_NATIONAL_ID").documentNumber(number).expiryDate(expiry)
                .status(KycDocumentVerificationStatus.VERIFIED).build();
    }

    private KycCenterService center() {
        return new KycCenterService(caseRepository, documentRepository, mock(UserRepository.class), outboxService);
    }

    @Test
    void theSameNumberOnTwoHoldersIsADuplicate() {
        KycDocument a = piece(1, "KYC-A", 1, "M 0123-45", null);
        KycDocument b = piece(2, "KYC-B", 2, "m012345", null);
        KycDocument c = piece(3, "KYC-C", 3, "AUTRE99", null);
        KycDocument sameOwner = piece(4, "KYC-A", 1, "M012345", null);
        when(documentRepository.findAll()).thenReturn(List.of(a, b, c, sameOwner));

        List<KycCenterService.DuplicateGroup> groups = center().duplicates();

        assertThat(groups).hasSize(1);
        assertThat(groups.get(0).normalizedNumber()).isEqualTo("M012345");
        assertThat(center().duplicateOf(a)).containsExactly("KYC-B");
        assertThat(center().duplicateOf(c)).isEmpty();
    }

    @Test
    void aRecentlyRemindedPieceIsSkipped() {
        LocalDate soon = LocalDate.now().plusDays(20);
        KycDocument fresh = piece(1, "KYC-A", 1, "N1", soon);
        KycDocument reminded = piece(2, "KYC-B", 2, "N2", soon);
        reminded.setRenewalRemindedAt(Instant.now().minus(2, ChronoUnit.DAYS));
        when(documentRepository.findAllByExpiryDateBetween(any(), any())).thenReturn(List.of(fresh, reminded));

        KycCenterService.RemindResult result = center().remind(90, null);

        assertThat(result.reminded()).isEqualTo(1);
        assertThat(result.skippedRecentlyReminded()).isEqualTo(1);
        assertThat(fresh.getRenewalRemindedAt()).isNotNull();
        verify(outboxService).publish(eq("KYC_DOCUMENT_EXPIRY_REMINDER"), eq("KYC_DOCUMENT"), eq("1"), anyMap());
    }

    @Test
    void theDashboardCountsQueuesDelaysAndBlockedCases() {
        Instant now = Instant.now();
        KycCase waiting = KycCase.builder().id(1L).code("A").status(KycCaseStatus.SUBMITTED).submittedAt(now.minus(10, ChronoUnit.DAYS))
                .slaDeadline(now.minus(1, ChronoUnit.DAYS)).build();
        KycCase decided = KycCase.builder().id(2L).code("B").status(KycCaseStatus.APPROVED).submittedAt(now.minus(3, ChronoUnit.DAYS))
                .reviewedAt(now.minus(1, ChronoUnit.DAYS)).reviewedBy(7L).build();
        KycCase stuck = KycCase.builder().id(3L).code("C").status(KycCaseStatus.PENDING_CORRECTION)
                .reviewedAt(now.minus(20, ChronoUnit.DAYS)).build();
        when(caseRepository.findAll()).thenReturn(List.of(waiting, decided, stuck));
        when(documentRepository.findAll()).thenReturn(List.of());

        KycCenterService.CenterDashboard dashboard = center().dashboard(null, null);

        assertThat(dashboard.waitingReview().overSevenDays()).isEqualTo(1);
        assertThat(dashboard.decidedInPeriod()).isEqualTo(1);
        assertThat(dashboard.averageProcessingHours()).isEqualTo(48.0);
        assertThat(dashboard.overdue()).extracting(KycCenterService.CaseRef::code).containsExactly("A");
        assertThat(dashboard.blocked()).extracting(KycCenterService.CaseRef::code).contains("C");
        assertThat(dashboard.agents()).extracting(KycCenterService.AgentLoad::userId).containsExactly(7L);
    }
}
