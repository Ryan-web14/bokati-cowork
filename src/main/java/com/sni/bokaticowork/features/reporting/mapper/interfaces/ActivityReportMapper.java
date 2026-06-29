package com.sni.bokaticowork.features.reporting.mapper.interfaces;

import com.sni.bokaticowork.features.reporting.dto.response.ActivityReportResponse.BookingSection;
import com.sni.bokaticowork.features.reporting.dto.response.ActivityReportResponse.ContractSection;
import com.sni.bokaticowork.features.reporting.dto.response.ActivityReportResponse.InvoiceSection;
import com.sni.bokaticowork.features.reporting.dto.response.ActivityReportResponse.MemberSection;
import com.sni.bokaticowork.features.reporting.dto.response.ActivityReportResponse.PaymentSection;
import com.sni.bokaticowork.features.reporting.dto.response.ActivityReportResponse.StockSection;
import com.sni.bokaticowork.features.reporting.dto.response.ActivityReportResponse.SubscriptionSection;
import com.sni.bokaticowork.features.reporting.dto.response.ActivityReportResponse.SupportSection;
import com.sni.bokaticowork.features.reporting.dto.response.ActivityReportResponse.Variation;
import com.sni.bokaticowork.features.reporting.mapper.decorator.ActivityReportMapperDecorator;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.math.BigDecimal;
import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
@DecoratedWith(ActivityReportMapperDecorator.class)
public interface ActivityReportMapper {

    default BookingSection toBookingSection(Object[] row, Variation variation) { return null; }

    default InvoiceSection toInvoiceSection(Object[] row, Variation variation) { return null; }

    default PaymentSection toPaymentSection(Object[] row, Variation variation) { return null; }

    default SubscriptionSection toSubscriptionSection(Object[] row, Variation variation) { return null; }

    default StockSection toStockSection(List<Object[]> rows, Variation variation) { return null; }

    default ContractSection toContractSection(Object[] row, Variation variation) { return null; }

    default MemberSection toMemberSection(Object[] row, Variation variation) { return null; }

    default SupportSection toSupportSection(Object[] row, Variation variation) { return null; }

    default Variation computeVariation(BigDecimal current, BigDecimal previous) { return null; }
}
