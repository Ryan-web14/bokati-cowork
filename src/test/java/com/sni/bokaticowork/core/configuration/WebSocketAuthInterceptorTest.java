package com.sni.bokaticowork.core.configuration;

import com.sni.bokaticowork.core.audit.service.interfaces.UserSessionService;
import com.sni.bokaticowork.security.admin.user.model.UserPrincipal;
import com.sni.bokaticowork.security.admin.user.model.Users;
import com.sni.bokaticowork.security.service.tokenService.implementation.JWTService;
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
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Une connexion WebSocket passe les memes controles que l'API.
 *
 * <p>La signature etait verifiee · ce point etait correct. Le type du jeton ne l'etait pas, donc un
 * jeton de rafraichissement de sept jours ouvrait une session ; et la revocation n'etait pas
 * consultee, donc une session fermee par deconnexion restait acceptee.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WebSocketAuthInterceptorTest {

    private static final String TOKEN = "un.jeton.quelconque";

    @Mock private JWTService jwtService;
    @Mock private CustomUserDetailService userDetailService;
    @Mock private UserSessionService sessionService;
    @InjectMocks private WebSocketAuthInterceptor interceptor;

    @BeforeEach
    void setUp() {
        Users user = new Users();
        user.setEmail("joel@example.com");
        when(userDetailService.loadUserByUsername("joel@example.com")).thenReturn(new UserPrincipal(user));
        when(jwtService.isAccessToken(TOKEN)).thenReturn(true);
        when(jwtService.extractSessionId(TOKEN)).thenReturn("SES-1");
        when(jwtService.extractEmail(TOKEN)).thenReturn("joel@example.com");
        when(sessionService.isSessionRevoked("SES-1")).thenReturn(false);
    }

    private Message<byte[]> connect(String authorization) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        if (authorization != null) {
            accessor.setNativeHeader("Authorization", authorization);
        }
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Object userOf(Message<?> message) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        return accessor == null ? null : accessor.getUser();
    }

    @Test
    @DisplayName("Un jeton d'accès valide ouvre la session")
    void aValidAccessTokenOpensTheSession() {
        Message<byte[]> result = (Message<byte[]>) interceptor.preSend(connect("Bearer " + TOKEN), null);

        assertThat(userOf(result)).isNotNull();
    }

    @Test
    @DisplayName("Un jeton de rafraîchissement ne vaut pas une session · il dure sept jours")
    void aRefreshTokenCannotOpenASession() {
        when(jwtService.isAccessToken(TOKEN)).thenReturn(false);

        assertThatThrownBy(() -> interceptor.preSend(connect("Bearer " + TOKEN), null))
                .isInstanceOf(MessagingException.class)
                .hasMessageContaining("access token");
    }

    @Test
    @DisplayName("Une session révoquée est refusée · la déconnexion doit fermer le temps réel aussi")
    void aRevokedSessionIsRefused() {
        when(sessionService.isSessionRevoked("SES-1")).thenReturn(true);

        assertThatThrownBy(() -> interceptor.preSend(connect("Bearer " + TOKEN), null))
                .isInstanceOf(MessagingException.class)
                .hasMessageContaining("revoked");
    }

    @Test
    @DisplayName("Une signature invalide refuse la connexion · elle ne la laisse plus sans identité")
    void anInvalidSignatureIsRefused() {
        when(jwtService.isAccessToken(TOKEN)).thenThrow(new JwtException("signature invalide"));

        assertThatThrownBy(() -> interceptor.preSend(connect("Bearer " + TOKEN), null))
                .isInstanceOf(MessagingException.class);
    }

    @Test
    @DisplayName("Un compte introuvable refuse la connexion")
    void anUnknownAccountIsRefused() {
        when(userDetailService.loadUserByUsername(anyString()))
                .thenThrow(new IllegalStateException("compte introuvable"));

        assertThatThrownBy(() -> interceptor.preSend(connect("Bearer " + TOKEN), null))
                .isInstanceOf(MessagingException.class);
    }

    @Test
    @DisplayName("Sans en-tête, la connexion s'ouvre sans identité · aucun abonnement ne passera")
    void withoutAHeaderTheSessionStaysAnonymous() {
        Message<byte[]> result = (Message<byte[]>) interceptor.preSend(connect(null), null);

        assertThat(userOf(result)).isNull();
    }

    @Test
    @DisplayName("Un en-tête qui n'est pas un Bearer est ignoré")
    void aNonBearerHeaderIsIgnored() {
        Message<byte[]> result = (Message<byte[]>) interceptor.preSend(connect("Basic abcdef"), null);

        assertThat(userOf(result)).isNull();
    }

    @Test
    @DisplayName("Seul CONNECT est contrôlé")
    void onlyConnectIsInspected() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setLeaveMutable(true);
        Message<byte[]> send = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThatCode(() -> interceptor.preSend(send, null)).doesNotThrowAnyException();
    }
}
