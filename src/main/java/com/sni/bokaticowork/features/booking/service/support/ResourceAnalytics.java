package com.sni.bokaticowork.features.booking.service.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.settings.platform.PlatformSettings;
import com.sni.bokaticowork.features.booking.enums.BookingLineType;
import com.sni.bokaticowork.features.booking.enums.BookingStatus;
import com.sni.bokaticowork.features.booking.model.Booking;
import com.sni.bokaticowork.features.booking.repository.BookingRepository;
import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.ressource.model.ResourceCost;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceCostRepository;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceRepository;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Lot R5e : pilotage des ressources · couts, marges, historique des prix appliques, taux
 * d'occupation et chiffre d'affaires, heures pleines et heures creuses.
 *
 * <p>Occupation : minutes reservees sur minutes ouvertes (creneaux actifs). Une piece louee entiere
 * compte une place ; un espace partage compte ses postes (quantite reservee sur capacite du
 * creneau). Les non-presentations occupent le creneau, elles comptent.</p>
 *
 * <p>Chiffre d'affaires : montant de la reservation (remises deduites) et supplements (services,
 * options, participants, depassement, non-presentation), sans la caution.</p>
 */
@Slf4j
@Component
public class ResourceAnalytics {

    public static final String PEAK_HOURS_KEY = "app.resources.peak-hours";
    static final Set<BookingStatus> COUNTED = Set.of(BookingStatus.CONFIRMED, BookingStatus.IN_PROGRESS,
            BookingStatus.COMPLETED, BookingStatus.NO_SHOW);
    private static final int STEP = 15;

    // ------------------------------------------------------------------ contrats

    public record CostRequest(String kind, String basis, BigDecimal amount, String label, LocalDate validFrom, LocalDate validUntil) {
    }

    public record CostResponse(Long id, String resourceCode, String kind, String basis, BigDecimal amount, String label,
                               LocalDate validFrom, LocalDate validUntil) {
    }

    /** Occupation et chiffre d'affaires d'un groupe (ressource, type, groupe ou site). */
    public record OccupancyRow(String key, String label, long openMinutes, long bookedMinutes, double occupancyRate,
                               long peakOpenMinutes, long peakBookedMinutes, double peakOccupancyRate,
                               long offPeakOpenMinutes, long offPeakBookedMinutes, double offPeakOccupancyRate,
                               int bookings, BigDecimal revenue) {
    }

    public record OccupancyReport(LocalDate from, LocalDate to, String groupBy, String peakHours, List<OccupancyRow> rows) {
    }

    public record MarginRow(String resourceCode, String resourceName, int bookings, long bookedMinutes, BigDecimal revenue,
                            BigDecimal costs, BigDecimal margin, Double marginRate, Map<String, BigDecimal> costsByKind) {
    }

    public record MarginReport(LocalDate from, LocalDate to, List<MarginRow> rows, BigDecimal revenue, BigDecimal costs,
                               BigDecimal margin) {
    }

    public record BookingMargin(String bookingNumber, String resourceCode, BigDecimal revenue, BigDecimal costs, BigDecimal margin,
                                Map<String, BigDecimal> costsByKind) {
    }

    /** Prix applique a une reservation, avec les regles de la trace. */
    public record AppliedPrice(String bookingNumber, LocalDateTime startedAt, LocalDateTime endedAt, String status, String unit,
                               BigDecimal unitPrice, BigDecimal subtotal, BigDecimal total, String currency, List<String> rules) {
    }

    private final BookingRepository bookings;
    private final ResourceRepository resources;
    private final ResourceCostRepository costs;
    private final EntityManager entityManager;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private PlatformSettings settings;

    public ResourceAnalytics(BookingRepository bookings, ResourceRepository resources, ResourceCostRepository costs,
                             EntityManager entityManager) {
        this.bookings = bookings;
        this.resources = resources;
        this.costs = costs;
        this.entityManager = entityManager;
    }

