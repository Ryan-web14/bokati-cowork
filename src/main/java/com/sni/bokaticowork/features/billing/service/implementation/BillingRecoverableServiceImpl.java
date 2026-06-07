package com.sni.bokaticowork.features.billing.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.billing.dto.request.AddRecoverableItemsRequest;
import com.sni.bokaticowork.features.billing.dto.request.RecoverItemRequest;
import com.sni.bokaticowork.features.billing.dto.request.WriteOffRecoverableRequest;
import com.sni.bokaticowork.features.billing.dto.response.BillingAgingReportResponse;
import com.sni.bokaticowork.features.billing.dto.response.BillingRecoverableResponse;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.model.BillingDocumentRecoverable;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRecoverableRepository;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingRecoverableService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class BillingRecoverableServiceImpl implements BillingRecoverableService {

    private final BillingDocumentRecoverableRepository recoverableRepository;
    private final BillingDocumentRepository documentRepository;
    private final SequenceGeneratorFacade sequenceGenerator;

    @Override
    public List<BillingRecoverableResponse> addItems(String documentNumber, AddRecoverableItemsRequest request) {
        BillingDocument document = findDocument(documentNumber);
        List<BillingRecoverableResponse> result = new ArrayList<>();
        for (var item : request.items()) {
            String number = sequenceGenerator.next("billing_recoverable");
            BillingDocumentRecoverable recoverable = BillingDocumentRecoverable.builder()
                    .recoverableNumber(number)
                    .document(document)
                    .itemDescription(item.itemDescription().trim())
                    .quantity(item.quantity() != null ? item.quantity() : BigDecimal.ONE)
                    .unit(trim(item.unit()))
                    .sourceType(trim(item.sourceType()))
                    .sourceCode(trim(item.sourceCode()))
                    .notes(trim(item.notes()))
                    .status("PENDING")
                    .build();
            result.add(toResponse(recoverableRepository.save(recoverable)));
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BillingRecoverableResponse> list(String documentNumber, String status) {
        BillingDocument document = findDocument(documentNumber);
        List<BillingDocumentRecoverable> items = StringUtils.hasText(status)
                ? recoverableRepository.findAllByDocumentAndStatusOrderByCreatedAtAsc(document, status.trim().toUpperCase())
                : recoverableRepository.findAllByDocumentOrderByCreatedAtAsc(document);
        return items.stream().map(this::toResponse).toList();
    }

    @Override
    public BillingRecoverableResponse markRecovered(String documentNumber, String recoverableNumber, RecoverItemRequest request) {
        BillingDocumentRecoverable recoverable = findRecoverable(documentNumber, recoverableNumber);
        if ("RECOVERED".equals(recoverable.getStatus()) || "WRITTEN_OFF".equals(recoverable.getStatus())) {
            throw new BadRequestException("Item is already " + recoverable.getStatus());
        }
        boolean partial = request != null && request.partialQuantity() != null
                && request.partialQuantity().compareTo(recoverable.getQuantity()) < 0;
        recoverable.setStatus(partial ? "PARTIALLY_RECOVERED" : "RECOVERED");
        recoverable.setRecoveredAt(Instant.now());
        if (request != null && StringUtils.hasText(request.notes())) {
            recoverable.setNotes(request.notes().trim());
        }
        return toResponse(recoverableRepository.save(recoverable));
    }

    @Override
    public BillingRecoverableResponse writeOff(String documentNumber, String recoverableNumber, WriteOffRecoverableRequest request) {
        BillingDocumentRecoverable recoverable = findRecoverable(documentNumber, recoverableNumber);
        if ("WRITTEN_OFF".equals(recoverable.getStatus())) {
            throw new BadRequestException("Item is already WRITTEN_OFF");
        }
        recoverable.setStatus("WRITTEN_OFF");
        if (request != null && StringUtils.hasText(request.notes())) {
            recoverable.setNotes(request.notes().trim());
        }
        return toResponse(recoverableRepository.save(recoverable));
    }

    @Override
    @Transactional(readOnly = true)
    public BillingAgingReportResponse agingReport(String customerType, String customerCode, String currency) {
        List<BillingDocument> documents = documentRepository.findForAgingReport(
                StringUtils.hasText(customerType) ? customerType.trim().toUpperCase() : null,
                StringUtils.hasText(customerCode) ? customerCode.trim() : null
        );
        LocalDate today = LocalDate.now();

        List<BillingAgingReportResponse.AgingDocument> agingDocs = new ArrayList<>();
        int cCurrent = 0, c1_30 = 0, c31_60 = 0, c61_90 = 0, cOver90 = 0;
        BigDecimal tCurrent = BigDecimal.ZERO, t1_30 = BigDecimal.ZERO, t31_60 = BigDecimal.ZERO,
                t61_90 = BigDecimal.ZERO, tOver90 = BigDecimal.ZERO;

        for (BillingDocument doc : documents) {
            BigDecimal balanceDue = doc.getBalanceDue();
            LocalDate dueDate = doc.getDueDate();
            long daysOverdue = 0;
            String bucket;
            if (dueDate == null || !today.isAfter(dueDate)) {
                bucket = "CURRENT";
            } else {
                daysOverdue = ChronoUnit.DAYS.between(dueDate, today);
                if (daysOverdue <= 30) bucket = "DAYS_1_TO_30";
                else if (daysOverdue <= 60) bucket = "DAYS_31_TO_60";
                else if (daysOverdue <= 90) bucket = "DAYS_61_TO_90";
                else bucket = "DAYS_OVER_90";
            }

            switch (bucket) {
                case "CURRENT"        -> { cCurrent++;  tCurrent  = tCurrent.add(balanceDue); }
                case "DAYS_1_TO_30"   -> { c1_30++;     t1_30     = t1_30.add(balanceDue); }
                case "DAYS_31_TO_60"  -> { c31_60++;    t31_60    = t31_60.add(balanceDue); }
                case "DAYS_61_TO_90"  -> { c61_90++;    t61_90    = t61_90.add(balanceDue); }
                default               -> { cOver90++;   tOver90   = tOver90.add(balanceDue); }
            }

            agingDocs.add(new BillingAgingReportResponse.AgingDocument(
                    doc.getDocumentNumber(),
                    doc.getCustomerType(),
                    doc.getCustomerCode(),
                    doc.getCustomerName(),
                    doc.getIssueDate(),
                    dueDate,
                    doc.getTotalAmount(),
                    balanceDue,
                    daysOverdue,
                    bucket,
                    doc.getStatus().name()
            ));
        }

        int grandCount = cCurrent + c1_30 + c31_60 + c61_90 + cOver90;
        BigDecimal grandTotal = tCurrent.add(t1_30).add(t31_60).add(t61_90).add(tOver90);

        return new BillingAgingReportResponse(
                Instant.now(),
                currency != null ? currency.toUpperCase() : "XAF",
                new BillingAgingReportResponse.AgingSummary(
                        new BillingAgingReportResponse.AgingBucket(cCurrent, tCurrent),
                        new BillingAgingReportResponse.AgingBucket(c1_30, t1_30),
                        new BillingAgingReportResponse.AgingBucket(c31_60, t31_60),
                        new BillingAgingReportResponse.AgingBucket(c61_90, t61_90),
                        new BillingAgingReportResponse.AgingBucket(cOver90, tOver90),
                        new BillingAgingReportResponse.AgingBucket(grandCount, grandTotal)
                ),
                agingDocs
        );
    }

    private BillingDocumentRecoverable findRecoverable(String documentNumber, String recoverableNumber) {
        BillingDocument document = findDocument(documentNumber);
        BillingDocumentRecoverable recoverable = recoverableRepository.findByRecoverableNumber(recoverableNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Recoverable not found: " + recoverableNumber));
        if (!recoverable.getDocument().getId().equals(document.getId())) {
            throw new BadRequestException("Recoverable does not belong to document " + documentNumber);
        }
        return recoverable;
    }

    private BillingDocument findDocument(String documentNumber) {
        return documentRepository.findByDocumentNumber(documentNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Billing document not found: " + documentNumber));
    }

    private BillingRecoverableResponse toResponse(BillingDocumentRecoverable r) {
        return new BillingRecoverableResponse(
                r.getRecoverableNumber(),
                r.getDocument().getDocumentNumber(),
                r.getItemDescription(),
                r.getQuantity(),
                r.getUnit(),
                r.getSourceType(),
                r.getSourceCode(),
                r.getStatus(),
                r.getNotes(),
                r.getRecoveredAt(),
                r.getRecoveredBy(),
                r.getCreatedAt()
        );
    }

    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
