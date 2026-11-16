package com.sni.bokaticowork.core.audit.traffic;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Trafic de la minute en cours, en memoire (centre d'audit, lot A3) : par minute et par route
 * (gabarit, pas l'adresse brute), nombre, erreurs 4xx et 5xx, duree totale, maximale et histogramme
 * des durees. Les requetes lentes ou en erreur 5xx attendent ici leur ecriture une par une. Tout est
 * ecrit chaque minute par {@code OperationsWriter} : rien n'est garde au-dela.
 */
@Component
public class TrafficRecorder {

    /** Bornes hautes des tranches de duree (ms) ; une derniere tranche au-dela. */
    public static final long[] BOUNDS = {5, 10, 25, 50, 75, 100, 150, 200, 300, 500, 750, 1_000, 1_500, 2_000, 3_000, 5_000, 10_000, 30_000};
    static final int MAX_PENDING_REQUESTS = 2_000;

    private final Map<Key, Bucket> buckets = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<SlowRequest> requests = new ConcurrentLinkedQueue<>();
    private final AtomicInteger pending = new AtomicInteger();

    record Key(Instant minute, String method, String route) {
    }

    /** Cumul d'une route sur une minute. */
    public static final class Bucket {
        long count;
        long clientErrors;
        long serverErrors;
        long totalMs;
        long maxMs;
        final int[] histogram = new int[BOUNDS.length + 1];

        synchronized void add(int status, long durationMs) {
            count++;
            if (status >= 500) {
                serverErrors++;
            } else if (status >= 400) {
                clientErrors++;
            }
            totalMs += durationMs;
            maxMs = Math.max(maxMs, durationMs);
            histogram[slot(durationMs)]++;
        }
    }

    /** Ligne prete a ecrire. */
    public record MinuteRow(Instant minute, String method, String route, long count, long clientErrors, long serverErrors,
                            long totalMs, long maxMs, int[] histogram) {
    }

    /** Requete gardee une par une : lente ou en erreur serveur. */
    public record SlowRequest(Instant createdAt, String requestId, String method, String path, String route, int status,
                              long durationMs, String userEmail, String clientIp, String reason) {
    }

    public void record(Instant at, String method, String route, int status, long durationMs) {
        Key key = new Key(at.truncatedTo(ChronoUnit.MINUTES), method, route);
        buckets.computeIfAbsent(key, k -> new Bucket()).add(status, durationMs);
    }

    public void keep(SlowRequest request) {
        if (pending.incrementAndGet() <= MAX_PENDING_REQUESTS) {
            requests.add(request);
        } else {
            pending.decrementAndGet();
        }
    }

    /** Minutes terminees (avant {@code before}) ; toutes si {@code before} est nul (arret). */
    public List<MinuteRow> drainMinutes(Instant before) {
        List<MinuteRow> rows = new ArrayList<>();
        Iterator<Map.Entry<Key, Bucket>> iterator = buckets.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Key, Bucket> entry = iterator.next();
            Key key = entry.getKey();
            if (before != null && !key.minute().isBefore(before)) {
                continue;
            }
            iterator.remove();
            Bucket bucket = entry.getValue();
            synchronized (bucket) {
                rows.add(new MinuteRow(key.minute(), key.method(), key.route(), bucket.count, bucket.clientErrors,
                        bucket.serverErrors, bucket.totalMs, bucket.maxMs, bucket.histogram.clone()));
            }
        }
        return rows;
    }

    public List<SlowRequest> drainRequests() {
        List<SlowRequest> drained = new ArrayList<>();
        SlowRequest request;
        while ((request = requests.poll()) != null) {
            pending.decrementAndGet();
            drained.add(request);
        }
        return drained;
    }

    static int slot(long durationMs) {
        for (int i = 0; i < BOUNDS.length; i++) {
            if (durationMs <= BOUNDS[i]) {
                return i;
            }
        }
        return BOUNDS.length;
    }

    /** Percentile approche : borne haute de la tranche qui l'atteint ; la duree maximale au-dela de la derniere. */
    public static long percentile(long[] histogram, double quantile, long maxMs) {
        long total = 0;
        for (long value : histogram) {
            total += value;
        }
        if (total == 0) {
            return 0;
        }
        long target = (long) Math.ceil(quantile * total);
        long cumulative = 0;
        for (int i = 0; i < histogram.length; i++) {
            cumulative += histogram[i];
            if (cumulative >= target) {
                return i < BOUNDS.length ? Math.min(BOUNDS[i], maxMs) : maxMs;
            }
        }
        return maxMs;
    }
}
