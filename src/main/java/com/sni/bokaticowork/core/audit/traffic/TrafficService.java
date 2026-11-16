package com.sni.bokaticowork.core.audit.traffic;

import com.sni.bokaticowork.core.audit.ops.OperationsService;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.sql.Array;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Trafic (centre d'audit, lot A3) : series dans le temps, repartition par route, requetes lentes ou en
 * erreur serveur. Les minutes restent 7 jours ({@code traffic_minute}) ; au-dela, le cumul horaire
 * ({@code traffic_hour}, 13 mois) prend le relais.
 */
@Slf4j
@Service
public class TrafficService {

    static final Duration MINUTE_RETENTION = Duration.ofDays(7);
    static final int MAX_POINTS = 2_000;

    private final JdbcTemplate jdbc;

    public TrafficService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record TrafficPoint(Instant period, long requests, long clientErrors, long serverErrors, long averageMs,
                               long p50Ms, long p95Ms, long maxMs) {
    }

    public record RouteTraffic(String method, String route, long requests, long clientErrors, long serverErrors,
                               long averageMs, long p50Ms, long p95Ms, long maxMs) {
    }

    public record RequestEntry(long id, Instant createdAt, String requestId, String method, String path, String route,
                               int status, long durationMs, String userEmail, String clientIp, String reason) {
    }

    /** Periode et source choisies pour une lecture. */
    record Window(Instant from, Instant to, String unit, String table, String column) {
    }

    // ── Cumul horaire ──────────────────────────────────────────────────────────

    /** Chaque heure : les trois dernieres heures terminees sont (re)cumulees, sans doublon. */
    @Scheduled(cron = "0 7 * * * *")
    public void rollupHours() {
        Instant end = Instant.now().truncatedTo(ChronoUnit.HOURS);
        rollup(end.minus(3, ChronoUnit.HOURS), end);
    }

    public int rollup(Instant from, Instant to) {
        return jdbc.update("""
                WITH h AS (
                    SELECT date_trunc('hour', minute) AS hour, http_method, route, i, sum(x)::int AS v
                    FROM traffic_minute, unnest(histogram) WITH ORDINALITY AS u(x, i)
                    WHERE minute >= ? AND minute < ?
                    GROUP BY 1, 2, 3, 4),
                ha AS (
                    SELECT hour, http_method, route, array_agg(v ORDER BY i) AS histogram FROM h GROUP BY 1, 2, 3),
                s AS (
                    SELECT date_trunc('hour', minute) AS hour, http_method, route, sum(request_count) AS n,
                           sum(client_errors) AS e4, sum(server_errors) AS e5, sum(total_ms) AS tot, max(max_ms) AS mx
                    FROM traffic_minute WHERE minute >= ? AND minute < ? GROUP BY 1, 2, 3)
                INSERT INTO traffic_hour (hour, http_method, route, request_count, client_errors, server_errors, total_ms, max_ms, histogram)
                SELECT s.hour, s.http_method, s.route, s.n, s.e4, s.e5, s.tot, s.mx, ha.histogram
                FROM s JOIN ha USING (hour, http_method, route)
                ON CONFLICT (hour, http_method, route) DO UPDATE SET
                    request_count = EXCLUDED.request_count, client_errors = EXCLUDED.client_errors,
                    server_errors = EXCLUDED.server_errors, total_ms = EXCLUDED.total_ms, max_ms = EXCLUDED.max_ms,
                    histogram = EXCLUDED.histogram
                """, ts(from), ts(to), ts(from), ts(to));
    }

    // ── Lectures ───────────────────────────────────────────────────────────────

    /** Serie dans le temps ; {@code granularity} MINUTE, HOUR ou DAY (choisie selon la periode si absente). */
    public List<TrafficPoint> series(Instant from, Instant to, String granularity, String route, String method) {
        Window window = window(from, to, granularity);
        long points = Duration.between(window.from(), window.to()).toSeconds() / switch (window.unit()) {
            case "minute" -> 60;
            case "hour" -> 3_600;
            default -> 86_400;
        };
        if (points > MAX_POINTS) {
            throw new BadRequestException("Trop de points (" + points + ") : choisir une granularite plus large");
        }
        OperationsService.Where where = filter(window, route, method);
        List<Object> args = new ArrayList<>();
        args.add(window.unit());
        args.addAll(List.of(where.args()));
        return jdbc.query(aggregate("date_trunc(?, " + window.column() + ") AS t", "t", window, where) + " ORDER BY t",
                (rs, i) -> {
                    long[] histogram = histogram(rs.getArray("hist"));
                    long n = rs.getLong("n");
                    long max = rs.getLong("mx");
                    return new TrafficPoint(rs.getTimestamp("t").toInstant(), n, rs.getLong("e4"), rs.getLong("e5"),
                            n == 0 ? 0 : rs.getLong("tot") / n, TrafficRecorder.percentile(histogram, 0.5, max),
                            TrafficRecorder.percentile(histogram, 0.95, max), max);
                }, args.toArray());
    }

    /** Routes de la periode, les plus demandees d'abord (100 au plus). */
    public List<RouteTraffic> routes(Instant from, Instant to, String sort) {
        Window window = window(from, to, null);
        OperationsService.Where where = filter(window, null, null);
        String order = switch (sort == null ? "requests" : sort.toLowerCase(Locale.ROOT)) {
            case "errors" -> "e5 DESC, e4 DESC, n DESC";
            case "slow" -> "mx DESC";
            default -> "n DESC";
        };
        return jdbc.query(aggregate("http_method, route", "http_method, route", window, where) + " ORDER BY " + order + " LIMIT 100",
                (rs, i) -> {
                    long[] histogram = histogram(rs.getArray("hist"));
                    long n = rs.getLong("n");
                    long max = rs.getLong("mx");
                    return new RouteTraffic(rs.getString("http_method"), rs.getString("route"), n, rs.getLong("e4"),
                            rs.getLong("e5"), n == 0 ? 0 : rs.getLong("tot") / n, TrafficRecorder.percentile(histogram, 0.5, max),
                            TrafficRecorder.percentile(histogram, 0.95, max), max);
                }, where.args());
    }

    /** Requetes gardees une par une (lentes, erreurs serveur). */
    public Page<RequestEntry> requests(Instant from, Instant to, String reason, String route, String user, String requestId,
                                       Integer minStatus, int page, int size) {
        OperationsService.Where where = new OperationsService.Where()
                .from("created_at", from)
                .to("created_at", to)
                .eqUpper("reason", reason)
                .like("route", route)
                .like("user_email", user)
                .eq("request_id", requestId);
        if (minStatus != null) {
            where.add("status >= ?", minStatus);
        }
        return OperationsService.page(jdbc, "SELECT * FROM request_log", where, "created_at DESC, id DESC", page, size,
                (rs, i) -> new RequestEntry(rs.getLong("id"), rs.getTimestamp("created_at").toInstant(), rs.getString("request_id"),
                        rs.getString("http_method"), rs.getString("path"), rs.getString("route"), rs.getInt("status"),
                        rs.getLong("duration_ms"), rs.getString("user_email"), rs.getString("client_ip"), rs.getString("reason")));
    }

    // ── Outils ─────────────────────────────────────────────────────────────────

    /** Periode par defaut : les dernieres 24 heures ; minutes si elles sont encore gardees, sinon heures. */
    Window window(Instant from, Instant to, String granularity) {
        Instant end = to == null ? Instant.now() : to;
        Instant start = from == null ? end.minus(1, ChronoUnit.DAYS) : from;
        if (!start.isBefore(end)) {
            throw new BadRequestException("from doit preceder to");
        }
        Duration length = Duration.between(start, end);
        String unit = StringUtils.hasText(granularity) ? granularity.trim().toLowerCase(Locale.ROOT)
                : length.compareTo(Duration.ofHours(6)) <= 0 ? "minute"
                : length.compareTo(Duration.ofDays(7)) <= 0 ? "hour" : "day";
        if (!List.of("minute", "hour", "day").contains(unit)) {
            throw new BadRequestException("granularity doit valoir MINUTE, HOUR ou DAY");
        }
        boolean minutesKept = start.isAfter(Instant.now().minus(MINUTE_RETENTION));
        if ("minute".equals(unit) && !minutesKept) {
            throw new BadRequestException("Les minutes ne sont gardees que 7 jours : choisir HOUR ou DAY");
        }
        boolean useMinutes = minutesKept && !"day".equals(unit);
        return new Window(start, end, unit, useMinutes ? "traffic_minute" : "traffic_hour", useMinutes ? "minute" : "hour");
    }

    private OperationsService.Where filter(Window window, String route, String method) {
        OperationsService.Where where = new OperationsService.Where()
                .from(window.column(), window.from());
        where.add(window.column() + " < ?", ts(window.to()));
        return where.eq("route", route).eqUpper("http_method", method);
    }

    /** Somme des compteurs et des histogrammes, regroupee par {@code groupBy}. */
    private static String aggregate(String select, String groupBy, Window window, OperationsService.Where where) {
        return """
                WITH base AS (SELECT %s, request_count, client_errors, server_errors, total_ms, max_ms, histogram
                              FROM %s%s),
                s AS (SELECT %s, sum(request_count) AS n, sum(client_errors) AS e4, sum(server_errors) AS e5,
                             sum(total_ms) AS tot, max(max_ms) AS mx FROM base GROUP BY %s),
                h AS (SELECT %s, i, sum(x) AS v FROM base, unnest(histogram) WITH ORDINALITY AS u(x, i) GROUP BY %s, i),
                ha AS (SELECT %s, array_agg(v ORDER BY i) AS hist FROM h GROUP BY %s)
                SELECT s.*, ha.hist FROM s JOIN ha USING (%s)
                """.formatted(select, window.table(), where.sql(), groupBy, groupBy, groupBy, groupBy, groupBy, groupBy, groupBy);
    }

    private static long[] histogram(Array array) throws SQLException {
        if (array == null) {
            return new long[0];
        }
        Object[] values = (Object[]) array.getArray();
        long[] histogram = new long[values.length];
        for (int i = 0; i < values.length; i++) {
            histogram[i] = values[i] == null ? 0 : ((Number) values[i]).longValue();
        }
        return histogram;
    }

    private static Timestamp ts(Instant instant) {
        return Timestamp.from(instant);
    }

}
