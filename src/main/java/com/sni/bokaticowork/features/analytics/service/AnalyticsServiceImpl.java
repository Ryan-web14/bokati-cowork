package com.sni.bokaticowork.features.analytics.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.analytics.dto.AnalyticsOverviewResponse;
import com.sni.bokaticowork.features.analytics.dto.BookingTrendResponse;
import com.sni.bokaticowork.features.analytics.dto.TopOwnerResponse;
import com.sni.bokaticowork.features.analytics.dto.TopResourceResponse;
import com.sni.bokaticowork.features.analytics.repository.AnalyticsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final AnalyticsRepository repository;

    @Override
    public AnalyticsOverviewResponse overview(LocalDate fromDate, LocalDate toDate) {
        Instant from = fromDate == null ? null : fromDate.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant to = toDate == null ? null : toDate.plusDays(1).atStartOfDay().minusNanos(1).toInstant(ZoneOffset.UTC);
        return new AnalyticsOverviewResponse(
                Instant.now(),
                fromDate,
                toDate,
                financial(from, to),
                payment(repository.payment(from, to)),
                wallet(repository.wallet(from, to)),
                booking(from, to),
                subscription(repository.subscription()),
                customer(repository.customer(from, to)),
                inventory(repository.inventory())
        );
    }

    @Override
    public AnalyticsOverviewResponse.FinancialMetrics financial(Instant fromDate, Instant toDate) {
        return financial(repository.financial(fromDate, toDate));
    }

    @Override
    public AnalyticsOverviewResponse.BookingMetrics booking(Instant fromDate, Instant toDate) {
        return booking(repository.booking(fromDate, toDate));
    }

    @Override
    public List<BookingTrendResponse> bookingTrend(Instant fromDate, Instant toDate, String groupBy) {
        String normalizedGroupBy = normalizeGroupBy(groupBy);
        return repository.bookingTrend(fromDate, toDate, normalizedGroupBy).stream()
                .map(this::bookingTrend)
                .toList();
    }

    @Override
    public List<TopOwnerResponse> topOwners(Instant fromDate, Instant toDate, int limit) {
        int normalizedLimit = normalizeLimit(limit);
        return repository.topOwners(fromDate, toDate, normalizedLimit).stream()
                .map(this::topOwner)
                .toList();
    }

    @Override
    public List<TopResourceResponse> topResources(Instant fromDate, Instant toDate, int limit) {
        int normalizedLimit = normalizeLimit(limit);
        return repository.topResources(fromDate, toDate, normalizedLimit).stream()
                .map(this::topResource)
                .toList();
    }

    private AnalyticsOverviewResponse.FinancialMetrics financial(Object[] row) {
        return new AnalyticsOverviewResponse.FinancialMetrics(
                longAt(row, 0),
                decimalAt(row, 1),
                decimalAt(row, 2),
                decimalAt(row, 3),
                longAt(row, 4),
                longAt(row, 5),
                decimalAt(row, 6),
                decimalAt(row, 7),
                decimalAt(row, 8)
        );
    }

    private AnalyticsOverviewResponse.PaymentMetrics payment(Object[] row) {
        return new AnalyticsOverviewResponse.PaymentMetrics(
                longAt(row, 0),
                decimalAt(row, 1),
                decimalAt(row, 2),
                decimalAt(row, 3),
                decimalAt(row, 4),
                decimalAt(row, 5),
                decimalAt(row, 6),
                longAt(row, 7),
                longAt(row, 8)
        );
    }

    private AnalyticsOverviewResponse.WalletMetrics wallet(Object[] row) {
        return new AnalyticsOverviewResponse.WalletMetrics(
                longAt(row, 0),
                decimalAt(row, 1),
                decimalAt(row, 2),
                decimalAt(row, 3),
                decimalAt(row, 4)
        );
    }

    private AnalyticsOverviewResponse.BookingMetrics booking(Object[] row) {
        return new AnalyticsOverviewResponse.BookingMetrics(
                longAt(row, 0),
                longAt(row, 1),
                longAt(row, 2),
                longAt(row, 3),
                longAt(row, 4),
                decimalAt(row, 5),
                longAt(row, 6),
                longAt(row, 7)
        );
    }

    private BookingTrendResponse bookingTrend(Object[] row) {
        return new BookingTrendResponse(
                dateAt(row, 0),
                longAt(row, 1),
                longAt(row, 2),
                longAt(row, 3),
                decimalAt(row, 4)
        );
    }

    private TopOwnerResponse topOwner(Object[] row) {
        return new TopOwnerResponse(
                stringAt(row, 0),
                stringAt(row, 1),
                stringAt(row, 2),
                longAt(row, 3),
                longAt(row, 4),
                longAt(row, 5),
                longAt(row, 6),
                decimalAt(row, 7),
                longAt(row, 8)
        );
    }

    private TopResourceResponse topResource(Object[] row) {
        return new TopResourceResponse(
                stringAt(row, 0),
                stringAt(row, 1),
                stringAt(row, 2),
                stringAt(row, 3),
                stringAt(row, 4),
                stringAt(row, 5),
                longAt(row, 6),
                longAt(row, 7),
                longAt(row, 8),
                longAt(row, 9),
                decimalAt(row, 10),
                longAt(row, 11)
        );
    }

    private AnalyticsOverviewResponse.SubscriptionMetrics subscription(Object[] row) {
        return new AnalyticsOverviewResponse.SubscriptionMetrics(
                longAt(row, 0),
                longAt(row, 1),
                longAt(row, 2),
                longAt(row, 3),
                decimalAt(row, 4),
                longAt(row, 5)
        );
    }

    private AnalyticsOverviewResponse.CustomerMetrics customer(Object[] row) {
        return new AnalyticsOverviewResponse.CustomerMetrics(
                longAt(row, 0),
                longAt(row, 1),
                longAt(row, 2),
                longAt(row, 3)
        );
    }

    private AnalyticsOverviewResponse.InventoryMetrics inventory(Object[] row) {
        return new AnalyticsOverviewResponse.InventoryMetrics(
                longAt(row, 0),
                decimalAt(row, 1),
                longAt(row, 2),
                longAt(row, 3)
        );
    }

    private long longAt(Object[] row, int index) {
        Object value = row[index];
        return value == null ? 0L : ((Number) value).longValue();
    }

    private BigDecimal decimalAt(Object[] row, int index) {
        Object value = row[index];
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        return BigDecimal.valueOf(((Number) value).doubleValue());
    }

    private String stringAt(Object[] row, int index) {
        Object value = row[index];
        return value == null ? null : String.valueOf(value);
    }

    private LocalDate dateAt(Object[] row, int index) {
        Object value = row[index];
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof java.sql.Date date) {
            return date.toLocalDate();
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toInstant().atZone(ZoneOffset.UTC).toLocalDate();
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime.toLocalDate();
        }
        if (value instanceof Instant instant) {
            return instant.atZone(ZoneOffset.UTC).toLocalDate();
        }
        return LocalDate.parse(String.valueOf(value));
    }

    private String normalizeGroupBy(String groupBy) {
        String normalized = groupBy == null || groupBy.isBlank()
                ? "day"
                : groupBy.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "day", "week", "month" -> normalized;
            default -> throw new BadRequestException("groupBy must be one of: day, week, month");
        };
    }

    private int normalizeLimit(int limit) {
        if (limit < 1) {
            throw new BadRequestException("limit must be greater than zero");
        }
        return Math.min(limit, 100);
    }
}
