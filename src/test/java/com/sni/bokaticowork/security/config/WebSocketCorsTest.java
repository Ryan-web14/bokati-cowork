package com.sni.bokaticowork.security.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Le client SockJS envoie ses requetes avec les identifiants · sans cet accord, le navigateur rejette
 * {@code /ws/info} et le temps reel ne s'ouvre jamais depuis l'administration.
 */
class WebSocketCorsTest {

    private final CorsConfigurationSource source = SecurityConfig.corsSource("https://elleaose.com,https://*.elleaose.com");

    private CorsConfiguration configFor(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setServletPath(path);
        return source.getCorsConfiguration(request);
    }

    @Test
    @DisplayName("/ws autorise les identifiants pour les origines de la plateforme")
    void webSocketAllowsCredentials() {
        CorsConfiguration config = configFor("/ws/info");

        assertThat(config.getAllowCredentials()).isTrue();
        assertThat(config.checkOrigin("https://admin.elleaose.com")).isEqualTo("https://admin.elleaose.com");
        assertThat(config.checkOrigin("https://attaquant.example")).isNull();
    }

    @Test
    @DisplayName("Le reste de l'API continue sans identifiants")
    void apiStaysWithoutCredentials() {
        assertThat(configFor("/sni/api/v1/support/tickets").getAllowCredentials()).isFalse();
    }
}
