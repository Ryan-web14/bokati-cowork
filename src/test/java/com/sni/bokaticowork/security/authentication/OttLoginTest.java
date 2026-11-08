package com.sni.bokaticowork.security.authentication;

import com.sni.bokaticowork.core.exception.customs.AccountLockedException;
import com.sni.bokaticowork.core.exception.customs.BadCredentialException;
import com.sni.bokaticowork.security.admin.provisioning.service.interfaces.UserProvisioningService;
import com.sni.bokaticowork.security.admin.user.model.UserPrincipal;
import com.sni.bokaticowork.security.admin.user.model.Users;
import com.sni.bokaticowork.security.authentication.dto.request.ValidateOttRequest;
import com.sni.bokaticowork.security.authentication.service.implementation.AuthenticationServiceImpl;
import com.sni.bokaticowork.security.authentication.service.interfaces.OneTimeTokenService;
import com.sni.bokaticowork.security.service.user.CustomUserDetailService;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * La connexion par code refuse en 401, et applique les refus de la connexion par mot de passe.
 *
 * <p>Un code faux ou echu levait une NullPointerException (500), et un compte verrouille ou
 * desactive se connectait quand meme · pire, la verification du portail le reactivait.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OttLoginTest {

    private static final String EMAIL = "joel@example.com";
    private static final ValidateOttRequest REQUEST = new ValidateOttRequest("123456", "jeton.de.verification");

    @Mock private OneTimeTokenService oneTimeTokenService;
    @Mock private CustomUserDetailService userDetailService;
    @Mock private UserProvisioningService userProvisioningService;
    @InjectMocks private AuthenticationServiceImpl service;

    private final MockHttpServletRequest http = new MockHttpServletRequest();

    private void codeAccepted(boolean enabled, boolean locked) {
        Users user = new Users();
        user.setEmail(EMAIL);
        user.setIsAccountEnabled(enabled);
        user.setIsAccountLocked(locked);
        when(oneTimeTokenService.validateOneTimeToken(anyString(), anyString())).thenReturn(Map.of(true, EMAIL));
        when(userDetailService.loadUserByUsername(EMAIL)).thenReturn(new UserPrincipal(user));
    }

    @Test
    @DisplayName("Un code faux ou echu donne un refus d'identifiants, pas une erreur serveur")
    void aWrongCodeIsABadCredential() {
        when(oneTimeTokenService.validateOneTimeToken(anyString(), anyString())).thenReturn(Map.of(false, ""));

        assertThatThrownBy(() -> service.validateOttLogin(REQUEST, http))
                .isInstanceOf(BadCredentialException.class);
    }

    @Test
    @DisplayName("Un jeton de verification echu donne aussi un refus d'identifiants")
    void anExpiredVerificationTokenIsABadCredential() {
        when(oneTimeTokenService.validateOneTimeToken(anyString(), anyString())).thenThrow(new JwtException("expired"));

        assertThatThrownBy(() -> service.validateOttLogin(REQUEST, http))
                .isInstanceOf(BadCredentialException.class);
    }

    @Test
    @DisplayName("Un compte verrouille ne se connecte pas par code, et n'est pas reactive")
    void aLockedAccountIsRefused() {
        codeAccepted(true, true);

        assertThatThrownBy(() -> service.validateOttLogin(REQUEST, http))
                .isInstanceOf(AccountLockedException.class);
        verify(userProvisioningService, never()).completePortalVerification(any(), any());
    }

    @Test
    @DisplayName("Un compte desactive ne se connecte pas par code, et n'est pas reactive")
    void aDisabledAccountIsRefused() {
        codeAccepted(false, false);

        assertThatThrownBy(() -> service.validateOttLogin(REQUEST, http))
                .isInstanceOf(BadCredentialException.class);
        verify(userProvisioningService, never()).completePortalVerification(any(), any());
    }
}
