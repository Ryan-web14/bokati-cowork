package com.sni.bokaticowork.security.ratelimit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * L'adresse lue ne doit pas etre celle que l'appelant s'est choisie.
 *
 * <p>C'etait le defaut : le plafond etait indexe sur la premiere valeur de
 * {@code X-Forwarded-For}, c'est-a-dire sur une chaine de caracteres que l'appelant ecrit
 * lui-meme. Une valeur differente a chaque requete donnait un compteur neuf a chaque requete,
 * et aucun plafond ne tenait · pas meme celui du login.</p>
 */
class ClientIpResolverTest {

    private ClientIpResolver resolverWith(int trustedProxyCount) {
        ClientIpResolver resolver = new ClientIpResolver();
        ReflectionTestUtils.setField(resolver, "trustedProxyCount", trustedProxyCount);
        return resolver;
    }

    private MockHttpServletRequest request(String remoteAddr, String forwarded) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(remoteAddr);
        if (forwarded != null) {
            request.addHeader("X-Forwarded-For", forwarded);
        }
        return request;
    }

    @Test
    @DisplayName("Le prefixe forge par l'appelant est ignore · c'est la faille corrigee")
    void forgedPrefixIsIgnored() {
        ClientIpResolver resolver = resolverWith(1);

        // L'attaquant est en 203.0.113.9 et prefixe ce qu'il veut. Le relais ajoute sa vraie
        // adresse a droite, et c'est elle qu'on lit.
        String first = resolver.resolve(request("10.0.0.1", "1.2.3.4, 203.0.113.9"));
        String second = resolver.resolve(request("10.0.0.1", "9.9.9.9, 203.0.113.9"));
        String third = resolver.resolve(request("10.0.0.1", "au-hasard, 203.0.113.9"));

        assertThat(first).isEqualTo("203.0.113.9");
        assertThat(second).isEqualTo("203.0.113.9");
        assertThat(third).isEqualTo("203.0.113.9");
    }

    @Test
    @DisplayName("Changer le prefixe ne change pas la cle de comptage")
    void counterKeyIsStableAcrossForgedPrefixes() {
        ClientIpResolver resolver = resolverWith(1);

        assertThat(resolver.resolve(request("10.0.0.1", "a, 203.0.113.9")))
                .isEqualTo(resolver.resolve(request("10.0.0.1", "b, c, d, 203.0.113.9")));
    }

    @Test
    @DisplayName("Sans en-tete, c'est l'adresse du pair direct")
    void fallsBackToRemoteAddress() {
        assertThat(resolverWith(1).resolve(request("198.51.100.4", null))).isEqualTo("198.51.100.4");
    }

    @Test
    @DisplayName("Zero relais de confiance ignore entierement l'en-tete")
    void zeroTrustedProxiesIgnoresHeader() {
        assertThat(resolverWith(0).resolve(request("198.51.100.4", "1.2.3.4, 5.6.7.8")))
                .isEqualTo("198.51.100.4");
    }

    @Test
    @DisplayName("Deux relais annonces lisent l'avant-derniere valeur")
    void twoTrustedProxiesReadSecondFromRight() {
        assertThat(resolverWith(2).resolve(request("10.0.0.1", "1.2.3.4, 203.0.113.9, 10.0.0.2")))
                .isEqualTo("203.0.113.9");
    }

    @Test
    @DisplayName("Une chaine plus courte que les relais annonces retombe sur le pair direct")
    void shorterChainThanDeclaredProxiesFallsBack() {
        // La configuration ne correspond pas a la realite · mieux vaut une adresse non
        // falsifiable qu'une position arbitraire dans la chaine.
        assertThat(ClientIpResolver.fromForwardedChain("1.2.3.4", 3, "10.0.0.1")).isEqualTo("10.0.0.1");
    }

    @Test
    @DisplayName("Une position vide dans la chaine retombe sur le pair direct")
    void blankEntryFallsBack() {
        assertThat(ClientIpResolver.fromForwardedChain("1.2.3.4,   ", 1, "10.0.0.1")).isEqualTo("10.0.0.1");
    }

    @Test
    @DisplayName("Une requete nulle rend une adresse inconnue plutot qu'une erreur")
    void nullRequestIsTolerated() {
        assertThat(resolverWith(1).resolve(null)).isEqualTo("unknown");
    }

    @Test
    @DisplayName("Un pair direct absent rend une adresse inconnue")
    void missingRemoteAddressIsNamed() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(null);
        assertThat(resolverWith(1).resolve(request)).isEqualTo("unknown");
    }
}
