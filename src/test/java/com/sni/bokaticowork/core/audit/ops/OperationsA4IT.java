package com.sni.bokaticowork.core.audit.ops;

import com.sni.bokaticowork.support.PostgresIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Lot A4 sur une vraie base : etat des taches cumule d'une ecriture a l'autre, executions et erreurs
 * serveur gardees puis relues, sante calculee. La migration passe la validation du schema.
 */
@Import({OperationsService.class})
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=validate")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class OperationsA4IT extends PostgresIntegrationTestBase {

    @Autowired private JdbcTemplate jdbc;
    @Autowired private OperationsService operations;

    @Test
    void taskTotalsAccumulateAndFailuresAreKept() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.initialize();
        OperationsWriter writer = new OperationsWriter(jdbc, scheduler, "web.1");
        try {
            ScheduledRuns.drain();
            run("DunningPolicyWorker", null);
            run("DunningPolicyWorker", new IllegalStateException("base indisponible"));
            writer.flush();
            run("DunningPolicyWorker", new IllegalStateException("base indisponible"));
            writer.flush();

            OperationsService.TaskStatus status = operations.tasks().stream()
                    .filter(t -> t.taskName().equals("DunningPolicyWorker")).findFirst().orElseThrow();
            assertThat(status.runCount()).isEqualTo(3);
            assertThat(status.failureCount()).isEqualTo(2);
            assertThat(status.consecutiveFailures()).isEqualTo(2);
            assertThat(status.lastOutcome()).isEqualTo("FAILURE");
            assertThat(status.lastError()).isEqualTo("IllegalStateException: base indisponible");
            assertThat(operations.runs("DunningPolicyWorker", "failure", null, null, 0, 10).getTotalElements()).isEqualTo(2);

            run("DunningPolicyWorker", null);
            writer.flush();
            assertThat(operations.tasks().stream().filter(t -> t.taskName().equals("DunningPolicyWorker")).findFirst()
                    .orElseThrow().consecutiveFailures()).isZero();

            Map<String, Object> health = operations.health();
            assertThat(health).containsKeys("status", "outbox", "integrations", "scheduledTasks", "errors");
        } finally {
            scheduler.shutdown();
            jdbc.update("DELETE FROM scheduled_task_run WHERE task_name = 'DunningPolicyWorker'");
            jdbc.update("DELETE FROM scheduled_task_status WHERE task_name = 'DunningPolicyWorker'");
        }
    }

    @Test
    void serverErrorsAreKeptWithTheirTraceAndThrottled() {
        AppErrors errors = new AppErrors(jdbc, Runnable::run);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/sni/api/v1/billing/documents/A4-TEST");
        long before = count();
        RuntimeException failure = new RuntimeException("echec", new IllegalArgumentException("cause profonde"));
        errors.record(500, "INTERNAL_SERVER_ERROR", "trace-a4", request, failure);
        errors.record(500, "INTERNAL_SERVER_ERROR", "trace-a4-bis", request, failure);
        assertThat(count()).isEqualTo(before + 1);

        OperationsService.AppError listed = operations.errors(Instant.now().minusSeconds(60), null, "A4-TEST", null, null, 0, 10)
                .getContent().getFirst();
        assertThat(listed.stackTrace()).isNull();
        assertThat(listed.rootCause()).isEqualTo("IllegalArgumentException: cause profonde");
        OperationsService.AppError detail = operations.error(listed.id());
        assertThat(detail.stackTrace()).contains("RuntimeException: echec");
        jdbc.update("DELETE FROM app_error WHERE path LIKE '%A4-TEST'");
    }

    private long count() {
        return jdbc.queryForObject("SELECT count(*) FROM app_error", Long.class);
    }

    private static void run(String task, RuntimeException failure) {
        ScheduledRuns.begin(task);
        if (failure != null) {
            ScheduledRuns.fail(failure);
        }
        ScheduledRuns.end();
    }

}
