package com.sni.bokaticowork.features.reporting.mapper.interfaces;

import com.sni.bokaticowork.features.reporting.dto.response.MemberProfileReportResponse.*;
import com.sni.bokaticowork.features.reporting.mapper.decorator.MemberProfileReportMapperDecorator;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
@DecoratedWith(MemberProfileReportMapperDecorator.class)
public interface MemberProfileReportMapper {

    default MemberInfo toMemberInfo(Object[] row) { return null; }

    default SubscriptionSummary toSubscriptionSummary(Object[] row) { return null; }

    default BookingSummary toBookingSummary(Object[] row) { return null; }

    default InvoiceSummary toInvoiceSummary(Object[] row) { return null; }

    default PaymentSummary toPaymentSummary(Object[] row) { return null; }

    default WalletInfo toWalletInfo(Object[] row) { return null; }

    default ContractSummary toContractSummary(Object[] row) { return null; }

    default TicketSummary toTicketSummary(Object[] row) { return null; }
}
