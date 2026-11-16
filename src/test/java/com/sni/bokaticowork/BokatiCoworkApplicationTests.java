package com.sni.bokaticowork;

import com.sni.bokaticowork.core.async.SchedulingConfig;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.impl.SequenceEngineStartupValidator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * L'application complete demarre avec la configuration de production.
 *
 * <p>Sans base ni service, comme l'entrainement du cache de demarrage au build Heroku
 * (tools/heroku/preparer-demarrage.sh, memes reglages) : base et services a une adresse fermee,
 * Flyway et validation du schema coupes, taches planifiees coupees, secrets aleatoires. Le test
 * attrape ce qu'aucun test unitaire ne voit : conflit de noms de beans (panne de production du
 * 15/11/2026), dependance manquante, propriete obligatoire absente, configuration de production
 * invalide. Pas de Docker : il tourne dans la suite normale.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, args = {
        "--bokati.scheduling.enabled=false",
        "--spring.flyway.enabled=false",
        "--spring.jpa.hibernate.ddl-auto=none",
        "--spring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=false",
        "--spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
        "--spring.datasource.hikari.initialization-fail-timeout=-1",
        "--spring.datasource.hikari.connection-timeout=250",
        "--JDBC_DATABASE_URL=jdbc:postgresql://127.0.0.1:1/demarrage",
        "--JDBC_DATABASE_USERNAME=demarrage",
        "--JDBC_DATABASE_PASSWORD=demarrage",
        "--CLOUDAMQP_URL=amqp://demarrage:demarrage@127.0.0.1:1/demarrage",
        "--REDIS_URL=redis://127.0.0.1:1",
        "--APP_BASE_URL=https://demarrage.invalid",
        "--MICROSOFT_GRAPH_CLIENT_ID=demarrage",
        "--MICROSOFT_GRAPH_CLIENT_SECRET=demarrage",
        "--MICROSOFT_GRAPH_TENANT_ID=demarrage",
        "--MINIO_ENDPOINT=http://127.0.0.1:1",
        "--MINIO_BUCKET=demarrage",
        "--MINIO_ACCESS_KEY=demarrage",
        "--MINIO_SECRET_KEY=demarrage",
        "--INFOBIP_WHATSAPP_ENABLED=false",
})
@ActiveProfiles("prod")
class BokatiCoworkApplicationTests {

    static {
        // Secrets de test tires au hasard, jamais ecrits dans le depot.
        System.setProperty("APP_JWT_SECRET", secret());
        System.setProperty("TOKEN_HASH_SECRET", secret());
        System.setProperty("BILLING_FISCAL_SIGNING_KEY", secret());
    }

    @Autowired
    private ApplicationContext context;

    /** Seul traitement de demarrage qui lit la base (verification des sequences) : sans base, il est remplace. */
    @MockitoBean
    private SequenceEngineStartupValidator sequenceEngineStartupValidator;

    private static String secret() {
        return UUID.randomUUID() + "-" + UUID.randomUUID();
    }

    @Test
    void theApplicationStartsWithTheProductionConfiguration() {
        assertThat(context.getBeanDefinitionCount()).isGreaterThan(1_000);
        // Planification coupee : le reglage est bien lu.
        assertThat(context.getBeanNamesForType(SchedulingConfig.class)).isEmpty();
    }
}
