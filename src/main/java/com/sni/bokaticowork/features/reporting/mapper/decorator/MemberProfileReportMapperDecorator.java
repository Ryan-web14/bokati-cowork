package com.sni.bokaticowork.features.reporting.mapper.decorator;

import com.sni.bokaticowork.features.reporting.dto.response.MemberProfileReportResponse.*;
import com.sni.bokaticowork.features.reporting.mapper.interfaces.MemberProfileReportMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;

@Component
public abstract class MemberProfileReportMapperDecorator implements MemberProfileReportMapper {

    @Override
    public MemberInfo toMemberInfo(Object[] row) {
        return new MemberInfo(
                str(row[0]),  // member_id
                str(row[1]),  // firstname
                str(row[2]),  // lastname
                str(row[3]),  // email
                str(row[4]),  // phone
                str(row[5]),  // whatsapp_phone
                str(row[6]),  // member_status
                str(row[9]),  // customer_code
                str(row[10]), // company_name
                str(row[11]), // job_title
                str(row[12]), // company_role
                toLocalDate(row[13]), // birth_date
                str(row[14]), // city
                str(row[15]), // country
                str(row[16]), // photo_url
                boolAt(row, 7), // portal_access
                toLocalDate(row[8])  // created_at
        );
    }

    @Override
    public SubscriptionSummary toSubscriptionSummary(Object[] row) {
        return new SubscriptionSummary(
                str(row[0]), str(row[1]), str(row[2]), str(row[3]),
                decimalAt(row, 4), str(row[5]),
                toLocalDate(row[6]), toLocalDate(row[7]),
                boolAt(row, 8)
        );
    }

    @Override
    public BookingSummary toBookingSummary(Object[] row) {
        return new BookingSummary(
                str(row[0]), str(row[1]), str(row[2]),
                toLocalDate(row[3]), intAt(row, 4),
                decimalAt(row, 5), str(row[6])
        );
    }

    @Override
    public InvoiceSummary toInvoiceSummary(Object[] row) {
        return new InvoiceSummary(
                str(row[0]), str(row[1]),
                decimalAt(row, 2), decimalAt(row, 3), decimalAt(row, 4),
                str(row[5]), toLocalDate(row[6]), toLocalDate(row[7])
        );
    }

    @Override
    public PaymentSummary toPaymentSummary(Object[] row) {
        return new PaymentSummary(
                str(row[0]), str(row[1]),
                decimalAt(row, 2), str(row[3]), str(row[4]),
                toLocalDate(row[5])
        );
    }

    @Override
    public WalletInfo toWalletInfo(Object[] row) {
        return new WalletInfo(
                str(row[0]),
                decimalAt(row, 1), decimalAt(row, 2), decimalAt(row, 3),
                str(row[4]), str(row[5])
        );
    }

    @Override
    public ContractSummary toContractSummary(Object[] row) {
        return new ContractSummary(
                str(row[0]), str(row[1]), str(row[2]),
                toLocalDate(row[3]), toLocalDate(row[4]),
                str(row[5]), toLocalDate(row[6])
        );
    }

    @Override
    public TicketSummary toTicketSummary(Object[] row) {
        return new TicketSummary(
                str(row[0]), str(row[1]), str(row[2]),
                str(row[3]), str(row[4]),
                toLocalDate(row[5]), toLocalDate(row[6])
        );
    }

    private String str(Object v) {
        return v == null ? null : v.toString();
    }

    private BigDecimal decimalAt(Object[] row, int i) {
        Object v = row[i];
        if (v == null) return BigDecimal.ZERO;
        if (v instanceof BigDecimal bd) return bd;
        return BigDecimal.valueOf(((Number) v).doubleValue());
    }

    private int intAt(Object[] row, int i) {
        Object v = row[i];
        return v == null ? 0 : ((Number) v).intValue();
    }

    private boolean boolAt(Object[] row, int i) {
        Object v = row[i];
        if (v == null) return false;
        if (v instanceof Boolean b) return b;
        return Boolean.parseBoolean(v.toString());
    }

    private LocalDate toLocalDate(Object v) {
        if (v == null) return null;
        if (v instanceof Date d) return d.toLocalDate();
        if (v instanceof LocalDate ld) return ld;
        if (v instanceof Timestamp ts) return ts.toLocalDateTime().toLocalDate();
        return LocalDate.parse(v.toString());
    }
}
