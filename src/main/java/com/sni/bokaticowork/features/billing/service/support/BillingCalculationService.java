package com.sni.bokaticowork.features.billing.service.support;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.billing.dto.request.CreateBillingDocumentDiscountRequest;
import com.sni.bokaticowork.features.billing.dto.request.CreateBillingDocumentLineRequest;
import com.sni.bokaticowork.features.billing.enums.BillingDiscountType;
import com.sni.bokaticowork.features.billing.enums.BillingLineType;
import com.sni.bokaticowork.features.billing.model.BillingDocumentLine;
import com.sni.bokaticowork.features.billing.model.ServiceCatalogItem;
import com.sni.bokaticowork.features.billing.repository.ServiceCatalogItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class BillingCalculationService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final BillingTaxRuleResolver taxRuleResolver;
    private final ServiceCatalogItemRepository catalogItemRepository;

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
        // Net HT des lignes exonerees. Indispensable : leur taxableAmount vaut zero par
        // construction, donc les additionner a la base taxable les ferait disparaitre du total.
        BigDecimal nonTaxableNet = BigDecimal.ZERO;

        int index = 1;
        for (CreateBillingDocumentLineRequest request : lines) {
            BillingDocumentLine line = calculateLine(request, index++, taxProfile);
            calculatedLines.add(line);
            if (Boolean.TRUE.equals(line.getOptional())) {
                // lignes optionnelles : calculées individuellement mais exclues des totaux document
                continue;
            }
            subtotal = subtotal.add(line.getSubtotalAmount());
            lineDiscount = lineDiscount.add(line.getDiscountAmount());
            taxable = taxable.add(line.getTaxableAmount());
            vat = vat.add(line.getVatAmount());
            additionalCent = additionalCent.add(line.getAdditionalCentAmount());
            tax = tax.add(line.getTaxAmount());
            if (!Boolean.TRUE.equals(line.getTaxable())) {
                nonTaxableNet = nonTaxableNet.add(
                        money(line.getSubtotalAmount().subtract(line.getDiscountAmount())));
            }
        }

        // La remise globale s'applique sur le HT net de TOUTES les lignes, exonerees comprises.
        //
        // Le total du document se calculait auparavant comme "base taxable + taxes". Comme
        // calculateTaxExcludedAmounts force taxableAmount a zero sur une ligne exoneree, toute
        // ligne non taxable etait comptee pour zero dans le total : un avoir - dont la ligne
        // d'ajustement est toujours taxable=false - ressortait a 0,00 et son application levait
        // "Payment amount must be positive", rendant impossible l'annulation d'une facture scellee.
        BigDecimal taxableNet = taxable;
        BigDecimal netHT = money(taxableNet.add(nonTaxableNet));
        BigDecimal documentDiscount = calculateDocumentDiscounts(netHT, discounts);
        BigDecimal discountAmount = money(lineDiscount.add(documentDiscount));

        // La remise globale est repartie au prorata entre part taxable et part exoneree : seule
        // la fraction imputee au taxable doit reduire l'assiette et donc les taxes.
        BigDecimal taxableShare = netHT.signum() > 0
                ? taxableNet.divide(netHT, 8, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal discountOnTaxable = money(documentDiscount.multiply(taxableShare));
        BigDecimal adjustedTaxable = money(taxableNet.subtract(discountOnTaxable));
        BigDecimal adjustedNonTaxable = money(nonTaxableNet.subtract(documentDiscount.subtract(discountOnTaxable)));

        // Réduction proportionnelle des taxes sur la base ajustée
        BigDecimal scaleFactor = taxableNet.compareTo(BigDecimal.ZERO) > 0
                ? adjustedTaxable.divide(taxableNet, 8, RoundingMode.HALF_UP)
                : BigDecimal.ONE;
        BigDecimal adjustedVat = money(vat.multiply(scaleFactor));
        BigDecimal adjustedAdditionalCent = money(additionalCent.multiply(scaleFactor));
        BigDecimal adjustedTax = money(adjustedVat.add(adjustedAdditionalCent));
        BigDecimal total = money(adjustedTaxable.add(adjustedNonTaxable).add(adjustedTax));

        if (total.signum() < 0) {
            throw new BadRequestException("Billing document total cannot be negative");
        }
        return new CalculatedDocument(
                calculatedLines,
                money(subtotal),
                discountAmount,
                adjustedTaxable,
                adjustedVat,
                adjustedAdditionalCent,
                adjustedTax,
                total
        );
    }

    /** Valeurs reprises du catalogue quand la ligne ne les precise pas. */
    private record CatalogDefaults(String category, String unit) {
        static final CatalogDefaults NONE = new CatalogDefaults(null, null);
    }

    private CatalogDefaults catalogDefaults(String itemCode) {
        if (!StringUtils.hasText(itemCode)) {
            return CatalogDefaults.NONE;
        }
        Optional<ServiceCatalogItem> item = catalogItemRepository.findByItemCode(itemCode.trim());
        return item.map(found -> new CatalogDefaults(found.getCategory(), found.getUnit()))
                .orElse(CatalogDefaults.NONE);
    }

    private String firstNonBlank(String preferred, String fallback) {
        if (StringUtils.hasText(preferred)) {
            return preferred.trim();
        }
        return StringUtils.hasText(fallback) ? fallback.trim() : null;
    }

    private BillingDocumentLine calculateLine(CreateBillingDocumentLineRequest request,
                                              int defaultOrder,
                                              BillingTaxRuleResolver.TaxProfile taxProfile) {
        BigDecimal quantity = positiveOrDefault(request.quantity(), BigDecimal.ONE, "Line quantity must be positive");
        BigDecimal unitPrice = positiveOrDefault(request.unitPrice(), BigDecimal.ZERO, "Line unit price cannot be negative");
        boolean taxable = request.taxable() == null || request.taxable();
        boolean taxIncluded = taxable && Boolean.TRUE.equals(request.taxIncluded());
        BigDecimal vatRate = request.vatRate() == null ? taxProfile.vatRate() : request.vatRate();
        BigDecimal additionalCentRate = request.additionalCentRate() == null ? taxProfile.additionalCentRate() : request.additionalCentRate();
        LineAmounts amounts = taxIncluded
                ? calculateTaxIncludedAmounts(quantity, unitPrice, request.discountRate(), request.discountAmount(), vatRate, additionalCentRate)
                : calculateTaxExcludedAmounts(quantity, unitPrice, request.discountRate(), request.discountAmount(), taxable, vatRate, additionalCentRate);

        // Categorie et unite sont figees sur la ligne. Quand elles ne sont pas fournies et que la
        // ligne designe un article du catalogue, on les en reprend : sans ce repli, elles
        // resteraient nulles dans la quasi-totalite des cas et n'apparaitraient jamais au PDF.
        CatalogDefaults defaults = catalogDefaults(request.itemCode());

        return BillingDocumentLine.builder()
                .lineOrder(request.lineOrder() == null ? defaultOrder : request.lineOrder())
                .lineType(request.lineType() == null ? BillingLineType.SERVICE : request.lineType())
                .itemCode(trim(request.itemCode()))
                .category(firstNonBlank(request.category(), defaults.category()))
                .description(request.description().trim())
                .detailedDescription(trim(request.detailedDescription()))
                .quantity(money(quantity))
                .unit(firstNonBlank(request.unit(), defaults.unit()))
                .unitPrice(money(unitPrice))
                .discountRate(request.discountRate() == null ? BigDecimal.ZERO : money(request.discountRate()))
                .discountAmount(amounts.discountAmount())
                .taxable(taxable)
                .taxIncluded(taxIncluded)
                .vatRate(taxable ? money(vatRate) : BigDecimal.ZERO)
                .additionalCentRate(taxable ? money(additionalCentRate) : BigDecimal.ZERO)
                .subtotalAmount(amounts.subtotalAmount())
                .taxableAmount(amounts.taxableAmount())
                .vatAmount(amounts.vatAmount())
                .additionalCentAmount(amounts.additionalCentAmount())
                .taxAmount(amounts.taxAmount())
                .totalAmount(amounts.totalAmount())
                .sourceType(trim(request.sourceType()))
                .sourceCode(trim(request.sourceCode()))
                .externalReference(trim(request.externalReference()))
                .notes(trim(request.notes()))
                .optional(Boolean.TRUE.equals(request.optional()))
                .build();
    }

    private LineAmounts calculateTaxExcludedAmounts(BigDecimal quantity,
                                                    BigDecimal unitPrice,
                                                    BigDecimal discountRate,
                                                    BigDecimal discountAmount,
                                                    boolean taxable,
                                                    BigDecimal vatRate,
                                                    BigDecimal additionalCentRate) {
        BigDecimal subtotal = money(quantity.multiply(unitPrice));
        BigDecimal rateDiscount = percentage(subtotal, discountRate);
        BigDecimal fixedDiscount = positiveOrDefault(discountAmount, BigDecimal.ZERO, "Line discount cannot be negative");
        BigDecimal discount = money(rateDiscount.add(fixedDiscount).min(subtotal));
        BigDecimal taxableAmount = taxable ? money(subtotal.subtract(discount)) : BigDecimal.ZERO;
        BigDecimal vatAmount = taxable ? percentage(taxableAmount, vatRate) : BigDecimal.ZERO;
        BigDecimal additionalCentAmount = taxable ? percentage(vatAmount, additionalCentRate) : BigDecimal.ZERO;
        BigDecimal taxAmount = money(vatAmount.add(additionalCentAmount));
        BigDecimal total = money(subtotal.subtract(discount).add(taxAmount));
        return new LineAmounts(subtotal, discount, taxableAmount, vatAmount, additionalCentAmount, taxAmount, total);
    }

    private LineAmounts calculateTaxIncludedAmounts(BigDecimal quantity,
                                                    BigDecimal unitPrice,
                                                    BigDecimal discountRate,
                                                    BigDecimal discountAmount,
                                                    BigDecimal vatRate,
                                                    BigDecimal additionalCentRate) {
        BigDecimal grossSubtotal = money(quantity.multiply(unitPrice));
        BigDecimal grossRateDiscount = percentage(grossSubtotal, discountRate);
        BigDecimal grossFixedDiscount = positiveOrDefault(discountAmount, BigDecimal.ZERO, "Line discount cannot be negative");
        BigDecimal grossDiscount = money(grossRateDiscount.add(grossFixedDiscount).min(grossSubtotal));
        BigDecimal grossTotal = money(grossSubtotal.subtract(grossDiscount));
        BigDecimal factor = taxFactor(vatRate, additionalCentRate);

        BigDecimal subtotal = money(grossSubtotal.divide(factor, 8, RoundingMode.HALF_UP));
        BigDecimal taxableAmount = money(grossTotal.divide(factor, 8, RoundingMode.HALF_UP));
        BigDecimal discount = money(subtotal.subtract(taxableAmount));
        BigDecimal taxAmount = money(grossTotal.subtract(taxableAmount));
        BigDecimal vatAmount = percentage(taxableAmount, vatRate);
        BigDecimal additionalCentAmount = money(taxAmount.subtract(vatAmount));
        if (additionalCentAmount.signum() < 0) {
            vatAmount = taxAmount;
            additionalCentAmount = BigDecimal.ZERO;
        }
        return new LineAmounts(subtotal, discount, taxableAmount, vatAmount, additionalCentAmount, taxAmount, grossTotal);
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

    private BigDecimal taxFactor(BigDecimal vatRate, BigDecimal additionalCentRate) {
        BigDecimal vatFactor = rateFactor(vatRate);
        BigDecimal additionalCentFactor = rateFactor(additionalCentRate);
        return BigDecimal.ONE.add(vatFactor).add(vatFactor.multiply(additionalCentFactor));
    }

    private BigDecimal rateFactor(BigDecimal rate) {
        if (rate == null || rate.signum() == 0) {
            return BigDecimal.ZERO;
        }
        if (rate.signum() < 0) {
            throw new BadRequestException("Percentage rate cannot be negative");
        }
        return rate.divide(HUNDRED, 8, RoundingMode.HALF_UP);
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

    private record LineAmounts(BigDecimal subtotalAmount,
                               BigDecimal discountAmount,
                               BigDecimal taxableAmount,
                               BigDecimal vatAmount,
                               BigDecimal additionalCentAmount,
                               BigDecimal taxAmount,
                               BigDecimal totalAmount) {
    }
}
