package com.sni.bokaticowork.features.reporting.service.implementation;

import com.sni.bokaticowork.features.reporting.dto.response.StockValuationReportResponse;
import com.sni.bokaticowork.features.reporting.dto.response.StockValuationReportResponse.*;
import com.sni.bokaticowork.features.reporting.mapper.interfaces.StockValuationReportMapper;
import com.sni.bokaticowork.features.reporting.repository.StockValuationReportRepository;
import com.sni.bokaticowork.features.reporting.service.interfaces.StockValuationReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class StockValuationReportServiceImpl implements StockValuationReportService {

    private final StockValuationReportRepository repository;
    private final StockValuationReportMapper mapper;

    @Override
    public StockValuationReportResponse stockValuation(int inactiveDays) {
        List<Object[]> catRows = repository.valuationByCategory();
        List<Object[]> locRows = repository.valuationByLocation();

        BigDecimal totalValue = catRows.stream()
                .map(r -> decimalAt(r, 4))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalItems = catRows.stream().mapToLong(r -> longAt(r, 2)).sum();

        List<CategoryValuation> byCategory = catRows.stream()
                .map(r -> mapper.toCategoryValuation(r, totalValue)).toList();

        List<LocationValuation> byLocation = locRows.stream()
                .map(r -> mapper.toLocationValuation(r, totalValue)).toList();

        List<LowStockItem> lowStock = repository.lowStockItems().stream()
                .map(mapper::toLowStockItem).toList();

        List<DeadStockItem> deadStock = repository.deadStock(inactiveDays).stream()
                .map(mapper::toDeadStockItem).toList();

        return new StockValuationReportResponse(
                Instant.now(), totalValue, totalItems, byLocation.size(),
                byCategory, byLocation, lowStock, deadStock
        );
    }

    private BigDecimal decimalAt(Object[] row, int i) {
        Object v = row[i];
        if (v == null) return BigDecimal.ZERO;
        if (v instanceof BigDecimal bd) return bd;
        return BigDecimal.valueOf(((Number) v).doubleValue());
    }

    private long longAt(Object[] row, int i) {
        Object v = row[i];
        return v == null ? 0L : ((Number) v).longValue();
    }
}
