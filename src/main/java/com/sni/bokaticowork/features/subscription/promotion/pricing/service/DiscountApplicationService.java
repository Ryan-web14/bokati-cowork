package com.sni.bokaticowork.features.subscription.promotion.pricing.service;

import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.subscription.promotion.model.Promotion;
import com.sni.bokaticowork.features.subscription.promotion.pricing.engine.PricingContext;
import com.sni.bokaticowork.features.subscription.promotion.pricing.engine.PricingEngine;
import com.sni.bokaticowork.features.subscription.promotion.pricing.engine.PricingResult;
import com.sni.bokaticowork.features.subscription.promotion.pricing.enums.DiscountDocumentType;
import com.sni.bokaticowork.features.subscription.promotion.pricing.enums.DiscountSourceType;
import com.sni.bokaticowork.features.subscription.promotion.pricing.model.AppliedDiscount;
import com.sni.bokaticowork.features.subscription.promotion.pricing.repository.AppliedDiscountRepository;
import com.sni.bokaticowork.features.subscription.promotion.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Transforme une evaluation en remise reellement accordee.
 *
 * <p>Evaluer et appliquer sont deux gestes distincts, et les confondre serait une faute : afficher
 * un panier consommerait alors une campagne, et un client qui regarde trois fois son panier
 * epuiserait un budget sans rien acheter. {@link PricingEngine} calcule et ne persiste rien ; ce
 * service est le seul a ecrire.</p>
 *
 * <p>La consommation du budget se fait <b>dans la transaction appelante</b>, sous verrou optimiste.
 * Une campagne epuisee cesse donc de s'appliquer immediatement, y compris sous forte concurrence,
 * ce qui est exactement la situation d'une campagne qui marche.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DiscountApplicationService {

    private final PricingEngine pricingEngine;
    private final PromotionRepository promotionRepository;
    private final AppliedDiscountRepository appliedDiscountRepository;
    private final SequenceGeneratorFacade sequenceGenerator;
    private final com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.service.CouponService couponService;

    /**
     * Evalue puis enregistre les remises sur le document designe.
     *
     * @param documentType nature du document remise, un document de facturation n'etant pas le seul
     *                     support possible · un abonnement et un pass en sont aussi
     * @return le resultat de l'evaluation, celui-la meme qui a ete applique
     */
    @Transactional
    public PricingResult apply(PricingContext context,
                               DiscountDocumentType documentType,
                               String documentCode,
                               String appliedBy) {
        PricingResult result = pricingEngine.evaluate(context);
        if (result.appliedRules().isEmpty()) {
            return result;
        }

        for (PricingResult.AppliedRule rule : result.appliedRules()) {
            record(rule, context, documentType, documentCode, appliedBy);
            if (rule.sourceType() == DiscountSourceType.PROMOTION) {
                consume(rule.sourceCode(), rule.discountAmount());
            } else if (rule.sourceType() == DiscountSourceType.COUPON) {
                // Le code est consomme ici, pas a la retenue · un panier abandonne ne le perd pas
                couponService.consume(rule.sourceCode(), context.subscriberType(), context.subscriberCode(),
                        rule.discountAmount(), context.currency(), documentCode, appliedBy);
            }
        }
        return result;
    }

    /**
     * Contre-passe une remise. La ligne n'est pas supprimee : une remise qui disparait sans trace
     * rend le cout d'une campagne incalculable retroactivement.
     */
    @Transactional
    public void reverse(DiscountDocumentType documentType, String documentCode, String reason, String reversedBy) {
        appliedDiscountRepository.findActiveByDocument(documentType.name(), documentCode)
                .forEach(discount -> {
                    discount.setReversedAt(java.time.Instant.now());
                    discount.setReversalReason(reason);
                    discount.setReversedBy(reversedBy);
                    appliedDiscountRepository.save(discount);
                    release(discount);
                });
    }

    // -----------------------------------------------------------------------------------------

    private void record(PricingResult.AppliedRule rule,
                        PricingContext context,
                        DiscountDocumentType documentType,
                        String documentCode,
                        String appliedBy) {
        BigDecimal original = rule.originalAmount() == null ? BigDecimal.ZERO : rule.originalAmount();
        appliedDiscountRepository.save(AppliedDiscount.builder()
                .discountNumber(sequenceGenerator.next("applied_discount"))
                .sourceType(rule.sourceType())
                .sourceCode(rule.sourceCode())
                .documentType(documentType)
                .documentCode(documentCode)
                .lineReference(rule.lineReference())
                .subscriberType(context.subscriberType())
                .subscriberCode(context.subscriberCode())
                .originalAmount(original)
                .discountAmount(rule.discountAmount())
                .finalAmount(original.subtract(rule.discountAmount()))
                .currency(context.currency())
                .reason(rule.explanation())
                .appliedBy(appliedBy)
                .build());
    }

    private void consume(String promotionCode, BigDecimal amount) {
        Promotion promotion = promotionRepository.findByCodeIgnoreCase(promotionCode).orElse(null);
        if (promotion == null) {
            log.warn("Promotion {} introuvable au moment de consommer son budget", promotionCode);
            return;
        }
        promotion.setConsumedBudgetAmount(nonNull(promotion.getConsumedBudgetAmount()).add(amount));
        promotion.setTotalDiscountGranted(nonNull(promotion.getTotalDiscountGranted()).add(amount));
        promotion.setRedemptionCount(promotion.getRedemptionCount() == null ? 1 : promotion.getRedemptionCount() + 1);
        promotionRepository.save(promotion);
    }

    private void release(AppliedDiscount discount) {
        if (discount.getSourceType() != DiscountSourceType.PROMOTION) {
            return;
        }
        Promotion promotion = promotionRepository.findByCodeIgnoreCase(discount.getSourceCode()).orElse(null);
        if (promotion == null) {
            return;
        }
        BigDecimal consumed = nonNull(promotion.getConsumedBudgetAmount()).subtract(discount.getDiscountAmount());
        promotion.setConsumedBudgetAmount(consumed.signum() < 0 ? BigDecimal.ZERO : consumed);
        // total_discount_granted n'est pas decremente : il mesure ce qui a ete accorde dans
        // l'histoire de la campagne, pas ce qu'elle doit encore.
        promotion.setRedemptionCount(Math.max(0, promotion.getRedemptionCount() == null ? 0 : promotion.getRedemptionCount() - 1));
        promotionRepository.save(promotion);
    }

    private BigDecimal nonNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
