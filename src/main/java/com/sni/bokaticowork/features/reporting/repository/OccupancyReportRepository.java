package com.sni.bokaticowork.features.reporting.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class OccupancyReportRepository {

    @PersistenceContext
    private EntityManager em;

    public Object[] totals(LocalDateTime from, LocalDateTime to) {
        String sql = """
                SELECT
                    count(b.id)                          AS total_bookings,
                    coalesce(sum(b.duration_minutes), 0) AS total_booked_minutes
                FROM booking b
                WHERE b.deleted = false
                  AND b.status IN ('CONFIRMED','IN_PROGRESS','COMPLETED')
                  AND (CAST(:from AS timestamp) IS NULL OR b.started_at >= CAST(:from AS timestamp))
                  AND (CAST(:to   AS timestamp) IS NULL OR b.started_at <= CAST(:to   AS timestamp))
                """;
        Query q = em.createNativeQuery(sql);
        if (from != null) q.setParameter("from", from);
        else              q.setParameter("from", (Object) null);
        if (to != null)   q.setParameter("to", to);
        else              q.setParameter("to", (Object) null);
        Object result = q.getSingleResult();
        return result instanceof Object[] arr ? arr : new Object[]{result};
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> byResourceType(LocalDateTime from, LocalDateTime to) {
        String sql = """
                SELECT
                    rt.code                              AS type_code,
                    rt.name                              AS type_name,
                    count(DISTINCT r.id)                 AS resource_count,
                    count(b.id)                          AS total_bookings,
                    coalesce(sum(b.duration_minutes), 0) AS total_booked_minutes,
                    coalesce(sum(b.total_amount), 0)     AS total_revenue
                FROM resource_type rt
                JOIN resource r ON r.type_id = rt.id AND r.deleted = false
                LEFT JOIN booking b ON b.resource_id = r.id
                    AND b.deleted = false
                    AND b.status IN ('CONFIRMED','IN_PROGRESS','COMPLETED')
                    AND (CAST(:from AS timestamp) IS NULL OR b.started_at >= CAST(:from AS timestamp))
                    AND (CAST(:to   AS timestamp) IS NULL OR b.started_at <= CAST(:to   AS timestamp))
                WHERE rt.active = true
                GROUP BY rt.id, rt.code, rt.name
                ORDER BY total_booked_minutes DESC
                """;
        Query q = em.createNativeQuery(sql);
        q.setParameter("from", from);
        q.setParameter("to", to);
        return q.getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> byResource(LocalDateTime from, LocalDateTime to) {
        String sql = """
                SELECT
                    r.code                               AS resource_code,
                    r.name                               AS resource_name,
                    rt.code                              AS type_code,
                    rt.name                              AS type_name,
                    count(b.id)                          AS booking_count,
                    coalesce(sum(b.duration_minutes), 0) AS booked_minutes,
                    coalesce(sum(b.total_amount), 0)     AS revenue
                FROM resource r
                JOIN resource_type rt ON rt.id = r.type_id
                LEFT JOIN booking b ON b.resource_id = r.id
                    AND b.deleted = false
                    AND b.status IN ('CONFIRMED','IN_PROGRESS','COMPLETED')
                    AND (CAST(:from AS timestamp) IS NULL OR b.started_at >= CAST(:from AS timestamp))
                    AND (CAST(:to   AS timestamp) IS NULL OR b.started_at <= CAST(:to   AS timestamp))
                WHERE r.deleted = false
                GROUP BY r.id, r.code, r.name, rt.code, rt.name
                ORDER BY booked_minutes DESC
                """;
        Query q = em.createNativeQuery(sql);
        q.setParameter("from", from);
        q.setParameter("to", to);
        return q.getResultList();
    }
}