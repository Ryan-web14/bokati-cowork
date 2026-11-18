package com.sni.bokaticowork.features.ressource.service.pricing;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.settings.platform.PlatformSettings;
import com.sni.bokaticowork.features.ressource.enums.ResourceBookingUnit;
import com.sni.bokaticowork.features.ressource.enums.ResourcePriceAdjustmentType;
import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.ressource.model.ResourcePricingRule;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourcePricingRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;

/**
 * Lot R1 : seul calcul du prix d'une ressource. Reservation, verification de disponibilite,
 * suggestions, devis, apercu, portail et catalogue l'appellent : un devis annonce ce qui sera facture.
 *
 * <ol>
 *   <li>Regle de base d'une unite (sans ajustement) : la plus prioritaire, puis la plus precise
 *   (le plus de conditions), puis la plus recente, parmi celles qui s'appliquent.</li>
 *   <li>Ajustements : un prix fixe remplace la base (le meilleur) ; puis un ecart non cumulable
 *   (le meilleur) et tous les ecarts cumulables, par priorite ; pourcentage en decimal ; jamais
 *   sous zero.</li>
 *   <li>Conditions d'une regle, toutes facultatives : validite, jours, plage horaire, segment de
 *   client, plan d'abonnement, participants, duree, delai avant le debut (anticipe, dernier
 *   moment), canal.</li>
 *   <li>A l'heure, la reservation est decoupee en tranches (minuit et bornes horaires des regles) :
 *   heures pleines et creuses au prorata. Un forfait est tarife au debut de la reservation.</li>
 *   <li>Unite : celle imposee par l'equipe, sinon la moins chere pour le client parmi celles qui ont
 *   un prix (sept heures au prix d'une journee si c'est moins cher).</li>
 * </ol>
 */
@Component
@RequiredArgsConstructor
public class ResourcePriceEngine {

    static final String HALF_DAY_HOURS = "app.resources.pricing.half-day-hours";
    static final String DAY_HOURS = "app.resources.pricing.day-hours";
    private static final String CURRENCY = "XAF";
    private static final List<ResourceBookingUnit> ORDER = List.of(ResourceBookingUnit.HOUR, ResourceBookingUnit.HALF_DAY,
            ResourceBookingUnit.DAY, ResourceBookingUnit.WEEK, ResourceBookingUnit.MONTH);

    private final ResourcePricingRuleRepository ruleRepository;

    /** Lot R5a : jours feries ; les feries nationaux seuls tant que le calendrier n'est pas branche (tests). */
    private java.util.function.Predicate<LocalDate> holidays =
            com.sni.bokaticowork.features.ressource.service.support.CongoHolidays::isHoliday;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void setCalendarDays(com.sni.bokaticowork.features.ressource.service.support.ResourceCalendarDays calendarDays) {
        if (calendarDays != null) {
            this.holidays = calendarDays::isHoliday;
        }
    }
    private PlatformSettings settings;

    @Autowired(required = false)
    void setSettings(PlatformSettings settings) {
        this.settings = settings;
    }

    /** Duree d'une demi-journee (5 h par defaut, decision D2). */
    public long halfDayMinutes() {
        return 60L * (settings == null ? 5 : Math.max(1, settings.integer(HALF_DAY_HOURS, 5)));
    }

    /** Duree d'une journee (10 h par defaut, decision D2). */
    public long dayMinutes() {
        return 60L * (settings == null ? 10 : Math.max(1, settings.integer(DAY_HOURS, 10)));
    }

