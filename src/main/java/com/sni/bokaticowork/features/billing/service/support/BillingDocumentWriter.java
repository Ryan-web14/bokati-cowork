package com.sni.bokaticowork.features.billing.service.support;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.billing.dto.request.CreateBillingDocumentAdvanceRequest;
import com.sni.bokaticowork.features.billing.dto.request.CreateBillingDocumentClauseRequest;
import com.sni.bokaticowork.features.billing.dto.request.CreateBillingDocumentDiscountRequest;
import com.sni.bokaticowork.features.billing.dto.request.CreateEarlyPaymentDiscountRequest;
import com.sni.bokaticowork.features.billing.enums.BillingAdvanceType;
import com.sni.bokaticowork.features.billing.enums.BillingDiscountType;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.model.BillingDocumentAdvance;
import com.sni.bokaticowork.features.billing.model.BillingDocumentClause;
import com.sni.bokaticowork.features.billing.model.BillingDocumentDiscount;
import com.sni.bokaticowork.features.billing.model.BillingDocumentEarlyPaymentDiscount;
import com.sni.bokaticowork.features.billing.model.BillingDocumentLine;
import com.sni.bokaticowork.features.billing.model.BillingDocumentTax;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentAdvanceRepository;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentClauseRepository;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentDiscountRepository;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentEarlyPaymentDiscountRepository;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentLineRepository;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentTaxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class BillingDocumentWriter {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final BillingDocumentRepository documentRepository;
    private final BillingDocumentLineRepository lineRepository;
    private final BillingDocumentDiscountRepository discountRepository;
    private final BillingDocumentTaxRepository taxRepository;
    private final BillingDocumentClauseRepository clauseRepository;
    private final BillingDocumentAdvanceRepository advanceRepository;
    private final BillingDocumentEarlyPaymentDiscountRepository earlyPaymentDiscountRepository;

    public BillingDocument save(BillingDocument document,
                                BillingCalculationService.CalculatedDocument calculation,
                                List<CreateBillingDocumentDiscountRequest> discounts,
                                List<CreateBillingDocumentClauseRequest> clauses,
                                CreateBillingDocumentAdvanceRequest advance) {
        BillingDocument saved = documentRepository.save(document);
        calculation.lines().forEach(line -> {
            line.setDocument(saved);
            lineRepository.save(line);
        });
        saveDiscounts(saved, discounts, calculation.totalAmount());
        saveTaxes(saved, calculation);
        saveClauses(saved, clauses);
        if (advance != null) {
            saveAdvance(saved, advance, calculation.lines());
        }
        return saved;
    }

    public void saveEarlyPaymentDiscount(BillingDocument document, CreateEarlyPaymentDiscountRequest request) {
        if (request == null) {
            return;
        }
        BigDecimal computed = document.getTotalAmount()
                .multiply(request.discountRate())
                .divide(HUNDRED, 4, java.math.RoundingMode.HALF_UP);
        String label = request.label() != null && !request.label().isBlank()
                ? request.label().trim()
                : String.format("Escompte %.2f%% si règlement avant le %s",
                        request.discountRate(),
                        request.ifPaidBefore().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        earlyPaymentDiscountRepository.save(BillingDocumentEarlyPaymentDiscount.builder()
                .document(document)
                .discountRate(request.discountRate())
                .ifPaidBefore(request.ifPaidBefore())
                .computedAmount(computed)
                .label(label)
                .build());
    }

    private void saveAdvance(BillingDocument document,
                             CreateBillingDocumentAdvanceRequest request,
                             List<BillingDocumentLine> lines) {
        List<Integer> included = request.includedLineOrders();
        List<Integer> excluded = request.excludedLineOrders();

        List<BillingDocumentLine> baseLines;
        if (included != null && !included.isEmpty()) {
            Set<Integer> includeSet = Set.copyOf(included);
            baseLines = lines.stream().filter(l -> includeSet.contains(l.getLineOrder())).toList();
        } else if (excluded != null && !excluded.isEmpty()) {
            Set<Integer> excludeSet = Set.copyOf(excluded);
            baseLines = lines.stream().filter(l -> !excludeSet.contains(l.getLineOrder())).toList();
        } else {
            baseLines = lines;
        }

        BigDecimal base = baseLines.stream()
                .map(BillingDocumentLine::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal computed;
        if (request.advanceType() == BillingAdvanceType.PERCENTAGE) {
            computed = base.multiply(request.advanceValue()).divide(HUNDRED, 4, RoundingMode.HALF_UP);
        } else {
            computed = request.advanceValue().min(base);
        }
        computed = computed.setScale(4, RoundingMode.HALF_UP);

        if (computed.signum() <= 0) {
            throw new BadRequestException("Le montant calculé de l'acompte doit être positif");
        }

        advanceRepository.save(BillingDocumentAdvance.builder()
                .document(document)
                .advanceType(request.advanceType())
                .advanceValue(request.advanceValue())
                .computedAmount(computed)
                .includedLineOrders(serializeOrders(included))
                .excludedLineOrders(serializeOrders(excluded))
                .paymentReference(trim(request.paymentReference()))
                .referenceLabel(trim(request.referenceLabel()))
                .dueDate(request.dueDate())
                .notes(trim(request.notes()))
                .build());
    }

    private String serializeOrders(List<Integer> orders) {
        if (orders == null || orders.isEmpty()) {
            return null;
        }
        return orders.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    private String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void saveDiscounts(BillingDocument document, List<CreateBillingDocumentDiscountRequest> discounts, BigDecimal baseAmount) {
        if (discounts == null || discounts.isEmpty()) {
            return;
        }
        for (CreateBillingDocumentDiscountRequest request : discounts) {
            BigDecimal amount = request.discountType() == BillingDiscountType.PERCENTAGE
                    ? baseAmount.multiply(request.value()).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)
                    : request.value();
            discountRepository.save(BillingDocumentDiscount.builder()
                    .document(document)
                    .discountCode(request.discountCode())
                    .description(request.description().trim())
                    .discountType(request.discountType())
                    .value(request.value())
                    .amount(amount)
                    .build());
        }
    }

    private void saveTaxes(BillingDocument document, BillingCalculationService.CalculatedDocument calculation) {
        if (calculation.vatAmount().signum() > 0) {
            taxRepository.save(BillingDocumentTax.builder()
                    .document(document)
                    .taxCode("VAT")
                    .taxName("TVA")
                    .rate(BigDecimal.ZERO)
                    .taxableAmount(calculation.taxableAmount())
                    .taxAmount(calculation.vatAmount())
                    .build());
        }
        if (calculation.additionalCentAmount().signum() > 0) {
            taxRepository.save(BillingDocumentTax.builder()
                    .document(document)
                    .taxCode("ADDITIONAL_CENT")
                    .taxName("Centime additionnel")
                    .rate(BigDecimal.ZERO)
                    .taxableAmount(calculation.vatAmount())
                    .taxAmount(calculation.additionalCentAmount())
                    .build());
        }
    }

    private void saveClauses(BillingDocument document, List<CreateBillingDocumentClauseRequest> clauses) {
        if (clauses == null || clauses.isEmpty()) {
            return;
        }
        int order = 1;
        for (CreateBillingDocumentClauseRequest request : clauses) {
            clauseRepository.save(BillingDocumentClause.builder()
                    .document(document)
                    .clauseCode(request.clauseCode())
                    .title(request.title().trim())
                    .body(request.body().trim())
                    .displayOrder(request.displayOrder() == null ? order : request.displayOrder())
                    .build());
            order++;
        }
    }
}
