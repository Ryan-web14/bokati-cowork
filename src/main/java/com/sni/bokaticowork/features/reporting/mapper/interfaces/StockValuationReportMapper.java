package com.sni.bokaticowork.features.reporting.mapper.interfaces;

import com.sni.bokaticowork.features.reporting.dto.response.StockValuationReportResponse.*;
import com.sni.bokaticowork.features.reporting.mapper.decorator.StockValuationReportMapperDecorator;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.math.BigDecimal;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
@DecoratedWith(StockValuationReportMapperDecorator.class)
public interface StockValuationReportMapper {

    default CategoryValuation toCategoryValuation(Object[] row, BigDecimal totalValue) { return null; }

    default LocationValuation toLocationValuation(Object[] row, BigDecimal totalValue) { return null; }

    default LowStockItem toLowStockItem(Object[] row) { return null; }

    default DeadStockItem toDeadStockItem(Object[] row) { return null; }
}
