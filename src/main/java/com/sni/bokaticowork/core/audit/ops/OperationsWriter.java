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
    private final com.sni.bokaticowork.core.audit.traffic.TrafficRecorder traffic;
    private volatile boolean stopping;

    public OperationsWriter(JdbcTemplate jdbc, @Qualifier("taskScheduler") TaskScheduler scheduler,
                            @Value("${DYNO:local}") String instance,
                            com.sni.bokaticowork.core.audit.traffic.TrafficRecorder traffic) {
        this.jdbc = jdbc;
        this.scheduler = scheduler;
        this.instance = instance;
        this.traffic = traffic;
    }

    @Override
    public void afterSingletonsInstantiated() {
        scheduler.scheduleWithFixedDelay(this::flush, Instant.now().plus(PERIOD), PERIOD);
    }

    @jakarta.annotation.PreDestroy
    void flushOnShutdown() {
        stopping = true;
        flush();
    }

    void flush() {
        ScheduledRuns.Drained drained = ScheduledRuns.drain();
        if (!drained.totals().isEmpty() || !drained.runs().isEmpty()) {
            try {
                writeTotals(drained.totals());
                writeRuns(drained.runs());
            } catch (RuntimeException exception) {
                log.warn("Centre d'audit : executions des taches non ecrites ({} taches, {} executions) : {}",
                        drained.totals().size(), drained.runs().size(), exception.getMessage());
            }
        }
        // Trafic : minutes terminees seulement, sauf a l'arret.
        List<com.sni.bokaticowork.core.audit.traffic.TrafficRecorder.MinuteRow> minutes =
                traffic.drainMinutes(stopping ? null : Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MINUTES));
        List<com.sni.bokaticowork.core.audit.traffic.TrafficRecorder.SlowRequest> requests = traffic.drainRequests();
        try {
            writeTraffic(minutes);
            writeRequests(requests);
        } catch (RuntimeException exception) {
            log.warn("Centre d'audit : trafic non ecrit ({} lignes, {} requetes) : {}", minutes.size(), requests.size(),
                    exception.getMessage());
        }
    }

    private void writeTraffic(List<com.sni.bokaticowork.core.audit.traffic.TrafficRecorder.MinuteRow> rows) {
        if (rows.isEmpty()) {
            return;
        }
        jdbc.batchUpdate("""
                INSERT INTO traffic_minute (minute, http_method, route, request_count, client_errors, server_errors,
                                            total_ms, max_ms, histogram)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (minute, http_method, route) DO UPDATE SET
                    request_count = traffic_minute.request_count + EXCLUDED.request_count,
                    client_errors = traffic_minute.client_errors + EXCLUDED.client_errors,
                    server_errors = traffic_minute.server_errors + EXCLUDED.server_errors,
                    total_ms = traffic_minute.total_ms + EXCLUDED.total_ms,
                    max_ms = greatest(traffic_minute.max_ms, EXCLUDED.max_ms),
                    histogram = (SELECT array_agg(a + b ORDER BY i)
                                 FROM unnest(traffic_minute.histogram, EXCLUDED.histogram) WITH ORDINALITY AS u(a, b, i))
                """, rows, rows.size(), (statement, row) -> {
            statement.setTimestamp(1, timestamp(row.minute()));
            statement.setString(2, row.method());
            statement.setString(3, row.route());
            statement.setLong(4, row.count());
            statement.setLong(5, row.clientErrors());
            statement.setLong(6, row.serverErrors());
            statement.setLong(7, row.totalMs());
            statement.setLong(8, row.maxMs());
            Integer[] histogram = new Integer[row.histogram().length];
            for (int i = 0; i < histogram.length; i++) {
                histogram[i] = row.histogram()[i];
            }
            statement.setArray(9, statement.getConnection().createArrayOf("integer", histogram));
        });
    }

    private void writeRequests(List<com.sni.bokaticowork.core.audit.traffic.TrafficRecorder.SlowRequest> requests) {
        if (requests.isEmpty()) {
            return;
        }
        jdbc.batchUpdate("""
                INSERT INTO request_log (created_at, request_id, http_method, path, route, status, duration_ms, user_email,
                                         client_ip, reason, instance)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, requests, requests.size(), (statement, request) -> {
            statement.setTimestamp(1, timestamp(request.createdAt()));
            statement.setString(2, request.requestId());
            statement.setString(3, request.method());
            statement.setString(4, request.path());
            statement.setString(5, request.route());
            statement.setInt(6, request.status());
            statement.setLong(7, request.durationMs());
            statement.setString(8, request.userEmail());
            statement.setString(9, request.clientIp());
            statement.setString(10, request.reason());
            statement.setString(11, instance);
        });
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
