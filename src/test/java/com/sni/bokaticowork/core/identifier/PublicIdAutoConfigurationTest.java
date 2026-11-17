package com.sni.bokaticowork.core.identifier;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

/** Le module des identifiants est bien pris par le JsonMapper que Spring Boot donne a Spring MVC. */
class PublicIdAutoConfigurationTest {

    public record Row(Long id, Long accountId) {
    }

    @Test
    void theBootJsonMapperWritesIdsAsCodes() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class))
                .withBean(PublicIdJacksonModule.class)
                .run(context -> {
                    String json = context.getBean(JsonMapper.class).writeValueAsString(new Row(7_319_298_661_696_688_123L, 12L));
                    assertThat(json).contains("\"id\":\"ROW-").contains("\"accountId\":\"ACC-");
                });
    }
}
