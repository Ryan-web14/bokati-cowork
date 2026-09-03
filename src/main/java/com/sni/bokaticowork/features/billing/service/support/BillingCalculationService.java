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

        // ── La remise globale porte sur le TTC ───────────────────────────────
        //
        // C'est ce qu'attend celui qui l'accorde : « je fais 10 000 de remise » veut dire que le
        // client paie 10 000 de moins, pas 11 890. Deduite du HT, une remise de 10 000 emportait
        // aussi 1 800 de TVA et 90 de centimes : le total baissait de 11 890 sans que personne ne
        // l'ait demande. Sur un pourcentage l'ecart ne se voyait pas · le calcul est lineaire · 
        // ce qui rendait le defaut invisible jusqu'a la premiere remise en valeur.
        BigDecimal grossTotal = money(taxableNet.add(nonTaxableNet).add(vat).add(additionalCent));
        DocumentDiscounts applied = applyDocumentDiscounts(grossTotal, discounts);
        BigDecimal documentDiscount = applied.total();
        BigDecimal netTotal = money(grossTotal.subtract(documentDiscount));

        // Le TTC remise est ensuite recompose : la remise reduit tout au prorata, base taxable,
        // TVA, centimes et part exoneree. Sans cela la somme des composantes ne redonnerait plus
        // le total, et la facture ne se ventilerait pas en comptabilite.
        BigDecimal keepRatio = grossTotal.signum() > 0
                ? netTotal.divide(grossTotal, 8, RoundingMode.HALF_UP)
                : BigDecimal.ONE;
        BigDecimal adjustedTaxable = money(taxableNet.multiply(keepRatio));
        BigDecimal adjustedNonTaxable = money(nonTaxableNet.multiply(keepRatio));
        BigDecimal adjustedVat = money(vat.multiply(keepRatio));
        BigDecimal adjustedAdditionalCent = money(additionalCent.multiply(keepRatio));
        BigDecimal adjustedTax = money(adjustedVat.add(adjustedAdditionalCent));

        // L'arrondi des quatre composantes peut s'ecarter d'un franc du total attendu · on le
        // reporte sur la base taxable, la plus grande, pour que l'egalite tienne exactement.
        BigDecimal recomposed = money(adjustedTaxable.add(adjustedNonTaxable).add(adjustedTax));
        BigDecimal drift = money(netTotal.subtract(recomposed));
        adjustedTaxable = money(adjustedTaxable.add(drift));
        BigDecimal total = money(adjustedTaxable.add(adjustedNonTaxable).add(adjustedTax));

        // La remise reportee sur le document reste exprimee en HT, comme les remises de ligne :
        // c'est la reduction reelle de l'assiette, et c'est elle qui doit se retrouver dans les
        // ecritures. Le montant TTC accorde figure sur chaque ligne de remise.
        BigDecimal discountOnBase = money(taxableNet.add(nonTaxableNet)
                .subtract(adjustedTaxable).subtract(adjustedNonTaxable));
        BigDecimal discountAmount = money(lineDiscount.add(discountOnBase));

        if (total.signum() < 0) {
            throw new BadRequestException("Billing document total cannot be negative");
        }
        return new CalculatedDocument(
                calculatedLines,
                money(subtotal),
                discountAmount,
                applied.amounts(),
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

    /**
     * Remises de document, chacune chiffree, sur la base TTC.
     *
     * <p>Les pourcentages sont <b>additifs</b> : chacun porte sur la meme base, jamais sur le reste
     * apres le precedent. 10 % puis 5 % retirent 15 %, et l'ordre des remises n'a aucune
     * incidence · une remise en cascade rendrait l'ordre significatif sans que rien a l'ecran ne
     * dise laquelle s'applique en premier.
     *
     * <p>Au-dela de la base, refus. Le montant etait auparavant ramene en silence a la base :
     * cent quarante pour cent devenaient cent, la facture ressortait a zero, et le garde-fou
     * d'emission annoncait « 100 % » sans jamais voir les 140 % demandes.
     */
    private DocumentDiscounts applyDocumentDiscounts(BigDecimal base,
                                                     List<CreateBillingDocumentDiscountRequest> discounts) {
        if (discounts == null || discounts.isEmpty()) {
            return new DocumentDiscounts(BigDecimal.ZERO, List.of());
        }
        List<BigDecimal> amounts = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (CreateBillingDocumentDiscountRequest discount : discounts) {
            BigDecimal value = positiveOrDefault(discount.value(), BigDecimal.ZERO,
                    "Discount value cannot be negative");
            BigDecimal amount = discount.discountType() == BillingDiscountType.PERCENTAGE
                    ? money(percentage(base, value))
                    : money(value);
            amounts.add(amount);
            total = total.add(amount);
        }
        total = money(total);
        if (total.compareTo(base) > 0) {
            throw new BadRequestException("Les remises demandées, " + total
                    + ", dépassent le total du document, " + base
                    + ". Une remise ne peut pas excéder 100 % : corrigez les taux ou les montants.");
        }
        return new DocumentDiscounts(total, amounts);
    }

    /** @param amounts montant accorde par remise, dans l'ordre de la demande */
    public record DocumentDiscounts(BigDecimal total, List<BigDecimal> amounts) {
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

    /**
     * @param discountAmounts montant accorde par remise de document, dans l'ordre de la demande ·
     *                        le writer les enregistre tels quels. Ils etaient auparavant
     *                        recalcules a l'ecriture, sur une autre base, si bien que le detail
     *                        contredisait le total : 10 701 stockes pour 10 000 deduits.
     */
    public record CalculatedDocument(List<BillingDocumentLine> lines,
                                     BigDecimal subtotalAmount,
                                     BigDecimal discountAmount,
                                     List<BigDecimal> discountAmounts,
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
