package com.sni.bokaticowork.features.reporting.mapper.interfaces;

import com.sni.bokaticowork.features.reporting.dto.response.SubscriptionReportResponse.*;
import com.sni.bokaticowork.features.reporting.mapper.decorator.SubscriptionReportMapperDecorator;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.math.BigDecimal;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
@DecoratedWith(SubscriptionReportMapperDecorator.class)
public interface SubscriptionReportMapper {

    default MrrDataPoint toMrrDataPoint(Object[] row) { return null; }

    default PlanDistribution toPlanDistribution(Object[] row, BigDecimal totalAmount) { return null; }

    default ChurnDataPoint toChurnDataPoint(Object[] row) { return null; }

    default UpcomingRenewal toUpcomingRenewal(Object[] row) { return null; }

    default PlanRevenue toPlanRevenue(Object[] row, BigDecimal totalRevenue) { return null; }
}
