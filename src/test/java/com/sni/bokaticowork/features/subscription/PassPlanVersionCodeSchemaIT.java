package com.sni.bokaticowork.features.subscription;

import com.sni.bokaticowork.features.subscription.repository.PassPlanVersionRepository;
import com.sni.bokaticowork.support.PostgresIntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Le code de version tient sur le schema de production · obligatoire, unique, et rempli a partir du
 * plan comme le fait la migration pour les versions existantes.
 */
@org.springframework.test.context.TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=validate")
class PassPlanVersionCodeSchemaIT extends PostgresIntegrationTestBase {

    @Autowired private DataSource dataSource;
    @Autowired private PassPlanVersionRepository versions;

    @Test
    @DisplayName("La colonne est obligatoire et unique, aucune version n'en est privee, la recherche repond")
    void schema() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        assertThat(jdbc.queryForObject("""
                SELECT is_nullable FROM information_schema.columns
                WHERE table_name = 'pass_plan_version' AND column_name = 'version_code'
                """, String.class)).isEqualTo("NO");
        assertThat(jdbc.queryForList("SELECT indexname FROM pg_indexes WHERE tablename = 'pass_plan_version'", String.class))
                .contains("uk_pass_plan_version_code");
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM pass_plan_version v
                JOIN pass_plan p ON p.id = v.plan_id
                WHERE v.version_code <> p.code || '-V' || v.version_number
                """, Integer.class)).isZero();
        assertThat(versions.findByVersionCode("INCONNU-V1")).isEmpty();
    }
}
