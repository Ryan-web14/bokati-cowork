package com.sni.bokaticowork.security.authentication;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.client.member.dto.response.MemberResponse;
import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.service.ReferralProgramService;
import com.sni.bokaticowork.features.subscription.promotion.pricing.referral.service.ReferralService;
import com.sni.bokaticowork.security.admin.provisioning.service.interfaces.UserProvisioningService;
import com.sni.bokaticowork.security.admin.user.service.interfaces.UserService;
import com.sni.bokaticowork.security.authentication.dto.request.RegisterMemberRequest;
import com.sni.bokaticowork.security.authentication.service.implementation.AuthenticationServiceImpl;
import com.sni.bokaticowork.security.authentication.service.interfaces.OneTimeTokenService;
import com.sni.bokaticowork.security.registration.EmailDomainPolicy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * L'inscription par un lien de parrainage : le code est verifie avant la creation du compte, et le
 * filleul enregistre une fois le membre cree.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RegisterWithReferralTest {

    @Mock private UserProvisioningService userProvisioningService;
    @Mock private UserService userService;
    @Mock private OneTimeTokenService oneTimeTokenService;
    @Mock private EmailDomainPolicy emailDomainPolicy;
    @Mock private ReferralProgramService referralProgramService;
    @Mock private ReferralService referralService;

    @InjectMocks private AuthenticationServiceImpl service;

    private static RegisterMemberRequest request(String referralCode) {
        return RegisterMemberRequest.builder().firstname("Awa").lastname("Ndoye").email("awa@example.com")
                .password("motdepasse-solide").phone("+242060000000").referralCode(referralCode).build();
    }

    @Test
    @DisplayName("Avec un code valable : verifie d'abord, filleul enregistre apres la creation du membre")
    void registersTheReferee() {
        MemberResponse member = new MemberResponse();
        member.setMemberId("MBR-77");
        when(userProvisioningService.registerMemberFromPortal(any())).thenReturn(member);

        service.register(request(" K7P2QX "));

        InOrder order = inOrder(referralProgramService, userProvisioningService, referralService);
        order.verify(referralProgramService).requireUsableCode("K7P2QX");
        order.verify(userProvisioningService).registerMemberFromPortal(any());
        order.verify(referralService).register("K7P2QX", "MEMBER", "MBR-77");
    }

    @Test
    @DisplayName("Un code refuse arrete l'inscription avant toute ecriture")
    void refusedCodeCreatesNothing() {
        doThrow(new BadRequestException("Code de parrainage refusé")).when(referralProgramService).requireUsableCode(anyString());

        assertThatThrownBy(() -> service.register(request("FERME"))).isInstanceOf(BadRequestException.class);
        verify(userProvisioningService, never()).registerMemberFromPortal(any());
    }

    @Test
    @DisplayName("Sans code, l'inscription ne touche pas au parrainage")
    void noCodeNoReferral() {
        when(userProvisioningService.registerMemberFromPortal(any())).thenReturn(new MemberResponse());

        service.register(request("  "));

        verify(referralProgramService, never()).requireUsableCode(anyString());
        verify(referralService, never()).register(anyString(), anyString(), anyString());
    }
}
