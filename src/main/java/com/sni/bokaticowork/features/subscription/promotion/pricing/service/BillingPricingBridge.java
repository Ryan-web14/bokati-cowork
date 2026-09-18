package com.sni.bokaticowork.features.subscription.promotion.pricing.service;

import com.sni.bokaticowork.features.billing.dto.request.CreateBillingDocumentDiscountRequest;
import com.sni.bokaticowork.features.billing.dto.request.CreateBillingDocumentLineRequest;
import com.sni.bokaticowork.features.billing.enums.BillingDiscountType;
import com.sni.bokaticowork.features.subscription.promotion.pricing.engine.PricingContext;
import com.sni.bokaticowork.features.subscription.promotion.pricing.engine.PricingEngine;
import com.sni.bokaticowork.features.subscription.promotion.pricing.engine.PricingResult;
import com.sni.bokaticowork.features.subscription.promotion.pricing.enums.DiscountDocumentType;
import com.sni.bokaticowork.features.subscription.promotion.pricing.enums.TargetScope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

/**
 * Fait entrer le moteur de tarification dans la facturation.
 *
 * <p>C'est le branchement qui manquait pour que tout ce qui precede serve a quelque chose. Le moteur
 * savait calculer une remise, la facturation savait en porter une, et rien ne les reliait :
 * {@code billing_document_discount.discount_code} etait un texte libre sans clef vers la campagne
 * qui l'avait produite.</p>
 *
 * <p>Le pont traduit dans les deux sens. A l'aller, les lignes de facture deviennent des lignes
 * tarifables · le moteur ne sait rien des documents de facturation, il manipule une portee et un
 * code. Au retour, chaque regle appliquee devient une remise de facture <b>portant son origine</b>,
 * ce qui rend enfin calculable le cout d'une campagne.</p>
 *
 * <p>Les substitutions de grille ne reviennent pas en remises, et c'est voulu : un tarif negocie
 * remplace le prix, il ne le reduit pas. Il doit donc arriver dans le prix unitaire de la ligne, pas
 * dans une ligne de remise qui laisserait croire a une faveur ponctuelle.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BillingPricingBridge {

    private final PricingEngine pricingEngine;
    private final DiscountApplicationService discountApplicationService;

    /**
     * @param lines     lignes retarifees, prix de grille inclus
     * @param discounts remises a ajouter au document, chacune portant son origine
     */
    public record PricedDocument(
            List<CreateBillingDocumentLineRequest> lines,
            List<CreateBillingDocumentDiscountRequest> discounts,
            PricingResult result
    ) {
    }

    /**
     * Tarife un document avant sa creation, sans rien persister.
     *
     * <p>Le resultat n'engage rien : c'est {@link #confirm} qui trace les remises et consomme les
     * budgets, une fois le document reellement cree. Les separer evite qu'un brouillon recalcule
     * trois fois n'epuise une campagne.</p>
     */
    @Transactional(readOnly = true)
    public PricedDocument price(String customerType,
                                String customerCode,
                                String segment,
                                String currency,
                                String billingCycle,
                                List<CreateBillingDocumentLineRequest> lines,
                                List<String> couponCodes,
                                boolean promotionsAllowed) {
        if (lines == null || lines.isEmpty()) {
            return new PricedDocument(List.of(), List.of(), null);
        }

        PricingContext context = toContext(customerType, customerCode, segment, currency,
                billingCycle, lines, couponCodes, promotionsAllowed);
        PricingResult result = pricingEngine.evaluate(context);

        return new PricedDocument(
                applyPriceOverrides(lines, result),
                result.appliedRules().stream().map(this::toDiscountRequest).toList(),
                result);
    }

    /**
     * Enregistre les remises reellement accordees sur un document existant, et consomme les budgets
     * des campagnes concernees.
     */
    @Transactional
    public void confirm(PricedDocument priced,
                        String customerType,
                        String customerCode,
                        String segment,
                        String currency,
                        String billingCycle,
                        List<String> couponCodes,
                        String documentNumber,
                        String appliedBy) {
        if (priced == null || priced.result() == null || priced.result().appliedRules().isEmpty()) {
            return;
        }
        PricingContext context = toContext(customerType, customerCode, segment, currency,
                billingCycle, priced.lines(), couponCodes, true);
        discountApplicationService.apply(context, DiscountDocumentType.BILLING_DOCUMENT, documentNumber, appliedBy);
    }

    // -----------------------------------------------------------------------------------------

    private PricingContext toContext(String customerType,
                                     String customerCode,
                                     String segment,
                                     String currency,
                                     String billingCycle,
                                     List<CreateBillingDocumentLineRequest> lines,
                                     List<String> couponCodes,
                                     boolean promotionsAllowed) {
        return new PricingContext(
                customerType, customerCode, segment, null, null, null,
                lines.stream().map(this::toPricingLine).toList(),
                billingCycle, "BILLING", null, null, currency,
                couponCodes == null ? List.of() : couponCodes,
                Instant.now(), promotionsAllowed);
    }

    /**
     * Traduit une ligne de facture en objet tarifable.
     *
     * <p>La portee vient de {@code sourceType} lorsqu'elle est renseignee, ce qui est le cas des
     * lignes issues d'un plan, d'un pass ou d'un article. A defaut, la ligne est traitee pour
     * elle-meme · une promotion ciblee ne la touchera pas, ce qui vaut mieux que de lui inventer une
     * portee et de la faire beneficier d'une offre qui ne la visait pas.</p>
     */
    private PricingContext.PricingLine toPricingLine(CreateBillingDocumentLineRequest line) {
        return new PricingContext.PricingLine(
                reference(line),
                scopeOf(line.sourceType()),
                StringUtils.hasText(line.sourceCode()) ? line.sourceCode() : line.itemCode(),
                line.category(),
                line.description(),
                line.quantity() == null ? 1 : line.quantity().intValue(),
                line.unitPrice(),
                null);
    }

    private TargetScope scopeOf(String sourceType) {
        if (!StringUtils.hasText(sourceType)) {
            return TargetScope.LINE;
        }
        try {
            return TargetScope.valueOf(sourceType.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            // Un type de source inconnu du moteur n'est pas une erreur : la facturation en connait
            // que le moteur n'a pas a connaitre. La ligne reste tarifable pour elle-meme.
            return TargetScope.LINE;
        }
    }

    private String reference(CreateBillingDocumentLineRequest line) {
        if (StringUtils.hasText(line.externalReference())) {
            return line.externalReference();
        }
        if (StringUtils.hasText(line.itemCode())) {
            return line.itemCode();
        }
        return line.lineOrder() == null ? line.description() : String.valueOf(line.lineOrder());
    }

    /**
     * Reporte les prix de grille dans les lignes du document. Le prix negocie devient le prix de la
     * ligne, sans jamais apparaitre comme une remise.
     */
    private List<CreateBillingDocumentLineRequest> applyPriceOverrides(List<CreateBillingDocumentLineRequest> lines,
                                                                       PricingResult result) {
        if (result.priceOverrides().isEmpty()) {
            return lines;
        }
        return lines.stream().map(line -> {
            String reference = reference(line);
            return result.priceOverrides().stream()
                    .filter(override -> reference.equals(override.lineReference()))
                    .findFirst()
                    .map(override -> withUnitPrice(line, override.effectiveUnitPrice()))
                    .orElse(line);
        }).toList();
    }

    private CreateBillingDocumentLineRequest withUnitPrice(CreateBillingDocumentLineRequest line, BigDecimal unitPrice) {
        return new CreateBillingDocumentLineRequest(
                line.lineOrder(), line.lineType(), line.itemCode(), line.description(),
                line.detailedDescription(), line.quantity(), unitPrice, line.discountRate(),
                line.discountAmount(), line.taxable(), line.taxIncluded(), line.vatRate(),
                line.additionalCentRate(), line.sourceType(), line.sourceCode(), line.unit(),
                line.externalReference(), line.notes(), line.optional(), line.category());
    }

    private CreateBillingDocumentDiscountRequest toDiscountRequest(PricingResult.AppliedRule rule) {
        return new CreateBillingDocumentDiscountRequest(
                rule.sourceCode(),
                StringUtils.hasText(rule.sourceName()) ? rule.sourceName() : rule.sourceCode(),
                // Le moteur a deja fait le calcul · la facture recoit un montant, pas un taux a
                // recalculer. Recalculer ici ferait diverger les deux et personne ne saurait lequel
                // fait foi.
                BillingDiscountType.FIXED_AMOUNT,
                rule.discountAmount(),
                rule.sourceType().name(),
                rule.sourceCode());
    }
}
