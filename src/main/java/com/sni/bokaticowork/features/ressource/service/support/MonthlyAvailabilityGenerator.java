package com.sni.bokaticowork.features.ressource.service.support;

import com.sni.bokaticowork.core.exception.customs.BaseException;
import com.sni.bokaticowork.features.ressource.dto.request.CreateResourceAvailabilityRequest;
import com.sni.bokaticowork.features.ressource.enums.ResourceStatus;
import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceAvailabilityRepository;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceRepository;
import com.sni.bokaticowork.features.ressource.service.interfaces.ResourceAvailabilityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Ouvre les disponibilites du mois en cours pour toutes les ressources reservables.
 *
 * <p>Les horaires viennent de la configuration · par defaut du lundi au vendredi de 08:00 a 18:00
 * pour tous, puis de 18:00 a 22:00 pour les abonnes, le samedi de 08:00 a 12:00, le dimanche ferme.
 * La plage va de maintenant a la fin du mois. La soiree est une plage distincte : une fermeture
 * declaree le soir seulement ne retire pas la journee.</p>
 *
 * <p>Le samedi apres-midi n'est pas ouvert · une derogation passe par l'accueil, qui ouvre et
 * reserve le creneau pour le client.</p>
 *
 * <p>Une ressource qui a deja au moins un creneau a venir dans le mois est sautee, telle quelle ·
 * l'administrateur qui a ouvert ses propres horaires ne les voit pas completes ni ecrasees.</p>
 *
 * <p>Chaque journee passe par {@link ResourceAvailabilityService#createAvailability}, une
 * transaction par jour · toutes les regles de la creation s'appliquent a l'identique (capacite,
 * alignement, fermetures, chevauchements), et une journee refusee (fermeture declaree, par exemple)
 * n'empeche pas les suivantes.</p>
 */
@Slf4j
@Component
public class MonthlyAvailabilityGenerator {

    private final ResourceRepository resourceRepository;
    private final ResourceAvailabilityRepository availabilityRepository;
    private final ResourceAvailabilityService availabilityService;
    private final ResourceSlotPolicy slotPolicy;
    private final Map<DayOfWeek, List<OpeningHours>> openingHours;

    public MonthlyAvailabilityGenerator(
            ResourceRepository resourceRepository,
            ResourceAvailabilityRepository availabilityRepository,
            ResourceAvailabilityService availabilityService,
            ResourceSlotPolicy slotPolicy,
            @Value("${bokati.resource.availability.generation.weekday-hours:08:00-18:00}") String weekdayHours,
            @Value("${bokati.resource.availability.generation.weekday-subscriber-hours:18:00-22:00}") String weekdaySubscriberHours,
            @Value("${bokati.resource.availability.generation.saturday-hours:08:00-12:00}") String saturdayHours,
            @Value("${bokati.resource.availability.generation.sunday-hours:}") String sundayHours) {
        this.resourceRepository = resourceRepository;
        this.availabilityRepository = availabilityRepository;
        this.availabilityService = availabilityService;
        this.slotPolicy = slotPolicy;
        this.openingHours = new EnumMap<>(DayOfWeek.class);
        OpeningHours weekday = OpeningHours.parse(weekdayHours);
        OpeningHours weekdayEvening = OpeningHours.parse(weekdaySubscriberHours);
        for (DayOfWeek day : new DayOfWeek[]{DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY}) {
            putIfOpen(day, weekday);
            putIfOpen(day, weekdayEvening);
        }
        putIfOpen(DayOfWeek.SATURDAY, OpeningHours.parse(saturdayHours));
        putIfOpen(DayOfWeek.SUNDAY, OpeningHours.parse(sundayHours));
    }

    private void putIfOpen(DayOfWeek day, OpeningHours hours) {
        if (hours != null) {
            openingHours.computeIfAbsent(day, ignored -> new ArrayList<>()).add(hours);
        }
    }

    /** Ce que le passage a fait · pour le journal du worker. */
    public record Report(int resourcesFilled, int resourcesSkipped, int rangesCreated, int rangesRefused) {
    }

    public Report generate(LocalDateTime now) {
        LocalDate lastDay = now.toLocalDate().with(TemporalAdjusters.lastDayOfMonth());
        LocalDateTime monthEnd = lastDay.plusDays(1).atStartOfDay();

        int filled = 0;
        int skipped = 0;
        int created = 0;
        int refused = 0;
        for (Resource resource : resourceRepository.findAll()) {
            if (!isBookable(resource)) {
                continue;
            }
            if (availabilityRepository.countByResourceAndStartedAtGreaterThanEqualAndEndedAtLessThanEqual(
                    resource, now, monthEnd) > 0) {
                skipped++;
                continue;
            }
            int[] outcome = fillMonth(resource, now, lastDay);
            created += outcome[0];
            refused += outcome[1];
            if (outcome[0] > 0) {
                filled++;
            }
        }
        return new Report(filled, skipped, created, refused);
    }

    /** {plages creees, plages refusees} pour une ressource. */
    private int[] fillMonth(Resource resource, LocalDateTime now, LocalDate lastDay) {
        int slotMinutes = slotPolicy.slotMinutes(resource);
        int created = 0;
        int refused = 0;
        for (LocalDate day = now.toLocalDate(); !day.isAfter(lastDay); day = day.plusDays(1)) {
            for (OpeningHours hours : openingHours.getOrDefault(day.getDayOfWeek(), List.of())) {
                LocalDateTime start = day.atTime(hours.open());
                LocalDateTime end = day.atTime(hours.close());
                if (start.isBefore(now)) {
                    start = nextBoundary(now, slotMinutes);
                }
                if (!start.isBefore(end)) {
                    continue;
                }
                try {
                    availabilityService.createAvailability(CreateResourceAvailabilityRequest.builder()
                            .resourceCode(resource.getCode())
                            .startedAt(start)
                            .endedAt(end)
                            .build());
                    created++;
                } catch (BaseException ex) {
                    // Refus metier · fermeture declaree, creneaux deja la, politique de la ressource.
                    // La plage est sautee, les suivantes continuent.
                    refused++;
                    log.debug("Disponibilites mensuelles · {} le {} sautee : {}", resource.getCode(), day, ex.getMessage());
                }
            }
        }
        return new int[]{created, refused};
    }

    private static boolean isBookable(Resource resource) {
        return Boolean.TRUE.equals(resource.getBookingEnabled())
                && Boolean.TRUE.equals(resource.getActive())
                && resource.getStatus() == ResourceStatus.ACTIVE
                && StringUtils.hasText(resource.getCode());
    }

    /** Le prochain debut de creneau a partir de maintenant, aligne sur la duree de creneau. */
    static LocalDateTime nextBoundary(LocalDateTime now, int slotMinutes) {
        LocalDateTime hour = now.withMinute(0).withSecond(0).withNano(0);
        LocalDateTime candidate = hour;
        // Strictement apres maintenant · un debut egal a maintenant serait deja passe quand
        // la creation le verifie, quelques millisecondes plus tard.
        while (!candidate.isAfter(now)) {
            candidate = candidate.plusMinutes(slotMinutes);
        }
        return candidate;
    }

    /** Une plage d'ouverture « HH:mm-HH:mm » · vide ou absente, le jour est ferme. */
    record OpeningHours(LocalTime open, LocalTime close) {

        static OpeningHours parse(String value) {
            if (!StringUtils.hasText(value)) {
                return null;
            }
            String[] parts = value.trim().split("-");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Plage horaire invalide, attendu HH:mm-HH:mm : " + value);
            }
            LocalTime open = LocalTime.parse(parts[0].trim());
            LocalTime close = LocalTime.parse(parts[1].trim());
            if (!open.isBefore(close)) {
                throw new IllegalArgumentException("Plage horaire vide ou inversee : " + value);
            }
            return new OpeningHours(open, close);
        }
    }
}
