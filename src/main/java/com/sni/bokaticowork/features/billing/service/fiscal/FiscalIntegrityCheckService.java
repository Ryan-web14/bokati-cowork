package com.sni.bokaticowork.features.billing.service.fiscal;

import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.model.FiscalIntegrityReportLog;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.billing.repository.FiscalIntegrityReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class FiscalIntegrityCheckService {

    private static final Pattern FISCAL_NUMBER_PATTERN =
            Pattern.compile("^([A-Z]+)(\\d{8})-(\\d+)([a-z0-9]{3})-([A-Z0-9]+)$");

    private final BillingDocumentRepository documentRepository;
    private final FiscalIntegrityReportRepository reportRepository;
    private final FiscalHashService hashService;
    private final FiscalSignatureService signatureService;

    @Transactional(readOnly = true)
    public FiscalIntegrityReport check() {
        List<BillingDocument> invoices = documentRepository
                .findAllValidatedOrderByValidatedAt(BillingDocumentType.INVOICE.name());

        int checked = 0, brokenChains = 0, missingSignatures = 0, numberingGaps = 0;

        String previousHash = null;
        long previousSeq = 0;
        int previousYear = 0;

        for (BillingDocument doc : invoices) {
            checked++;

            // 1 — Continuité numérotation (réinitialisée par année)
            int docYear = doc.getFiscalDate() != null ? doc.getFiscalDate().getYear() : 0;
            long seq = extractSeq(doc.getFiscalNumber());
            if (docYear != previousYear) {
                // Nouvelle année : la séquence doit repartir à 1
                if (seq != 1) numberingGaps++;
                previousSeq = seq;
                previousYear = docYear;
                previousHash = null; // La chaîne repart à l'entrée de chaque année
            } else {
                if (seq != previousSeq + 1) numberingGaps++;
                previousSeq = seq;
            }

            // 2 — Chaînage hash
            String expectedHash = hashService.compute(doc, previousHash);
            if (!expectedHash.equals(doc.getCurrentHash())) brokenChains++;
            previousHash = doc.getCurrentHash();

            // 3 — Signature HMAC
            if (doc.getFiscalSignature() == null
                    || !signatureService.verify(doc.getCurrentHash(), doc.getFiscalSignature())) {
                missingSignatures++;
            }
        }

        boolean valid = checked == 0
                || (brokenChains + missingSignatures + numberingGaps == 0);

        return new FiscalIntegrityReport(valid, checked, brokenChains, missingSignatures, numberingGaps, Instant.now());
    }

    @Transactional
    public FiscalIntegrityReport checkAndSave(String triggeredBy) {
        FiscalIntegrityReport report = check();
        FiscalIntegrityReportLog entry = FiscalIntegrityReportLog.builder()
                .valid(report.valid())
                .checkedInvoices(report.checkedInvoices())
                .brokenChains(report.brokenChains())
                .missingSignatures(report.missingSignatures())
                .numberingGaps(report.numberingGaps())
                .checkedAt(report.checkedAt())
                .triggeredBy(triggeredBy)
                .createdAt(Instant.now())
                .build();
        reportRepository.save(entry);
        if (!report.valid()) {
            log.warn(
                    "SEFC integrity check anomalies — chains:{} signatures:{} gaps:{} invoices:{}",
                    report.brokenChains(), report.missingSignatures(),
                    report.numberingGaps(), report.checkedInvoices());
        }
        return report;
    }

    @Transactional(readOnly = true)
    public Optional<FiscalIntegrityReportLog> lastReport() {
        return reportRepository.findLatest();
    }

    private long extractSeq(String fiscalNumber) {
        if (fiscalNumber == null) return -1;
        try {
            Matcher m = FISCAL_NUMBER_PATTERN.matcher(fiscalNumber);
            if (m.matches()) return Long.parseLong(m.group(3));
        } catch (NumberFormatException ignored) {
        }
        return -1;
    }

    public record FiscalIntegrityReport(
            boolean valid,
            int checkedInvoices,
            int brokenChains,
            int missingSignatures,
            int numberingGaps,
            Instant checkedAt
    ) {}
}
