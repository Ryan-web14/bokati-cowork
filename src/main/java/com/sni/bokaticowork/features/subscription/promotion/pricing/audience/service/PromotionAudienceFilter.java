package com.sni.bokaticowork.features.subscription.promotion.pricing.audience.service;

import com.sni.bokaticowork.features.subscription.promotion.pricing.audience.enums.PromotionAudienceType;
import com.sni.bokaticowork.features.subscription.promotion.pricing.audience.model.PromotionAudience;
import com.sni.bokaticowork.features.subscription.promotion.pricing.audience.repository.PromotionAudienceRepository;
import com.sni.bokaticowork.features.subscription.promotion.pricing.audience.repository.PromotionBeneficiaryRepository;
import com.sni.bokaticowork.features.subscription.promotion.pricing.engine.PricingContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Dit quelles promotions un souscripteur donne a le droit de voir.
 *
 * <p>Distinct des conditions, et la distinction est utile : les conditions decrivent <b>quand</b>
 * une offre s'applique, l'audience decrit <b>a qui</b> elle est destinee. Une offre reservee a
 * quarante invites nommes peut parfaitement exiger en plus un panier minimum.</p>
 *
 * <p>La regle qui commande la conception : <b>une promotion nominative n'apparait qu'a ses
 * beneficiaires</b>. Elle ne doit fuiter ni dans le catalogue public, ni dans une reponse consultee
 * par un autre client. C'est pourquoi le filtre vit ici, devant le moteur, et non dans une interface
 * qui pourrait oublier de l'appliquer.</p>
 *
 * <p>Tout se resout en <b>deux requetes</b> pour l'ensemble des campagnes candidates. Interroger
 * promotion par promotion produirait autant d'aller-retours que de campagnes actives, a chaque
 * affichage de panier.</p>
 */
@Component
@RequiredArgsConstructor
public class PromotionAudienceFilter {

    private final PromotionAudienceRepository audienceRepository;
    private final PromotionBeneficiaryRepository beneficiaryRepository;

    /**
     * @param promotionIds campagnes candidates
     * @return identifiants de celles que ce souscripteur peut voir
     */
    @Transactional(readOnly = true)
    public Set<Long> visibleTo(Collection<Long> promotionIds, PricingContext context) {
        if (promotionIds == null || promotionIds.isEmpty()) {
            return Set.of();
        }

        Map<Long, List<PromotionAudience>> audiencesByPromotion = audienceRepository
                .findAllByPromotionIds(promotionIds).stream()
                .collect(Collectors.groupingBy(audience -> audience.getPromotion().getId()));

        Set<Long> nominative = promotionIds.stream()
                .filter(id -> requiresNamedList(audiencesByPromotion.get(id)))
                .collect(Collectors.toSet());

        Set<Long> granted = nominative.isEmpty() || !StringUtils.hasText(context.subscriberCode())
                ? Set.of()
                : new HashSet<>(beneficiaryRepository.findGrantedPromotionIds(
                nominative, context.subscriberType(), context.subscriberCode()));

        Set<Long> visible = new HashSet<>();
        for (Long promotionId : promotionIds) {
            List<PromotionAudience> audiences = audiencesByPromotion.get(promotionId);
            // Sans audience declaree, la campagne s'adresse a tous. C'est le cas de toutes celles
            // qui existaient avant ce ciblage, qui ne doivent pas disparaitre du jour au lendemain.
            if (audiences == null || audiences.isEmpty()) {
                visible.add(promotionId);
                continue;
            }
            if (audiences.stream().anyMatch(audience -> matches(audience, context, granted, promotionId))) {
                visible.add(promotionId);
            }
        }
        return visible;
    }

    // -----------------------------------------------------------------------------------------

    /**
     * Plusieurs audiences sur une meme promotion se lisent comme un « ou » : une offre destinee aux
     * etudiants <i>et</i> a trente invites nommes s'adresse aux deux.
     */
    private boolean matches(PromotionAudience audience, PricingContext context, Set<Long> granted, Long promotionId) {
        return switch (audience.getAudienceType()) {
            case ALL -> true;
            case SUBSCRIBER_LIST -> granted.contains(promotionId);
            case SEGMENT -> equalsIgnoreCase(audience.getSegmentCode(), context.subscriberSegment());
            case BUSINESS_ENTITY -> equalsIgnoreCase(audience.getBusinessEntityCode(), context.subscriberCode());
            case PLAN -> context.linesOrEmpty().stream()
                    .anyMatch(line -> equalsIgnoreCase(audience.getPlanCode(), line.code()));
            // Une cohorte se materialise en beneficiaires au moment ou la campagne est lancee, et
            // non recalculee a chaque panier : une regle evaluee a la volee ferait entrer et sortir
            // des gens de l'offre au fil du temps, sans que personne ne l'ait decide.
            case COHORT -> granted.contains(promotionId);
        };
    }

    private boolean requiresNamedList(List<PromotionAudience> audiences) {
        return audiences != null && audiences.stream().anyMatch(audience ->
                audience.getAudienceType() == PromotionAudienceType.SUBSCRIBER_LIST
                        || audience.getAudienceType() == PromotionAudienceType.COHORT);
    }

    private boolean equalsIgnoreCase(String expected, String actual) {
        return StringUtils.hasText(expected) && expected.equalsIgnoreCase(actual);
    }
}
