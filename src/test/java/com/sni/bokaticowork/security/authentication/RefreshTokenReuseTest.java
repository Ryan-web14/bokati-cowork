package com.sni.bokaticowork.security.authentication;

import com.sni.bokaticowork.core.audit.service.interfaces.UserSessionService;
import com.sni.bokaticowork.core.exception.customs.BadCredentialException;
import com.sni.bokaticowork.security.admin.user.model.UserPrincipal;
import com.sni.bokaticowork.security.admin.user.model.Users;
import com.sni.bokaticowork.security.authentication.dto.request.RefreshTokenRequest;
import com.sni.bokaticowork.security.authentication.dto.response.LoginResponse;
import com.sni.bokaticowork.security.authentication.service.implementation.AuthenticationServiceImpl;
import com.sni.bokaticowork.security.model.RefreshToken;
import com.sni.bokaticowork.security.service.tokenService.implementation.JWTService;
import com.sni.bokaticowork.security.service.tokenService.interfaces.RefreshTokenService;
import com.sni.bokaticowork.security.service.user.CustomUserDetailService;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Le rafraichissement refuse ce qui doit l'etre, et reconnait un jeton vole.
 *
 * <p>Un jeton deja consomme etait simplement refuse, sans consequence · le voleur qui avait fait
 * tourner la chaine le premier gardait sa session sept jours. Une session fermee se rouvrait par
 * un rafraichissement, et un compte desactive pouvait encore en obtenir un.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RefreshTokenReuseTest {

    private static final String TOKEN = "jeton.de.rafraichissement";
    private static final String SESSION = "11111111-1111-1111-1111-111111111111";

    @Mock private UserSessionService sessionService;
    @Mock private JWTService jwtService;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private CustomUserDetailService userDetailService;
    @InjectMocks private AuthenticationServiceImpl service;

    private Users user;
    private final MockHttpServletRequest http = new MockHttpServletRequest();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "refreshReuseGraceSeconds", 30L);
        user = account(true);
        when(jwtService.isTokenValid(TOKEN, user)).thenReturn(true);
        when(jwtService.extractSessionId(TOKEN)).thenReturn(SESSION);
        when(sessionService.isSessionRevoked(SESSION)).thenReturn(false);
        when(userDetailService.loadUserByUsername(user.getEmail())).thenReturn(new UserPrincipal(user));
        when(jwtService.generateAccesToken(any(), anyString())).thenReturn("nouvel.acces");
        when(jwtService.generateRefreshToken(any(), anyString())).thenReturn("nouveau.rafraichissement");
    }

    private static Users account(boolean enabled) {
        Users account = new Users();
        account.setId(42L);
        account.setUserId("USR-42");
        account.setEmail("joel@example.com");
        account.setIsAccountEnabled(enabled);
        return account;
    }

    private RefreshToken stored(boolean revoked, Instant revokedAt) {
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setExpiration(Instant.now().plusSeconds(3600));
        token.setRevoked(revoked);
        token.setRevokedAt(revokedAt);
        when(refreshTokenService.findByToken(TOKEN)).thenReturn(Optional.of(token));
        return token;
    }

    private LoginResponse refresh() {
        return service.refreshToken(new RefreshTokenRequest(TOKEN), http);
    }

    @Test
    @DisplayName("Un jeton actif donne une nouvelle paire et fait tourner la chaine")
    void anActiveTokenIsRotated() {
        stored(false, null);

        LoginResponse response = refresh();

        assertThat(response.getAccessToken()).isEqualTo("nouvel.acces");
        verify(refreshTokenService).rotateToken(TOKEN, "nouveau.rafraichissement");
        verify(sessionService, never()).revokeAllForUser(any());
    }

    @Test
    @DisplayName("Un jeton consomme depuis longtemps qui revient ferme toutes les sessions du compte")
    void aLongConsumedTokenRevokesEverything() {
        stored(true, Instant.now().minusSeconds(600));

        assertThatThrownBy(this::refresh).isInstanceOf(BadCredentialException.class);
        verify(sessionService).revokeAllForUser(user);
        verify(refreshTokenService, never()).rotateToken(anyString(), anyString());
    }

    @Test
    @DisplayName("Un jeton consomme a l'instant est un second onglet · refuse, sans tout fermer")
    void aJustConsumedTokenIsARace() {
        stored(true, Instant.now().minusSeconds(2));

        assertThatThrownBy(this::refresh).isInstanceOf(BadCredentialException.class);
        verify(sessionService, never()).revokeAllForUser(any());
    }

    @Test
    @DisplayName("Un jeton revoque avant que la date soit enregistree ne declenche pas l'alerte")
    void anUndatedRevokedTokenIsOnlyRefused() {
        stored(true, null);

        assertThatThrownBy(this::refresh).isInstanceOf(BadCredentialException.class);
        verify(sessionService, never()).revokeAllForUser(any());
    }

    @Test
    @DisplayName("Un jeton inconnu est un 401, et plus un 404")
    void anUnknownTokenIsUnauthorized() {
        when(refreshTokenService.findByToken(TOKEN)).thenReturn(Optional.empty());

        assertThatThrownBy(this::refresh).isInstanceOf(BadCredentialException.class);
    }

    @Test
    @DisplayName("Une session fermee ne se rouvre pas par un rafraichissement")
    void aRevokedSessionIsNotReopened() {
        RefreshToken token = stored(false, null);
        when(sessionService.isSessionRevoked(SESSION)).thenReturn(true);

        assertThatThrownBy(this::refresh).isInstanceOf(BadCredentialException.class);
        verify(refreshTokenService).revoke(token);
        verify(sessionService, never()).refreshSession(anyString(), anyString());
    }

    @Test
    @DisplayName("Un compte desactive n'obtient plus de jeton, et perd ses sessions")
    void aDisabledAccountGetsNothing() {
        stored(false, null);
        when(userDetailService.loadUserByUsername(user.getEmail())).thenReturn(new UserPrincipal(account(false)));

        assertThatThrownBy(this::refresh).isInstanceOf(BadCredentialException.class);
        verify(sessionService).revokeAllForUser(any());
        verify(refreshTokenService, never()).rotateToken(anyString(), anyString());
    }

    @Test
    @DisplayName("Un compte supprime n'obtient plus de jeton")
    void aDeletedAccountGetsNothing() {
        RefreshToken token = stored(false, null);
        when(userDetailService.loadUserByUsername(user.getEmail())).thenThrow(new UsernameNotFoundException("absent"));

        assertThatThrownBy(this::refresh).isInstanceOf(BadCredentialException.class);
        verify(refreshTokenService).revoke(token);
    }

    @Test
    @DisplayName("Un jeton dont la signature ou la date ne tient plus est revoque et refuse")
    void anInvalidJwtIsRevoked() {
        RefreshToken token = stored(false, null);
        when(jwtService.isTokenValid(TOKEN, user)).thenThrow(new JwtException("expire"));

        assertThatThrownBy(this::refresh).isInstanceOf(BadCredentialException.class);
        verify(refreshTokenService).revoke(token);
    }
}
