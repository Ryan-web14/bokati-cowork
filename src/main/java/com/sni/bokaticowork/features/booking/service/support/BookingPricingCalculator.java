package com.sni.bokaticowork.features.booking.service.support;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.ressource.enums.ResourceBookingUnit;
import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.ressource.model.ResourcePricingRule;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourcePricingRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class BookingPricingCalculator {

    /**
     * Une demi-journee vaut 5 heures, une journee 10 · de 08h00 a 18h00.
     *
     * <p>La journee valait 480 minutes, soit 8 heures : une reservation de 8h etait facturee au
     * tarif journalier alors qu'il reste deux heures ouvrables devant elle.
     */
    private static final long HALF_DAY_MINUTES = 300;
    public static final long DAY_MINUTES = 600;
    private static final long WEEK_MINUTES = 10080;
    private static final long MONTH_MINUTES = 43200;

    /** Ordre de repli quand la ressource ne porte aucune grille pour l'unite appelee. */
    private static final List<ResourceBookingUnit> FALLBACK_ORDER = List.of(
            ResourceBookingUnit.HOUR, ResourceBookingUnit.HALF_DAY, ResourceBookingUnit.DAY,
            ResourceBookingUnit.WEEK, ResourceBookingUnit.MONTH);

    private final ResourcePricingRuleRepository pricingRuleRepository;

    public Price calculate(Resource resource, LocalDateTime startedAt, LocalDateTime endedAt, int quantity) {
        return calculate(resource, startedAt, endedAt, quantity, null);
    }

    /**
     * Montant d'une reservation, l'unite pouvant etre imposee.
     *
     * <p>{@code forcedUnit} n'est offert qu'aux points d'entree d'administration. Le portail client
     * ne porte aucun champ correspondant : ses reservations passent forcement par la duree, seule
     * facon de garantir qu'un client ne choisisse pas son propre tarif.
     *
     * <p>L'unite imposee change la grille appliquee, pas le prix : celui-ci reste lu dans la regle
     * tarifaire de la ressource pour cette unite. Un montant facture reste donc rattachable a une
     * regle, et explicable apres coup.
     */
    public Price calculate(Resource resource, LocalDateTime startedAt, LocalDateTime endedAt,
                           int quantity, ResourceBookingUnit forcedUnit) {
        Map<ResourceBookingUnit, ResourcePricingRule> rulesByUnit = activeRulesByUnit(resource);
        ResourcePricingRule rule = forcedUnit == null
                ? selectRule(rulesByUnit, startedAt, endedAt)
                : forcedRule(rulesByUnit, forcedUnit, resource);
        ResourceBookingUnit unit = rule == null ? ResourceBookingUnit.HOUR : rule.getResourceBookingUnit();
        BigDecimal unitPrice = BigDecimal.valueOf(rule == null || rule.getPrice() == null ? 0 : rule.getPrice());
        BigDecimal units = unitCount(unit, startedAt, endedAt, forcedUnit != null)
                .multiply(BigDecimal.valueOf(billableQuantity(resource, quantity)));
        BigDecimal amount = unitPrice.multiply(units).setScale(4, RoundingMode.HALF_UP);
        return new Price(unit, unitPrice, units, amount, "XAF");
    }

    /**
     * Nombre d'unites facturees.
     *
     * <p>Un forfait impose reste <b>un</b> forfait, quelle que soit la duree : demander la
     * demi-journee sur six heures facture une demi-journee. C'est le sens meme du geste · sans
     * cela, l'arrondi au superieur en compterait deux, et imposer le forfait couterait plus cher
     * que de laisser le calcul automatique, ce qui viderait l'override de son objet.
     *
     * <p>L'heure fait exception : elle suit toujours la duree reelle, imposee ou non. Un forfait
     * couvre une plage, une heure se compte.
     */
    private BigDecimal unitCount(ResourceBookingUnit unit, LocalDateTime startedAt,
                                 LocalDateTime endedAt, boolean imposed) {
        if (imposed && unit != ResourceBookingUnit.HOUR) {
            return BigDecimal.ONE.setScale(4, RoundingMode.HALF_UP);
        }
        return units(unit, startedAt, endedAt);
    }

    private BigDecimal units(ResourceBookingUnit unit, LocalDateTime startedAt, LocalDateTime endedAt) {
        long minutes = Duration.between(startedAt, endedAt).toMinutes();
        return switch (unit) {
            case HOUR -> BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);
            case HALF_DAY -> ceil(minutes, HALF_DAY_MINUTES);
            case DAY -> ceil(minutes, DAY_MINUTES);
            case WEEK -> ceil(minutes, WEEK_MINUTES);
            case MONTH -> ceil(minutes, MONTH_MINUTES);
        };
    }

    /**
     * Unite que la duree appelle, avant tout arbitrage sur les regles disponibles.
     *
     * <p>Le forfait ne vaut qu'a sa duree exacte : cinq heures pile sont une demi-journee, cinq
     * heures et demie repassent a l'heure. Un forfait couvrant toute une tranche ferait payer
     * cinq heures a qui en reserve cinq et demie, et le systeme facturerait moins que le temps
     * occupe sans que personne ne l'ait decide.
     */
    /**
     * Regle correspondant a l'unite imposee, ou refus.
     *
     * <p>Pas de repli ici, contrairement au calcul automatique : un administrateur qui demande
     * explicitement une demi-journee sur une ressource qui n'en a pas doit l'apprendre, et non se
     * voir facturer une autre unite sous le meme geste.
     */
    private ResourcePricingRule forcedRule(Map<ResourceBookingUnit, ResourcePricingRule> rulesByUnit,
                                           ResourceBookingUnit forcedUnit,
                                           Resource resource) {
        ResourcePricingRule rule = rulesByUnit.get(forcedUnit);
        if (rule == null) {
            throw new BadRequestException("La ressource " + resource.getCode()
                    + " n'a aucune grille tarifaire active en " + forcedUnit
                    + " · creez-la avant d'imposer cette unite, ou laissez le calcul automatique.");
        }
        return rule;
    }

    private ResourceBookingUnit unitForDuration(long minutes) {
        if (minutes >= MONTH_MINUTES) {
            return ResourceBookingUnit.MONTH;
        }
        if (minutes >= WEEK_MINUTES) {
            return ResourceBookingUnit.WEEK;
        }
        if (minutes >= DAY_MINUTES) {
            return ResourceBookingUnit.DAY;
        }
        if (minutes == HALF_DAY_MINUTES) {
            return ResourceBookingUnit.HALF_DAY;
        }
        return ResourceBookingUnit.HOUR;
    }

    private ResourcePricingRule selectRule(Map<ResourceBookingUnit, ResourcePricingRule> rulesByUnit,
                                           LocalDateTime startedAt,
                                           LocalDateTime endedAt) {
        if (rulesByUnit.isEmpty()) {
            return null;
        }

        ResourceBookingUnit intended = unitForDuration(Duration.between(startedAt, endedAt).toMinutes());
        if (rulesByUnit.containsKey(intended)) {
            return rulesByUnit.get(intended);
        }

        // La ressource n'a pas de grille pour cette unite · on retombe sur ce qu'elle porte,
        // l'heure d'abord, qui est la seule unite dont le montant suit la duree reelle.
        for (ResourceBookingUnit fallback : FALLBACK_ORDER) {
            if (rulesByUnit.containsKey(fallback)) {
                return rulesByUnit.get(fallback);
            }
        }
        return null;
    }

    private Map<ResourceBookingUnit, ResourcePricingRule> activeRulesByUnit(Resource resource) {
        List<ResourcePricingRule> rules = pricingRuleRepository.findAllActiveByResourceId(resource.getId());
        Map<ResourceBookingUnit, ResourcePricingRule> rulesByUnit = new EnumMap<>(ResourceBookingUnit.class);
        for (ResourcePricingRule rule : rules) {
            if (rule.getResourceBookingUnit() != null && !rulesByUnit.containsKey(rule.getResourceBookingUnit())) {
                rulesByUnit.put(rule.getResourceBookingUnit(), rule);
            }
        }
        return rulesByUnit;
    }

    private BigDecimal ceil(long minutes, long denominator) {
        return BigDecimal.valueOf((long) Math.ceil((double) minutes / denominator)).setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * Quantite qui multiplie le prix.
     *
     * <p>On loue une salle, pas un siege. Multiplier par le nombre de participants facturait cent
     * vingt mille francs une salle a vingt mille pour six personnes · la quantite n'y est
     * qu'indicative, et le tarif est celui de la piece.</p>
     */
    private int billableQuantity(Resource resource, int quantity) {
        return resource != null && resource.isWholeResourceBooking() ? 1 : quantity;
    }

    public record Price(ResourceBookingUnit unit, BigDecimal unitPrice, BigDecimal quantity, BigDecimal amount, String currency) {
    }
}