    @Autowired(required = false)
    void setSettings(PlatformSettings settings) {
        this.settings = settings;
    }

    // ------------------------------------------------------------------ couts

    @Transactional(readOnly = true)
    public List<CostResponse> costs(String resourceCode) {
        Resource resource = resource(resourceCode);
        return costs.findAllByResourceOrderByKindAscIdAsc(resource).stream().map(ResourceAnalytics::toResponse).toList();
    }

    @Transactional
    public CostResponse addCost(String resourceCode, CostRequest request) {
        if (request == null || request.amount() == null || request.amount().signum() < 0) {
            throw new BadRequestException("Le montant du coût est requis, positif ou nul");
        }
        ResourceCost cost = ResourceCost.builder()
                .resource(resource(resourceCode))
                .kind(parse(ResourceCost.Kind.class, request.kind(), "Nature du coût : RENT, CLEANING, ENERGY, DEPRECIATION, SERVICES ou OTHER"))
                .basis(parse(ResourceCost.Basis.class, request.basis(), "Base du coût : MONTHLY, PER_BOOKING ou PER_HOUR"))
                .amount(request.amount())
                .label(StringUtils.hasText(request.label()) ? request.label().trim() : null)
                .validFrom(request.validFrom())
                .validUntil(request.validUntil())
                .build();
        if (cost.getValidFrom() != null && cost.getValidUntil() != null && cost.getValidUntil().isBefore(cost.getValidFrom())) {
            throw new BadRequestException("La fin de validité doit suivre le début");
        }
        return toResponse(costs.save(cost));
    }

    @Transactional
    public void deleteCost(Long id) {
        costs.delete(costs.findById(id).orElseThrow(() -> new ResourceNotFoundException("Coût introuvable : " + id)));
    }

