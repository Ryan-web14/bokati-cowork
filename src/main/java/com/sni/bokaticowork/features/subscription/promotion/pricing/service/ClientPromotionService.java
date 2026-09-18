package com.sni.bokaticowork.features.subscription.promotion.pricing.service;

import com.sni.bokaticowork.features.subscription.promotion.model.Promotion;
import com.sni.bokaticowork.features.subscription.promotion.pricing.audience.service.PromotionAudienceFilter;
import com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.model.Coupon;
import com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.service.CouponService;
import com.sni.bokaticowork.features.subscription.promotion.pricing.dto.ClientPromotionView;
import com.sni.bokaticowork.features.subscription.promotion.pricing.engine.PricingContext;
import com.sni.bokaticowork.features.subscription.promotion.pricing.engine.PricingEngine;
import com.sni.bokaticowork.features.subscription.promotion.pricing.engine.PricingResult;
import com.sni.bokaticowork.features.subscription.promotion.pricing.enums.DiscountSourceType;
import com.sni.bokaticowork.features.subscription.promotion.pricing.enums.PromotionType;
import com.sni.bokaticowork.features.subscription.promotion.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Ce a quoi un client a droit, maintenant.
 *
 * <p>Quatre familles reunies dans une seule liste : les promotions automatiques deja appliquees, les
 * promotions nominatives qui lui sont reservees, les coupons qui lui ont ete attribues, et les
 * coupons publics en cours.</p>
 *
 * <p>La liste montre aussi ce a quoi il n'a <b>pas</b> droit, avec la raison. Un client qui comprend
 * pourquoi son code est refuse n'ecrit pas au support · et celui qui voit ce qui lui manque a une
 * raison d'y revenir.</p>
 *
 * <p>Une reserve, et elle est importante : une promotion nominative n'apparait qu'a son
 * beneficiaire. Le filtre d'audience est applique ici comme il l'est dans le moteur, sans quoi cette
 * liste deviendrait le moyen le plus simple de decouvrir les gestes commerciaux faits aux autres.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClientPromotionService {

    private final PromotionRepository promotionRepository;
    private final PromotionAudienceFilter audienceFilter;
    private final CouponService couponService;
    private final PricingEngine pricingEngine;

    /**
     * @param context panier courant, ou contexte vide pour une consultation hors achat · l'economie
     *                estimee n'est calculable que lorsqu'il y a quelque chose a reduire
     */
    @Transactional(readOnly = true)
    public List<ClientPromotionView> availableFor(PricingContext context) {
        List<ClientPromotionView> views = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();

        PricingResult result = context.linesOrEmpty().isEmpty() ? null : pricingEngine.evaluate(context);

        appendApplied(views, seen, result);
        appendVisiblePromotions(views, seen, context, result);
        appendCoupons(views, seen, context);
        return views;
    }

    // -----------------------------------------------------------------------------------------

    /** Ce qui joue deja sur le panier · le client doit le voir compte, pas le chercher. */
    private void appendApplied(List<ClientPromotionView> views, Set<String> seen, PricingResult result) {
        if (result == null) {
            return;
        }
        for (PricingResult.AppliedRule rule : result.appliedRules()) {
            if (!seen.add(rule.sourceCode())) {
                continue;
            }
            views.add(new ClientPromotionView(
                    kindOf(rule),
                    rule.sourceType() == DiscountSourceType.COUPON
                            ? rule.sourceCode() : null,
                    rule.sourceName(),
                    rule.explanation(),
                    rule.discountAmount(),
                    result.currency(),
                    null,
                    ClientPromotionView.Status.ALREADY_APPLIED,
                    null));
        }
    }

    /**
     * Les campagnes que ce client peut voir et qui ne jouent pas encore.
     *
     * <p>Une campagne non retenue sur le panier courant n'est pas forcement hors de portee : elle
     * peut attendre un montant minimum, un plan particulier, un jour de la semaine. Le refus rendu
     * par le moteur dit lequel, et c'est ce refus qu'on montre.</p>
     */
    private void appendVisiblePromotions(List<ClientPromotionView> views,
                                         Set<String> seen,
                                         PricingContext context,
                                         PricingResult result) {
        List<Promotion> candidates = promotionRepository.findEvaluable(
                PromotionType.AUTOMATIC.name(), context.evaluationDateOrNow());
        if (candidates.isEmpty()) {
            return;
        }
        Set<Long> visible = audienceFilter.visibleTo(
                candidates.stream().map(Promotion::getId).toList(), context);

        for (Promotion promotion : candidates) {
            if (!visible.contains(promotion.getId()) || !seen.add(promotion.getCode())) {
                continue;
            }
            String refusal = refusalFor(result, promotion.getCode());
            views.add(refusal == null
                    ? ClientPromotionView.applicable(ClientPromotionView.Kind.AUTOMATIC, null,
                    promotion.getName(), promotion.getDescription(), null, null, promotion.getEndsAt())
                    : ClientPromotionView.refused(ClientPromotionView.Kind.AUTOMATIC, null,
                    promotion.getName(), promotion.getDescription(), promotion.getEndsAt(),
                    ClientPromotionView.Status.NOT_ELIGIBLE, refusal));
        }
    }

    /** Les codes dont ce client dispose · un coupon nominatif n'apparait qu'a son titulaire. */
    private void appendCoupons(List<ClientPromotionView> views, Set<String> seen, PricingContext context) {
        if (!StringUtils.hasText(context.subscriberCode())) {
            return;
        }
        List<Coupon> coupons = couponService.availableFor(context.subscriberType(), context.subscriberCode());
        Instant now = context.evaluationDateOrNow();

        for (Coupon coupon : coupons) {
            if (!seen.add(coupon.getCode())) {
                continue;
            }
            ClientPromotionView.Kind kind = StringUtils.hasText(coupon.getAssignedToCode())
                    ? ClientPromotionView.Kind.COUPON_ASSIGNED
                    : ClientPromotionView.Kind.COUPON_PUBLIC;
            Promotion promotion = coupon.getPromotion();
            String label = promotion == null ? coupon.getCode() : promotion.getName();
            String description = promotion == null ? null : promotion.getDescription();

            String refusal = couponService.rejectionReason(coupon, context.subscriberType(),
                    context.subscriberCode(), now);
            views.add(refusal == null
                    ? ClientPromotionView.applicable(kind, coupon.getCode(), label, description,
                    null, null, coupon.getValidUntil())
                    : ClientPromotionView.refused(kind, coupon.getCode(), label, description,
                    coupon.getValidUntil(), statusFor(refusal), refusal));
        }
    }

    // -----------------------------------------------------------------------------------------

    private ClientPromotionView.Kind kindOf(PricingResult.AppliedRule rule) {
        return rule.sourceType() == DiscountSourceType.COUPON
                ? ClientPromotionView.Kind.COUPON_ASSIGNED
                : ClientPromotionView.Kind.AUTOMATIC;
    }

    private String refusalFor(PricingResult result, String code) {
        if (result == null) {
            return null;
        }
        boolean applied = result.appliedRules().stream()
                .anyMatch(rule -> code.equals(rule.sourceCode()));
        if (applied) {
            return null;
        }
        return result.rejections().stream()
                .filter(rejection -> code.equals(rejection.sourceCode()))
                .map(PricingResult.Rejection::reason)
                .findFirst()
                .orElse(null);
    }

    /**
     * Traduit un motif en statut affichable.
     *
     * <p>Deux motifs meritent leur propre statut parce qu'ils appellent des actions differentes :
     * un code expire ne reviendra pas, un code epuise peut se rouvrir si la campagne est rechargee.
     * Tout le reste est une question d'eligibilite.</p>
     */
    private ClientPromotionView.Status statusFor(String refusal) {
        String reason = refusal.toLowerCase();
        if (reason.contains("expir")) {
            return ClientPromotionView.Status.EXPIRED;
        }
        if (reason.contains("épuis") || reason.contains("limite d'utilisation")) {
            return ClientPromotionView.Status.EXHAUSTED;
        }
        return ClientPromotionView.Status.NOT_ELIGIBLE;
    }
}
