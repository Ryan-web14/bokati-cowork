package com.sni.bokaticowork.features.ressource.service.implementation;

import org.springframework.beans.factory.annotation.Autowired;
import com.sni.bokaticowork.core.settings.platform.PlatformSettings;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.exception.customs.ValidationException;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.ressource.dto.request.CreateResourcePricingRuleRequest;
import com.sni.bokaticowork.features.ressource.dto.request.UpdateResourcePricingRuleRequest;
import com.sni.bokaticowork.features.ressource.dto.response.ResourcePriceQuoteResponse;
import com.sni.bokaticowork.features.ressource.dto.response.ResourcePricingRuleResponse;
import com.sni.bokaticowork.features.ressource.enums.ResourceBookingUnit;
import com.sni.bokaticowork.features.ressource.enums.ResourcePriceAdjustmentType;
import com.sni.bokaticowork.features.ressource.dto.request.BulkCreateResourcePricingRuleRequest;
import com.sni.bokaticowork.features.ressource.dto.response.BulkResourceOperationResponse;
import com.sni.bokaticowork.features.ressource.dto.response.PricingRuleConflictPreviewResponse;
import com.sni.bokaticowork.features.ressource.service.support.ResourceBulkExecutor;
import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.ressource.model.ResourcePolicy;
import com.sni.bokaticowork.features.ressource.model.ResourcePricingRule;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourcePricingRuleRepository;
import com.sni.bokaticowork.features.ressource.service.interfaces.ResourcePricingRuleService;
import com.sni.bokaticowork.features.ressource.service.interfaces.ResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@Transactional
@RequiredArgsConstructor
public class ResourcePricingRuleServiceImpl implements ResourcePricingRuleService {

    private final ResourcePricingRuleRepository pricingRuleRepository;
    private final ResourceService resourceService;
    private final ResourceBulkExecutor bulkExecutor;

    @org.springframework.beans.factory.annotation.Value("${bokati.resource.bulk-max-resources:20}")
    private int maxBulkResources;

    private static final String RESOURCE_BULK_MAX_RESOURCES_KEY = "bokati.resource.bulk-max-resources";

    private PlatformSettings platformSettings;

    @Autowired(required = false)
    void setPlatformSettings(PlatformSettings platformSettings) {
        this.platformSettings = platformSettings;
    }

    private int maxBulkResources() {
        return platformSettings == null ? maxBulkResources : platformSettings.integer(RESOURCE_BULK_MAX_RESOURCES_KEY, maxBulkResources);
    }

    /**
     * Delegue a {@link #createPricingRule} ressource par ressource · la validation existante
     * s'applique donc a l'identique. Volontairement non transactionnelle : c'est l'executeur qui
     * ouvre une transaction par ressource.
     */
    @Override
    public BulkResourceOperationResponse createPricingRuleBulk(BulkCreateResourcePricingRuleRequest request) {
        return bulkExecutor.run(request.getResourceCodes(), maxBulkResources(), code -> {
            createPricingRule(request.forResource(code));
            return null;
        });
    }

