package com.sni.bokaticowork.features.ressource.repository;

import com.sni.bokaticowork.features.ressource.repository.repo.ResourcePricingRuleRepository;
import com.sni.bokaticowork.support.PostgresIntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * La migration du numero court tient sur le schema de production.
 */
class PricingRuleShortIdIT extends PostgresIntegrationTestBase {

    @Autowired private ResourcePricingRuleRepository repository;
    @Autowired private DataSource dataSource;

    @Test
    @DisplayName("La colonne short_id est obligatoire et alimentee par sa sequence")
    void shortIdColumnIsFedByASequence() {
        Map<String, Object> column = new JdbcTemplate(dataSource).queryForMap("""
                SELECT is_nullable, column_default
                FROM information_schema.columns
                WHERE table_name = 'resource_pricing_rule' AND column_name = 'short_id'
                """);

        assertThat(column.get("is_nullable")).isEqualTo("NO");
        assertThat(String.valueOf(column.get("column_default"))).contains("resource_pricing_rule_short_id_seq");
    }

    @Test
    @DisplayName("La recherche par numero court s'execute")
    void findByShortIdRuns() {
        assertThat(repository.findByShortId(-1L)).isEmpty();
    }
}
