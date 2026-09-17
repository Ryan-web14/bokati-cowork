package com.sni.bokaticowork.support;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Socle des tests d'integration sur un PostgreSQL reel.
 *
 * <p>Couvre ce qu'un test Mockito ne peut pas voir : les declencheurs d'immuabilite, les requetes
 * natives, les contraintes et index, et le comportement sous concurrence.</p>
 *
 * <p>Le jeu complet des migrations du dossier <strong>production</strong> est applique, ce qui
 * verifie au passage que ces migrations reconstruisent un schema complet sur une base vierge.</p>
 *
 * <p>Le dossier de developpement n'est pas utilisable ici : six de ses migrations echouent sur une
 * base vierge, faute de reprendre des colonnes creees a l'epoque par la generation automatique de
 * schema. Les deux environnements existants ayant ete amorces depuis un schema preexistant, le
 * defaut n'est jamais apparu. Voir la note dans
 * docs/backend/inventory-lot0-verification.md.</p>
 *
 * <p>Ces tests exigent un demon Docker joignable. Ils portent le tag {@code integration} pour
 * pouvoir etre exclus d'une execution rapide.</p>
 */
@Tag("integration")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public abstract class PostgresIntegrationTestBase {

    /**
     * Conteneur partage par toutes les classes de test.
     *
     * <p>Demarre a la main dans un bloc statique plutot que via l'extension {@code @Testcontainers} :
     * celle-ci arrete le conteneur apres chaque classe, ce qui rejouerait l'integralite des
     * migrations a chaque fois. Ici il demarre une fois par execution et la JVM le nettoie a la
     * sortie via le conteneur de controle de Testcontainers.</p>
     */
    protected static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"))
                    .withDatabaseName("cowork_test_db")
                    .withUsername("cowork")
                    .withPassword("cowork");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");

        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration-prod");
        registry.add("spring.flyway.baseline-on-migrate", () -> "true");
        registry.add("spring.flyway.clean-disabled", () -> "true");

        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.jpa.properties.hibernate.default_schema", () -> "public");
        registry.add("spring.jpa.show-sql", () -> "false");
    }
}
