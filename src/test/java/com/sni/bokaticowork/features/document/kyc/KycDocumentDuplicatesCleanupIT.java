package com.sni.bokaticowork.features.document.kyc;

import com.sni.bokaticowork.support.PostgresIntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Le nettoyage des pieces KYC en double ne retire que les doublons.
 *
 * <p>La migration s'est deja appliquee sur une base vide au demarrage du conteneur · elle y passe,
 * mais n'y trouve rien a nettoyer. On la rejoue ici sur des tables reduites aux colonnes qu'elle
 * lit, dans un schema a part, avec des doublons poses expres.</p>
 */
class KycDocumentDuplicatesCleanupIT extends PostgresIntegrationTestBase {

    @Autowired private DataSource dataSource;

    @Test
    @DisplayName("Un fichier en double et un verso devenu piece sont retires, le reste est garde")
    void removesOnlyDuplicates() throws Exception {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("""
                CREATE SCHEMA kyc_cleanup_test;
                SET LOCAL search_path TO kyc_cleanup_test;
                CREATE TABLE kyc_document (id BIGINT PRIMARY KEY, kyc_case_id BIGINT, document_id BIGINT NOT NULL, back_document_id BIGINT);
                CREATE TABLE kyc_verification (id BIGINT PRIMARY KEY, kyc_document_id BIGINT NOT NULL);
                CREATE TABLE kyc_document_ocr_result (id BIGINT PRIMARY KEY, kyc_document_id BIGINT NOT NULL UNIQUE);
                -- Dossier 1 : CNI envoyee une fois, enregistree deux fois (10 et 11), verso 300 devenu piece (12)
                INSERT INTO kyc_document VALUES (10, 1, 100, 300), (11, 1, 100, NULL), (12, 1, 300, NULL);
                -- Dossier 1 : une autre piece, seule · gardee
                INSERT INTO kyc_document VALUES (13, 1, 200, NULL);
                -- Dossier 2 (renouvellement) : le meme fichier 100 · garde, autre dossier
                INSERT INTO kyc_document VALUES (20, 2, 100, NULL);
                INSERT INTO kyc_verification VALUES (1, 11), (2, 13);
                INSERT INTO kyc_document_ocr_result VALUES (1, 11), (2, 10);
                """);

        String script = new String(new ClassPathResource("db/migration-prod/V257__kyc_document_duplicates_cleanup.sql")
                .getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        jdbc.execute(script);

        List<Long> remaining = jdbc.queryForList("SELECT id FROM kyc_document ORDER BY id", Long.class);
        assertThat(remaining).containsExactly(10L, 13L, 20L);
        // La verification du doublon est rattachee a la piece gardee.
        assertThat(jdbc.queryForObject("SELECT kyc_document_id FROM kyc_verification WHERE id = 1", Long.class)).isEqualTo(10L);
        assertThat(jdbc.queryForList("SELECT kyc_document_id FROM kyc_document_ocr_result ORDER BY id", Long.class))
                .containsExactly(10L);
    }

    @Autowired private com.sni.bokaticowork.features.document.kyc.repository.KycDocumentRepository kycDocumentRepository;

    @Test
    @DisplayName("Le numero court d'une piece est obligatoire, alimente par sa sequence, et recherchable")
    void shortIdIsFedByASequence() {
        java.util.Map<String, Object> column = new JdbcTemplate(dataSource).queryForMap("""
                SELECT is_nullable, column_default
                FROM information_schema.columns
                WHERE table_schema = 'public' AND table_name = 'kyc_document' AND column_name = 'short_id'
                """);
        assertThat(column.get("is_nullable")).isEqualTo("NO");
        assertThat(String.valueOf(column.get("column_default"))).contains("kyc_document_short_id_seq");
        assertThat(kycDocumentRepository.findByShortId(-1L)).isEmpty();
    }
}
