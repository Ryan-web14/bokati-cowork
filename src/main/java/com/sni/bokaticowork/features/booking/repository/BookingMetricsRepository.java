package com.sni.bokaticowork.features.booking.repository;

import com.sni.bokaticowork.features.booking.dto.response.BookingMetricsOverviewResponse;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
@RequiredArgsConstructor
public class BookingMetricsRepository {

    private final EntityManager entityManager;

    public BookingMetricsOverviewResponse overview() {
        Object[] row = (Object[]) entityManager.createNativeQuery("""
                SELECT
                  COUNT(*) FILTER (WHERE status = 'DRAFT') AS draft_bookings,
                  COUNT(*) FILTER (WHERE status = 'PENDING_APPROVAL') AS pending_approval_bookings,
                  COUNT(*) FILTER (WHERE status = 'CONFIRMED') AS confirmed_bookings,
                  COUNT(*) FILTER (WHERE status = 'COMPLETED') AS completed_bookings,
                  COUNT(*) FILTER (WHERE status = 'CANCELLED') AS cancelled_bookings,
                  COUNT(*) FILTER (WHERE status = 'NO_SHOW') AS no_show_bookings,
                  (SELECT COUNT(*) FROM booking_hold WHERE status = 'ACTIVE' AND expires_at > now()) AS active_holds,
                  COALESCE(SUM(total_amount) FILTER (WHERE status = 'CONFIRMED'), 0) AS confirmed_amount,
                  COALESCE(SUM(total_amount) FILTER (WHERE status = 'COMPLETED'), 0) AS completed_amount
                FROM booking
                WHERE deleted = false
                """).getSingleResult();
        return new BookingMetricsOverviewResponse(
                asLong(row[0]),
                asLong(row[1]),
                asLong(row[2]),
                asLong(row[3]),
                asLong(row[4]),
                asLong(row[5]),
                asLong(row[6]),
                asBigDecimal(row[7]),
                asBigDecimal(row[8])
        );
    }

    private long asLong(Object value) {
        return value == null ? 0 : ((Number) value).longValue();
    }

    private BigDecimal asBigDecimal(Object value) {
        return value == null ? BigDecimal.ZERO : (BigDecimal) value;
    }
}