    @Override
    @Transactional(readOnly = true)
    public PricingRuleConflictPreviewResponse previewPricingRuleBulk(BulkCreateResourcePricingRuleRequest request) {
        ResourceBookingUnit unit = parseUnit(request.getBookingUnit());
        int priority = request.getPriority() == null ? 0 : request.getPriority();
        List<PricingRuleConflictPreviewResponse.ResourcePreview> previews = new ArrayList<>();

        // Meme normalisation que la creation · un lot accepte ici doit l'etre a la creation.
        List<String> codes = bulkExecutor.normalizeCodes(request.getResourceCodes(), maxBulkResources());

        for (String code : codes) {
            Resource resource = resourceService.getResourceForService(code);
            List<ResourcePricingRule> existing = pricingRuleRepository.findAllActiveByResourceId(resource.getId());

            List<PricingRuleConflictPreviewResponse.CompetingRule> overlapping = existing.stream()
                    .filter(rule -> rule.getResourceBookingUnit() == unit)
                    .filter(rule -> overlaps(rule, request))
                    .map(rule -> new PricingRuleConflictPreviewResponse.CompetingRule(
                            rule.getShortId(), rule.getLabel(), rule.getPrice(),
                            rule.getPriority(), rule.getDayOfWeek(),
                            asText(rule.getStartsAt()), asText(rule.getEndsAt()),
                            // A priorite egale, findApplicableRules departage par id decroissant ·
                            // la regle nouvelle, plus recente, l'emporterait donc.
                            orZero(rule.getPriority()) > priority))
                    .toList();

            boolean duplicate = existing.stream()
                    .anyMatch(rule -> rule.getResourceBookingUnit() == unit && isStrictDuplicate(rule, request));

            previews.add(new PricingRuleConflictPreviewResponse.ResourcePreview(
                    resource.getCode(), duplicate, overlapping));
        }

        return new PricingRuleConflictPreviewResponse(
                previews.size(),
                (int) previews.stream().filter(p -> !p.overlapping().isEmpty()).count(),
                (int) previews.stream().filter(PricingRuleConflictPreviewResponse.ResourcePreview::duplicate).count(),
                previews);
    }

    /**
     * Les deux regles peuvent-elles s'appliquer au meme moment ?
     *
     * <p>Une borne absente vaut « toujours » · une regle sans jour ni horaire couvre toute la
     * semaine, et chevauche donc n'importe quelle autre.
     */
    private boolean overlaps(ResourcePricingRule rule, BulkCreateResourcePricingRuleRequest request) {
        if (rule.getDayOfWeek() != null && request.getDayOfWeek() != null
                && !rule.getDayOfWeek().equals(request.getDayOfWeek())) {
            return false;
        }
        if (rule.getStartsAt() != null && request.getEndsAt() != null
                && !rule.getStartsAt().isBefore(request.getEndsAt())) {
            return false;
        }
        if (request.getStartsAt() != null && rule.getEndsAt() != null
                && !request.getStartsAt().isBefore(rule.getEndsAt())) {
            return false;
        }
        if (rule.getValidUntil() != null && request.getValidFrom() != null
                && rule.getValidUntil().isBefore(request.getValidFrom())) {
            return false;
        }
        return request.getValidUntil() == null || rule.getValidFrom() == null
                || !request.getValidUntil().isBefore(rule.getValidFrom());
    }

    /** Regle strictement identique · elle n'apporte rien et brouille l'arbitrage par priorite. */
    private boolean isStrictDuplicate(ResourcePricingRule rule, BulkCreateResourcePricingRuleRequest request) {
        return java.util.Objects.equals(rule.getPrice(), request.getPrice())
                && java.util.Objects.equals(rule.getDayOfWeek(), request.getDayOfWeek())
                && java.util.Objects.equals(rule.getStartsAt(), request.getStartsAt())
                && java.util.Objects.equals(rule.getEndsAt(), request.getEndsAt())
                && java.util.Objects.equals(rule.getValidFrom(), request.getValidFrom())
                && java.util.Objects.equals(rule.getValidUntil(), request.getValidUntil());
    }

    private int orZero(Integer value) {
        return value == null ? 0 : value;
    }

    private String asText(java.time.LocalTime time) {
        return time == null ? null : time.toString();
    }

