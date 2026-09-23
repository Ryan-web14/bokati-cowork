package com.sni.bokaticowork.features.payment.selfservice;

import com.sni.bokaticowork.features.payment.enums.PaymentChannel;
import com.sni.bokaticowork.features.payment.service.support.PaymentOriginResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Qui paie · on le lit dans la session, on ne le demande a personne.
 *
 * <p>Un parametre de plus sur chaque methode de paiement aurait ete oublie quelque part, et c'est
 * precisement la qu'on aurait voulu prevenir la caisse.</p>
 */
class PaymentOriginResolverTest {

    private final PaymentOriginResolver resolver = new PaymentOriginResolver();

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(String name, String... roles) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                name, "n/a", java.util.Arrays.stream(roles).map(SimpleGrantedAuthority::new).toList()));
    }

    @Test
    @DisplayName("Un membre qui paie pour lui-meme est du libre-service")
    void aMemberIsSelfService() {
        authenticate("joel@example.com", "ROLE_MEMBER");
        assertThat(resolver.current()).isEqualTo(PaymentChannel.SELF_SERVICE);
        assertThat(resolver.currentActor()).isEqualTo("joel@example.com");
    }

    @Test
    @DisplayName("Un caissier opere au nom de la maison")
    void aCashierIsBackOffice() {
        authenticate("caisse@elleaose.com", "ROLE_CASHIER");
        assertThat(resolver.current()).isEqualTo(PaymentChannel.BACK_OFFICE);
    }

    @Test
    @DisplayName("Un agent qui porte aussi un role client reste un agent")
    void anyBackOfficeRoleWins() {
        authenticate("admin@elleaose.com", "ROLE_MEMBER", "ROLE_ADMIN");
        assertThat(resolver.current()).isEqualTo(PaymentChannel.BACK_OFFICE);
    }

    @Test
    @DisplayName("Sans session · worker, rappel d'operateur, reprise de l'outbox")
    void noSessionIsSystem() {
        SecurityContextHolder.clearContext();
        assertThat(resolver.current()).isEqualTo(PaymentChannel.SYSTEM);
        assertThat(resolver.currentActor()).isNull();
    }

    @Test
    @DisplayName("Une route publique porte une authentification anonyme · ce n'est pas un client")
    void anonymousIsSystem() {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
        assertThat(resolver.current()).isEqualTo(PaymentChannel.SYSTEM);
        assertThat(resolver.currentActor()).isNull();
    }
}
