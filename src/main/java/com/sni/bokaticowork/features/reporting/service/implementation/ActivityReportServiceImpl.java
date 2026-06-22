package com.sni.bokaticowork.features.reporting.service.implementation;

import com.sni.bokaticowork.features.reporting.dto.response.ActivityReportResponse;
import com.sni.bokaticowork.features.reporting.dto.response.ActivityReportResponse.*;
import com.sni.bokaticowork.features.reporting.mapper.interfaces.ActivityReportMapper;
import com.sni.bokaticowork.features.reporting.repository.ActivityReportRepository;
import com.sni.bokaticowork.features.reporting.service.interfaces.ActivityReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ActivityReportServiceImpl implements ActivityReportService {

    private final ActivityReportRepository repository;
    private final ActivityReportMapper mapper;

    @Override
    public ActivityReportResponse activityReport(LocalDate from, LocalDate to, boolean compareWithPrevious) {
        Instant fromInst = toStartInstant(from);
        Instant toInst = toEndInstant(to);

        Instant prevFrom = null, prevTo = null;
        if (compareWithPrevious && from != null && to != null) {
            long days = ChronoUnit.DAYS.between(from, to) + 1;
            LocalDate prevFromDate = from.minusDays(days);
            LocalDate prevToDate = from.minusDays(1);
            prevFrom = toStartInstant(prevFromDate);
            prevTo = toEndInstant(prevToDate);
        }

        Object[] bookingRow = repository.bookingStats(fromInst, toInst);
        Object[] invoiceRow = repository.invoiceStats(fromInst, toInst);
        Object[] paymentRow = repository.paymentStats(fromInst, toInst);
        Object[] subRow = repository.subscriptionStats(fromInst, toInst);
        var stockRows = repository.stockMovementStats(fromInst, toInst);
        Object[] contractRow = repository.contractStats(fromInst, toInst);
        Object[] memberRow = repository.newMemberStats(fromInst, toInst);
        Object[] supportRow = repository.supportTicketStats(fromInst, toInst);

        Variation bookingVar = null, invoiceVar = null, paymentVar = null;
        if (compareWithPrevious && prevFrom != null) {
            Object[] prevBooking = repository.bookingStats(prevFrom, prevTo);
            bookingVar = mapper.computeVariation(decimalAt(bookingRow, 4), decimalAt(prevBooking, 4));

            Object[] prevInvoice = repository.invoiceStats(prevFrom, prevTo);
            invoiceVar = mapper.computeVariation(decimalAt(invoiceRow, 1), decimalAt(prevInvoice, 1));

            Object[] prevPayment = repository.paymentStats(prevFrom, prevTo);
            paymentVar = mapper.computeVariation(decimalAt(paymentRow, 1), decimalAt(prevPayment, 1));
        }

        return new ActivityReportResponse(
                Instant.now(), from, to,
                mapper.toBookingSection(bookingRow, bookingVar),
                mapper.toInvoiceSection(invoiceRow, invoiceVar),
                mapper.toPaymentSection(paymentRow, paymentVar),
                mapper.toSubscriptionSection(subRow, null),
                mapper.toStockSection(stockRows, null),
                mapper.toContractSection(contractRow, null),
                mapper.toMemberSection(memberRow, null),
                mapper.toSupportSection(supportRow, null)
        );
    }

    @Override
    public byte[] activityReportCsv(LocalDate from, LocalDate to, boolean compareWithPrevious) {
        ActivityReportResponse report = activityReport(from, to, compareWithPrevious);
        StringBuilder sb = new StringBuilder();
        sb.append("Section;Indicateur;Valeur\n");

        sb.append("Réservations;Total;").append(report.bookings().totalCount()).append('\n');
        sb.append("Réservations;Confirmées;").append(report.bookings().confirmedCount()).append('\n');
        sb.append("Réservations;Terminées;").append(report.bookings().completedCount()).append('\n');
        sb.append("Réservations;Annulées;").append(report.bookings().cancelledCount()).append('\n');
        sb.append("Réservations;Revenu;").append(report.bookings().totalRevenue()).append('\n');

        sb.append("Factures;Total;").append(report.invoices().totalCount()).append('\n');
        sb.append("Factures;Facturé;").append(report.invoices().totalInvoiced()).append('\n');
        sb.append("Factures;Payé;").append(report.invoices().totalPaid()).append('\n');
        sb.append("Factures;Encours;").append(report.invoices().totalOutstanding()).append('\n');

        sb.append("Paiements;Transactions;").append(report.payments().transactionCount()).append('\n');
        sb.append("Paiements;Total reçu;").append(report.payments().totalReceived()).append('\n');

        sb.append("Abonnements;Actifs;").append(report.subscriptions().activeCount()).append('\n');
        sb.append("Abonnements;Nouveaux;").append(report.subscriptions().newCount()).append('\n');
        sb.append("Abonnements;Annulés;").append(report.subscriptions().cancelledCount()).append('\n');

        sb.append("Stock;Entrées;").append(report.stock().inboundMovements()).append('\n');
        sb.append("Stock;Sorties;").append(report.stock().outboundMovements()).append('\n');

        sb.append("Contrats;Signés;").append(report.contracts().signedCount()).append('\n');
        sb.append("Contrats;Expirés;").append(report.contracts().expiredCount()).append('\n');
        sb.append("Contrats;Actifs;").append(report.contracts().activeCount()).append('\n');

        sb.append("Membres;Actifs;").append(report.members().totalActive()).append('\n');
        sb.append("Membres;Nouveaux;").append(report.members().newCount()).append('\n');

        sb.append("Support;Ouverts;").append(report.support().openedCount()).append('\n');
        sb.append("Support;Résolus;").append(report.support().resolvedCount()).append('\n');
        sb.append("Support;Fermés;").append(report.support().closedCount()).append('\n');

        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private Instant toStartInstant(LocalDate date) {
        return date == null ? null : date.atStartOfDay().toInstant(ZoneOffset.UTC);
    }

    private Instant toEndInstant(LocalDate date) {
        return date == null ? null : date.plusDays(1).atStartOfDay().minusNanos(1).toInstant(ZoneOffset.UTC);
    }

    private BigDecimal decimalAt(Object[] row, int i) {
        Object v = row[i];
        if (v == null) return BigDecimal.ZERO;
        if (v instanceof BigDecimal bd) return bd;
        return BigDecimal.valueOf(((Number) v).doubleValue());
    }
}
