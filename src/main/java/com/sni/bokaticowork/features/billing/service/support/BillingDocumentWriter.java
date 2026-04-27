package com.sni.bokaticowork.features.billing.service.support;

import com.sni.bokaticowork.features.billing.dto.request.CreateBillingDocumentClauseRequest;
import com.sni.bokaticowork.features.billing.dto.request.CreateBillingDocumentDiscountRequest;
import com.sni.bokaticowork.features.billing.enums.BillingDiscountType;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.model.BillingDocumentClause;
import com.sni.bokaticowork.features.billing.model.BillingDocumentDiscount;
import com.sni.bokaticowork.features.billing.model.BillingDocumentLine;
import com.sni.bokaticowork.features.billing.model.BillingDocumentTax;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentClauseRepository;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentDiscountRepository;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentLineRepository;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentTaxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
@RequiredArgsConstructor
public class BillingDocumentWriter {

    private final BillingDocumentRepository documentRepository;
    private final BillingDocumentLineRepository lineRepository;
    private final BillingDocumentDiscountRepository discountRepository;
    private final BillingDocumentTaxRepository taxRepository;
    private final BillingDocumentClauseRepository clauseRepository;

    public BillingDocument save(BillingDocument document,
                                BillingCalculationService.CalculatedDocument calculation,
                                List<CreateBillingDocumentDiscountRequest> discounts,
                                List<CreateBillingDocumentClauseRequest> clauses) {
        BillingDocument saved = documentRepository.save(document);
        calculation.lines().forEach(line -> {
            line.setDocument(saved);
            lineRepository.save(line);
        });
        saveDiscounts(saved, discounts, calculation.totalAmount());
        saveTaxes(saved, calculation);
        saveClauses(saved, clauses);
        return saved;
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
