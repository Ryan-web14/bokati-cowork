package com.sni.bokaticowork.core.configuration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Un client ne doit pas pouvoir ecouter la caisse.
 *
 * <p>Le seul controle etait d'etre authentifie, et uniquement pour {@code /topic/}. N'importe quel
 * membre du portail s'abonnait donc a {@code /topic/admin/alerts} avec son propre jeton et
 * recevait en direct « untel a regle tant depuis son espace », les alertes de caisse et les
 * evenements de securite des portefeuilles · le flux de paiement nominatif de toute la maison. Les
 * files {@code /queue/} n'exigeaient meme pas d'identite.</p>
 */
class WebSocketSubscriptionInterceptorTest {

    private final WebSocketSubscriptionInterceptor interceptor = new WebSocketSubscriptionInterceptor();

    private Authentication member() {
        return new UsernamePasswordAuthenticationToken("joel@example.com", null,
                List.of(new SimpleGrantedAuthority("ROLE_MEMBER")));
    }

    private Authentication cashier() {
        return new UsernamePasswordAuthenticationToken("caisse@elleaose.com", null,
                List.of(new SimpleGrantedAuthority("ROLE_CASHIER")));
    }

    private Message<byte[]> subscribe(Authentication user, String destination) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination(destination);
        accessor.setUser(user);
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    // -----------------------------------------------------------------------------------------
    // Les diffusions internes
    // -----------------------------------------------------------------------------------------

    @ParameterizedTest
    @DisplayName("Un membre du portail ne s'abonne à aucune diffusion interne")
    @ValueSource(strings = {
            "/topic/admin/alerts",
            "/topic/inventory/alerts",
            "/topic/support/tickets/TCK-1"
    })
    void aMemberCannotListenToInternalBroadcasts(String destination) {
        assertThatThrownBy(() -> interceptor.preSend(subscribe(member(), destination), null))
                .isInstanceOf(MessagingException.class)
                .hasMessageContaining(destination);
    }

    @ParameterizedTest
    @DisplayName("Le personnel s'abonne aux diffusions internes")
    @ValueSource(strings = {
            "/topic/admin/alerts",
            "/topic/inventory/alerts",
            "/topic/support/tickets/TCK-1"
    })
    void staffCanListenToInternalBroadcasts(String destination) {
        assertThatCode(() -> interceptor.preSend(subscribe(cashier(), destination), null))
                .doesNotThrowAnyException();
    }

    // -----------------------------------------------------------------------------------------
    // Ce qui reste ouvert à tout compte
    // -----------------------------------------------------------------------------------------

    @ParameterizedTest
    @DisplayName("Un membre s'abonne à ses propres files · elles sont isolées par principal")
    @ValueSource(strings = {
            "/user/queue/notifications",
            "/user/queue/notifications/unread-count",
            "/queue/notifications"
    })
    void aMemberKeepsItsOwnQueues(String destination) {
        assertThatCode(() -> interceptor.preSend(subscribe(member(), destination), null))
                .doesNotThrowAnyException();
    }

    // -----------------------------------------------------------------------------------------
    // Sans identité
    // -----------------------------------------------------------------------------------------

    @ParameterizedTest
    @DisplayName("Sans identité, aucun abonnement · les files aussi, qui ne demandaient rien")
    @ValueSource(strings = {
            "/topic/admin/alerts",
            "/topic/anything",
            "/queue/notifications",
            "/user/queue/notifications"
    })
    void noIdentityNoSubscription(String destination) {
        assertThatThrownBy(() -> interceptor.preSend(subscribe(null, destination), null))
                .isInstanceOf(MessagingException.class);
    }

    @Test
    @DisplayName("Une authentification non authentifiée ne passe pas")
    void anUnauthenticatedTokenIsRefused() {
        UsernamePasswordAuthenticationToken anonymous =
                new UsernamePasswordAuthenticationToken("qui", "que");
        assertThat(anonymous.isAuthenticated()).isFalse();

        assertThatThrownBy(() -> interceptor.preSend(subscribe(anonymous, "/topic/anything"), null))
                .isInstanceOf(MessagingException.class);
    }

    // -----------------------------------------------------------------------------------------
    // Ce que l'intercepteur ne regarde pas
    // -----------------------------------------------------------------------------------------

    @Test
    @DisplayName("Seul SUBSCRIBE est contrôlé · un envoi passe sans examen")
    void onlySubscribeIsInspected() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setDestination("/topic/admin/alerts");
        accessor.setLeaveMutable(true);
        Message<byte[]> send = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThatCode(() -> interceptor.preSend(send, null)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("La règle de destination se lit seule")
    void theDestinationRuleReadsOnItsOwn() {
        assertThat(WebSocketDestinations.isAdminOnly("/topic/admin/alerts")).isTrue();
        assertThat(WebSocketDestinations.isAdminOnly("/topic/inventory/alerts")).isTrue();
        assertThat(WebSocketDestinations.isAdminOnly("/topic/support/tickets/1")).isTrue();
        assertThat(WebSocketDestinations.isAdminOnly("/user/queue/notifications")).isFalse();
        assertThat(WebSocketDestinations.isAdminOnly(null)).isFalse();
    }
}
