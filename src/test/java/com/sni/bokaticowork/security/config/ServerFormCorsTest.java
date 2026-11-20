package com.sni.bokaticowork.security.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.assertj.core.api.Assertions.assertThat;

/** Les formulaires servis par le serveur postent avec « Origin: null » : CORS ne doit pas s'y appliquer. */
class ServerFormCorsTest {

    private final CorsConfigurationSource source = SecurityConfig.corsSource("https://elleaose.com,https://*.elleaose.com");

    @Test
    void serverRenderedFormsAreOutsideCorsAndTheApiIsNot() {
        assertThat(source.getCorsConfiguration(new MockHttpServletRequest("POST", "/sni/api/v1/auth/super-admin-setup/form"))).isNull();
        assertThat(source.getCorsConfiguration(new MockHttpServletRequest("POST", "/sni/api/v1/auth/password-reset/form"))).isNull();
        assertThat(source.getCorsConfiguration(new MockHttpServletRequest("POST", "/sni/api/v1/auth/login"))).isNotNull();
        assertThat(source.getCorsConfiguration(new MockHttpServletRequest("POST", "/sni/api/v1/auth/password-reset/request"))).isNotNull();
    }
}