    @Override
    public com.sni.bokaticowork.features.ressource.dto.response.ResourcePricingRuleResponse createPricingRule(CreateResourcePricingRuleRequest request) {
        List<String> errors = validateRequest(request);
        if (!errors.isEmpty()) {
            throw new ValidationException("Invalid resource pricing rule request", errors);
        }

        Resource resource = resourceService.getResourceForService(request.getResourceCode().trim());
        ResourcePricingRule rule = ResourcePricingRule.builder()
                .resource(resource)
                .resourceBookingUnit(parseUnit(request.getBookingUnit()))
                .price(request.getPrice())
                .label(request.getLabel())
                .dayOfWeek(request.getDayOfWeek())
                .startsAt(request.getStartsAt())
                .endsAt(request.getEndsAt())
                .adjustmentType(parseAdjustmentType(request.getAdjustmentType()))
                .adjustmentValue(request.getAdjustmentValue())
                .validFrom(request.getValidFrom())
                .validUntil(request.getValidUntil())
                .lastMinuteMinutes(request.getLastMinuteMinutes())
                .priority(request.getPriority() == null ? 0 : request.getPriority())
                .active(request.getActive() == null ? Boolean.TRUE : request.getActive())
                .daysOfWeek(normalizeDays(request.getDaysOfWeek()))
                .customerSegment(upper(request.getCustomerSegment()))
                .planCode(blankToNull(request.getPlanCode()))
                .minParticipants(request.getMinParticipants())
                .maxParticipants(request.getMaxParticipants())
                .minDurationMinutes(request.getMinDurationMinutes())
                .maxDurationMinutes(request.getMaxDurationMinutes())
                .minLeadMinutes(request.getMinLeadMinutes())
                .channel(upper(request.getChannel()))
                .stackable(Boolean.TRUE.equals(request.getStackable()))
                .perParticipant(Boolean.TRUE.equals(request.getPerParticipant()))
                .build();

        // Lot R1 : doublon strict refuse ; version 1 gardee (numero court relu apres l'insertion).
        governance.rejectDuplicate(rule);
        pricingRuleRepository.saveAndFlush(rule);
        governance.record(rule, "CREATE");
        return toResponse(rule);
    }