    public PriceResult price(Resource resource, LocalDateTime start, LocalDateTime end, int quantity,
                             ResourceBookingUnit forcedUnit, PriceContext context) {
        if (start == null || end == null || !end.isAfter(start)) {
            throw new BadRequestException("La fin doit suivre le début");
        }
        PriceContext ctx = context == null ? PriceContext.anonymous() : context;
        List<ResourcePricingRule> rules = ruleRepository.findAllActiveByResourceId(resource.getId()).stream()
                .filter(rule -> !Boolean.TRUE.equals(rule.getDeleted()))
                .toList();
        int participants = Math.max(1, quantity);
        long minutes = Duration.between(start, end).toMinutes();

        if (forcedUnit != null) {
            Candidate forced = cost(resource, rules, forcedUnit, start, end, participants, minutes, ctx, true);
            if (forced == null) {
                throw new BadRequestException("La ressource " + resource.getCode() + " n'a aucun prix de base en " + forcedUnit
                        + " applicable à ce créneau · créez-le avant d'imposer cette unité, ou laissez le calcul automatique.");
            }
            return forced.result(List.of(new PriceResult.Alternative(forcedUnit, forced.amount, true)));
        }

        Map<ResourceBookingUnit, Candidate> candidates = new EnumMap<>(ResourceBookingUnit.class);
        for (ResourceBookingUnit unit : ORDER) {
            Candidate candidate = cost(resource, rules, unit, start, end, participants, minutes, ctx, false);
            if (candidate != null) {
                candidates.put(unit, candidate);
            }
        }
        if (candidates.isEmpty()) {
            return PriceResult.free(ResourceBookingUnit.HOUR, multiplier(resource, null, participants));
        }
        ResourceBookingUnit intended = intended(minutes);
        Candidate best = candidates.values().stream()
                .min(Comparator.comparing((Candidate c) -> c.amount)
                        .thenComparing(c -> c.unit == intended ? 0 : 1)
                        .thenComparing(c -> ORDER.indexOf(c.unit)))
                .orElseThrow();
        List<PriceResult.Alternative> alternatives = candidates.values().stream()
                .map(c -> new PriceResult.Alternative(c.unit, c.amount, c == best))
                .toList();
        return best.result(alternatives);
    }

    /**
     * Prix de base d'affichage (catalogue) : regle de base de la plus petite unite qui en a une,
     * pour un client public, aujourd'hui, sans ajustement.
     */
    public Optional<ResourcePricingRule> listPriceRule(Resource resource) {
        LocalDateTime now = LocalDateTime.now(java.time.ZoneId.of("Africa/Brazzaville"));
        List<ResourcePricingRule> rules = ruleRepository.findAllActiveByResourceId(resource.getId()).stream()
                .filter(rule -> !Boolean.TRUE.equals(rule.getDeleted())).toList();
        for (ResourceBookingUnit unit : ORDER) {
            Optional<ResourcePricingRule> base = rules.stream()
                    .filter(rule -> unit == rule.getResourceBookingUnit() && isBase(rule))
                    .filter(rule -> validOn(rule, now.toLocalDate()))
                    .filter(rule -> !StringUtils.hasText(rule.getCustomerSegment()) || "PUBLIC".equalsIgnoreCase(rule.getCustomerSegment()))
                    .min(PRIORITY_ORDER.reversed().thenComparing(ResourcePriceEngine::specificity));
            if (base.isPresent()) {
                return base;
            }
        }
        return Optional.empty();
    }

    // ------------------------------------------------------------------ calcul d'une unite

    private final class Candidate {
        final ResourceBookingUnit unit;
        final BigDecimal units;
        final int multiplier;
        final BigDecimal amount;
        final List<PriceResult.Line> lines;

        Candidate(ResourceBookingUnit unit, BigDecimal units, int multiplier, BigDecimal amount, List<PriceResult.Line> lines) {
            this.unit = unit;
            this.units = units;
            this.multiplier = multiplier;
            this.amount = amount;
            this.lines = lines;
        }

        PriceResult result(List<PriceResult.Alternative> alternatives) {
            BigDecimal divisor = units.multiply(BigDecimal.valueOf(Math.max(1, multiplier)));
            BigDecimal unitPrice = divisor.signum() == 0 ? BigDecimal.ZERO : amount.divide(divisor, 4, RoundingMode.HALF_UP);
            return new PriceResult(unit, unitPrice, units, multiplier, amount, CURRENCY, lines, alternatives);
        }
    }

