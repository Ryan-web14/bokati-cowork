package com.sni.bokaticowork.features.reporting.service.implementation;

import com.sni.bokaticowork.features.reporting.dto.response.DebtRecoveryReportResponse;
import com.sni.bokaticowork.features.reporting.dto.response.DebtRecoveryReportResponse.*;
import com.sni.bokaticowork.features.reporting.mapper.interfaces.DebtRecoveryReportMapper;
import com.sni.bokaticowork.features.reporting.repository.DebtRecoveryReportRepository;
import com.sni.bokaticowork.features.reporting.service.interfaces.DebtRecoveryReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class DebtRecoveryReportServiceImpl implements DebtRecoveryReportService {

    private final DebtRecoveryReportRepository repository;
    private final DebtRecoveryReportMapper mapper;

    @Override
    public DebtRecoveryReportResponse debtRecovery(String sortBy) {
        List<UnpaidInvoice> invoices = repository.unpaidInvoices(sortBy)
                .stream().map(mapper::toUnpaidInvoice).toList();

        List<Object[]> agingRows = repository.agingSummary();
        Map<String, Object[]> byBucket = new HashMap<>();
        for (Object[] row : agingRows) {
            byBucket.put(row[0].toString(), row);
        }

        BigDecimal total = invoices.stream()
                .map(UnpaidInvoice::balanceDue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        AgingSummary aging = new AgingSummary(
                mapper.toAgingBucket("CURRENT", "Non échu", byBucket.get("CURRENT"), total),
                mapper.toAgingBucket("1_30", "1 – 30 jours", byBucket.get("1_30"), total),
                mapper.toAgingBucket("31_60", "31 – 60 jours", byBucket.get("31_60"), total),
                mapper.toAgingBucket("61_90", "61 – 90 jours", byBucket.get("61_90"), total),
                mapper.toAgingBucket("90_PLUS", "+ 90 jours", byBucket.get("90_PLUS"), total)
        );

        return new DebtRecoveryReportResponse(
                Instant.now(), total, invoices.size(), aging, invoices
        );
    }

    @Override
    public byte[] debtRecoveryCsv(String sortBy) {
        DebtRecoveryReportResponse report = debtRecovery(sortBy);
        StringBuilder sb = new StringBuilder();
        sb.append("N° Facture;Client;Code Client;Email;Téléphone;Date émission;Échéance;Montant total;Payé;Solde dû;Jours retard;Tranche\n");
        for (UnpaidInvoice inv : report.unpaidInvoices()) {
            sb.append(inv.documentNumber()).append(';')
              .append(inv.customerName()).append(';')
              .append(inv.customerCode()).append(';')
              .append(inv.customerEmail() != null ? inv.customerEmail() : "").append(';')
              .append(inv.customerPhone() != null ? inv.customerPhone() : "").append(';')
              .append(inv.issueDate()).append(';')
              .append(inv.dueDate()).append(';')
              .append(inv.totalAmount()).append(';')
              .append(inv.paidAmount()).append(';')
              .append(inv.balanceDue()).append(';')
              .append(inv.agingDays()).append(';')
              .append(inv.agingBucket()).append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
}
