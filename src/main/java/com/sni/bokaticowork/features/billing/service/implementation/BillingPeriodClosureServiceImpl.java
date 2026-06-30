package com.sni.bokaticowork.features.billing.service.implementation;

import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.model.BillingPeriodClosure;
import com.sni.bokaticowork.features.billing.repository.BillingPeriodClosureRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HexFormat;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class BillingPeriodClosureServiceImpl {

    private static final List<BillingDocumentType> CLOSABLE_TYPES = List.of(
            BillingDocumentType.INVOICE,
            BillingDocumentType.CREDIT_NOTE,
            BillingDocumentType.CORRECTIVE_INVOICE,
            BillingDocumentType.DEBIT_NOTE
    );

    private final BillingPeriodClosureRepository closureRepository;

    public void computeAndSave(String periodType, LocalDate refDate, String computedBy) {
        String periodLabel = buildPeriodLabel(periodType, refDate);
        LocalDate periodStart = periodStart(periodType, refDate);
        LocalDate periodEnd   = periodEnd(periodType, refDate);

        for (BillingDocumentType docType : CLOSABLE_TYPES) {
            if (closureRepository.existsByPeriodTypeAndPeriodLabelAndDocumentType(
                    periodType, periodLabel, docType.name())) {
                log.debug("Closure already computed for {} {} {}", periodType, periodLabel, docType);
                continue;
            }

            Object[] totals = closureRepository.aggregateForClosure(
                    docType.name(), periodStart, periodEnd);

            int totalDocs        = totals[0] != null ? ((Number) totals[0]).intValue() : 0;
            BigDecimal totalInv  = asBigDecimal(totals[1]);
            BigDecimal totalPaid = asBigDecimal(totals[2]);
            BigDecimal totalCrn  = docType == BillingDocumentType.CREDIT_NOTE ? totalInv : BigDecimal.ZERO;

            BillingPeriodClosure previous = closureRepository
                    .findLatestByDocumentType(docType.name()).orElse(null);
            String previousHash    = previous != null ? previous.getClosureHash() : "GENESIS";
            BigDecimal cumulative  = previous != null
                    ? previous.getCumulativeTotal().add(totalInv)
                    : totalInv;

            String payload = docType.name() + "|" + periodLabel + "|" + totalInv + "|"
                    + totalPaid + "|" + totalCrn + "|" + previousHash;
            String hash = sha256(payload);

            BillingPeriodClosure closure = BillingPeriodClosure.builder()
                    .periodType(periodType)
                    .periodLabel(periodLabel)
                    .documentType(docType.name())
                    .totalDocuments(totalDocs)
                    .totalInvoiced(totalInv)
                    .totalPaid(totalPaid)
                    .totalCreditNotes(totalCrn)
                    .cumulativeTotal(cumulative)
                    .closureHash(hash)
                    .previousClosureHash(previousHash)
                    .computedAt(Instant.now())
                    .computedBy(computedBy)
                    .build();

            closureRepository.save(closure);
            log.info("Period closure saved: {} {} {} — {} docs, {} invoiced",
                    periodType, periodLabel, docType, totalDocs, totalInv);
        }
    }

    private String buildPeriodLabel(String periodType, LocalDate refDate) {
        return switch (periodType) {
            case "DAY"   -> refDate.toString();                           // 2026-06-29
            case "MONTH" -> YearMonth.from(refDate).toString();           // 2026-06
            case "YEAR"  -> String.valueOf(refDate.getYear());            // 2026
            default -> throw new IllegalArgumentException("Unknown period type: " + periodType);
        };
    }

    private LocalDate periodStart(String periodType, LocalDate refDate) {
        return switch (periodType) {
            case "DAY"   -> refDate;
            case "MONTH" -> refDate.withDayOfMonth(1);
            case "YEAR"  -> refDate.withDayOfYear(1);
            default -> refDate;
        };
    }

    private LocalDate periodEnd(String periodType, LocalDate refDate) {
        return switch (periodType) {
            case "DAY"   -> refDate;
            case "MONTH" -> YearMonth.from(refDate).atEndOfMonth();
            case "YEAR"  -> refDate.withMonth(12).withDayOfMonth(31);
            default -> refDate;
        };
    }

    private BigDecimal asBigDecimal(Object value) {
        if (value instanceof BigDecimal bd) return bd;
        if (value instanceof Number n) return new BigDecimal(n.toString());
        return BigDecimal.ZERO;
    }

    private String sha256(String payload) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
