package com.sni.bokaticowork.core.audit.ops;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Executions des taches planifiees (centre d'audit, lot A4), gardees en memoire jusqu'a l'ecriture
 * de la minute suivante ({@link OperationsWriter}).
 *
 * <p>Par tache : nombre d'executions, d'echecs, durees, derniere execution. Une execution est gardee
 * une par une quand elle echoue, quand elle est lente, ou quand la tache tourne rarement (rien depuis
 * dix minutes) : les taches quotidiennes et horaires ont ainsi toutes leurs executions, les taches de
 * quelques secondes seulement leurs echecs.</p>
 */
public final class ScheduledRuns {

    static final Duration SLOW = Duration.ofSeconds(30);
    static final Duration RARE = Duration.ofMinutes(10);
    static final int MAX_PENDING_RUNS = 2_000;

    /** Execution en cours sur ce fil. */
    private static final ThreadLocal<Current> CURRENT = new ThreadLocal<>();

    private static final Map<String, Totals> TOTALS = new ConcurrentHashMap<>();
    private static final Map<String, Instant> LAST_START = new ConcurrentHashMap<>();
    private static final ConcurrentLinkedQueue<Run> RUNS = new ConcurrentLinkedQueue<>();
    private static final AtomicInteger PENDING = new AtomicInteger();

    private ScheduledRuns() {
    }

    private static final class Current {
        final String task;
        final Instant startedAt;
        Throwable failure;

        Current(String task, Instant startedAt) {
            this.task = task;
            this.startedAt = startedAt;
        }
    }

    /** Une execution gardee une par une. */
    public record Run(String task, Instant startedAt, Instant finishedAt, long durationMs, String outcome, String error, String reason) {
    }

    /** Cumul d'une tache depuis la derniere ecriture. */
    static final class Totals {
        long runs;
        long failures;
        long totalMs;
        long maxMs;
        Instant lastStartedAt;
        Instant lastFinishedAt;
        long lastDurationMs;
        String lastOutcome;
        String lastError;
        Instant lastFailureAt;
        /** Echecs consecutifs : -1 si la derniere execution a reussi (remise a zero). */
        int consecutiveFailures;
        boolean resetConsecutive;
    }

    /** Snapshot d'une tache, pret a ecrire. */
    public record TaskTotals(String task, long runs, long failures, long totalMs, long maxMs, Instant lastStartedAt,
                             Instant lastFinishedAt, long lastDurationMs, String lastOutcome, String lastError,
                             Instant lastFailureAt, int consecutiveFailures, boolean resetConsecutive) {
    }

    static void begin(String task) {
        CURRENT.set(new Current(task, Instant.now()));
    }

    /** Appele par le gestionnaire d'erreurs du planificateur : l'execution en cours a echoue. */
    public static void fail(Throwable failure) {
        Current current = CURRENT.get();
        if (current != null && current.failure == null) {
            current.failure = failure;
        }
    }

    static void end() {
        Current current = CURRENT.get();
        CURRENT.remove();
        if (current == null) {
            return;
        }
        Instant finishedAt = Instant.now();
        long durationMs = Duration.between(current.startedAt, finishedAt).toMillis();
        boolean failed = current.failure != null;
        String error = failed ? describe(current.failure) : null;
        Instant previousStart = LAST_START.put(current.task, current.startedAt);

        Totals totals = TOTALS.computeIfAbsent(current.task, key -> new Totals());
        synchronized (totals) {
            totals.runs++;
            totals.totalMs += durationMs;
            totals.maxMs = Math.max(totals.maxMs, durationMs);
            totals.lastStartedAt = current.startedAt;
            totals.lastFinishedAt = finishedAt;
            totals.lastDurationMs = durationMs;
            totals.lastOutcome = failed ? "FAILURE" : "SUCCESS";
            if (failed) {
                totals.failures++;
                totals.lastError = error;
                totals.lastFailureAt = finishedAt;
                totals.consecutiveFailures++;
            } else {
                totals.consecutiveFailures = 0;
                totals.resetConsecutive = true;
            }
        }

        String reason = failed ? "FAILURE"
                : durationMs >= SLOW.toMillis() ? "SLOW"
                : previousStart == null || Duration.between(previousStart, current.startedAt).compareTo(RARE) >= 0 ? "PERIODIC"
                : null;
        if (reason != null && PENDING.incrementAndGet() <= MAX_PENDING_RUNS) {
            RUNS.add(new Run(current.task, current.startedAt, finishedAt, durationMs, totals.lastOutcome, error, reason));
        } else if (reason != null) {
            PENDING.decrementAndGet();
        }
    }

    /** Prend et remet a zero les cumuls et les executions en attente d'ecriture. */
    static Drained drain() {
        List<TaskTotals> totals = new ArrayList<>();
        for (Map.Entry<String, Totals> entry : TOTALS.entrySet()) {
            Totals value = entry.getValue();
            synchronized (value) {
                if (value.runs == 0) {
                    continue;
                }
                totals.add(new TaskTotals(entry.getKey(), value.runs, value.failures, value.totalMs, value.maxMs,
                        value.lastStartedAt, value.lastFinishedAt, value.lastDurationMs, value.lastOutcome, value.lastError,
                        value.lastFailureAt, value.consecutiveFailures, value.resetConsecutive));
                value.runs = 0;
                value.failures = 0;
                value.totalMs = 0;
                value.maxMs = 0;
                value.lastError = null;
                value.lastFailureAt = null;
                value.consecutiveFailures = 0;
                value.resetConsecutive = false;
            }
        }
        List<Run> runs = new ArrayList<>();
        Run run;
        while ((run = RUNS.poll()) != null) {
            PENDING.decrementAndGet();
            runs.add(run);
        }
        return new Drained(totals, runs);
    }

    record Drained(List<TaskTotals> totals, List<Run> runs) {
    }

    static String describe(Throwable failure) {
        String text = failure.getClass().getSimpleName() + (failure.getMessage() == null ? "" : ": " + failure.getMessage());
        return text.length() > 1000 ? text.substring(0, 1000) : text;
    }
}
