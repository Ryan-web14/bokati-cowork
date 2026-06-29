package com.sni.bokaticowork.features.reporting.mapper.interfaces;

import com.sni.bokaticowork.features.reporting.dto.response.ResourceUtilizationReportResponse.HourlyHeatmap;
import com.sni.bokaticowork.features.reporting.dto.response.ResourceUtilizationReportResponse.ResourceUtilization;
import com.sni.bokaticowork.features.reporting.dto.response.ResourceUtilizationReportResponse.UnderutilizedResource;
import com.sni.bokaticowork.features.reporting.mapper.decorator.ResourceUtilizationReportMapperDecorator;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.math.BigDecimal;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
@DecoratedWith(ResourceUtilizationReportMapperDecorator.class)
public interface ResourceUtilizationReportMapper {

    default ResourceUtilization toResourceUtilization(Object[] row, int periodDays) { return null; }

    default HourlyHeatmap toHourlyHeatmap(Object[] row, long totalBookings) { return null; }

    default UnderutilizedResource toUnderutilizedResource(ResourceUtilization r) { return null; }
}
