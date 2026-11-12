package com.sni.bokaticowork.features.ressource.service.support;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.ressource.model.ResourceOpeningHours;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceOpeningHoursRepository;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Les horaires propres a une ressource · ils remplacent les horaires globaux pour elle seule, et
 * valent pour les creneaux generes ensuite : ceux deja ouverts ne bougent pas.
 */
@Service
@RequiredArgsConstructor
public class ResourceOpeningHoursService {

    private final ResourceRepository resourceRepository;
    private final ResourceOpeningHoursRepository repository;
    private final OpeningHoursResolver resolver;

    public record Range(DayOfWeek dayOfWeek, LocalTime open, LocalTime close) {
    }

    /** @param custom la ressource a ses horaires · sinon {@code hours} sont les horaires globaux */
    public record View(String resourceCode, boolean custom, List<Range> hours) {
    }

    @Transactional(readOnly = true)
    public View get(String resourceCode) {
        return view(resource(resourceCode));
    }

    /** Remplace les horaires de la ressource · un jour absent de la liste est ferme. */
    @Transactional
    public View replace(String resourceCode, List<Range> ranges, String actor) {
        Resource resource = resource(resourceCode);
        if (ranges == null || ranges.isEmpty()) {
            throw new BadRequestException("Précisez au moins une plage, ou revenez aux horaires globaux.");
        }
        ensureCoherent(ranges);
        repository.deleteAllByResource(resource);
        repository.flush();
        for (Range range : ranges) {
            repository.save(ResourceOpeningHours.builder()
                    .resource(resource)
                    .dayOfWeek(range.dayOfWeek())
                    .openTime(range.open())
                    .closeTime(range.close())
                    .updatedBy(actor)
                    .build());
        }
        repository.flush();
        return view(resource);
    }

    /** La ressource revient aux horaires globaux. */
    @Transactional
    public View clear(String resourceCode) {
        Resource resource = resource(resourceCode);
        repository.deleteAllByResource(resource);
        repository.flush();
        return view(resource);
    }

    private static void ensureCoherent(List<Range> ranges) {
        if (ranges.stream().anyMatch(range -> range == null || range.dayOfWeek() == null || range.open() == null || range.close() == null)) {
            throw new BadRequestException("Chaque plage précise son jour, son ouverture et sa fermeture.");
        }
        List<Range> sorted = new ArrayList<>(ranges);
        sorted.sort(Comparator.comparing(Range::dayOfWeek).thenComparing(Range::open));
        Range previous = null;
        for (Range range : sorted) {
            if (!range.open().isBefore(range.close())) {
                throw new BadRequestException("Une plage doit s'ouvrir avant de se fermer : " + range.dayOfWeek()
                        + " " + range.open() + "-" + range.close() + ".");
            }
            if (previous != null && previous.dayOfWeek() == range.dayOfWeek() && range.open().isBefore(previous.close())) {
                throw new BadRequestException("Deux plages se chevauchent le " + range.dayOfWeek() + ".");
            }
            previous = range;
        }
    }

    private View view(Resource resource) {
        List<ResourceOpeningHours> own = resolver.own(resource);
        if (!own.isEmpty()) {
            return new View(resource.getCode(), true, own.stream()
                    .map(range -> new Range(range.getDayOfWeek(), range.getOpenTime(), range.getCloseTime())).toList());
        }
        List<Range> global = new ArrayList<>();
        resolver.global().forEach((day, hours) -> hours.forEach(hour -> global.add(new Range(day, hour.open(), hour.close()))));
        return new View(resource.getCode(), false, global);
    }

    private Resource resource(String code) {
        return resourceRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Ressource introuvable : " + code));
    }
}
