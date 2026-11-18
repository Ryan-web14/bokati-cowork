package com.sni.bokaticowork.features.ressource.service.support;

import com.sni.bokaticowork.core.settings.platform.PlatformSettings;
import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.ressource.model.ResourceOpeningHours;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceOpeningHoursRepository;
import com.sni.bokaticowork.features.ressource.service.support.MonthlyAvailabilityGenerator.OpeningHours;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Les horaires qui s'appliquent · ceux de la ressource si elle en a, sinon les horaires globaux.
 *
 * <p>Les horaires globaux viennent des parametres de la plateforme, et a defaut de la configuration
 * : du lundi au vendredi 08:00-18:00 pour tous puis 18:00-22:00 pour les abonnes, le samedi
 * 08:00-12:00, le dimanche ferme. La soiree est une plage distincte : une fermeture declaree le soir
 * seulement ne retire pas la journee.</p>
 */
@Component
public class OpeningHoursResolver {

    static final String WEEKDAY = "bokati.resource.availability.generation.weekday-hours";
    static final String WEEKDAY_SUBSCRIBERS = "bokati.resource.availability.generation.weekday-subscriber-hours";
    static final String SATURDAY = "bokati.resource.availability.generation.saturday-hours";
    static final String SUNDAY = "bokati.resource.availability.generation.sunday-hours";

    private final PlatformSettings settings;
    private final ResourceOpeningHoursRepository resourceHours;
    private final String weekdayHours;
    private final String weekdaySubscriberHours;
    private final String saturdayHours;
    private final String sundayHours;

    public OpeningHoursResolver(PlatformSettings settings, ResourceOpeningHoursRepository resourceHours,
                                @Value("${" + WEEKDAY + ":08:00-18:00}") String weekdayHours,
                                @Value("${" + WEEKDAY_SUBSCRIBERS + ":18:00-22:00}") String weekdaySubscriberHours,
                                @Value("${" + SATURDAY + ":08:00-12:00}") String saturdayHours,
                                @Value("${" + SUNDAY + ":}") String sundayHours) {
        this.settings = settings;
        this.resourceHours = resourceHours;
        this.weekdayHours = weekdayHours;
        this.weekdaySubscriberHours = weekdaySubscriberHours;
        this.saturdayHours = saturdayHours;
        this.sundayHours = sundayHours;
        // Une plage mal ecrite ne bloque plus le demarrage : elle est signalee et remplacee par
        // le defaut (horaires par defaut de la plateforme).
        global();
    }

    public Map<DayOfWeek, List<OpeningHours>> forResource(Resource resource) {
        List<ResourceOpeningHours> own = own(resource);
        if (own.isEmpty()) {
            return global();
        }
        Map<DayOfWeek, List<OpeningHours>> hours = new EnumMap<>(DayOfWeek.class);
        for (ResourceOpeningHours range : own) {
            hours.computeIfAbsent(range.getDayOfWeek(), ignored -> new ArrayList<>())
                    .add(new OpeningHours(range.getOpenTime(), range.getCloseTime()));
        }
        return hours;
    }

    private ResourceCalendarDays calendarDays;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void setCalendarDays(ResourceCalendarDays calendarDays) {
        this.calendarDays = calendarDays;
    }

    /**
     * Lot R5a : horaires d'une date precise · jour particulier (ressource, puis general), jour ferie
     * (parametre), sinon la semaine type.
     */
    public List<OpeningHours> forDate(Resource resource, java.time.LocalDate date) {
        if (calendarDays != null) {
            java.util.Optional<List<OpeningHours>> special = calendarDays.hoursOn(resource, date);
            if (special.isPresent()) {
                return special.get();
            }
        }
        return forResource(resource).getOrDefault(date.getDayOfWeek(), List.of());
    }

    public List<ResourceOpeningHours> own(Resource resource) {
        return resourceHours == null ? List.of() : resourceHours.findAllByResourceOrderByDayOfWeekAscOpenTimeAsc(resource);
    }

    public Map<DayOfWeek, List<OpeningHours>> global() {
        Map<DayOfWeek, List<OpeningHours>> hours = new EnumMap<>(DayOfWeek.class);
        OpeningHours weekday = tolerant(WEEKDAY, weekdayHours, "08:00-18:00");
        OpeningHours evening = tolerant(WEEKDAY_SUBSCRIBERS, weekdaySubscriberHours, "18:00-22:00");
        for (DayOfWeek day : new DayOfWeek[]{DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY}) {
            putIfOpen(hours, day, weekday);
            putIfOpen(hours, day, evening);
        }
        putIfOpen(hours, DayOfWeek.SATURDAY, tolerant(SATURDAY, saturdayHours, "08:00-12:00"));
        putIfOpen(hours, DayOfWeek.SUNDAY, tolerant(SUNDAY, sundayHours, ""));
        return hours;
    }

    /**
     * Plage d'un parametre ; mal ecrite, elle est signalee et remplacee par la configuration, puis
     * par le defaut de la plateforme. Jamais d'exception : un reglage ne bloque ni le demarrage ni
     * l'ouverture des creneaux.
     */
    private OpeningHours tolerant(String key, String configured, String platformDefault) {
        String value = value(key, configured);
        try {
            return OpeningHours.parse(value);
        } catch (RuntimeException ex) {
            LOG.warn("Horaires {} mal ecrits ({}) : valeur ignoree, defaut applique", key, value);
        }
        try {
            return OpeningHours.parse(configured);
        } catch (RuntimeException ex) {
            return platformDefault.isEmpty() ? null : OpeningHours.parse(platformDefault);
        }
    }

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(OpeningHoursResolver.class);

    private String value(String key, String fallback) {
        return settings == null ? fallback : settings.text(key, fallback);
    }

    private static void putIfOpen(Map<DayOfWeek, List<OpeningHours>> hours, DayOfWeek day, OpeningHours range) {
        if (range != null) {
            hours.computeIfAbsent(day, ignored -> new ArrayList<>()).add(range);
        }
    }
}
