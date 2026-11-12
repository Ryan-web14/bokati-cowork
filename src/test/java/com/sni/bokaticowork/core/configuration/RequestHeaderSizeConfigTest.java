package com.sni.bokaticowork.core.configuration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.unit.DataSize;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Le jeton d'acces d'un administrateur porte toutes ses permissions et depasse 8 Ko : la limite
 * d'en-tete de Tomcat doit rester au-dessus, sinon chaque appel authentifie repond 400.
 */
class RequestHeaderSizeConfigTest {

    @Test
    @DisplayName("La limite d'en-tete des requetes est de 64 Ko au moins")
    void headerLimitFitsAnAdministratorToken() throws Exception {
        StandardEnvironment environment = new StandardEnvironment();
        new YamlPropertySourceLoader().load("application", new ClassPathResource("application.yml"))
                .forEach(source -> environment.getPropertySources().addLast(source));
        DataSize size = Binder.get(environment).bind("server.max-http-request-header-size", DataSize.class).get();
        assertThat(size.toKilobytes()).isGreaterThanOrEqualTo(64);
    }
}
