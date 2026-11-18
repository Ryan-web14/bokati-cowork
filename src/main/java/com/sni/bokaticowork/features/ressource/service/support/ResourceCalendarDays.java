package com.sni.bokaticowork.features.ressource.service.support;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.settings.platform.PlatformSettings;
import com.sni.bokaticowork.features.ressource.dto.request.ResourceCalendarDayRequest;
import com.sni.bokaticowork.features.ressource.dto.response.ResourceCalendarDayResponse;
import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.ressource.model.ResourceCalendarDay;
import com.sni.bokaticowork.features.ressource.model.ResourceCalendarDayKind;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceCalendarDayRepository;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceRepository;
import com.sni.bokaticowork.features.ressource.service.support.MonthlyAvailabilityGenerator.OpeningHours;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Lot R5a : jours feries (nationaux calcules et ajoutes par l'equipe), horaires exceptionnels et
 * jours fermes, pris en compte par l'ouverture des creneaux, le prix et le calendrier.
 */
@Component
public class ResourceCalendarDays {

    public static final String NATIONAL_KEY = "app.resources.holidays.national";
    public static final String HOLIDAY_HOURS_KEY = "app.resources.holidays.hours";

    private final ResourceCalendarDayRepository repository;
    private final ResourceRepository resourceRepository;
    private PlatformSettings settings;

    public ResourceCalendarDays(ResourceCalendarDayRepository repository, ResourceRepository resourceRepository) {
        this.repository = repository;
        this.resourceRepository = resourceRepository;
    }

    @Autowired(required = false)
    void setSettings(PlatformSettings settings) {
        this.settings = settings;
    }

    private boolean nationalEnabled() {
        return settings == null || settings.bool(NATIONAL_KEY, true);
    }

    /** Libelle du jour ferie (national ou ajoute par l'equipe pour toutes les ressources), ou {@code null}. */
    public String holidayLabel(LocalDate date) {
        Optional<ResourceCalendarDay> general = repository.findFirstByDayDateAndResourceIsNull(date);
        if (general.isPresent() && general.get().getKind() == ResourceCalendarDayKind.HOLIDAY) {
            return StringUtils.hasText(general.get().getLabel()) ? general.get().getLabel() : "Jour férié";
        }
        return nationalEnabled() ? CongoHolidays.label(date) : null;
    }

    public boolean isHoliday(LocalDate date) {
        return holidayLabel(date) != null;
    }

    /** Libelle a afficher dans le calendrier pour ce jour (ferie, horaires exceptionnels, fermeture), ou {@code null}. */
    public String dayLabel(Resource resource, LocalDate date) {
        Optional<ResourceCalendarDay> day = dayFor(resource, date);
        if (day.isPresent()) {
            ResourceCalendarDay found = day.get();
            if (StringUtils.hasText(found.getLabel())) {
                return found.getLabel();
            }
            return switch (found.getKind()) {
                case HOLIDAY -> "Jour férié";
                case SPECIAL_HOURS -> "Horaires exceptionnels : " + found.getOpeningHours();
                case CLOSED -> "Fermé";
            };
        }
        return nationalEnabled() ? CongoHolidays.label(date) : null;
    }

    private Optional<ResourceCalendarDay> dayFor(Resource resource, LocalDate date) {
        Optional<ResourceCalendarDay> day = resource == null || resource.getId() == null ? Optional.empty()
                : repository.findFirstByDayDateAndResource(date, resource);
        return day.isPresent() ? day : repository.findFirstByDayDateAndResourceIsNull(date);
    }

    /**
     * Horaires imposes a cette date pour la ressource, ou vide si la semaine type s'applique.
     * Ordre : jour propre a la ressource, jour general, jour ferie national (parametre
     * {@code app.resources.holidays.hours} : NORMAL = semaine type, CLOSED = ferme, ou des plages).
     */
    public Optional<List<OpeningHours>> hoursOn(Resource resource, LocalDate date) {
        Optional<ResourceCalendarDay> day = dayFor(resource, date);
        if (day.isPresent()) {
            ResourceCalendarDay found = day.get();
            if (found.getKind() == ResourceCalendarDayKind.CLOSED) {
                return Optional.of(List.of());
            }
            if (StringUtils.hasText(found.getOpeningHours())) {
                return Optional.of(parse(found.getOpeningHours()));
            }
            return found.getKind() == ResourceCalendarDayKind.HOLIDAY ? holidayHours() : Optional.of(List.of());
        }
        return nationalEnabled() && CongoHolidays.isHoliday(date) ? holidayHours() : Optional.empty();
    }

    private Optional<List<OpeningHours>> holidayHours() {
        String value = settings == null ? "NORMAL" : settings.text(HOLIDAY_HOURS_KEY, "NORMAL");
        if (value == null || value.isBlank() || "NORMAL".equalsIgnoreCase(value.trim())) {
            return Optional.empty();
        }
        if ("CLOSED".equalsIgnoreCase(value.trim())) {
            return Optional.of(List.of());
        }
        return Optional.of(parse(value));
    }

    static List<OpeningHours> parse(String value) {
        List<OpeningHours> ranges = new ArrayList<>();
        for (String part : value.split(",")) {
            if (part.isBlank()) {
                continue;
            }
            OpeningHours range = OpeningHours.parse(part.trim());
            if (range != null) {
                ranges.add(range);
            }
        }
        return ranges;
    }

    // ------------------------------------------------------------------ consultation et saisie

    @Transactional(readOnly = true)
    public List<ResourceCalendarDayResponse> list(LocalDate from, LocalDate to, String resourceCode) {
        LocalDate start = from == null ? LocalDate.now() : from;
        LocalDate end = to == null ? start.plusMonths(3) : to;
        if (end.isBefore(start)) {
            throw new BadRequestException("La date de fin doit suivre la date de début");
        }
        if (start.plusYears(2).isBefore(end)) {
            throw new BadRequestException("Plage trop longue : deux ans au plus");
        }
        Long resourceId = StringUtils.hasText(resourceCode) ? resource(resourceCode).getId() : null;
        Map<String, ResourceCalendarDayResponse> days = new TreeMap<>();
        if (nationalEnabled()) {
            for (int year = start.getYear(); year <= end.getYear(); year++) {
                CongoHolidays.of(year).forEach((date, label) -> {
                    if (!date.isBefore(start) && !date.isAfter(end)) {
                        days.put(date + "|", new ResourceCalendarDayResponse(null, date, null, "HOLIDAY", label, null, true));
                    }
                });
            }
        }
        for (ResourceCalendarDay day : repository.findBetween(start, end, resourceId)) {
            String owner = day.getResource() == null ? "" : day.getResource().getCode();
            days.put(day.getDayDate() + "|" + owner, toResponse(day));
        }
        return new ArrayList<>(days.values());
    }

    @Transactional
    public ResourceCalendarDayResponse create(ResourceCalendarDayRequest request, String actor) {
        if (request == null || request.getDate() == null || !StringUtils.hasText(request.getKind())) {
            throw new BadRequestException("La date et la nature du jour (HOLIDAY, SPECIAL_HOURS ou CLOSED) sont requises");
        }
        ResourceCalendarDayKind kind;
        try {
            kind = ResourceCalendarDayKind.valueOf(request.getKind().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Nature de jour invalide : HOLIDAY, SPECIAL_HOURS ou CLOSED");
        }
        String hours = StringUtils.hasText(request.getOpeningHours()) ? request.getOpeningHours().trim() : null;
        if (kind == ResourceCalendarDayKind.SPECIAL_HOURS && hours == null) {
            throw new BadRequestException("Des horaires exceptionnels demandent des plages (HH:mm-HH:mm, séparées par des virgules)");
        }
        if (hours != null) {
            try {
                parse(hours);
            } catch (RuntimeException ex) {
                throw new BadRequestException("Horaires invalides, attendu HH:mm-HH:mm séparées par des virgules : " + hours);
            }
        }
        Resource resource = StringUtils.hasText(request.getResourceCode()) ? resource(request.getResourceCode()) : null;
        Optional<ResourceCalendarDay> existing = resource == null
                ? repository.findFirstByDayDateAndResourceIsNull(request.getDate())
                : repository.findFirstByDayDateAndResource(request.getDate(), resource);
        if (existing.isPresent()) {
            throw new ConflictException("Ce jour est déjà défini (" + existing.get().getKind()
                    + ") : supprimez-le avant d'en saisir un autre");
        }
        ResourceCalendarDay day = repository.save(ResourceCalendarDay.builder()
                .dayDate(request.getDate())
                .resource(resource)
                .kind(kind)
                .label(StringUtils.hasText(request.getLabel()) ? request.getLabel().trim() : null)
                .openingHours(kind == ResourceCalendarDayKind.CLOSED ? null : hours)
                .createdBy(actor)
                .build());
        return toResponse(day);
    }

    @Transactional
    public void delete(Long id) {
        ResourceCalendarDay day = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Jour particulier introuvable : " + id));
        repository.delete(day);
    }

    private Resource resource(String code) {
        return resourceRepository.findByCode(code.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Ressource introuvable : " + code.trim()));
    }

    private static ResourceCalendarDayResponse toResponse(ResourceCalendarDay day) {
        return new ResourceCalendarDayResponse(day.getId(), day.getDayDate(),
                day.getResource() == null ? null : day.getResource().getCode(), day.getKind().name(),
                day.getLabel(), day.getOpeningHours(), false);
    }
}