    private Candidate cost(Resource resource, List<ResourcePricingRule> rules, ResourceBookingUnit unit, LocalDateTime start,
                           LocalDateTime end, int participants, long minutes, PriceContext ctx, boolean imposed) {
        if (rules.stream().noneMatch(rule -> unit == rule.getResourceBookingUnit() && isBase(rule))) {
            return null;
        }
        if (unit != ResourceBookingUnit.HOUR) {
            Rate rate = rate(rules, unit, start, start, minutes, participants, ctx);
            if (rate == null) {
                return null;
            }
            BigDecimal count = forfaitCount(unit, start, end, minutes, imposed);
            int multiplier = multiplier(resource, rate.base, participants);
            BigDecimal amount = rate.value.multiply(count).multiply(BigDecimal.valueOf(multiplier)).setScale(4, RoundingMode.HALF_UP);
            return new Candidate(unit, count, multiplier, amount,
                    List.of(new PriceResult.Line(start.toString(), end.toString(), count, rate.value, rate.base.getShortId(),
                            rate.base.getLabel(), rate.adjustments, amount)));
        }
        // A l'heure : tranches aux bornes des regles et a minuit, prix au prorata.
        List<LocalDateTime> bounds = bounds(rules, start, end);
        List<PriceResult.Line> lines = new ArrayList<>();
        BigDecimal amount = BigDecimal.ZERO;
        int multiplier = 1;
        for (int i = 0; i + 1 < bounds.size(); i++) {
            LocalDateTime a = bounds.get(i);
            LocalDateTime b = bounds.get(i + 1);
            Rate rate = rate(rules, unit, a, start, minutes, participants, ctx);
            if (rate == null) {
                return null;
            }
            multiplier = multiplier(resource, rate.base, participants);
            BigDecimal hours = BigDecimal.valueOf(Duration.between(a, b).toMinutes()).divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);
            BigDecimal lineAmount = rate.value.multiply(hours).multiply(BigDecimal.valueOf(multiplier)).setScale(4, RoundingMode.HALF_UP);
            PriceResult.Line previous = lines.isEmpty() ? null : lines.get(lines.size() - 1);
            if (previous != null && Objects.equals(previous.baseRuleId(), rate.base.getShortId())
                    && previous.unitPrice().compareTo(rate.value) == 0 && previous.adjustments().equals(rate.adjustments)) {
                lines.set(lines.size() - 1, new PriceResult.Line(previous.from(), b.toString(), previous.units().add(hours),
                        previous.unitPrice(), previous.baseRuleId(), previous.baseRuleLabel(), previous.adjustments(),
                        previous.amount().add(lineAmount)));
            } else {
                lines.add(new PriceResult.Line(a.toString(), b.toString(), hours, rate.value, rate.base.getShortId(),
                        rate.base.getLabel(), rate.adjustments, lineAmount));
            }
            amount = amount.add(lineAmount);
        }
        BigDecimal hours = BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);
        return new Candidate(unit, hours, multiplier, amount.setScale(4, RoundingMode.HALF_UP), List.copyOf(lines));
    }

    /** Nombre de forfaits : un seul quand l'equipe impose l'unite ; sur plusieurs jours, un par jour touche. */
    BigDecimal forfaitCount(ResourceBookingUnit unit, LocalDateTime start, LocalDateTime end, long minutes, boolean imposed) {
        if (imposed) {
            return BigDecimal.ONE.setScale(4, RoundingMode.HALF_UP);
        }
        long days = ChronoUnit.DAYS.between(start.toLocalDate(), end.minusNanos(1).toLocalDate()) + 1;
        long count = switch (unit) {
            case HALF_DAY -> days > 1 ? 2 * days : ceil(minutes, halfDayMinutes());
            case DAY -> days > 1 ? days : ceil(minutes, dayMinutes());
            case WEEK -> Math.max(1, ceil(days, 7));
            case MONTH -> {
                long months = ChronoUnit.MONTHS.between(start, end);
                yield Math.max(1, months + (start.plusMonths(months).isBefore(end) ? 1 : 0));
            }
            case HOUR -> ceil(minutes, 60);
        };
        return BigDecimal.valueOf(Math.max(1, count)).setScale(4, RoundingMode.HALF_UP);
    }

    private ResourceBookingUnit intended(long minutes) {
        if (minutes >= 43200) return ResourceBookingUnit.MONTH;
        if (minutes >= 10080) return ResourceBookingUnit.WEEK;
        if (minutes >= dayMinutes()) return ResourceBookingUnit.DAY;
        if (minutes == halfDayMinutes()) return ResourceBookingUnit.HALF_DAY;
        return ResourceBookingUnit.HOUR;
    }

    /** On loue une salle entiere, pas un siege ; une regle « par participant » multiplie toujours. */
    private static int multiplier(Resource resource, ResourcePricingRule base, int participants) {
        if (base != null && Boolean.TRUE.equals(base.getPerParticipant())) {
            return participants;
        }
        return resource != null && resource.isWholeResourceBooking() ? 1 : participants;
    }

    // ------------------------------------------------------------------ regles

    private record Rate(ResourcePricingRule base, BigDecimal value, List<PriceResult.Adjustment> adjustments) {
    }

    private static final Comparator<ResourcePricingRule> PRIORITY_ORDER = Comparator
            .comparing((ResourcePricingRule rule) -> rule.getPriority() == null ? 0 : rule.getPriority());

    /** Meilleure regle : priorite, puis precision, puis la plus recente. */
    private static final Comparator<ResourcePricingRule> BEST = PRIORITY_ORDER
            .thenComparing(ResourcePriceEngine::specificity)
            .thenComparing(rule -> rule.getCreatedAt() == null ? java.time.Instant.EPOCH : rule.getCreatedAt())
            .thenComparing(rule -> rule.getId() == null ? 0L : rule.getId());

    private Rate rate(List<ResourcePricingRule> rules, ResourceBookingUnit unit, LocalDateTime at, LocalDateTime start,
                      long minutes, int participants, PriceContext ctx) {
        List<ResourcePricingRule> applicable = rules.stream()
                .filter(rule -> unit == rule.getResourceBookingUnit())
                .filter(rule -> applies(rule, at, start, minutes, participants, ctx))
                .toList();
        Optional<ResourcePricingRule> base = applicable.stream().filter(ResourcePriceEngine::isBase).max(BEST);
        if (base.isEmpty()) {
            return null;
        }
        BigDecimal value = BigDecimal.valueOf(base.get().getPrice());
        List<PriceResult.Adjustment> applied = new ArrayList<>();
        List<ResourcePricingRule> adjustments = applicable.stream().filter(rule -> rule.getAdjustmentType() != null).toList();

        Optional<ResourcePricingRule> fixed = adjustments.stream()
                .filter(rule -> rule.getAdjustmentType() == ResourcePriceAdjustmentType.FIXED_PRICE).max(BEST);
        if (fixed.isPresent()) {
            value = BigDecimal.valueOf(fixed.get().getAdjustmentValue() == null ? 0 : fixed.get().getAdjustmentValue());
            applied.add(adjustment(fixed.get()));
        }
        List<ResourcePricingRule> deltas = new ArrayList<>();
        adjustments.stream().filter(rule -> rule.getAdjustmentType() != ResourcePriceAdjustmentType.FIXED_PRICE)
                .filter(rule -> !Boolean.TRUE.equals(rule.getStackable())).max(BEST).ifPresent(deltas::add);
        adjustments.stream().filter(rule -> rule.getAdjustmentType() != ResourcePriceAdjustmentType.FIXED_PRICE)
                .filter(rule -> Boolean.TRUE.equals(rule.getStackable())).forEach(deltas::add);
        deltas.sort(BEST.reversed());
        for (ResourcePricingRule delta : deltas) {
            BigDecimal amount = BigDecimal.valueOf(delta.getAdjustmentValue() == null ? 0 : delta.getAdjustmentValue());
            value = switch (delta.getAdjustmentType()) {
                case AMOUNT_DELTA -> value.add(amount);
                case PERCENT_DELTA -> value.add(value.multiply(amount).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
                case FIXED_PRICE -> value;
            };
            applied.add(adjustment(delta));
        }
        if (value.signum() < 0) {
            value = BigDecimal.ZERO;
        }
        return new Rate(base.get(), value.setScale(4, RoundingMode.HALF_UP), List.copyOf(applied));
    }

    private static PriceResult.Adjustment adjustment(ResourcePricingRule rule) {
        return new PriceResult.Adjustment(rule.getShortId(), rule.getLabel(), rule.getAdjustmentType().name(), rule.getAdjustmentValue());
    }

    static boolean isBase(ResourcePricingRule rule) {
        return rule.getAdjustmentType() == null && rule.getPrice() != null;
    }

    /** La regle s'applique-t-elle a cet instant de la reservation ? */
    boolean applies(ResourcePricingRule rule, LocalDateTime at, LocalDateTime start, long minutes, int participants, PriceContext ctx) {
        if (!validOn(rule, at.toLocalDate())) {
            return false;
        }
        List<Integer> days = days(rule);
        if (!days.isEmpty() && !days.contains(at.getDayOfWeek().getValue())) {
            return false;
        }
        if (!inWindow(rule.getStartsAt(), rule.getEndsAt(), at.toLocalTime())) {
            return false;
        }
        // Lot B10 : regle reservee a des clients (prix au cas par cas, membres, segments, plans).
        if (StringUtils.hasText(rule.getAudience())) {
            java.util.Set<String> keys = ctx.audienceKeys();
            if (com.sni.bokaticowork.features.ressource.dto.request.AudienceRef.split(rule.getAudience()).stream().noneMatch(ref -> keys.contains(ref.type() + ":" + ref.code()))) {
                return false;
            }
        }
        // Lot R5a : jours feries (nationaux calcules et ajoutes par l'equipe).
        if (StringUtils.hasText(rule.getHolidayMode())) {
            boolean holiday = holidays.test(at.toLocalDate());
            if ("ONLY".equalsIgnoreCase(rule.getHolidayMode()) != holiday) {
                return false;
            }
        }
        if (StringUtils.hasText(rule.getCustomerSegment())
                && (ctx.segments() == null || ctx.segments().stream().noneMatch(s -> s.equalsIgnoreCase(rule.getCustomerSegment())))) {
            return false;
        }
        if (StringUtils.hasText(rule.getPlanCode()) && !rule.getPlanCode().equalsIgnoreCase(ctx.planCode())) {
            return false;
        }
        if (rule.getMinParticipants() != null && participants < rule.getMinParticipants()) return false;
        if (rule.getMaxParticipants() != null && participants > rule.getMaxParticipants()) return false;
        if (rule.getMinDurationMinutes() != null && minutes < rule.getMinDurationMinutes()) return false;
        if (rule.getMaxDurationMinutes() != null && minutes > rule.getMaxDurationMinutes()) return false;
        long lead = ctx.now() == null ? Long.MAX_VALUE : Duration.between(ctx.now(), start).toMinutes();
        if (rule.getLastMinuteMinutes() != null && lead > rule.getLastMinuteMinutes()) return false;
        if (rule.getMinLeadMinutes() != null && lead < rule.getMinLeadMinutes()) return false;
        return !StringUtils.hasText(rule.getChannel()) || rule.getChannel().equalsIgnoreCase(ctx.channel());
    }

    private static boolean validOn(ResourcePricingRule rule, LocalDate date) {
        return (rule.getValidFrom() == null || !date.isBefore(rule.getValidFrom()))
                && (rule.getValidUntil() == null || !date.isAfter(rule.getValidUntil()));
    }

    /** Plage horaire [debut, fin[ ; une plage qui passe minuit (22:00-06:00) est admise. */
    static boolean inWindow(LocalTime from, LocalTime to, LocalTime time) {
        if (from == null && to == null) return true;
        LocalTime start = from == null ? LocalTime.MIN : from;
        if (to == null) return !time.isBefore(start);
        if (start.isBefore(to)) return !time.isBefore(start) && time.isBefore(to);
        return !time.isBefore(start) || time.isBefore(to);
    }

    static List<Integer> days(ResourcePricingRule rule) {
        if (StringUtils.hasText(rule.getDaysOfWeek())) {
            return Arrays.stream(rule.getDaysOfWeek().split("[,; ]+")).filter(StringUtils::hasText)
                    .map(String::trim).map(Integer::valueOf).toList();
        }
        return rule.getDayOfWeek() == null ? List.of() : List.of(rule.getDayOfWeek());
    }

    /** Nombre de conditions posees : la regle la plus precise l'emporte a priorite egale. */
    static int specificity(ResourcePricingRule rule) {
        int count = 0;
        if (!days(rule).isEmpty()) count++;
        if (rule.getStartsAt() != null || rule.getEndsAt() != null) count++;
        if (rule.getValidFrom() != null || rule.getValidUntil() != null) count++;
        if (StringUtils.hasText(rule.getCustomerSegment())) count++;
        if (StringUtils.hasText(rule.getPlanCode())) count += 2;
        if (rule.getMinParticipants() != null || rule.getMaxParticipants() != null) count++;
        if (rule.getMinDurationMinutes() != null || rule.getMaxDurationMinutes() != null) count++;
        if (rule.getLastMinuteMinutes() != null || rule.getMinLeadMinutes() != null) count++;
        if (StringUtils.hasText(rule.getChannel())) count++;
        // Lot B10 : un prix designe pour des clients l'emporte sur les conditions generales.
        if (StringUtils.hasText(rule.getAudience())) count += 3;
        return count;
    }

    /** Bornes des tranches horaires : debut, fin, minuits, et bornes horaires des regles a l'heure. */
    private static List<LocalDateTime> bounds(List<ResourcePricingRule> rules, LocalDateTime start, LocalDateTime end) {
        TreeSet<LocalDateTime> points = new TreeSet<>();
        points.add(start);
        points.add(end);
        List<LocalTime> times = new ArrayList<>();
        times.add(LocalTime.MIDNIGHT);
        for (ResourcePricingRule rule : rules) {
            if (rule.getResourceBookingUnit() != ResourceBookingUnit.HOUR) continue;
            if (rule.getStartsAt() != null) times.add(rule.getStartsAt());
            if (rule.getEndsAt() != null) times.add(rule.getEndsAt());
        }
        for (LocalDate day = start.toLocalDate(); !day.isAfter(end.toLocalDate()); day = day.plusDays(1)) {
            for (LocalTime time : times) {
                LocalDateTime point = day.atTime(time);
                if (point.isAfter(start) && point.isBefore(end)) {
                    points.add(point);
                }
            }
        }
        return new ArrayList<>(points);
    }

    private static long ceil(long value, long divisor) {
        return (long) Math.ceil((double) value / divisor);
    }
}