    @Override
    @Transactional(readOnly = true)
    public ResourcePricingRuleResponse getPricingRule(Long id) {
        return toResponse(getPricingRuleForService(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<ResourcePricingRuleResponse> list(Pageable pageable) {
        Page<ResourcePricingRuleResponse> page = pricingRuleRepository.findAllByDeletedFalse(pageable).map(this::toResponse);
        return new PaginatedResponse<>(page);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<ResourcePricingRuleResponse> listByResource(String resourceCode, Pageable pageable) {
        Resource resource = resourceService.getResourceForService(resourceCode);
        Page<ResourcePricingRuleResponse> page = pricingRuleRepository.findAllByResourceAndDeletedFalse(resource, pageable).map(this::toResponse);
        return new PaginatedResponse<>(page);
    }

    @Override
    @Transactional(readOnly = true)
    public ResourcePriceQuoteResponse quote(String resourceCode, String bookingUnit, LocalDateTime startedAt, LocalDateTime endedAt) {
        return quote(resourceCode, bookingUnit, startedAt, endedAt, 1, null, null, null);
    }

    /**
     * Lot R1 : devis par le moteur unique, avec le contexte du titulaire (le meme que la reservation).
     * {@code bookingUnit} impose l'unite (equipe) ; vide, l'unite la moins chere est retenue.
     */
    @Override
    @Transactional(readOnly = true)
    public ResourcePriceQuoteResponse quote(String resourceCode, String bookingUnit, LocalDateTime startedAt, LocalDateTime endedAt,
                                            Integer quantity, String ownerType, String ownerCode, String channel) {
        if (!StringUtils.hasText(resourceCode) || startedAt == null || endedAt == null) {
            throw new BadRequestException("La ressource, le début et la fin sont requis");
        }
        if (!endedAt.isAfter(startedAt)) {
            throw new BadRequestException("La fin doit suivre le début");
        }
        Resource resource = resourceService.getResourceForService(resourceCode);
        validateAgainstPolicy(resource, startedAt, endedAt);
        com.sni.bokaticowork.features.ressource.enums.ResourceBookingUnit unit = StringUtils.hasText(bookingUnit) ? parseUnit(bookingUnit) : null;
        com.sni.bokaticowork.features.ressource.service.pricing.PriceContext context = priceContextResolver == null
                ? com.sni.bokaticowork.features.ressource.service.pricing.PriceContext.anonymous()
                : priceContextResolver.forOwner(ownerType, ownerCode);
        if (StringUtils.hasText(channel)) {
            context = new com.sni.bokaticowork.features.ressource.service.pricing.PriceContext(context.segments(), context.planCode(),
                    channel.trim().toUpperCase(java.util.Locale.ROOT), context.now());
        }
        com.sni.bokaticowork.features.ressource.service.pricing.PriceResult result =
                priceEngine.price(resource, startedAt, endedAt, quantity == null ? 1 : quantity, unit, context);
        if (result.lines().isEmpty()) {
            throw new ResourceNotFoundException("Aucun prix de base actif pour la ressource " + resourceCode);
        }
        com.sni.bokaticowork.features.ressource.service.pricing.PriceResult.Line first = result.lines().get(0);
        com.sni.bokaticowork.features.ressource.service.pricing.PriceResult.Adjustment adjustment =
                first.adjustments().isEmpty() ? null : first.adjustments().get(0);
        Integer basePrice = first.baseRuleId() == null ? null
                : pricingRuleRepository.findByShortId(first.baseRuleId()).map(ResourcePricingRule::getPrice).orElse(null);
        return ResourcePriceQuoteResponse.builder()
                .resourceCode(resource.getCode())
                .bookingUnit(result.unit().name())
                .startedAt(startedAt)
                .endedAt(endedAt)
                .basePrice(basePrice)
                .finalPrice(result.amount().setScale(0, java.math.RoundingMode.HALF_UP).intValue())
                .appliedRuleId(adjustment != null ? adjustment.ruleId() : first.baseRuleId())
                .appliedRuleLabel(adjustment != null ? adjustment.label() : first.baseRuleLabel())
                .adjustmentType(adjustment == null ? null : adjustment.type())
                .adjustmentValue(adjustment == null ? null : adjustment.value())
                .units(result.units())
                .quantity(result.multiplier())
                .amount(result.amount())
                .currency(result.currency())
                .lines(result.lines())
                .alternatives(result.alternatives())
                .build();
    }

    private com.sni.bokaticowork.features.ressource.service.pricing.ResourcePriceEngine priceEngine;
    private com.sni.bokaticowork.features.ressource.service.pricing.PricingRuleGovernance governance;

    @org.springframework.beans.factory.annotation.Autowired
    void setGovernance(com.sni.bokaticowork.features.ressource.service.pricing.PricingRuleGovernance governance) {
        this.governance = governance;
    }

    /** Lot R1 : versions d'une regle, de la plus recente a la plus ancienne. */
    @Override
    @Transactional(readOnly = true)
    public java.util.List<com.sni.bokaticowork.features.ressource.model.ResourcePricingRuleVersion> history(Long id) {
        Long shortId = pricingRuleRepository.findByShortId(id).or(() -> pricingRuleRepository.findById(id))
                .map(ResourcePricingRule::getShortId)
                .orElseThrow(() -> new ResourceNotFoundException("Règle tarifaire introuvable : " + id));
        return governance.history(shortId);
    }

    /** Lot R1 : regles ambigues d'une ressource. */
    @Override
    @Transactional(readOnly = true)
    public java.util.List<com.sni.bokaticowork.features.ressource.service.pricing.PricingRuleGovernance.Conflict> conflicts(String resourceCode) {
        return governance.conflicts(resourceService.getResourceForService(resourceCode).getId());
    }
    private com.sni.bokaticowork.features.ressource.service.pricing.PriceContextResolver priceContextResolver;

    @org.springframework.beans.factory.annotation.Autowired
    void setPricing(com.sni.bokaticowork.features.ressource.service.pricing.ResourcePriceEngine priceEngine,
                    com.sni.bokaticowork.features.ressource.service.pricing.PriceContextResolver priceContextResolver) {
        this.priceEngine = priceEngine;
        this.priceContextResolver = priceContextResolver;
    }

    @Override
    public ResourcePricingRuleResponse updatePricingRule(Long id, UpdateResourcePricingRuleRequest request) {
        ResourcePricingRule rule = getPricingRuleForService(id);
        if (request.getBookingUnit() != null) {
            rule.setResourceBookingUnit(parseUnit(request.getBookingUnit()));
        }
        if (request.getPrice() != null) {
            if (request.getPrice() < 0) throw new BadRequestException("Price must be zero or greater");
            rule.setPrice(request.getPrice());
        }
        if (request.getLabel() != null)            rule.setLabel(request.getLabel());
        if (request.getDayOfWeek() != null)        rule.setDayOfWeek(request.getDayOfWeek());
        if (request.getStartsAt() != null)         rule.setStartsAt(request.getStartsAt());
        if (request.getEndsAt() != null)           rule.setEndsAt(request.getEndsAt());
        if (request.getAdjustmentType() != null)   rule.setAdjustmentType(parseAdjustmentType(request.getAdjustmentType()));
        if (request.getAdjustmentValue() != null)  rule.setAdjustmentValue(request.getAdjustmentValue());
        if (request.getValidFrom() != null)        rule.setValidFrom(request.getValidFrom());
        if (request.getValidUntil() != null)       rule.setValidUntil(request.getValidUntil());
        if (request.getLastMinuteMinutes() != null) rule.setLastMinuteMinutes(request.getLastMinuteMinutes());
        if (request.getPriority() != null)         rule.setPriority(request.getPriority());
        if (request.getActive() != null)           rule.setActive(request.getActive());
        // Lot R1 : conditions nouvelles (chaine vide pour vider un champ texte).
        if (request.getDaysOfWeek() != null)        rule.setDaysOfWeek(normalizeDays(request.getDaysOfWeek()));
        if (request.getCustomerSegment() != null)   rule.setCustomerSegment(upper(request.getCustomerSegment()));
        if (request.getPlanCode() != null)          rule.setPlanCode(blankToNull(request.getPlanCode()));
        if (request.getMinParticipants() != null)   rule.setMinParticipants(request.getMinParticipants());
        if (request.getMaxParticipants() != null)   rule.setMaxParticipants(request.getMaxParticipants());
        if (request.getMinDurationMinutes() != null) rule.setMinDurationMinutes(request.getMinDurationMinutes());
        if (request.getMaxDurationMinutes() != null) rule.setMaxDurationMinutes(request.getMaxDurationMinutes());
        if (request.getMinLeadMinutes() != null)    rule.setMinLeadMinutes(request.getMinLeadMinutes());
        if (request.getChannel() != null)           rule.setChannel(upper(request.getChannel()));
        if (request.getStackable() != null)        rule.setStackable(request.getStackable());
        if (request.getPerParticipant() != null)   rule.setPerParticipant(request.getPerParticipant());
        governance.rejectDuplicate(rule);
        ResourcePricingRule saved = pricingRuleRepository.save(rule);
        governance.record(saved, "UPDATE");
        return toResponse(saved);
    }

    @Override
    public void updateActive(Long id, Boolean active) {
        if (active == null) {
            throw new BadRequestException("Pricing rule active flag is required");
        }
        ResourcePricingRule rule = getPricingRuleForService(id);
        rule.setActive(active);
        pricingRuleRepository.save(rule);
        governance.record(rule, active ? "ACTIVATE" : "DEACTIVATE");
    }

    @Override
    public void deletePricingRule(Long id) {
        // Lot R1 : suppression douce ; la regle et son historique restent pour expliquer les prix passes.
        ResourcePricingRule rule = getPricingRuleForService(id);
        rule.setDeleted(Boolean.TRUE);
        rule.setActive(Boolean.FALSE);
        pricingRuleRepository.save(rule);
        governance.record(rule, "DELETE");
    }

    private static final java.util.Set<String> SEGMENTS = java.util.Set.of("PUBLIC", "MEMBER", "SUBSCRIBER", "COMPANY", "PARTNER");
    private static final java.util.Set<String> CHANNELS = java.util.Set.of("PORTAL", "TEAM");

    private static String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static String upper(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase(java.util.Locale.ROOT) : null;
    }

    /** « 6, 7 » devient « 6,7 » ; chaque jour entre 1 (lundi) et 7 (dimanche). */
    private static String normalizeDays(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        java.util.TreeSet<Integer> days = new java.util.TreeSet<>();
        for (String part : value.split("[,; ]+")) {
            if (!StringUtils.hasText(part)) continue;
            int day;
            try {
                day = Integer.parseInt(part.trim());
            } catch (NumberFormatException ex) {
                throw new BadRequestException("Jour invalide : " + part + " (1 = lundi ... 7 = dimanche)");
            }
            if (day < 1 || day > 7) {
                throw new BadRequestException("Jour invalide : " + day + " (1 = lundi ... 7 = dimanche)");
            }
            days.add(day);
        }
        return days.isEmpty() ? null : days.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
    }

    private ResourcePricingRule getPricingRuleForService(Long id) {
        if (id == null) {
            throw new BadRequestException("Pricing rule id is required");
        }
        // Le numero court est ce que l'API expose · l'identifiant genere reste accepte pour les
        // clients qui l'avaient garde, et ne peut pas se confondre avec un numero court.
        return pricingRuleRepository.findByShortId(id)
                .or(() -> pricingRuleRepository.findById(id))
                .filter(rule -> !Boolean.TRUE.equals(rule.getDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Règle tarifaire introuvable : " + id));
    }

    private ResourcePricingRuleResponse toResponse(ResourcePricingRule rule) {
        return ResourcePricingRuleResponse.builder()
                .id(rule.getShortId())
                .resourceCode(rule.getResource().getCode())
                .bookingUnit(rule.getResourceBookingUnit() == null ? null : rule.getResourceBookingUnit().name())
                .price(rule.getPrice())
                .label(rule.getLabel())
                .dayOfWeek(rule.getDayOfWeek())
                .startsAt(rule.getStartsAt())
                .endsAt(rule.getEndsAt())
                .adjustmentType(rule.getAdjustmentType() == null ? null : rule.getAdjustmentType().name())
                .adjustmentValue(rule.getAdjustmentValue())
                .validFrom(rule.getValidFrom())
                .validUntil(rule.getValidUntil())
                .lastMinuteMinutes(rule.getLastMinuteMinutes())
                .priority(rule.getPriority())
                .active(rule.getActive())
                .build();
    }

    private void validateAgainstPolicy(Resource resource, LocalDateTime startedAt, LocalDateTime endedAt) {
        ResourcePolicy policy = resource.getEffectivePolicy();
        if (policy == null) {
            return;
        }
        long durationMinutes = java.time.Duration.between(startedAt, endedAt).toMinutes();
        if (policy.getMinBookingDurationMinutes() != null && durationMinutes < policy.getMinBookingDurationMinutes()) {
            throw new BadRequestException("Booking duration (" + durationMinutes + " min) is below the minimum allowed (" + policy.getMinBookingDurationMinutes() + " min)");
        }
        if (policy.getMaxBookingDurationMinutes() != null && durationMinutes > policy.getMaxBookingDurationMinutes()) {
            throw new BadRequestException("Booking duration (" + durationMinutes + " min) exceeds the maximum allowed (" + policy.getMaxBookingDurationMinutes() + " min)");
        }
        if (policy.getMinBookingNoticeMinutes() != null && policy.getMinBookingNoticeMinutes() > 0) {
            long noticeMinutes = java.time.Duration.between(LocalDateTime.now(), startedAt).toMinutes();
            if (noticeMinutes < policy.getMinBookingNoticeMinutes()) {
                throw new BadRequestException("Booking requires at least " + policy.getMinBookingNoticeMinutes() + " minutes advance notice");
            }
        }
    }

    private ResourceBookingUnit parseUnit(String value) {
        try {
            return ResourceBookingUnit.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid booking unit: " + value, ex);
        }
    }

    private ResourcePriceAdjustmentType parseAdjustmentType(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return ResourcePriceAdjustmentType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid adjustment type: " + value, ex);
        }
    }

    private boolean appliesLastMinute(ResourcePricingRule rule, LocalDateTime startedAt) {
        return rule.getLastMinuteMinutes() == null || !LocalDateTime.now().plusMinutes(rule.getLastMinuteMinutes()).isBefore(startedAt);
    }

    private Integer applyAdjustment(Integer basePrice, ResourcePricingRule applied) {
        if (applied.getAdjustmentType() == null) {
            return applied.getPrice();
        }
        Integer adjustment = applied.getAdjustmentValue() == null ? 0 : applied.getAdjustmentValue();
        return switch (applied.getAdjustmentType()) {
            case FIXED_PRICE -> adjustment;
            case AMOUNT_DELTA -> Math.max(0, basePrice + adjustment);
            case PERCENT_DELTA -> Math.max(0, basePrice + (basePrice * adjustment / 100));
        };
    }

    private long quantityFor(ResourceBookingUnit unit, LocalDateTime startedAt, LocalDateTime endedAt) {
        long minutes = ChronoUnit.MINUTES.between(startedAt, endedAt);
        return switch (unit) {
            case HOUR     -> Math.max(1, (long) Math.ceil(minutes / 60.0));
            case HALF_DAY -> Math.max(1, (long) Math.ceil(minutes / (60.0 * 12)));
            case DAY      -> Math.max(1, (long) Math.ceil(minutes / (60.0 * 24)));
            case WEEK     -> Math.max(1, (long) Math.ceil(minutes / (60.0 * 24 * 7)));
            case MONTH    -> Math.max(1, ChronoUnit.MONTHS.between(startedAt, endedAt)
                    + (startedAt.plusMonths(ChronoUnit.MONTHS.between(startedAt, endedAt)).isBefore(endedAt) ? 1 : 0));
        };
    }

    private List<String> validateRequest(CreateResourcePricingRuleRequest request) {
        List<String> errors = new ArrayList<>();
        if (request == null) {
            errors.add("Invalid resource pricing rule request, the request body is required");
            return errors;
        }
        if (!StringUtils.hasText(request.getResourceCode())) {
            errors.add("Invalid resource pricing rule request, the resource code is required");
        }
        if (!StringUtils.hasText(request.getBookingUnit())) {
            errors.add("Invalid resource pricing rule request, the booking unit is required");
        }
        if (request.getPrice() == null || request.getPrice() < 0) {
            errors.add("Invalid resource pricing rule request, the price must be zero or greater");
        }
        if (request.getDayOfWeek() != null && (request.getDayOfWeek() < 1 || request.getDayOfWeek() > 7)) {
            errors.add("Invalid resource pricing rule request, dayOfWeek must be between 1 and 7");
        }
        if (request.getStartsAt() != null && request.getEndsAt() != null && !request.getEndsAt().isAfter(request.getStartsAt())) {
            errors.add("Invalid resource pricing rule request, endsAt must be after startsAt");
        }
        if (request.getValidFrom() != null && request.getValidUntil() != null && request.getValidUntil().isBefore(request.getValidFrom())) {
            errors.add("Invalid resource pricing rule request, validUntil must be after validFrom");
        }
        if (StringUtils.hasText(request.getCustomerSegment()) && !SEGMENTS.contains(request.getCustomerSegment().trim().toUpperCase(java.util.Locale.ROOT))) {
            errors.add("customerSegment : PUBLIC, MEMBER, SUBSCRIBER, COMPANY ou PARTNER");
        }
        if (StringUtils.hasText(request.getChannel()) && !CHANNELS.contains(request.getChannel().trim().toUpperCase(java.util.Locale.ROOT))) {
            errors.add("channel : PORTAL ou TEAM");
        }
        if (request.getMinParticipants() != null && request.getMaxParticipants() != null && request.getMinParticipants() > request.getMaxParticipants()) {
            errors.add("minParticipants doit être inférieur ou égal à maxParticipants");
        }
        if (request.getMinDurationMinutes() != null && request.getMaxDurationMinutes() != null
                && request.getMinDurationMinutes() > request.getMaxDurationMinutes()) {
            errors.add("minDurationMinutes doit être inférieur ou égal à maxDurationMinutes");
        }
        return errors;
    }
}