    private static <E extends Enum<E>> E parse(Class<E> type, String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BadRequestException(message);
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(message);
        }
    }

    private static CostResponse toResponse(ResourceCost cost) {
        return new CostResponse(cost.getId(), cost.getResource().getCode(), cost.getKind().name(), cost.getBasis().name(),
                cost.getAmount(), cost.getLabel(), cost.getValidFrom(), cost.getValidUntil());
    }

    // ------------------------------------------------------------------ occupation et chiffre d'affaires

    @Transactional(readOnly = true)
    public OccupancyReport occupancy(LocalDate from, LocalDate to, String groupBy, String siteCode) {
        LocalDate start = from == null ? LocalDate.now().withDayOfMonth(1) : from;
        LocalDate end = to == null ? LocalDate.now() : to;
        checkRange(start, end);
        String group = StringUtils.hasText(groupBy) ? groupBy.trim().toUpperCase(Locale.ROOT) : "RESOURCE";
        if (!Set.of("RESOURCE", "TYPE", "GROUP", "SITE").contains(group)) {
            throw new BadRequestException("Regroupement : RESOURCE, TYPE, GROUP ou SITE");
        }
        LocalDateTime rangeStart = start.atStartOfDay();
        LocalDateTime rangeEnd = end.plusDays(1).atStartOfDay();
        List<LocalTime[]> peak = peakHours();

        Map<String, Acc> acc = new LinkedHashMap<>();
        Map<Long, Resource> byId = new HashMap<>();
        for (Resource resource : resources.findAll()) {
            if (Boolean.TRUE.equals(resource.getDeleted())) continue;
            if (StringUtils.hasText(siteCode) && (resource.getSite() == null || !siteCode.trim().equalsIgnoreCase(resource.getSite().getCode()))) {
                continue;
            }
            byId.put(resource.getId(), resource);
        }

        List<Object[]> slots = entityManager.createQuery("""
                        select a.resource.id, a.startedAt, a.endedAt, a.totalCapacity from ResourceAvailability a
                        where (a.active is null or a.active = true) and a.startedAt < :to and a.endedAt > :from
                        """, Object[].class)
                .setParameter("from", rangeStart).setParameter("to", rangeEnd).getResultList();
        for (Object[] row : slots) {
            Resource resource = byId.get((Long) row[0]);
            if (resource == null) continue;
            int weight = resource.isWholeResourceBooking() ? 1 : Math.max(1, row[3] == null ? 1 : (Integer) row[3]);
            long[] split = split(clip((LocalDateTime) row[1], rangeStart, rangeEnd), clip((LocalDateTime) row[2], rangeStart, rangeEnd), peak);
            Acc a = acc.computeIfAbsent(key(resource, group), k -> new Acc(label(resource, group)));
            a.openPeak += split[0] * weight;
            a.openOff += split[1] * weight;
        }
        for (Booking booking : counted(rangeStart, rangeEnd)) {
            Resource resource = byId.get(booking.getResource().getId());
            if (resource == null) continue;
            int weight = resource.isWholeResourceBooking() ? 1 : Math.max(1, booking.getQuantity() == null ? 1 : booking.getQuantity());
            long[] split = split(clip(booking.getStartedAt(), rangeStart, rangeEnd), clip(booking.getEndedAt(), rangeStart, rangeEnd), peak);
            Acc a = acc.computeIfAbsent(key(resource, group), k -> new Acc(label(resource, group)));
            a.bookedPeak += split[0] * weight;
            a.bookedOff += split[1] * weight;
            if (!booking.getStartedAt().isBefore(rangeStart)) {
                a.bookings++;
                a.revenue = a.revenue.add(revenue(booking));
            }
        }
        List<OccupancyRow> rows = new ArrayList<>();
        acc.forEach((key, a) -> rows.add(new OccupancyRow(key, a.label, a.openPeak + a.openOff, a.bookedPeak + a.bookedOff,
                rate(a.bookedPeak + a.bookedOff, a.openPeak + a.openOff), a.openPeak, a.bookedPeak, rate(a.bookedPeak, a.openPeak),
                a.openOff, a.bookedOff, rate(a.bookedOff, a.openOff), a.bookings, a.revenue)));
        rows.sort(Comparator.comparing(OccupancyRow::occupancyRate).reversed());
        return new OccupancyReport(start, end, group, peakHoursText(), rows);
    }

    private static final class Acc {
        final String label;
        long openPeak;
        long openOff;
        long bookedPeak;
        long bookedOff;
        int bookings;
        BigDecimal revenue = BigDecimal.ZERO;

        Acc(String label) {
            this.label = label;
        }
    }

    private static String key(Resource resource, String group) {
        return switch (group) {
            case "TYPE" -> resource.getResourceType() == null ? "-" : resource.getResourceType().getCode();
            case "GROUP" -> resource.getResourceGroup() == null ? "-" : resource.getResourceGroup().getCode();
            case "SITE" -> resource.getSite() == null ? "-" : resource.getSite().getCode();
            default -> resource.getCode();
        };
    }

    private static String label(Resource resource, String group) {
        return switch (group) {
            case "TYPE" -> resource.getResourceType() == null ? "Sans type" : resource.getResourceType().getName();
            case "GROUP" -> resource.getResourceGroup() == null ? "Sans groupe" : resource.getResourceGroup().getName();
            case "SITE" -> resource.getSite() == null ? "Sans site" : resource.getSite().getName();
            default -> resource.getName();
        };
    }

    // ------------------------------------------------------------------ marges

    @Transactional(readOnly = true)
    public MarginReport margins(LocalDate from, LocalDate to, String resourceCode) {
        LocalDate start = from == null ? LocalDate.now().withDayOfMonth(1) : from;
        LocalDate end = to == null ? LocalDate.now() : to;
        checkRange(start, end);
        LocalDateTime rangeStart = start.atStartOfDay();
        LocalDateTime rangeEnd = end.plusDays(1).atStartOfDay();
        List<Resource> scope = StringUtils.hasText(resourceCode) ? List.of(resource(resourceCode))
                : resources.findAll().stream().filter(r -> !Boolean.TRUE.equals(r.getDeleted())).toList();
        Map<Long, List<Booking>> byResource = new HashMap<>();
        for (Booking booking : counted(rangeStart, rangeEnd)) {
            if (!booking.getStartedAt().isBefore(rangeStart)) {
                byResource.computeIfAbsent(booking.getResource().getId(), k -> new ArrayList<>()).add(booking);
            }
        }
        List<MarginRow> rows = new ArrayList<>();
        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalCosts = BigDecimal.ZERO;
        for (Resource resource : scope) {
            List<Booking> list = byResource.getOrDefault(resource.getId(), List.of());
            List<ResourceCost> resourceCosts = costs.findAllByResourceOrderByKindAscIdAsc(resource);
            if (list.isEmpty() && resourceCosts.isEmpty()) continue;
            BigDecimal revenue = list.stream().map(this::revenue).reduce(BigDecimal.ZERO, BigDecimal::add);
            long minutes = list.stream().mapToLong(b -> Duration.between(b.getStartedAt(), b.getEndedAt()).toMinutes()).sum();
            Map<String, BigDecimal> byKind = new LinkedHashMap<>();
            for (ResourceCost cost : resourceCosts) {
                BigDecimal amount = switch (cost.getBasis()) {
                    case MONTHLY -> monthlyShare(cost, start, end);
                    case PER_BOOKING -> cost.getAmount().multiply(BigDecimal.valueOf(
                            list.stream().filter(b -> cost.validOn(b.getStartedAt().toLocalDate())).count()));
                    case PER_HOUR -> cost.getAmount().multiply(BigDecimal.valueOf(list.stream()
                            .filter(b -> cost.validOn(b.getStartedAt().toLocalDate()))
                            .mapToLong(b -> Duration.between(b.getStartedAt(), b.getEndedAt()).toMinutes()).sum()))
                            .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
                };
                byKind.merge(cost.getKind().name(), amount, BigDecimal::add);
            }
            BigDecimal costTotal = byKind.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add).setScale(0, RoundingMode.HALF_UP);
            BigDecimal margin = revenue.subtract(costTotal);
            rows.add(new MarginRow(resource.getCode(), resource.getName(), list.size(), minutes, revenue, costTotal, margin,
                    revenue.signum() == 0 ? null : margin.multiply(BigDecimal.valueOf(100)).divide(revenue, 1, RoundingMode.HALF_UP).doubleValue(),
                    byKind));
            totalRevenue = totalRevenue.add(revenue);
            totalCosts = totalCosts.add(costTotal);
        }
        rows.sort(Comparator.comparing(MarginRow::margin).reversed());
        return new MarginReport(start, end, rows, totalRevenue, totalCosts, totalRevenue.subtract(totalCosts));
    }

    /** Part d'un cout mensuel sur la periode, jour par jour (mois de longueur reelle). */
    static BigDecimal monthlyShare(ResourceCost cost, LocalDate start, LocalDate end) {
        BigDecimal share = BigDecimal.ZERO;
        for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
            if (cost.validOn(day)) {
                share = share.add(cost.getAmount().divide(BigDecimal.valueOf(YearMonth.from(day).lengthOfMonth()), 6, RoundingMode.HALF_UP));
            }
        }
        return share.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Marge d'une reservation : son chiffre d'affaires moins ses couts (par reservation, par heure, et
     * la part des couts mensuels au prorata de ses minutes sur les minutes ouvertes du mois).
     */
    @Transactional(readOnly = true)
    public BookingMargin bookingMargin(String bookingNumber) {
        Booking booking = bookings.findByBookingNumber(bookingNumber.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Réservation introuvable : " + bookingNumber.trim()));
        Resource resource = booking.getResource();
        LocalDate day = booking.getStartedAt().toLocalDate();
        long minutes = Duration.between(booking.getStartedAt(), booking.getEndedAt()).toMinutes();
        Map<String, BigDecimal> byKind = new LinkedHashMap<>();
        for (ResourceCost cost : costs.findAllByResourceOrderByKindAscIdAsc(resource)) {
            if (!cost.validOn(day)) continue;
            BigDecimal amount = switch (cost.getBasis()) {
                case PER_BOOKING -> cost.getAmount();
                case PER_HOUR -> cost.getAmount().multiply(BigDecimal.valueOf(minutes)).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
                case MONTHLY -> {
                    long open = openMinutes(resource, YearMonth.from(day));
                    yield open <= 0 ? BigDecimal.ZERO : cost.getAmount().multiply(BigDecimal.valueOf(minutes))
                            .divide(BigDecimal.valueOf(open), 2, RoundingMode.HALF_UP);
                }
            };
            byKind.merge(cost.getKind().name(), amount, BigDecimal::add);
        }
        BigDecimal revenue = revenue(booking);
        BigDecimal costTotal = byKind.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add).setScale(0, RoundingMode.HALF_UP);
        return new BookingMargin(booking.getBookingNumber(), resource.getCode(), revenue, costTotal, revenue.subtract(costTotal), byKind);
    }

    private long openMinutes(Resource resource, YearMonth month) {
        List<Object[]> rows = entityManager.createQuery("""
                        select a.startedAt, a.endedAt from ResourceAvailability a
                        where a.resource.id = :id and (a.active is null or a.active = true) and a.startedAt < :to and a.endedAt > :from
                        """, Object[].class)
                .setParameter("id", resource.getId())
                .setParameter("from", month.atDay(1).atStartOfDay())
                .setParameter("to", month.plusMonths(1).atDay(1).atStartOfDay())
                .getResultList();
        return rows.stream().mapToLong(row -> Duration.between((LocalDateTime) row[0], (LocalDateTime) row[1]).toMinutes()).sum();
    }

    // ------------------------------------------------------------------ historique des prix

    @Transactional(readOnly = true)
    public List<AppliedPrice> priceHistory(String resourceCode, LocalDate from, LocalDate to) {
        Resource resource = resource(resourceCode);
        LocalDate start = from == null ? LocalDate.now().minusMonths(3) : from;
        LocalDate end = to == null ? LocalDate.now().plusMonths(3) : to;
        checkRange(start, end);
        List<Booking> found = entityManager.createQuery("""
                        select b from Booking b where b.resource = :resource and b.startedAt >= :from and b.startedAt < :to
                        order by b.startedAt desc
                        """, Booking.class)
                .setParameter("resource", resource)
                .setParameter("from", start.atStartOfDay())
                .setParameter("to", end.plusDays(1).atStartOfDay())
                .setMaxResults(1000)
                .getResultList();
        return found.stream().map(booking -> new AppliedPrice(booking.getBookingNumber(), booking.getStartedAt(), booking.getEndedAt(),
                booking.getStatus() == null ? null : booking.getStatus().name(),
                booking.getBookingUnit() == null ? null : booking.getBookingUnit().name(), booking.getUnitPrice(),
                booking.getSubtotalAmount(), booking.getTotalAmount(), booking.getCurrency(), rules(booking.getPriceTrace()))).toList();
    }

    /** Regles et ajustements nommes dans la trace du prix (lot R1). */
    List<String> rules(String trace) {
        if (!StringUtils.hasText(trace)) {
            return List.of();
        }
        try {
            List<String> names = new ArrayList<>();
            JsonNode root = objectMapper.readTree(trace);
            collect(root, names);
            return names.stream().distinct().toList();
        } catch (Exception ex) {
            return List.of();
        }
    }

    private static void collect(JsonNode node, List<String> names) {
        if (node == null) return;
        if (node.isObject()) {
            JsonNode label = node.get("label");
            if (label == null) label = node.get("ruleLabel");
            if (label != null && label.isTextual() && !label.asText().isBlank()) {
                names.add(label.asText());
            }
            node.elements().forEachRemaining(child -> collect(child, names));
        } else if (node.isArray()) {
            node.forEach(child -> collect(child, names));
        }
    }

    // ------------------------------------------------------------------ outils

    private List<Booking> counted(LocalDateTime from, LocalDateTime to) {
        return entityManager.createQuery("""
                        select b from Booking b join fetch b.resource
                        where b.status in :statuses and b.startedAt < :to and b.endedAt > :from
                        """, Booking.class)
                .setParameter("statuses", COUNTED)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();
    }

    /** Montant de la reservation (remises deduites) et supplements, sans la caution. */
    BigDecimal revenue(Booking booking) {
        BigDecimal total = booking.getTotalAmount() == null ? BigDecimal.ZERO : booking.getTotalAmount();
        BigDecimal extras = entityManager.createQuery("""
                        select coalesce(sum(l.amount), 0) from BookingLine l
                        where l.booking = :booking and l.lineType in :types
                        """, BigDecimal.class)
                .setParameter("booking", booking)
                .setParameter("types", List.of(BookingLineType.SERVICE, BookingLineType.AMENITY, BookingLineType.PARTICIPANT,
                        BookingLineType.OVERRUN, BookingLineType.NO_SHOW))
                .getSingleResult();
        return total.add(extras == null ? BigDecimal.ZERO : extras);
    }

    /** Minutes [heures pleines, heures creuses] d'une plage, par pas de 15 minutes. */
    static long[] split(LocalDateTime start, LocalDateTime end, List<LocalTime[]> peak) {
        long peakMinutes = 0;
        long offMinutes = 0;
        for (LocalDateTime t = start; t.isBefore(end); t = t.plusMinutes(STEP)) {
            LocalDateTime next = t.plusMinutes(STEP).isAfter(end) ? end : t.plusMinutes(STEP);
            long minutes = Duration.between(t, next).toMinutes();
            if (isPeak(t, peak)) peakMinutes += minutes;
            else offMinutes += minutes;
        }
        return new long[]{peakMinutes, offMinutes};
    }

    static boolean isPeak(LocalDateTime at, List<LocalTime[]> peak) {
        if (at.getDayOfWeek() == DayOfWeek.SATURDAY || at.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return false;
        }
        LocalTime time = at.toLocalTime();
        return peak.stream().anyMatch(range -> !time.isBefore(range[0]) && time.isBefore(range[1]));
    }

    private String peakHoursText() {
        return settings == null ? "09:00-12:00,14:00-17:00" : settings.text(PEAK_HOURS_KEY, "09:00-12:00,14:00-17:00");
    }

    List<LocalTime[]> peakHours() {
        List<LocalTime[]> ranges = new ArrayList<>();
        for (String part : peakHoursText().split(",")) {
            String[] bounds = part.trim().split("-");
            if (bounds.length == 2) {
                try {
                    ranges.add(new LocalTime[]{LocalTime.parse(bounds[0].trim()), LocalTime.parse(bounds[1].trim())});
                } catch (RuntimeException ex) {
                    log.warn("Heures pleines mal ecrites ignorees : {}", part);
                }
            }
        }
        return ranges;
    }

    private static LocalDateTime clip(LocalDateTime value, LocalDateTime from, LocalDateTime to) {
        return value.isBefore(from) ? from : value.isAfter(to) ? to : value;
    }

    private static double rate(long booked, long open) {
        return open <= 0 ? 0 : Math.round(booked * 1000.0 / open) / 10.0;
    }

    private static void checkRange(LocalDate start, LocalDate end) {
        if (end.isBefore(start)) {
            throw new BadRequestException("La date de fin doit suivre la date de début");
        }
        if (start.plusDays(400).isBefore(end)) {
            throw new BadRequestException("Plage trop longue : 400 jours au plus");
        }
    }

    private Resource resource(String code) {
        if (!StringUtils.hasText(code)) {
            throw new BadRequestException("Le code de la ressource est requis");
        }
        return resources.findByCode(code.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Ressource introuvable : " + code.trim()));
    }
}
