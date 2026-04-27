package com.sni.bokaticowork.features.booking.service.implementation;

import com.sni.bokaticowork.features.booking.dto.response.BookingMetricsOverviewResponse;
import com.sni.bokaticowork.features.booking.repository.BookingMetricsRepository;
import com.sni.bokaticowork.features.booking.service.interfaces.BookingMetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BookingMetricsServiceImpl implements BookingMetricsService {

    private final BookingMetricsRepository metricsRepository;

    @Override
    public BookingMetricsOverviewResponse overview() {
        return metricsRepository.overview();
    }
}
