package com.sni.bokaticowork.features.subscription.promotion.pricing.pricelist.service;

import com.sni.bokaticowork.features.subscription.promotion.pricing.engine.PricingContext;
import com.sni.bokaticowork.features.subscription.promotion.pricing.enums.TargetScope;
import com.sni.bokaticowork.features.subscription.promotion.pricing.pricelist.model.PriceList;
import com.sni.bokaticowork.features.subscription.promotion.pricing.pricelist.model.PriceListEntry;
import com.sni.bokaticowork.features.subscription.promotion.pricing.pricelist.repository.PriceListEntryRepository;
import com.sni.bokaticowork.features.subscription.promotion.pricing.pricelist.repository.PriceListRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Determine le prix effectif de chaque ligne avant toute promotion.
 *
 * <p>Une grille ne reduit pas un prix, elle le remplace. C'est pourquoi elle s'applique a l'etape 2,
 * avant les campagnes, et pourquoi son effet n'apparait jamais comme une remise : la substitution
 * est faite, les promotions qui suivent travaillent sur le prix negocie.</p>
 *
 * <p><b>Une seule grille s'applique a une ligne.</b> Les faire se composer produirait des tarifs que
 * personne n'a decides et que personne ne saurait reconstituer. Le departage se fait par priorite,
 * puis par specificite : un tarif nomme l'emporte sur un tarif d'entreprise, qui l'emporte sur un
 * tarif de partenaire, de segment, puis sur le tarif public.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PriceListResolver {

    private static final int MONEY_SCALE = 4;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final PriceListRepository priceListRepository;
    private final PriceListEntryRepository entryRepository;

    /**
     * @param lines     lignes au prix effectif, dans l'ordre d'origine
     * @param overrides substitutions reellement operees, pour que la simulation puisse les montrer
     */
    public record Resolution(List<PricingContext.PricingLine> lines, List<PriceOverride> overrides) {

        public boolean unchanged() {
            return overrides.isEmpty();
        }
    }

    public record PriceOverride(
            String lineReference,
            String priceListCode,
            String priceListName,
            BigDecimal catalogUnitPrice,
            BigDecimal effectiveUnitPrice,
            int quantity
    ) {
    }

    @Transactional(readOnly = true)
    public Resolution resolve(PricingContext context) {
        List<PricingContext.PricingLine> lines = context.linesOrEmpty();
        if (lines.isEmpty()) {
            return new Resolution(lines, List.of());
        }

        List<PriceList> candidates = priceListRepository.findCandidates(
                context.currency(),
                context.subscriberCode(),
                context.subscriberSegment(),
                context.subscriberCode(),
                context.subscriberSegment(),
                context.evaluationDateOrNow());
        if (candidates.isEmpty()) {
            return new Resolution(lines, List.of());
        }

        candidates.sort(Comparator
                .comparingInt((PriceList list) -> list.getPriority() == null ? Integer.MAX_VALUE : list.getPriority())
                .thenComparingInt(PriceList::specificity));

        Map<Long, List<PriceListEntry>> entriesByList = entryRepository
                .findAllByPriceListIds(candidates.stream().map(PriceList::getId).toList())
                .stream().collect(Collectors.groupingBy(entry -> entry.getPriceList().getId()));

        List<PricingContext.PricingLine> effective = new ArrayList<>(lines.size());
        List<PriceOverride> overrides = new ArrayList<>();

        for (PricingContext.PricingLine line : lines) {
            Applied applied = firstMatch(candidates, entriesByList, line, context.billingCycle());
            if (applied == null) {
                effective.add(line);
                continue;
            }
            BigDecimal newUnitPrice = effectiveUnitPrice(applied.entry(), line.unitPrice());
            if (newUnitPrice.compareTo(line.unitPrice()) == 0) {
                effective.add(line);
                continue;
            }
            effective.add(new PricingContext.PricingLine(
                    line.reference(), line.scope(), line.code(), line.categoryCode(), line.label(),
                    line.quantity(), newUnitPrice, line.setupFee()));
            overrides.add(new PriceOverride(
                    line.reference(), applied.list().getCode(), applied.list().getName(),
                    line.unitPrice(), newUnitPrice, line.quantity()));
        }
        return new Resolution(List.copyOf(effective), List.copyOf(overrides));
    }

    // -----------------------------------------------------------------------------------------

    /**
     * Premiere grille qui tarife cette ligne. L'ordre des candidates portant deja la priorite et la
     * specificite, la premiere trouvee est la bonne · inutile de toutes les examiner.
     */
    private Applied firstMatch(List<PriceList> candidates,
                               Map<Long, List<PriceListEntry>> entriesByList,
                               PricingContext.PricingLine line,
                               String billingCycle) {
        for (PriceList list : candidates) {
            PriceListEntry entry = bestEntry(entriesByList.getOrDefault(list.getId(), List.of()), line, billingCycle);
            if (entry != null) {
                return new Applied(list, entry);
            }
        }
        return null;
    }

    /**
     * Ligne de grille retenue pour cet objet : celle dont le palier est le plus eleve parmi ceux que
     * la quantite atteint. C'est ce qui produit un degressif sans table supplementaire.
     */
    private PriceListEntry bestEntry(List<PriceListEntry> entries,
                                     PricingContext.PricingLine line,
                                     String billingCycle) {
        return entries.stream()
                .filter(entry -> targets(entry, line))
                .filter(entry -> entry.getMinQuantity() == null || line.quantity() >= entry.getMinQuantity())
                .filter(entry -> matchesCycle(entry, billingCycle))
                .max(Comparator.comparingInt(entry -> entry.getMinQuantity() == null ? 1 : entry.getMinQuantity()))
                .orElse(null);
    }

    private boolean targets(PriceListEntry entry, PricingContext.PricingLine line) {
        if (entry.getTargetScope() == TargetScope.CATEGORY) {
            return entry.getTargetCode() != null && entry.getTargetCode().equalsIgnoreCase(line.categoryCode());
        }
        if (entry.getTargetScope() != line.scope()) {
            return false;
        }
        // Une ligne sans code couvre toute la portee · « toutes les salles », et non une en
        // particulier.
        return !StringUtils.hasText(entry.getTargetCode())
                || entry.getTargetCode().equalsIgnoreCase(line.code());
    }

    private boolean matchesCycle(PriceListEntry entry, String billingCycle) {
        return !StringUtils.hasText(entry.getBillingCycle())
                || entry.getBillingCycle().equalsIgnoreCase(billingCycle);
    }

    private BigDecimal effectiveUnitPrice(PriceListEntry entry, BigDecimal catalogUnitPrice) {
        BigDecimal catalog = catalogUnitPrice == null ? BigDecimal.ZERO : catalogUnitPrice;
        BigDecimal computed = switch (entry.getPriceMode()) {
            case FIXED_PRICE -> entry.getValue();
            case PERCENTAGE_OFF_LIST -> catalog.subtract(
                    catalog.multiply(entry.getValue()).divide(HUNDRED, MONEY_SCALE, RoundingMode.HALF_UP));
            case FIXED_AMOUNT_OFF_LIST -> catalog.subtract(entry.getValue());
        };
        // Un tarif ne descend pas sous zero, meme si la grille a ete saisie de travers.
        return computed.max(BigDecimal.ZERO).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private record Applied(PriceList list, PriceListEntry entry) {
    }
}
