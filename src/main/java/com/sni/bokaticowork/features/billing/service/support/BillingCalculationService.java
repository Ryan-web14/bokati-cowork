package com.sni.bokaticowork.features.billing.service.support;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.billing.dto.request.CreateBillingDocumentDiscountRequest;
import com.sni.bokaticowork.features.billing.dto.request.CreateBillingDocumentLineRequest;
import com.sni.bokaticowork.features.billing.enums.BillingDiscountType;
import com.sni.bokaticowork.features.billing.enums.BillingLineType;
import com.sni.bokaticowork.features.billing.model.BillingDocumentLine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class BillingCalculationService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final BillingTaxRuleResolver taxRuleResolver;

    public CalculatedDocument calculate(List<CreateBillingDocumentLineRequest> lines,
                                        List<CreateBillingDocumentDiscountRequest> discounts) {
        if (lines == null || lines.isEmpty()) {
            throw new BadRequestException("At least one billing line is required");
        }

        BillingTaxRuleResolver.TaxProfile taxProfile = taxRuleResolver.defaultTaxProfile();
        List<BillingDocumentLine> calculatedLines = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal lineDiscount = BigDecimal.ZERO;
        BigDecimal taxable = BigDecimal.ZERO;
        BigDecimal vat = BigDecimal.ZERO;
        BigDecimal additionalCent = BigDecimal.ZERO;
        BigDecimal tax = BigDecimal.ZERO;
        BigDecimal totalBeforeDocumentDiscount = BigDecimal.ZERO;

        int index = 1;
        for (CreateBillingDocumentLineRequest request : lines) {
            BillingDocumentLine line = calculateLine(request, index++, taxProfile);
            calculatedLines.add(line);
            subtotal = subtotal.add(line.getSubtotalAmount());
            lineDiscount = lineDiscount.add(line.getDiscountAmount());
            taxable = taxable.add(line.getTaxableAmount());
            vat = vat.add(line.getVatAmount());
            additionalCent = additionalCent.add(line.getAdditionalCentAmount());
            tax = tax.add(line.getTaxAmount());
            totalBeforeDocumentDiscount = totalBeforeDocumentDiscount.add(line.getTotalAmount());
        }

        BigDecimal documentDiscount = calculateDocumentDiscounts(totalBeforeDocumentDiscount, discounts);
        BigDecimal discountAmount = money(lineDiscount.add(documentDiscount));
        BigDecimal total = money(totalBeforeDocumentDiscount.subtract(documentDiscount));
        if (total.signum() < 0) {
            throw new BadRequestException("Billing document total cannot be negative");
        }
        return new CalculatedDocument(
                calculatedLines,
                money(subtotal),
                discountAmount,
                money(taxable),
                money(vat),
                money(additionalCent),
                money(tax),
                total
        );
    }

    private BillingDocumentLine calculateLine(CreateBillingDocumentLineRequest request,
                                              int defaultOrder,
                                              BillingTaxRuleResolver.TaxProfile taxProfile) {
        BigDecimal quantity = positiveOrDefault(request.quantity(), BigDecimal.ONE, "Line quantity must be positive");
        BigDecimal unitPrice = positiveOrDefault(request.unitPrice(), BigDecimal.ZERO, "Line unit price cannot be negative");
        BigDecimal subtotal = money(quantity.multiply(unitPrice));
        BigDecimal rateDiscount = percentage(subtotal, request.discountRate());
        BigDecimal fixedDiscount = positiveOrDefault(request.discountAmount(), BigDecimal.ZERO, "Line discount cannot be negative");
        BigDecimal discount = money(rateDiscount.add(fixedDiscount).min(subtotal));
        boolean taxable = request.taxable() == null || request.taxable();
        BigDecimal vatRate = request.vatRate() == null ? taxProfile.vatRate() : request.vatRate();
        BigDecimal additionalCentRate = request.additionalCentRate() == null ? taxProfile.additionalCentRate() : request.additionalCentRate();
        BigDecimal taxableAmount = taxable ? money(subtotal.subtract(discount)) : BigDecimal.ZERO;
        BigDecimal vatAmount = taxable ? percentage(taxableAmount, vatRate) : BigDecimal.ZERO;
        BigDecimal additionalCentAmount = taxable ? percentage(vatAmount, additionalCentRate) : BigDecimal.ZERO;
        BigDecimal taxAmount = money(vatAmount.add(additionalCentAmount));
        BigDecimal total = money(subtotal.subtract(discount).add(taxAmount));

        return BillingDocumentLine.builder()
                .lineOrder(request.lineOrder() == null ? defaultOrder : request.lineOrder())
                .lineType(request.lineType() == null ? BillingLineType.SERVICE : request.lineType())
                .itemCode(trim(request.itemCode()))
                .description(request.description().trim())
                .detailedDescription(trim(request.detailedDescription()))
                .quantity(money(quantity))
                .unitPrice(money(unitPrice))
                .discountRate(request.discountRate() == null ? BigDecimal.ZERO : money(request.discountRate()))
                .discountAmount(discount)
                .taxable(taxable)
                .vatRate(taxable ? money(vatRate) : BigDecimal.ZERO)
                .additionalCentRate(taxable ? money(additionalCentRate) : BigDecimal.ZERO)
                .subtotalAmount(subtotal)
                .taxableAmount(taxableAmount)
                .vatAmount(vatAmount)
                .additionalCentAmount(additionalCentAmount)
                .taxAmount(taxAmount)
                .totalAmount(total)
                .sourceType(trim(request.sourceType()))
                .sourceCode(trim(request.sourceCode()))
                .build();
    }

    private BigDecimal calculateDocumentDiscounts(BigDecimal base, List<CreateBillingDocumentDiscountRequest> discounts) {
        if (discounts == null || discounts.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal amount = BigDecimal.ZERO;
        for (CreateBillingDocumentDiscountRequest discount : discounts) {
            BigDecimal value = positiveOrDefault(discount.value(), BigDecimal.ZERO, "Discount value cannot be negative");
            if (discount.discountType() == BillingDiscountType.PERCENTAGE) {
                amount = amount.add(percentage(base, value));
            } else {
                amount = amount.add(value);
            }
        }
        return money(amount.min(base));
    }

    private BigDecimal percentage(BigDecimal amount, BigDecimal rate) {
        if (rate == null || rate.signum() == 0) {
            return BigDecimal.ZERO;
        }
        if (rate.signum() < 0) {
            throw new BadRequestException("Percentage rate cannot be negative");
        }
        return money(amount.multiply(rate).divide(HUNDRED, 4, RoundingMode.HALF_UP));
    }

    private BigDecimal positiveOrDefault(BigDecimal value, BigDecimal defaultValue, String error) {
        BigDecimal candidate = value == null ? defaultValue : value;
        if (candidate.signum() < 0) {
            throw new BadRequestException(error);
        }
        return candidate;
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(4, RoundingMode.HALF_UP);
    }

    private String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record CalculatedDocument(List<BillingDocumentLine> lines,
                                     BigDecimal subtotalAmount,
                                     BigDecimal discountAmount,
                                     BigDecimal taxableAmount,
                                     BigDecimal vatAmount,
                                     BigDecimal additionalCentAmount,
                                     BigDecimal taxAmount,
                                     BigDecimal totalAmount) {
    }
}
