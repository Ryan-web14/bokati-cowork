package com.sni.bokaticowork.security.filter;

import com.sni.bokaticowork.core.audit.service.interfaces.UserSessionService;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.security.admin.user.model.UserPrincipal;
import com.sni.bokaticowork.security.admin.user.model.Users;
import com.sni.bokaticowork.security.service.tokenService.implementation.JWTService;
import com.sni.bokaticowork.security.service.user.CustomUserDetailService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Un jeton encore valide ne suffit plus · le compte doit l'etre aussi.
 *
 * <p>Le filtre relisait le compte a chaque appel sans regarder son etat. Un compte desactive par
 * un administrateur gardait l'acces jusqu'a l'expiration de son jeton, et un compte supprime
 * provoquait un 500.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JWTFilterAccountStateTest {

    private static final String TOKEN = "un.jeton.d.acces";
    private static final String EMAIL = "joel@example.com";

    @Mock private JWTService jwtService;
    @Mock private CustomUserDetailService userDetailService;
    @Mock private UserSessionService sessionService;
    @InjectMocks private JWTFilter filter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        when(jwtService.extractSessionId(TOKEN)).thenReturn("SES-1");
        when(sessionService.isSessionRevoked("SES-1")).thenReturn(false);
        when(jwtService.isAccessToken(TOKEN)).thenReturn(true);
        when(jwtService.extractEmail(TOKEN)).thenReturn(EMAIL);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private static UserPrincipal principal(boolean enabled) {
        Users user = new Users();
        user.setEmail(EMAIL);
        user.setIsAccountEnabled(enabled);
        return new UserPrincipal(user);
    }

    private MockHttpServletResponse call() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", ApiPath.V1 + "/customers");
        request.addHeader("Authorization", "Bearer " + TOKEN);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }

    @Test
    @DisplayName("Un compte actif est authentifie")
    void anEnabledAccountIsAuthenticated() throws Exception {
        when(userDetailService.loadUserByUsername(EMAIL)).thenReturn(principal(true));

        MockHttpServletResponse response = call();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }

    @Test
    @DisplayName("Un compte desactive est refuse aussitot, meme avec un jeton encore valide")
    void aDisabledAccountIsRejectedAtOnce() throws Exception {
        when(userDetailService.loadUserByUsername(EMAIL)).thenReturn(principal(false));

        MockHttpServletResponse response = call();

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Un compte supprime donne un 401, et plus un 500")
    void aDeletedAccountIsUnauthorized() throws Exception {
        when(userDetailService.loadUserByUsername(EMAIL)).thenThrow(new UsernameNotFoundException("absent"));

        MockHttpServletResponse response = call();

        assertThat(response.getStatus()).isEqualTo(401);
    }
}
