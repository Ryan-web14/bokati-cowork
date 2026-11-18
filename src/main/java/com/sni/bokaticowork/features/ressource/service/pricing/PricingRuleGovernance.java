package com.sni.bokaticowork.features.ressource.service.pricing;

import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.features.ressource.model.ResourcePricingRule;
import com.sni.bokaticowork.features.ressource.model.ResourcePricingRuleVersion;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourcePricingRuleRepository;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourcePricingRuleVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Lot R1 : versions et coherence des regles tarifaires.
 *
 * <ul>
 *   <li>Versions : une photo de la regle a chaque changement ({@link #record}).</li>
 *   <li>Doublon strict refuse : meme ressource, meme unite, meme role (base ou meme ajustement),
 *   memes conditions et meme priorite qu'une regle active.</li>
 *   <li>Conflits signales ({@link #conflicts}) : deux regles actives de meme unite, meme role, meme
 *   priorite et meme precision dont les conditions se recouvrent ; le moteur les departage par la
 *   plus recente, ce qui n'est pas forcement voulu.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class PricingRuleGovernance {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final ResourcePricingRuleRepository ruleRepository;
    private final ResourcePricingRuleVersionRepository versionRepository;

    public void record(ResourcePricingRule rule, String changeType) {
        if (rule.getShortId() == null) {
            return;
        }
        int version = (int) versionRepository.countByRuleShortId(rule.getShortId()) + 1;
        versionRepository.save(ResourcePricingRuleVersion.builder()
                .ruleShortId(rule.getShortId())
                .resourceCode(rule.getResource() == null ? null : rule.getResource().getCode())
                .version(version)
                .changeType(changeType)
                .snapshot(snapshot(rule))
                .changedBy(actor())
                .changedAt(Instant.now())
                .build());
    }

    public List<ResourcePricingRuleVersion> history(Long shortId) {
        return versionRepository.findAllByRuleShortIdOrderByVersionDesc(shortId);
    }

    /** Refuse une regle identique a une regle active de la meme ressource (sauf elle-meme). */
    public void rejectDuplicate(ResourcePricingRule candidate) {
        if (candidate.getResource() == null) {
            return;
        }
        for (ResourcePricingRule other : ruleRepository.findAllActiveByResourceId(candidate.getResource().getId())) {
            if (Boolean.TRUE.equals(other.getDeleted()) || Objects.equals(other.getId(), candidate.getId())) {
                continue;
            }
            if (sameRole(candidate, other) && Objects.equals(priority(candidate), priority(other)) && sameConditions(candidate, other)) {
                throw new ConflictException("Règle en double de la règle " + other.getShortId() + " (" + other.getLabel()
                        + ") : même unité, mêmes conditions, même priorité. Modifiez-la plutôt, ou changez la priorité.");
            }
        }
    }

    /** Paire de regles ambigues d'une ressource. */
    public record Conflict(Long firstRuleId, String firstLabel, Long secondRuleId, String secondLabel, String unit, String reason) {
    }

    public List<Conflict> conflicts(Long resourceId) {
        List<ResourcePricingRule> rules = ruleRepository.findAllActiveByResourceId(resourceId).stream()
                .filter(rule -> !Boolean.TRUE.equals(rule.getDeleted())).toList();
        List<Conflict> conflicts = new ArrayList<>();
        for (int i = 0; i < rules.size(); i++) {
            for (int j = i + 1; j < rules.size(); j++) {
                ResourcePricingRule a = rules.get(i);
                ResourcePricingRule b = rules.get(j);
                if (sameRole(a, b) && Objects.equals(priority(a), priority(b))
                        && ResourcePriceEngine.specificity(a) == ResourcePriceEngine.specificity(b) && overlap(a, b)) {
                    boolean nonStackableAdjustments = a.getAdjustmentType() != null
                            && !Boolean.TRUE.equals(a.getStackable()) && !Boolean.TRUE.equals(b.getStackable());
                    if (a.getAdjustmentType() == null || nonStackableAdjustments) {
                        conflicts.add(new Conflict(a.getShortId(), a.getLabel(), b.getShortId(), b.getLabel(),
                                String.valueOf(a.getResourceBookingUnit()),
                                (a.getAdjustmentType() == null ? "Deux prix de base" : "Deux ajustements non cumulables")
                                        + " de même priorité et de même précision s'appliquent aux mêmes créneaux : la plus récente l'emporte. "
                                        + "Donnez-leur des priorités différentes ou des conditions distinctes."));
                    }
                }
            }
        }
        return conflicts;
    }

    // ------------------------------------------------------------------ comparaisons

    private static boolean sameRole(ResourcePricingRule a, ResourcePricingRule b) {
        return a.getResourceBookingUnit() == b.getResourceBookingUnit() && a.getAdjustmentType() == b.getAdjustmentType();
    }

    private static int priority(ResourcePricingRule rule) {
        return rule.getPriority() == null ? 0 : rule.getPriority();
    }

    private static boolean sameConditions(ResourcePricingRule a, ResourcePricingRule b) {
        return ResourcePriceEngine.days(a).equals(ResourcePriceEngine.days(b))
                && Objects.equals(a.getStartsAt(), b.getStartsAt()) && Objects.equals(a.getEndsAt(), b.getEndsAt())
                && Objects.equals(a.getValidFrom(), b.getValidFrom()) && Objects.equals(a.getValidUntil(), b.getValidUntil())
                && sameText(a.getCustomerSegment(), b.getCustomerSegment()) && sameText(a.getPlanCode(), b.getPlanCode())
                && sameText(a.getChannel(), b.getChannel())
                && sameText(a.getAudience(), b.getAudience())
                && Objects.equals(a.getMinParticipants(), b.getMinParticipants()) && Objects.equals(a.getMaxParticipants(), b.getMaxParticipants())
                && Objects.equals(a.getMinDurationMinutes(), b.getMinDurationMinutes())
                && Objects.equals(a.getMaxDurationMinutes(), b.getMaxDurationMinutes())
                && Objects.equals(a.getMinLeadMinutes(), b.getMinLeadMinutes())
                && Objects.equals(a.getLastMinuteMinutes(), b.getLastMinuteMinutes());
    }

    /** Les conditions de deux regles peuvent-elles viser le meme creneau ? */
    private static boolean overlap(ResourcePricingRule a, ResourcePricingRule b) {
        List<Integer> da = ResourcePriceEngine.days(a);
        List<Integer> db = ResourcePriceEngine.days(b);
        if (!da.isEmpty() && !db.isEmpty() && da.stream().noneMatch(db::contains)) return false;
        if (!timesOverlap(a.getStartsAt(), a.getEndsAt(), b.getStartsAt(), b.getEndsAt())) return false;
        if (!datesOverlap(a.getValidFrom(), a.getValidUntil(), b.getValidFrom(), b.getValidUntil())) return false;
        return compatible(a.getCustomerSegment(), b.getCustomerSegment()) && compatible(a.getPlanCode(), b.getPlanCode())
                && compatible(a.getChannel(), b.getChannel())
                && sameText(a.getAudience(), b.getAudience())
                && rangesOverlap(a.getMinParticipants(), a.getMaxParticipants(), b.getMinParticipants(), b.getMaxParticipants())
                && rangesOverlap(a.getMinDurationMinutes(), a.getMaxDurationMinutes(), b.getMinDurationMinutes(), b.getMaxDurationMinutes());
    }

    private static boolean timesOverlap(LocalTime s1, LocalTime e1, LocalTime s2, LocalTime e2) {
        if ((s1 == null && e1 == null) || (s2 == null && e2 == null)) return true;
        for (int minute = 0; minute < 24 * 60; minute += 15) {
            LocalTime time = LocalTime.MIN.plusMinutes(minute);
            if (ResourcePriceEngine.inWindow(s1, e1, time) && ResourcePriceEngine.inWindow(s2, e2, time)) return true;
        }
        return false;
    }

    private static boolean datesOverlap(LocalDate f1, LocalDate u1, LocalDate f2, LocalDate u2) {
        return (u2 == null || f1 == null || !f1.isAfter(u2)) && (u1 == null || f2 == null || !f2.isAfter(u1));
    }

    private static boolean rangesOverlap(Integer min1, Integer max1, Integer min2, Integer max2) {
        return (max2 == null || min1 == null || min1 <= max2) && (max1 == null || min2 == null || min2 <= max1);
    }

    private static boolean compatible(String a, String b) {
        return !StringUtils.hasText(a) || !StringUtils.hasText(b) || a.trim().equalsIgnoreCase(b.trim());
    }

    private static boolean sameText(String a, String b) {
        return Objects.equals(StringUtils.hasText(a) ? a.trim().toUpperCase() : null, StringUtils.hasText(b) ? b.trim().toUpperCase() : null);
    }

    private static String snapshot(ResourcePricingRule rule) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", rule.getShortId());
        map.put("resourceCode", rule.getResource() == null ? null : rule.getResource().getCode());
        map.put("bookingUnit", String.valueOf(rule.getResourceBookingUnit()));
        map.put("price", rule.getPrice());
        map.put("label", rule.getLabel());
        map.put("daysOfWeek", StringUtils.hasText(rule.getDaysOfWeek()) ? rule.getDaysOfWeek()
                : rule.getDayOfWeek() == null ? null : String.valueOf(rule.getDayOfWeek()));
        map.put("startsAt", rule.getStartsAt() == null ? null : rule.getStartsAt().toString());
        map.put("endsAt", rule.getEndsAt() == null ? null : rule.getEndsAt().toString());
        map.put("adjustmentType", rule.getAdjustmentType() == null ? null : rule.getAdjustmentType().name());
        map.put("adjustmentValue", rule.getAdjustmentValue());
        map.put("validFrom", rule.getValidFrom() == null ? null : rule.getValidFrom().toString());
        map.put("validUntil", rule.getValidUntil() == null ? null : rule.getValidUntil().toString());
        map.put("customerSegment", rule.getCustomerSegment());
        map.put("planCode", rule.getPlanCode());
        map.put("minParticipants", rule.getMinParticipants());
        map.put("maxParticipants", rule.getMaxParticipants());
        map.put("minDurationMinutes", rule.getMinDurationMinutes());
        map.put("maxDurationMinutes", rule.getMaxDurationMinutes());
        map.put("minLeadMinutes", rule.getMinLeadMinutes());
        map.put("lastMinuteMinutes", rule.getLastMinuteMinutes());
        map.put("channel", rule.getChannel());
        map.put("audience", rule.getAudience());
        map.put("stackable", rule.getStackable());
        map.put("perParticipant", rule.getPerParticipant());
        map.put("priority", rule.getPriority());
        map.put("active", rule.getActive());
        map.put("deleted", rule.getDeleted());
        return JSON.writeValueAsString(map);
    }

    private static String actor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? "SYSTEM" : auth.getName();
    }
}
