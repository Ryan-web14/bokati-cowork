package com.sni.bokaticowork.core.settings.platform;

import com.sni.bokaticowork.core.audit.repository.SettingsAuditLogsRepository;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceOpeningHoursRepository;
import com.sni.bokaticowork.support.PostgresIntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.boot.logging.DeferredLogs;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Les parametres tiennent sur le schema de production, et leur chargement au demarrage lit bien la
 * table sans jamais empecher l'application de partir.
 */
@org.springframework.test.context.TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=validate")
class PlatformSettingsSchemaIT extends PostgresIntegrationTestBase {

    @Autowired private DataSource dataSource;
    @Autowired private PlatformSettingRepository settings;
    @Autowired private SettingsAuditLogsRepository audit;
    @Autowired private ResourceOpeningHoursRepository openingHours;
    @org.springframework.beans.factory.annotation.Value("${spring.datasource.username}") private String username;
    @org.springframework.beans.factory.annotation.Value("${spring.datasource.password}") private String password;

    @Test
    @DisplayName("Les tables et les requetes repondent")
    void schema() {
        settings.saveAndFlush(PlatformSetting.builder().key("bokati.marketplace.payment-window-minutes")
                .value("45").updatedBy("admin").updatedAt(Instant.now()).build());

        assertThat(settings.findById("bokati.marketplace.payment-window-minutes")).isPresent();
        assertThat(audit.findBySettingKeyOrderByChangedAtDesc("bokati.inconnu", PageRequest.of(0, 5))).isEmpty();
        assertThat(openingHours.count()).isZero();
    }

    @Test
    @org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    @DisplayName("Au demarrage, une valeur enregistree passe devant le fichier · jamais une cle hors catalogue ni un secret")
    void storedValuesOverrideTheFile() throws SQLException {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.update("INSERT INTO platform_setting (setting_key, value) VALUES ('bokati.marketplace.payment-window-minutes', '45'), "
                + "('spring.datasource.url', 'jdbc:postgresql://pirate/x'), ('app.security.jwt.secret', 'pirate')");
        try {
            MockEnvironment environment = new MockEnvironment();
            environment.setProperty("spring.datasource.url", jdbcUrl());
            environment.setProperty("spring.datasource.username", username);
            environment.setProperty("spring.datasource.password", password);
            environment.setProperty("bokati.marketplace.payment-window-minutes", "30");
            environment.setProperty("app.security.jwt.secret", "vrai-secret");

            new PlatformSettingsEnvironmentPostProcessor(new DeferredLogs()).postProcessEnvironment(environment, null);

            assertThat(environment.getProperty("bokati.marketplace.payment-window-minutes")).isEqualTo("45");
            assertThat(environment.getProperty("app.security.jwt.secret")).isEqualTo("vrai-secret");
            assertThat(environment.getProperty("spring.datasource.url")).isEqualTo(jdbcUrl());
        } finally {
            jdbc.update("DELETE FROM platform_setting");
        }
    }

    private String jdbcUrl() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            return connection.getMetaData().getURL();
        }
    }

    @Test
    @DisplayName("Une base injoignable laisse la configuration telle quelle")
    void unreachableDatabaseDoesNotStopTheStart() throws SQLException {
        MockEnvironment environment = new MockEnvironment();
        try (Connection connection = dataSource.getConnection()) {
            assertThat(connection.isValid(1)).isTrue();
        }
        environment.setProperty("spring.datasource.url", "jdbc:postgresql://127.0.0.1:1/absente");

        new PlatformSettingsEnvironmentPostProcessor(new DeferredLogs()).postProcessEnvironment(environment, null);

        assertThat(environment.getPropertySources().contains(PlatformSettingsEnvironmentPostProcessor.SOURCE_NAME)).isFalse();
    }
}
