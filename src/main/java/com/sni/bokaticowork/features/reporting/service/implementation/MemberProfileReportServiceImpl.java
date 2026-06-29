package com.sni.bokaticowork.features.reporting.service.implementation;

import com.sni.bokaticowork.features.reporting.dto.response.MemberProfileReportResponse;
import com.sni.bokaticowork.features.reporting.dto.response.MemberProfileReportResponse.*;
import com.sni.bokaticowork.features.reporting.mapper.interfaces.MemberProfileReportMapper;
import com.sni.bokaticowork.features.reporting.repository.MemberProfileReportRepository;
import com.sni.bokaticowork.features.reporting.service.interfaces.MemberProfileReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MemberProfileReportServiceImpl implements MemberProfileReportService {

    private static final int DEFAULT_LIMIT = 20;

    private final MemberProfileReportRepository repository;
    private final MemberProfileReportMapper mapper;

    @Override
    public MemberProfileReportResponse memberProfile(String memberId) {
        MemberInfo memberInfo = mapper.toMemberInfo(repository.memberProfile(memberId));

        List<SubscriptionSummary> subscriptions = repository.memberSubscriptions(memberId)
                .stream().map(mapper::toSubscriptionSummary).toList();

        List<BookingSummary> bookings = repository.memberBookings(memberId, DEFAULT_LIMIT)
                .stream().map(mapper::toBookingSummary).toList();

        List<InvoiceSummary> invoices = repository.memberInvoices(memberId, DEFAULT_LIMIT)
                .stream().map(mapper::toInvoiceSummary).toList();

        List<PaymentSummary> payments = repository.memberPayments(memberId, DEFAULT_LIMIT)
                .stream().map(mapper::toPaymentSummary).toList();

        List<Object[]> walletRows = repository.memberWallet(memberId);
        WalletInfo wallet = walletRows.isEmpty() ? null : mapper.toWalletInfo(walletRows.getFirst());

        List<ContractSummary> contracts = repository.memberContracts(memberId)
                .stream().map(mapper::toContractSummary).toList();

        List<TicketSummary> tickets = repository.memberTickets(memberId)
                .stream().map(mapper::toTicketSummary).toList();

        return new MemberProfileReportResponse(
                Instant.now(), memberInfo,
                subscriptions, bookings, invoices, payments,
                wallet, contracts, tickets
        );
    }
}
