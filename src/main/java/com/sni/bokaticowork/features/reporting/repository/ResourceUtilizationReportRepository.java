package com.sni.bokaticowork.features.reporting.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public class ResourceUtilizationReportRepository {

    @PersistenceContext
    private EntityManager em;

    @SuppressWarnings("unchecked")
    public List<Object[]> utilizationByResource(Instant from, Instant to) {
        return list("""
                SELECT
                    r.code AS resource_code,
                    r.name AS resource_name,
                    rt.name AS resource_type,
                    count(*) AS total_bookings,
                    count(*) FILTER (WHERE extract(hour FROM b.started_at) >= 6  AND extract(hour FROM b.started_at) < 12) AS morning,
                    count(*) FILTER (WHERE extract(hour FROM b.started_at) >= 12 AND extract(hour FROM b.started_at) < 18) AS afternoon,
                    count(*) FILTER (WHERE extract(hour FROM b.started_at) >= 18 AND extract(hour FROM b.started_at) < 22) AS evening,
                    coalesce(sum(b.duration_minutes), 0) AS booked_minutes,
                    coalesce(sum(b.total_amount), 0) AS total_revenue
                FROM resource r
                LEFT JOIN resource_type rt ON rt.id = r.type_id
                LEFT JOIN booking b ON b.resource_id = r.id
                    AND b.deleted = false
                    AND b.status NOT IN ('CANCELLED','NO_SHOW')
                    AND (CAST(:from AS timestamp) IS NULL OR b.started_at >= CAST(:from AS timestamp))
                    AND (CAST(:to   AS timestamp) IS NULL OR b.started_at <= CAST(:to   AS timestamp))
                WHERE r.deleted = false AND r.active = true AND r.booking_enabled = true
                GROUP BY r.code, r.name, rt.name
                ORDER BY booked_minutes DESC
                """, from, to);
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> peakHours(Instant from, Instant to) {
        return list("""
                SELECT
                    extract(hour FROM b.started_at)::integer AS hour,
                    count(*) AS booking_count
                FROM booking b
                WHERE b.deleted = false
                  AND b.status NOT IN ('CANCELLED','NO_SHOW')
                  AND (CAST(:from AS timestamp) IS NULL OR b.started_at >= CAST(:from AS timestamp))
                  AND (CAST(:to   AS timestamp) IS NULL OR b.started_at <= CAST(:to   AS timestamp))
                GROUP BY 1
                ORDER BY 1
                """, from, to);
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> list(String sql, Instant from, Instant to) {
        Query q = em.createNativeQuery(sql);
        if (sql.contains(":from")) q.setParameter("from", from);
        if (sql.contains(":to"))   q.setParameter("to", to);
        return q.getResultList();
    }
}
