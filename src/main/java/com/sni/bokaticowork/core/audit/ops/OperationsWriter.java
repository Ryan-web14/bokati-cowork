package com.sni.bokaticowork.core.audit.ops;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Ecrit chaque minute, par lots, ce que le centre d'audit a garde en memoire (lot A4) : etat de
 * chaque tache planifiee et executions gardees une par une. Tourne sur chaque instance, hors de la
 * planification reservee au dyno designe ; a l'arret, une derniere ecriture.
 */
@Slf4j
@Component
public class OperationsWriter implements SmartInitializingSingleton {

    static final Duration PERIOD = Duration.ofMinutes(1);

    private final JdbcTemplate jdbc;
    private final TaskScheduler scheduler;
    private final String instance;

    public OperationsWriter(JdbcTemplate jdbc, @Qualifier("taskScheduler") TaskScheduler scheduler,
                            @Value("${DYNO:local}") String instance) {
        this.jdbc = jdbc;
        this.scheduler = scheduler;
        this.instance = instance;
    }

    @Override
    public void afterSingletonsInstantiated() {
        scheduler.scheduleWithFixedDelay(this::flush, Instant.now().plus(PERIOD), PERIOD);
    }

    @jakarta.annotation.PreDestroy
    void flushOnShutdown() {
        flush();
    }

    void flush() {
        ScheduledRuns.Drained drained = ScheduledRuns.drain();
        if (drained.totals().isEmpty() && drained.runs().isEmpty()) {
            return;
        }
        try {
            writeTotals(drained.totals());
            writeRuns(drained.runs());
        } catch (RuntimeException exception) {
            log.warn("Centre d'audit : executions des taches non ecrites ({} taches, {} executions) : {}",
                    drained.totals().size(), drained.runs().size(), exception.getMessage());
        }
    }

    private void writeTotals(List<ScheduledRuns.TaskTotals> totals) {
        if (totals.isEmpty()) {
            return;
        }
        jdbc.batchUpdate("""
                INSERT INTO scheduled_task_status (task_name, last_started_at, last_finished_at, last_duration_ms, last_outcome,
                                                   last_error, last_failure_at, run_count, failure_count, consecutive_failures,
                                                   total_duration_ms, max_duration_ms, instance, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now())
                ON CONFLICT (task_name) DO UPDATE SET
                    last_started_at = EXCLUDED.last_started_at,
                    last_finished_at = EXCLUDED.last_finished_at,
                    last_duration_ms = EXCLUDED.last_duration_ms,
                    last_outcome = EXCLUDED.last_outcome,
                    last_error = coalesce(EXCLUDED.last_error, scheduled_task_status.last_error),
                    last_failure_at = coalesce(EXCLUDED.last_failure_at, scheduled_task_status.last_failure_at),
                    run_count = scheduled_task_status.run_count + EXCLUDED.run_count,
                    failure_count = scheduled_task_status.failure_count + EXCLUDED.failure_count,
                    consecutive_failures = CASE WHEN ? THEN EXCLUDED.consecutive_failures
                                                ELSE scheduled_task_status.consecutive_failures + EXCLUDED.consecutive_failures END,
                    total_duration_ms = scheduled_task_status.total_duration_ms + EXCLUDED.total_duration_ms,
                    max_duration_ms = greatest(scheduled_task_status.max_duration_ms, EXCLUDED.max_duration_ms),
                    instance = EXCLUDED.instance,
                    updated_at = now()
                """, totals, totals.size(), (statement, t) -> {
            statement.setString(1, t.task());
            statement.setTimestamp(2, timestamp(t.lastStartedAt()));
            statement.setTimestamp(3, timestamp(t.lastFinishedAt()));
            statement.setLong(4, t.lastDurationMs());
            statement.setString(5, t.lastOutcome());
            statement.setString(6, t.lastError());
            statement.setTimestamp(7, timestamp(t.lastFailureAt()));
            statement.setLong(8, t.runs());
            statement.setLong(9, t.failures());
            statement.setInt(10, t.consecutiveFailures());
            statement.setLong(11, t.totalMs());
            statement.setLong(12, t.maxMs());
            statement.setString(13, instance);
            statement.setBoolean(14, t.resetConsecutive());
        });
    }

    private void writeRuns(List<ScheduledRuns.Run> runs) {
        if (runs.isEmpty()) {
            return;
        }
        jdbc.batchUpdate("""
                INSERT INTO scheduled_task_run (task_name, started_at, finished_at, duration_ms, outcome, error, reason, instance)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, runs, runs.size(), (statement, run) -> {
            statement.setString(1, run.task());
            statement.setTimestamp(2, timestamp(run.startedAt()));
            statement.setTimestamp(3, timestamp(run.finishedAt()));
            statement.setLong(4, run.durationMs());
            statement.setString(5, run.outcome());
            statement.setString(6, run.error());
            statement.setString(7, run.reason());
            statement.setString(8, instance);
        });
    }

    private static Timestamp timestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }
}
