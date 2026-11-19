package com.sni.bokaticowork.features.booking.service.support;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.utils.time.AppTime;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Lot B9g : tableau de bord des reservations et qualite par ressource, calcules en SQL sur la
 * periode (dates de debut des reservations).
 */
@Component
@RequiredArgsConstructor
public class BookingInsights {

    public record ChannelLine(String channel, long bookings, BigDecimal revenue) {
    }

    public record ResourceLine(String resourceCode, String resourceName, long bookings, BigDecimal revenue) {
    }

    public record Dashboard(LocalDate from, LocalDate to, long bookings, long confirmed, long completed, long cancelled, long lateCancellations,
                            long noShows, long expired, BigDecimal noShowRate, BigDecimal averageLeadHours, BigDecimal revenue,
                            List<ChannelLine> channels, List<ResourceLine> topResources) {
    }

    public record ReviewLine(String resourceCode, String resourceName, long reviews, BigDecimal averageScore, long lowScores) {
    }

    private final JdbcTemplate jdbc;

    @Transactional(readOnly = true)
    public Dashboard dashboard(LocalDate from, LocalDate to, int lateCancelHours) {
        LocalDate start = from == null ? AppTime.today().withDayOfMonth(1) : from;
        LocalDate end = to == null ? AppTime.today() : to;
        if (end.isBefore(start)) {
            throw new BadRequestException("La fin de la période doit être après le début");
        }
        Timestamp a = Timestamp.valueOf(start.atStartOfDay());
        Timestamp b = Timestamp.valueOf(end.plusDays(1).atStartOfDay());
        String zone = AppTime.zone().getId();
        Map<String, Object> row = jdbc.queryForMap("""
                SELECT COUNT(*) AS total,
                       COUNT(*) FILTER (WHERE status IN ('CONFIRMED', 'IN_PROGRESS')) AS confirmed,
                       COUNT(*) FILTER (WHERE status = 'COMPLETED') AS completed,
                       COUNT(*) FILTER (WHERE status = 'CANCELLED') AS cancelled,
                       COUNT(*) FILTER (WHERE status = 'CANCELLED' AND cancelled_at IS NOT NULL
                                        AND (cancelled_at AT TIME ZONE ?) > started_at - make_interval(hours => ?)) AS late,
                       COUNT(*) FILTER (WHERE status = 'NO_SHOW') AS no_shows,
                       COUNT(*) FILTER (WHERE status = 'EXPIRED') AS expired,
                       AVG(EXTRACT(EPOCH FROM (started_at - (created_at AT TIME ZONE ?))) / 3600.0) AS lead_hours,
                       COALESCE(SUM(total_amount) FILTER (WHERE status IN ('CONFIRMED', 'IN_PROGRESS', 'COMPLETED', 'NO_SHOW')), 0) AS revenue
                FROM booking WHERE deleted = false AND started_at >= ? AND started_at < ?
                """, zone, Math.max(0, lateCancelHours), zone, a, b);
        long total = number(row.get("total")).longValue();
        long noShows = number(row.get("no_shows")).longValue();
        long finished = noShows + number(row.get("completed")).longValue();
        BigDecimal noShowRate = finished == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(noShows * 100.0 / finished).setScale(1, RoundingMode.HALF_UP);
        BigDecimal lead = row.get("lead_hours") == null ? BigDecimal.ZERO : new BigDecimal(row.get("lead_hours").toString()).setScale(1, RoundingMode.HALF_UP);
        List<ChannelLine> channels = new ArrayList<>();
        jdbc.query("""
                SELECT COALESCE(channel, 'TEAM') AS channel, COUNT(*) AS n,
                       COALESCE(SUM(total_amount) FILTER (WHERE status IN ('CONFIRMED', 'IN_PROGRESS', 'COMPLETED', 'NO_SHOW')), 0) AS revenue
                FROM booking WHERE deleted = false AND started_at >= ? AND started_at < ?
                GROUP BY COALESCE(channel, 'TEAM') ORDER BY n DESC
                """, rs -> {
            channels.add(new ChannelLine(rs.getString("channel"), rs.getLong("n"), rs.getBigDecimal("revenue")));
        }, a, b);
        List<ResourceLine> resources = new ArrayList<>();
        jdbc.query("""
                SELECT r.code, r.name, COUNT(*) AS n,
                       COALESCE(SUM(b.total_amount) FILTER (WHERE b.status IN ('CONFIRMED', 'IN_PROGRESS', 'COMPLETED', 'NO_SHOW')), 0) AS revenue
                FROM booking b JOIN resource r ON r.id = b.resource_id
                WHERE b.deleted = false AND b.started_at >= ? AND b.started_at < ?
                GROUP BY r.code, r.name ORDER BY n DESC LIMIT 10
                """, rs -> {
            resources.add(new ResourceLine(rs.getString("code"), rs.getString("name"), rs.getLong("n"), rs.getBigDecimal("revenue")));
        }, a, b);
        return new Dashboard(start, end, total, number(row.get("confirmed")).longValue(), number(row.get("completed")).longValue(),
                number(row.get("cancelled")).longValue(), number(row.get("late")).longValue(), noShows, number(row.get("expired")).longValue(),
                noShowRate, lead, row.get("revenue") == null ? BigDecimal.ZERO : new BigDecimal(row.get("revenue").toString()), channels, resources);
    }

    /** Avis par ressource sur la periode : nombre, moyenne, notes basses (au plus {@code lowScore}). */
    @Transactional(readOnly = true)
    public List<ReviewLine> reviews(LocalDate from, LocalDate to, String resourceCode, int lowScore) {
        LocalDate start = from == null ? AppTime.today().minusMonths(3) : from;
        LocalDate end = to == null ? AppTime.today() : to;
        List<ReviewLine> lines = new ArrayList<>();
        jdbc.query("""
                SELECT r.code, r.name, COUNT(*) AS n, AVG(b.csat_score) AS average, COUNT(*) FILTER (WHERE b.csat_score <= ?) AS low
                FROM booking b JOIN resource r ON r.id = b.resource_id
                WHERE b.deleted = false AND b.csat_score IS NOT NULL AND b.started_at >= ? AND b.started_at < ?
                  AND (CAST(? AS text) IS NULL OR r.code = ?)
                GROUP BY r.code, r.name ORDER BY average ASC
                """, rs -> {
            lines.add(new ReviewLine(rs.getString("code"), rs.getString("name"), rs.getLong("n"),
                    rs.getBigDecimal("average") == null ? BigDecimal.ZERO : rs.getBigDecimal("average").setScale(2, RoundingMode.HALF_UP),
                    rs.getLong("low")));
        }, lowScore, Timestamp.valueOf(start.atStartOfDay()), Timestamp.valueOf(end.plusDays(1).atStartOfDay()), resourceCode, resourceCode);
        return lines;
    }

    private static Number number(Object value) {
        return value instanceof Number n ? n : 0;
    }
}
