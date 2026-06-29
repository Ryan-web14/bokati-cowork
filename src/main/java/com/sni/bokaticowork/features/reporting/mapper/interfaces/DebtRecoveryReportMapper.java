package com.sni.bokaticowork.features.reporting.mapper.interfaces;

import com.sni.bokaticowork.features.reporting.dto.response.DebtRecoveryReportResponse.AgingBucket;
import com.sni.bokaticowork.features.reporting.dto.response.DebtRecoveryReportResponse.UnpaidInvoice;
import com.sni.bokaticowork.features.reporting.mapper.decorator.DebtRecoveryReportMapperDecorator;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.math.BigDecimal;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
@DecoratedWith(DebtRecoveryReportMapperDecorator.class)
public interface DebtRecoveryReportMapper {

    default UnpaidInvoice toUnpaidInvoice(Object[] row) { return null; }

    default AgingBucket toAgingBucket(String key, String label, Object[] row, BigDecimal total) { return null; }
}
