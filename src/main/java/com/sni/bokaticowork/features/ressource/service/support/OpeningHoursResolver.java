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
        // Une plage mal ecrite dans la configuration arrete le demarrage, plutot que d'ouvrir n'importe quoi.
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

    public List<ResourceOpeningHours> own(Resource resource) {
        return resourceHours == null ? List.of() : resourceHours.findAllByResourceOrderByDayOfWeekAscOpenTimeAsc(resource);
    }

    public Map<DayOfWeek, List<OpeningHours>> global() {
        Map<DayOfWeek, List<OpeningHours>> hours = new EnumMap<>(DayOfWeek.class);
        OpeningHours weekday = OpeningHours.parse(value(WEEKDAY, weekdayHours));
        OpeningHours evening = OpeningHours.parse(value(WEEKDAY_SUBSCRIBERS, weekdaySubscriberHours));
        for (DayOfWeek day : new DayOfWeek[]{DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY}) {
            putIfOpen(hours, day, weekday);
            putIfOpen(hours, day, evening);
        }
        putIfOpen(hours, DayOfWeek.SATURDAY, OpeningHours.parse(value(SATURDAY, saturdayHours)));
        putIfOpen(hours, DayOfWeek.SUNDAY, OpeningHours.parse(value(SUNDAY, sundayHours)));
        return hours;
    }

    private String value(String key, String fallback) {
        return settings == null ? fallback : settings.text(key, fallback);
    }

    private static void putIfOpen(Map<DayOfWeek, List<OpeningHours>> hours, DayOfWeek day, OpeningHours range) {
        if (range != null) {
            hours.computeIfAbsent(day, ignored -> new ArrayList<>()).add(range);
        }
    }
}
