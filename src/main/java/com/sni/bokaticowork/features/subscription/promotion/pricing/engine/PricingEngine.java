package com.sni.bokaticowork.features.subscription.promotion.pricing.engine;

import com.sni.bokaticowork.features.subscription.promotion.model.Promotion;
import com.sni.bokaticowork.features.subscription.promotion.pricing.enums.DiscountSourceType;
import com.sni.bokaticowork.features.subscription.promotion.pricing.enums.PromotionType;
import com.sni.bokaticowork.features.subscription.promotion.pricing.enums.RewardType;
import com.sni.bokaticowork.features.subscription.promotion.pricing.enums.TargetScope;
import com.sni.bokaticowork.features.subscription.promotion.pricing.model.PromotionCondition;
import com.sni.bokaticowork.features.subscription.promotion.pricing.model.PromotionReward;
import com.sni.bokaticowork.features.subscription.promotion.pricing.audience.service.PromotionAudienceFilter;
import com.sni.bokaticowork.features.subscription.promotion.pricing.pricelist.service.PriceListResolver;
import com.sni.bokaticowork.features.subscription.promotion.pricing.repository.PromotionConditionRepository;
import com.sni.bokaticowork.features.subscription.promotion.pricing.repository.PromotionRewardRepository;
import com.sni.bokaticowork.features.subscription.promotion.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Calcule ce qu'un panier coute reellement.
 *
 * <p>C'etait la piece manquante. Le module promotion pouvait creer une campagne, l'activer,
 * enregistrer qu'un abonne l'avait utilisee, mais aucun montant n'etait jamais calcule ni applique
 * nulle part. Tout le reste du module etait declaratif.</p>
 *
 * <h2>Ordre d'evaluation, fige une fois pour toutes</h2>
 *
 * <p>L'ordre change le montant final. Le laisser implicite produit des ecarts que personne ne sait
 * expliquer trois mois plus tard.</p>
 *
 * <ol>
 *   <li>Prix catalogue, porte par les lignes du contexte.</li>
 *   <li>Grille tarifaire · remplace le prix catalogue, sans se voir comme une remise.</li>
 *   <li>Prix negocie d'un abonnement derive · terme de contrat, pas promotion. <i>Lot G.</i></li>
 *   <li>Promotions automatiques, par priorite croissante.</li>
 *   <li>Coupons saisis, par priorite croissante.</li>
 *   <li>Plafonnement par {@code maxDiscountAmount} et par le budget restant.</li>
 *   <li>Arrondi a l'entier, une seule fois, a la fin.</li>
 * </ol>
 *
 * <p>L'etape 3 arrive avec les abonnements derives. L'ordre, lui, est deja celui-ci : l'y inserer
 * ne deplacera rien de ce qui existe.</p>
 *
 * <h2>Les remises ne se composent pas</h2>
 *
 * <p>Chaque regle calcule son montant <b>sur le prix d'origine</b>, jamais sur ce qu'il reste apres
 * les precedentes. Dix pour cent et mille francs de remise sur dix mille font deux mille, pas mille
 * neuf cents.</p>
 *
 * <p>Le choix est deliberе. Une remise composee ferait dependre la valeur d'un coupon de ce qui
 * s'est applique avant lui : le meme code vaudrait moins parce qu'une campagne automatique tournait
 * ce jour-la, ce qui est indefendable devant un client. Ici chaque ligne du detail s'explique seule,
 * « dix pour cent de votre commande », et le cumul reste borne par le total, qu'aucune combinaison
 * ne peut faire passer sous zero.</p>
 *
 * <h2>Ce que le moteur ne fait pas</h2>
 *
 * <p>Il ne persiste rien et ne consomme aucun budget. Evaluer et appliquer sont deux gestes
 * distincts, sans quoi afficher un panier consommerait une campagne. La consommation appartient a
 * {@link com.sni.bokaticowork.features.subscription.promotion.pricing.service.DiscountApplicationService}.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PricingEngine {

    /** Les montants se calculent au centieme, et ne s'arrondissent qu'a la toute fin. */
    private static final int WORKING_SCALE = 4;

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final PromotionRepository promotionRepository;
    private final PromotionConditionRepository conditionRepository;
    private final PromotionRewardRepository rewardRepository;
    private final PromotionConditionEvaluator conditionEvaluator;
    private final CouponLookup couponLookup;
    private final PriceListResolver priceListResolver;
    private final PromotionAudienceFilter audienceFilter;

    @Transactional(readOnly = true)
    public PricingResult evaluate(PricingContext context) {
        BigDecimal catalogTotal = totalOf(context.linesOrEmpty());

        // Etape 2 · la grille remplace le prix catalogue. Tout ce qui suit travaille sur le prix
        // negocie, y compris les pourcentages : une remise de dix pour cent accordee a un
        // partenaire porte sur son tarif, pas sur un tarif public qu'il ne paie jamais.
        PriceListResolver.Resolution resolution = priceListResolver.resolve(context);
        PricingContext effective = context.withLines(resolution.lines());
        BigDecimal originalTotal = totalOf(resolution.lines());
        List<PricingResult.PriceOverride> overrides = resolution.overrides().stream()
                .map(override -> new PricingResult.PriceOverride(
                        override.lineReference(), override.priceListCode(), override.priceListName(),
                        override.catalogUnitPrice(), override.effectiveUnitPrice(), override.quantity()))
                .toList();

        List<PricingResult.AppliedRule> applied = new ArrayList<>();
        List<PricingResult.Rejection> rejections = new ArrayList<>();
        List<PricingResult.NonMonetaryGrant> grants = new ArrayList<>();

        if (!effective.promotionsAllowed()) {
            // Etape 3 · un prix deja negocie n'a pas vocation a recevoir une remise supplementaire.
            return result(catalogTotal, originalTotal, BigDecimal.ZERO, effective, applied, rejections, grants, overrides);
        }

        Ledger ledger = new Ledger(originalTotal);
        applyAutomaticPromotions(effective, ledger, applied, rejections, grants);
        applyCoupons(effective, ledger, applied, rejections, grants);
        return result(catalogTotal, originalTotal, ledger.discountTotal, effective, applied, rejections, grants, overrides);
    }

    // -----------------------------------------------------------------------------------------
    // Etape 4 · promotions automatiques
    // -----------------------------------------------------------------------------------------

    private void applyAutomaticPromotions(PricingContext context,
                                          Ledger ledger,
                                          List<PricingResult.AppliedRule> applied,
                                          List<PricingResult.Rejection> rejections,
                                          List<PricingResult.NonMonetaryGrant> grants) {
        List<Promotion> all = promotionRepository.findEvaluable(
                PromotionType.AUTOMATIC.name(), context.evaluationDateOrNow());
        if (all.isEmpty()) {
            return;
        }

        // Une promotion nominative n'apparait qu'a ses beneficiaires. Le filtre est applique ici,
        // devant le moteur, et non dans une interface qui pourrait oublier de le faire : c'est la
        // seule position ou l'oubli est impossible.
        Set<Long> visible = audienceFilter.visibleTo(all.stream().map(Promotion::getId).toList(), context);
        List<Promotion> candidates = all.stream()
                .filter(promotion -> visible.contains(promotion.getId()))
                .toList();
        if (candidates.isEmpty()) {
            return;
        }

        List<Long> ids = candidates.stream().map(Promotion::getId).toList();
        Map<Long, List<PromotionCondition>> conditionsByPromotion = conditionRepository.findAllByPromotionIds(ids)
                .stream().collect(Collectors.groupingBy(condition -> condition.getPromotion().getId()));
        Map<Long, List<PromotionReward>> rewardsByPromotion = rewardRepository.findAllByPromotionIds(ids)
                .stream().collect(Collectors.groupingBy(reward -> reward.getPromotion().getId()));

        for (Promotion promotion : candidates) {
            if (!conditionEvaluator.matches(conditionsByPromotion.getOrDefault(promotion.getId(), List.of()), context)) {
                continue;
            }
            boolean stop = applyOne(promotion, rewardsByPromotion.getOrDefault(promotion.getId(), List.of()),
                    DiscountSourceType.PROMOTION, promotion.getCode(), context, ledger, applied, rejections, grants);
            if (stop) {
                break;
            }
        }
    }

    // -----------------------------------------------------------------------------------------
    // Etape 5 - coupons saisis
    // -----------------------------------------------------------------------------------------

    /**
     * Les coupons s'evaluent apres les promotions automatiques, sur ce qu'il reste a payer. Un code
     * refuse n'interrompt rien : il est rendu avec son motif, et les autres continuent d'etre
     * examines. Un panier a trois codes dont un perime doit passer, pas echouer en bloc.
     */
    private void applyCoupons(PricingContext context,
                              Ledger ledger,
                              List<PricingResult.AppliedRule> applied,
                              List<PricingResult.Rejection> rejections,
                              List<PricingResult.NonMonetaryGrant> grants) {
        List<String> codes = context.couponCodesOrEmpty();
        if (codes.isEmpty() || ledger.exhausted()) {
            return;
        }

        List<CouponLookup.ResolvedCoupon> resolved = couponLookup.resolve(
                codes, context.subscriberType(), context.subscriberCode(), context.evaluationDateOrNow());

        List<CouponLookup.ResolvedCoupon> accepted = new ArrayList<>();
        for (CouponLookup.ResolvedCoupon candidate : resolved) {
            if (candidate.accepted()) {
                accepted.add(candidate);
            } else {
                rejections.add(new PricingResult.Rejection(
                        DiscountSourceType.COUPON, candidate.code(), candidate.rejectionReason()));
            }
        }
        if (accepted.isEmpty()) {
            return;
        }

        accepted.sort(Comparator.comparingInt(candidate -> priority(candidate.promotion())));
        List<Long> ids = accepted.stream().map(candidate -> candidate.promotion().getId()).toList();
        Map<Long, List<PromotionCondition>> conditionsByPromotion = conditionRepository.findAllByPromotionIds(ids)
                .stream().collect(Collectors.groupingBy(condition -> condition.getPromotion().getId()));
        Map<Long, List<PromotionReward>> rewardsByPromotion = rewardRepository.findAllByPromotionIds(ids)
                .stream().collect(Collectors.groupingBy(reward -> reward.getPromotion().getId()));

        for (CouponLookup.ResolvedCoupon candidate : accepted) {
            Promotion promotion = candidate.promotion();
            if (!conditionEvaluator.matches(conditionsByPromotion.getOrDefault(promotion.getId(), List.of()), context)) {
                rejections.add(new PricingResult.Rejection(DiscountSourceType.COUPON, candidate.code(),
                        "Ce code ne s'applique pas a votre panier"));
                continue;
            }
            boolean stop = applyOne(promotion, rewardsByPromotion.getOrDefault(promotion.getId(), List.of()),
                    DiscountSourceType.COUPON, candidate.code(), context, ledger, applied, rejections, grants);
            if (stop) {
                break;
            }
        }
    }

    /**
     * Applique une campagne, quelle que soit la facon dont elle a ete atteinte.
     *
     * @return vrai lorsque l'evaluation doit s'arreter, promotion exclusive ou plus rien a reduire
     */
    private boolean applyOne(Promotion promotion,
                             List<PromotionReward> rewards,
                             DiscountSourceType sourceType,
                             String sourceCode,
                             PricingContext context,
                             Ledger ledger,
                             List<PricingResult.AppliedRule> applied,
                             List<PricingResult.Rejection> rejections,
                             List<PricingResult.NonMonetaryGrant> grants) {
        // Regle de cumul : une promotion non cumulable ne rejoint pas une remise deja retenue.
        if (ledger.somethingApplied && !Boolean.TRUE.equals(promotion.getStackable())) {
            rejections.add(new PricingResult.Rejection(sourceType, sourceCode,
                    "Cette promotion ne se cumule pas avec une remise deja appliquee"));
            return false;
        }
        if (rewards.isEmpty()) {
            rejections.add(new PricingResult.Rejection(sourceType, sourceCode,
                    "Cette promotion n'accorde rien"));
            return false;
        }

        List<Contribution> contributions = new ArrayList<>();
        BigDecimal granted = BigDecimal.ZERO;
        for (PromotionReward reward : rewards) {
            if (!reward.getRewardType().isMonetary()) {
                grants.add(nonMonetary(promotion, reward));
                continue;
            }
            BigDecimal amount = monetaryAmount(reward, context, ledger.remainingBase.subtract(granted));
            if (amount.signum() > 0) {
                contributions.add(new Contribution(reward.getRewardType(), amount));
                granted = granted.add(amount);
            }
        }

        // Etape 6 - plafonds, celui de la promotion puis l'enveloppe restante de la campagne.
        BigDecimal capped = capped(granted, promotion, rejections, sourceType, sourceCode);
        if (capped.signum() <= 0) {
            return false;
        }

        BigDecimal base = ledger.remainingBase;
        for (Contribution contribution : scaled(contributions, granted, capped)) {
            ledger.order++;
            applied.add(new PricingResult.AppliedRule(
                    ledger.order,
                    sourceType,
                    sourceCode,
                    promotion.getName(),
                    contribution.rewardType(),
                    null,
                    base,
                    contribution.amount(),
                    promotion.getName()));
        }

        ledger.record(capped);
        // Une promotion exclusive qui s'applique arrete l'evaluation des suivantes.
        return Boolean.TRUE.equals(promotion.getExclusive()) || ledger.exhausted();
    }

    /**
     * Repartit un plafonnement sur les recompenses qui l'ont produit.
     *
     * <p>Une promotion peut accorder plusieurs choses a la fois, typiquement un pourcentage et la
     * dispense de frais d'entree. Les fondre dans une seule ligne etiquetee du type de la premiere
     * rendrait la facture fausse : personne ne saurait quelle part correspond aux frais dispenses,
     * et l'abonnement ne pourrait pas en tenir compte separement.</p>
     *
     * <p>Quand le plafond mord, la reduction est repartie au prorata. Retirer la difference de la
     * derniere ligne serait plus simple, et attribuerait arbitrairement tout l'effort du plafond a
     * une recompense qui n'en est pas plus responsable que les autres.</p>
     */
    private List<Contribution> scaled(List<Contribution> contributions, BigDecimal granted, BigDecimal capped) {
        if (contributions.size() == 1 || granted.compareTo(capped) == 0 || granted.signum() == 0) {
            return contributions.size() == 1
                    ? List.of(new Contribution(contributions.getFirst().rewardType(), capped))
                    : contributions;
        }
        List<Contribution> scaled = new ArrayList<>(contributions.size());
        BigDecimal distributed = BigDecimal.ZERO;
        for (int index = 0; index < contributions.size(); index++) {
            Contribution contribution = contributions.get(index);
            BigDecimal share = index == contributions.size() - 1
                    // Le reliquat va a la derniere, pour que la somme egale exactement le plafond.
                    ? capped.subtract(distributed)
                    : contribution.amount().multiply(capped)
                    .divide(granted, WORKING_SCALE, RoundingMode.HALF_UP);
            distributed = distributed.add(share);
            scaled.add(new Contribution(contribution.rewardType(), share));
        }
        return scaled;
    }

    private record Contribution(RewardType rewardType, BigDecimal amount) {
    }

    private int priority(Promotion promotion) {
        return promotion.getPriority() == null ? Integer.MAX_VALUE : promotion.getPriority();
    }

    /** Etat courant de l'evaluation, ce qui reste a reduire et ce qui a deja ete accorde. */
    private static final class Ledger {
        private BigDecimal remainingBase;
        private BigDecimal discountTotal = BigDecimal.ZERO;
        private boolean somethingApplied;
        private int order;

        private Ledger(BigDecimal originalTotal) {
            this.remainingBase = originalTotal;
        }

        private void record(BigDecimal granted) {
            discountTotal = discountTotal.add(granted);
            remainingBase = remainingBase.subtract(granted);
            somethingApplied = true;
        }

        private boolean exhausted() {
            return remainingBase.signum() <= 0;
        }
    }

    // -----------------------------------------------------------------------------------------

    private BigDecimal monetaryAmount(PromotionReward reward, PricingContext context, BigDecimal remainingBase) {
        BigDecimal base = baseFor(reward, context);
        if (base.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal raw = switch (reward.getRewardType()) {
            case PERCENTAGE_OFF -> base.multiply(rate(reward.getValue())).divide(HUNDRED, WORKING_SCALE, RoundingMode.HALF_UP);
            case FIXED_AMOUNT_OFF -> reward.getValue() == null ? BigDecimal.ZERO : reward.getValue();
            case WAIVE_SETUP_FEE -> setupFees(reward, context);
            default -> BigDecimal.ZERO;
        };
        if (reward.getMaxAmount() != null && raw.compareTo(reward.getMaxAmount()) > 0) {
            raw = reward.getMaxAmount();
        }
        // Aucune remise ne descend le total sous zero : le reliquat doit devenir un credit
        // explicite, jamais une facture negative.
        return raw.min(base).min(remainingBase.max(BigDecimal.ZERO));
    }

    /**
     * Assiette de la recompense. Une portee globale prend tout le panier ; toute autre portee ne
     * prend que les lignes qui la concernent, faute de quoi « dix pour cent sur les salles » se
     * calculerait aussi sur les cafes.
     */
    private BigDecimal baseFor(PromotionReward reward, PricingContext context) {
        if (reward.getTargetScope() == null || reward.getTargetScope() == TargetScope.WHOLE_ORDER) {
            return context.linesOrEmpty().stream()
                    .map(PricingContext.PricingLine::lineAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        return context.linesOrEmpty().stream()
                .filter(line -> targets(reward, line))
                .map(PricingContext.PricingLine::lineAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal setupFees(PromotionReward reward, PricingContext context) {
        return context.linesOrEmpty().stream()
                .filter(line -> reward.getTargetScope() == null
                        || reward.getTargetScope() == TargetScope.WHOLE_ORDER
                        || targets(reward, line))
                .map(PricingContext.PricingLine::setupFeeOrZero)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private boolean targets(PromotionReward reward, PricingContext.PricingLine line) {
        if (reward.getTargetScope() == TargetScope.LINE) {
            return reward.getTargetCode() == null || reward.getTargetCode().equalsIgnoreCase(line.reference());
        }
        if (reward.getTargetScope() == TargetScope.CATEGORY) {
            return reward.getTargetCode() != null && reward.getTargetCode().equalsIgnoreCase(line.categoryCode());
        }
        if (line.scope() != reward.getTargetScope()) {
            return false;
        }
        return reward.getTargetCode() == null || reward.getTargetCode().equalsIgnoreCase(line.code());
    }

    private BigDecimal capped(BigDecimal granted, Promotion promotion, List<PricingResult.Rejection> rejections,
                              DiscountSourceType sourceType, String sourceCode) {
        BigDecimal capped = granted;
        if (promotion.getMaxDiscountAmount() != null && capped.compareTo(promotion.getMaxDiscountAmount()) > 0) {
            capped = promotion.getMaxDiscountAmount();
        }
        BigDecimal remainingBudget = promotion.remainingBudget();
        if (remainingBudget != null && capped.compareTo(remainingBudget) > 0) {
            if (remainingBudget.signum() <= 0) {
                rejections.add(new PricingResult.Rejection(sourceType, sourceCode,
                        "Le budget de cette campagne est epuise"));
                return BigDecimal.ZERO;
            }
            capped = remainingBudget;
        }
        return capped;
    }

    private PricingResult.NonMonetaryGrant nonMonetary(Promotion promotion, PromotionReward reward) {
        return new PricingResult.NonMonetaryGrant(
                DiscountSourceType.PROMOTION,
                promotion.getCode(),
                reward.getRewardType(),
                reward.getTargetCode(),
                reward.getValue(),
                promotion.getName());
    }

    private BigDecimal totalOf(List<PricingContext.PricingLine> lines) {
        return lines.stream()
                .map(line -> line.lineAmount().add(line.setupFeeOrZero()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal rate(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /**
     * Etape 7 · l'arrondi n'a lieu qu'ici. Arrondir a chaque regle ferait diverger le total de la
     * somme de ses lignes, et l'ecart grandirait avec le nombre de regles.
     */
    private PricingResult result(BigDecimal catalogTotal,
                                 BigDecimal originalTotal,
                                 BigDecimal discountTotal,
                                 PricingContext context,
                                 List<PricingResult.AppliedRule> applied,
                                 List<PricingResult.Rejection> rejections,
                                 List<PricingResult.NonMonetaryGrant> grants,
                                 List<PricingResult.PriceOverride> overrides) {
        BigDecimal original = round(originalTotal);
        BigDecimal discount = round(discountTotal.min(originalTotal).max(BigDecimal.ZERO));
        return new PricingResult(
                round(catalogTotal),
                original,
                discount,
                original.subtract(discount),
                context.currency(),
                List.copyOf(applied),
                List.copyOf(rejections),
                List.copyOf(grants),
                List.copyOf(overrides));
    }

    private BigDecimal round(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(0, RoundingMode.HALF_UP);
    }
}
