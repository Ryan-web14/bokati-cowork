package com.sni.bokaticowork.core.configuration;

import com.sni.bokaticowork.core.audit.service.interfaces.UserSessionService;
import com.sni.bokaticowork.security.admin.user.model.UserPrincipal;
import com.sni.bokaticowork.security.service.tokenService.implementation.JWTService;
import com.sni.bokaticowork.security.service.user.CustomUserDetailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * L'ouverture d'une connexion WebSocket passe les memes controles que l'API.
 *
 * <p>La signature et l'expiration du jeton etaient bien verifiees · ce point etait correct. Deux
 * autres ne l'etaient pas :</p>
 * <ul>
 *   <li>le <b>type</b> du jeton · un jeton de rafraichissement, valable sept jours, ou un jeton de
 *       verification d'adresse ouvrait une session authentifiee ;</li>
 *   <li>la <b>revocation</b> · une session fermee par deconnexion restait acceptee, parce que
 *       {@code /ws/**} est exclu du filtre HTTP et que personne ne consultait le registre des
 *       sessions ici.</li>
 * </ul>
 *
 * <p>Et un jeton invalide laissait simplement la connexion sans identite au lieu de la refuser ·
 * ce qui, avant le durcissement des abonnements, ouvrait les files {@code /queue/}. Une connexion
 * qui presente un jeton inacceptable est desormais rejetee.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JWTService jwtService;
    private final CustomUserDetailService userDetailService;
    private final UserSessionService sessionService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() != StompCommand.CONNECT) {
            return message;
        }

        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            // Pas de jeton · la connexion s'ouvre sans identite, et aucun abonnement ne passera.
            log.debug("CONNECT WebSocket sans en-tete Authorization");
            return message;
        }

        String token = authHeader.substring(BEARER_PREFIX.length());
        try {
            if (!jwtService.isAccessToken(token)) {
                throw new MessagingException("Only an access token can open a WebSocket session");
            }
            String sessionId = jwtService.extractSessionId(token);
            if (sessionService.isSessionRevoked(sessionId)) {
                throw new MessagingException("Session revoked");
            }
            String email = jwtService.extractEmail(token);
            if (email == null) {
                throw new MessagingException("Token carries no subject");
            }
            UserPrincipal principal = (UserPrincipal) userDetailService.loadUserByUsername(email);
            accessor.setUser(new UsernamePasswordAuthenticationToken(
                    principal, null, principal.getAuthorities()));
        } catch (MessagingException ex) {
            log.warn("CONNECT WebSocket refuse · {}", ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            // Signature invalide, jeton expire, compte introuvable · on refuse, on ne poursuit pas
            // sans identite.
            log.warn("CONNECT WebSocket refuse · {}", ex.getMessage());
            throw new MessagingException("WebSocket authentication failed", ex);
        }

        return message;
    }
}
