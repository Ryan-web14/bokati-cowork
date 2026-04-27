package com.sni.bokaticowork.features.subscription.metrics.service.impl;

import com.sni.bokaticowork.features.subscription.metrics.dto.SubscriptionMetricsOverviewResponse;
import com.sni.bokaticowork.features.subscription.metrics.repository.SubscriptionMetricsRepository;
import com.sni.bokaticowork.features.subscription.metrics.service.SubscriptionMetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.BigInteger;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SubscriptionMetricsServiceImpl implements SubscriptionMetricsService {

    private final SubscriptionMetricsRepository metricsRepository;

    @Override
    public SubscriptionMetricsOverviewResponse overview() {
        Object[] row = metricsRepository.overview();
        return new SubscriptionMetricsOverviewResponse(
                asLong(row[0]),
                asLong(row[1]),
                asLong(row[2]),
                asLong(row[3]),
                asLong(row[4]),
                asLong(row[5]),
                asLong(row[6]),
                asLong(row[7]),
                asBigDecimal(row[8]),
                asBigDecimal(row[9]),
                asBigDecimal(row[10]),
                asBigDecimal(row[11]),
                asLong(row[12]),
                asBigDecimal(row[13]),
                asLong(row[14]),
                asBigDecimal(row[15])
        );
    }

    private long asLong(Object value) {
        if (value instanceof BigInteger bigInteger) {
            return bigInteger.longValue();
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return 0L;
    }

    private BigDecimal asBigDecimal(Object value) {
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        if (value instanceof BigInteger bigInteger) {
            return new BigDecimal(bigInteger);
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return BigDecimal.ZERO;
    }
}
