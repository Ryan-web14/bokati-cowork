package com.sni.bokaticowork.features.portal.guard;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.features.client.member.enums.MemberStatus;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.client.member.repository.repo.MemberRepository;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import com.sni.bokaticowork.features.document.kyc.KycCaseStatus;
import com.sni.bokaticowork.features.document.kyc.model.KycCase;
import com.sni.bokaticowork.features.document.kyc.repository.KycCaseRepository;
import com.sni.bokaticowork.features.document.kyc.repository.KycDocumentRepository;
import com.sni.bokaticowork.security.admin.user.model.UserPrincipal;
import com.sni.bokaticowork.security.admin.user.model.Users;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Lot KG1 : un membre deja valide dont le dossier est rouvert (niveau KYC releve, piece ajoutee)
 * garde l'acces a son espace ; un dossier renvoye en correction le bloque toujours.
 */
class ClientOnboardingGuardReopenKg1Test {

    private final MemberRepository members = mock(MemberRepository.class);
    private final KycCaseRepository cases = mock(KycCaseRepository.class);
    private final KycDocumentRepository pieces = mock(KycDocumentRepository.class);
    private final ClientOnboardingGuard guard = new ClientOnboardingGuard(members, cases, pieces, new ObjectMapper());

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private boolean pass(MemberStatus memberStatus, KycCaseStatus caseStatus) throws Exception {
        Users user = new Users();
        user.setId(3L);
        UserPrincipal principal = new UserPrincipal(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
        Member member = new Member();
        member.setId(7L);
        member.setUser(user);
        member.setStatus(memberStatus);
        when(members.findByUser_IdAndDeletedFalse(anyLong())).thenReturn(Optional.of(member));
        when(cases.findFirstByOwnerTypeAndOwnerIdOrderByStartedAtDesc(any(), anyLong())).thenReturn(Optional.of(
                KycCase.builder().code("KYC-1").status(caseStatus).ownerType(DocumentOwnerType.MEMBER).ownerId(7L).build()));
        when(pieces.findAllByKycCaseOrderByIdAsc(any())).thenReturn(List.of());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/sni/api/v1/client/bookings");
        return guard.preHandle(request, new MockHttpServletResponse(), null);
    }

    @Test
    void anActiveMemberWithAReopenedCaseKeepsAccess() throws Exception {
        assertThat(pass(MemberStatus.ACTIVE, KycCaseStatus.IN_PROGRESS)).isTrue();
        assertThat(pass(MemberStatus.ACTIVE, KycCaseStatus.UNDER_REVIEW)).isTrue();
    }

    @Test
    void aCorrectionOrANewMemberIsStillBlocked() throws Exception {
        assertThat(pass(MemberStatus.ACTIVE, KycCaseStatus.PENDING_CORRECTION)).isFalse();
        assertThat(pass(MemberStatus.PENDING, KycCaseStatus.IN_PROGRESS)).isFalse();
    }
}
