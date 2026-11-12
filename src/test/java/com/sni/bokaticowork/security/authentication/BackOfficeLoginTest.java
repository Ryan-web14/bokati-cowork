package com.sni.bokaticowork.security.authentication;

import com.sni.bokaticowork.core.audit.service.interfaces.UserSessionService;
import com.sni.bokaticowork.core.exception.customs.BackOfficeAccessDeniedException;
import com.sni.bokaticowork.core.exception.customs.BadCredentialException;
import com.sni.bokaticowork.security.admin.user.model.UserPrincipal;
import com.sni.bokaticowork.security.admin.user.model.Users;
import com.sni.bokaticowork.security.admin.user.service.interfaces.UserService;
import com.sni.bokaticowork.security.authentication.dto.request.LoginRequest;
import com.sni.bokaticowork.security.authentication.service.implementation.AuthenticationServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * La connexion a l'espace d'administration ({@code space = BACK_OFFICE}) refuse un compte sans
 * role d'equipe, apres le mot de passe et avant toute session ; le portail client ne change pas.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BackOfficeLoginTest {

    @Mock private AuthenticationManager authenticationManager;
    @Mock private UserSessionService sessionService;
    @Mock private UserService userService;
    @InjectMocks private AuthenticationServiceImpl service;

    private final MockHttpServletRequest http = new MockHttpServletRequest();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void passwordAccepted(String... authorities) {
        UserPrincipal principal = mock(UserPrincipal.class);
        when(principal.getUsername()).thenReturn("compte@elleaose.com");
        when(principal.getUser()).thenReturn(new Users());
        List<SimpleGrantedAuthority> granted = List.of(authorities).stream().map(SimpleGrantedAuthority::new).toList();
        doReturn(granted).when(principal).getAuthorities();
        UsernamePasswordAuthenticationToken authenticated = UsernamePasswordAuthenticationToken.authenticated(principal, null, granted);
        when(authenticationManager.authenticate(any())).thenReturn(authenticated);
    }

    @Test
    @DisplayName("Un membre est refuse a l'espace d'administration, sans session ni echec de connexion compte")
    void aMemberIsRefusedOnTheBackOffice() {
        passwordAccepted("ROLE_MEMBER", "CLIENT_PORTAL:READ");

        assertThatThrownBy(() -> service.login(new LoginRequest("membre@example.com", "secret", "BACK_OFFICE"), http))
                .isInstanceOf(BackOfficeAccessDeniedException.class);
        verify(sessionService, never()).invalidateOldestSessionByUser(any());
        verify(userService, never()).recordLoginFailure(any());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Un membre de l'equipe passe le controle de l'espace d'administration")
    void staffPassesTheBackOfficeGate() {
        passwordAccepted("ROLE_ADMIN");
        try {
            service.login(new LoginRequest("admin@elleaose.com", "secret", "back_office"), http);
        } catch (RuntimeException ignored) {
            // La creation de session n'est pas simulee ici : seul compte le passage du controle.
            assertThat(ignored).isNotInstanceOf(BackOfficeAccessDeniedException.class);
        }
        verify(sessionService).invalidateOldestSessionByUser(any());
    }

    @Test
    @DisplayName("Sans espace indique (portail client), un membre se connecte comme avant")
    void withoutSpaceAMemberStillLogsIn() {
        passwordAccepted("ROLE_MEMBER");
        try {
            service.login(new LoginRequest("membre@example.com", "secret"), http);
        } catch (RuntimeException ignored) {
            assertThat(ignored).isNotInstanceOf(BackOfficeAccessDeniedException.class);
        }
        verify(sessionService).invalidateOldestSessionByUser(any());
    }

    @Test
    @DisplayName("Un mauvais mot de passe reste un refus d'identifiants, meme pour l'espace d'administration")
    void aWrongPasswordIsStillABadCredential() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));
        assertThatThrownBy(() -> service.login(new LoginRequest("membre@example.com", "faux", "BACK_OFFICE"), http))
                .isInstanceOf(BadCredentialException.class);
    }
}
