package com.sni.bokaticowork.features.ressource.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.core.exception.customs.ValidationException;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.ressource.dto.request.CreateResourceAvailabilityRequest;
import com.sni.bokaticowork.features.ressource.dto.request.ReleaseResourceAvailabilityRequest;
import com.sni.bokaticowork.features.ressource.dto.request.ReserveResourceAvailabilityRequest;
import com.sni.bokaticowork.features.ressource.dto.response.ResourceAvailabilityGroupResponse;
import com.sni.bokaticowork.features.ressource.dto.response.ResourceAvailabilityResponse;
import com.sni.bokaticowork.features.ressource.dto.response.ResourceAvailabilityWindowResponse;
import com.sni.bokaticowork.features.ressource.enums.ResourceStatus;
import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.ressource.dto.request.BulkCreateResourceAvailabilityRequest;
import com.sni.bokaticowork.features.ressource.dto.response.BulkResourceOperationResponse;
import com.sni.bokaticowork.features.ressource.service.support.ResourceBulkExecutor;
import com.sni.bokaticowork.features.ressource.service.support.ResourceSlotPolicy;
import com.sni.bokaticowork.features.ressource.model.ResourceAvailability;
import com.sni.bokaticowork.features.ressource.model.ResourcePolicy;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceAvailabilityRepository;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceClosureRepository;
import com.sni.bokaticowork.features.ressource.service.interfaces.ResourceAvailabilityService;
import com.sni.bokaticowork.features.ressource.service.interfaces.ResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
@RequiredArgsConstructor
public class ResourceAvailabilityServiceImpl implements ResourceAvailabilityService {

    private static final int MAX_AVAILABILITY_CREATION_MONTHS = 1;
    private static final LocalTime WORKING_DAY_START = LocalTime.of(8, 0);
    private static final LocalTime WORKING_DAY_END = LocalTime.of(20, 0);

    private final ResourceAvailabilityRepository availabilityRepository;
    private final ResourceClosureRepository closureRepository;
    private final ResourceService resourceService;
    private final ResourceSlotPolicy slotPolicy;
    private final ResourceBulkExecutor bulkExecutor;

    /** Borne d'un appel groupe · une plage d'un mois produit deja ~384 creneaux par ressource. */
    @org.springframework.beans.factory.annotation.Value("${bokati.resource.bulk-max-resources:20}")
    private int maxBulkResources;

    /**
     * Ouvre la meme plage sur plusieurs ressources.
     *
     * <p>Delegue a {@link #createAvailability} ressource par ressource · toute la validation
     * existante s'applique donc a l'identique, et rien n'est duplique. La methode n'est
     * volontairement pas transactionnelle : c'est l'executeur qui ouvre une transaction par
     * ressource, sans quoi le premier echec annulerait les creneaux deja ecrits.
     */
    @Override
    public BulkResourceOperationResponse createAvailabilityBulk(BulkCreateResourceAvailabilityRequest request) {
        // Une duree non diviseur de 60 est un defaut de la requete, pas des ressources : la laisser
        // filtrer dans la boucle rendrait vingt lignes ecartees identiques sous un HTTP 200, alors
        // que rien de ce qui est demande ne peut aboutir. Refus net, avant toute ecriture.
        slotPolicy.assertAcceptable(request.getSlotDurationMinutes());

        return bulkExecutor.run(request.getResourceCodes(), maxBulkResources, code -> {
            createAvailability(request.forResource(code));
            return countCreated(code, request);
        });
    }

    /** Creneaux effectivement ecrits pour cette ressource sur la plage demandee. */
    private int countCreated(String resourceCode, BulkCreateResourceAvailabilityRequest request) {
        return (int) availabilityRepository.countByResourceAndStartedAtGreaterThanEqualAndEndedAtLessThanEqual(
                resourceService.getResourceForService(resourceCode),
                request.getStartedAt(), request.getEndedAt());
    }

    @Override
    public void createAvailability(CreateResourceAvailabilityRequest request) {
        List<String> errors = validateCreateRequest(request);
        if (!errors.isEmpty()) {
            throw new ValidationException("Invalid resource availability request", errors);
        }

        Resource resource = resourceService.getResourceForService(request.getResourceCode().trim());
        resource = applyRequestedSlotDuration(resource, request.getSlotDurationMinutes());
        assertResourceBookable(resource);
        slotPolicy.assertAligned(resource, request.getStartedAt(), request.getEndedAt());
        assertAvailabilityCreationWindow(request.getStartedAt(), request.getEndedAt());

        if (closureRepository.existsActiveOverlap(resource, request.getStartedAt(), request.getEndedAt())) {
            throw new ConflictException("resource availability", "the requested range overlaps an active closure");
        }

        if (availabilityRepository.existsActiveOverlap(resource, request.getStartedAt(), request.getEndedAt())) {
            throw new ConflictException("resource availability", "the requested range overlaps existing availability slots");
        }

        int capacity = resolveCapacity(resource, request.getCapacity());
        List<ResourceAvailability> slots = buildSlots(resource, request.getStartedAt(), request.getEndedAt(), capacity, request.getActive());
        availabilityRepository.saveAll(slots);
    }

    @Override
    public PaginatedResponse<ResourceAvailabilityResponse> list(Pageable pageable) {
        expirePastAvailabilitySlots();
        Page<ResourceAvailabilityResponse> page = availabilityRepository.findAllByEndedAtAfter(LocalDateTime.now(), pageable)
                .map(this::toResponse);
        return new PaginatedResponse<>(page);
    }

    @Override
    public PaginatedResponse<ResourceAvailabilityResponse> listByResource(String resourceCode, Pageable pageable) {
        Resource resource = resourceService.getResourceForService(resourceCode);
        expirePastAvailabilitySlots();
        Page<ResourceAvailabilityResponse> page = availabilityRepository.findAllByResourceAndEndedAtAfter(resource, LocalDateTime.now(), pageable)
                .map(this::toResponse);
        return new PaginatedResponse<>(page);
    }

    @Override
    public List<ResourceAvailabilityGroupResponse> listGroupedByResource() {
        expirePastAvailabilitySlots();
        Map<String, ResourceAvailabilityGroupResponse> grouped = new LinkedHashMap<>();

        availabilityRepository.findFutureForGroupedView(LocalDateTime.now()).forEach(slot -> {
            Resource resource = slot.getResource();
            grouped.computeIfAbsent(resource.getCode(), code -> ResourceAvailabilityGroupResponse.builder()
                    .resourceCode(code)
                    .resourceName(resource.getName())
                    .availabilities(new ArrayList<>())
                    .build()
            ).getAvailabilities().add(toResponse(slot));
        });

        return new ArrayList<>(grouped.values());
    }

    @Override
    public List<ResourceAvailabilityWindowResponse> findRemainingWindows(
            String resourceCode,
            LocalDateTime startedAt,
            LocalDateTime endedAt,
            Integer durationMinutes,
            Integer quantity
    ) {
        expirePastAvailabilitySlots();
        Resource resource = resourceService.getResourceForService(resourceCode);
        int normalizedQuantity = normalizeQuantity(quantity);
        if (startedAt == null && endedAt == null && durationMinutes == null) {
            return findFullRemainingWindows(resource, normalizedQuantity);
        }

        int normalizedDuration = normalizeDurationMinutes(resource, durationMinutes);
        validateSearchWindow(startedAt, endedAt, normalizedDuration);
        // Une recherche que la politique n'autorise pas ne rend pas d'erreur : elle ne rend rien.
        // Demander les creneaux du jour sur une ressource a preavis long repondait 409, ce qui est
        // indiscernable d'une panne cote client alors qu'il n'y a simplement aucune place.
        if (policyViolation(resource, startedAt, startedAt.plusMinutes(normalizedDuration)) != null) {
            return List.of();
        }

        List<ResourceAvailability> slots = availabilityRepository.findCandidateSlots(resource, startedAt, endedAt).stream()
                .sorted(Comparator.comparing(ResourceAvailability::getStartedAt))
                .toList();

        int requiredSlots = slotPolicy.slotCount(resource, normalizedDuration);
        List<ResourceAvailabilityWindowResponse> windows = new ArrayList<>();

        for (int index = 0; index <= slots.size() - requiredSlots; index++) {
            List<ResourceAvailability> candidate = slots.subList(index, index + requiredSlots);
            if (!isContiguous(candidate)) {
                continue;
            }
            if (!respectsBookingNotice(resource, candidate.getFirst().getStartedAt())) {
                continue;
            }
            if (!allReservable(candidate, normalizedQuantity, resource)) {
                continue;
            }

            int remainingCapacity = candidate.stream()
                    .map(ResourceAvailability::getRemainingCapacity)
                    .min(Integer::compareTo)
                    .orElse(0);

            windows.add(ResourceAvailabilityWindowResponse.builder()
                    .resourceCode(resource.getCode())
                    .startedAt(candidate.getFirst().getStartedAt())
                    .endedAt(candidate.getLast().getEndedAt())
                    .durationMinutes(normalizedDuration)
                    .remainingCapacity(remainingCapacity)
                    .slotCount(requiredSlots)
                    .build());
        }

        return windows;
    }

    @Override
    public void reserve(ReserveResourceAvailabilityRequest request) {
        expirePastAvailabilitySlots();
        List<String> errors = validateReservationRequest(request);
        if (!errors.isEmpty()) {
            throw new ValidationException("Invalid resource reservation request", errors);
        }

        Resource resource = resourceService.getResourceForService(request.getResourceCode().trim());
        slotPolicy.assertAligned(resource, request.getStartedAt(), request.getEndedAt());
        assertResourceBookable(resource);
        assertPolicyAllowsWindow(resource, request.getStartedAt(), request.getEndedAt());

        if (closureRepository.existsActiveOverlap(resource, request.getStartedAt(), request.getEndedAt())) {
            throw new ConflictException("resource reservation", "the requested range overlaps an active closure");
        }

        List<ResourceAvailability> slots = availabilityRepository.lockAllSlotsInRange(resource, request.getStartedAt(), request.getEndedAt());
        assertExactRequestedCoverage(slots, request.getStartedAt(), request.getEndedAt());

        if (!allReservable(slots, request.getQuantity(), resource)) {
            throw new ConflictException("resource reservation", "insufficient remaining availability for the requested range");
        }

        for (ResourceAvailability slot : slots) {
            // Une salle se loue entiere : elle prend tout le creneau, quel que soit le nombre de
            // participants annonce. Ne retirer que la quantite demandee laissait des places libres
            // sur une piece deja louee, et un second client pouvait s'y presenter.
            int consumed = resource.isWholeResourceBooking()
                    ? slot.getRemainingCapacity()
                    : request.getQuantity();
            slot.setRemainingCapacity(slot.getRemainingCapacity() - consumed);
            slot.setAvailable(slot.getRemainingCapacity() > 0);
        }

        availabilityRepository.saveAll(slots);
    }

    @Override
    public void release(ReleaseResourceAvailabilityRequest request) {
        expirePastAvailabilitySlots();
        List<String> errors = validateReleaseRequest(request);
        if (!errors.isEmpty()) {
            throw new ValidationException("Invalid resource availability release request", errors);
        }

        Resource resource = resourceService.getResourceForService(request.getResourceCode().trim());
        slotPolicy.assertAligned(resource, request.getStartedAt(), request.getEndedAt());
        List<ResourceAvailability> slots = availabilityRepository.lockAllSlotsInRange(resource, request.getStartedAt(), request.getEndedAt());
        assertExactRequestedCoverage(slots, request.getStartedAt(), request.getEndedAt());

        for (ResourceAvailability slot : slots) {
            // Symetrique de la reservation : ce qui a ete pris en entier se rend en entier.
            int restored = resource.isWholeResourceBooking()
                    ? slot.getTotalCapacity()
                    : Math.min(slot.getTotalCapacity(), slot.getRemainingCapacity() + request.getQuantity());
            slot.setRemainingCapacity(restored);
            slot.setAvailable(restored > 0);
        }

        availabilityRepository.saveAll(slots);
    }

    private void assertResourceBookable(Resource resource) {
        if (!Boolean.TRUE.equals(resource.getBookingEnabled())) {
            throw new ConflictException("resource availability", "the resource is not bookable");
        }
        if (!Boolean.TRUE.equals(resource.getActive())) {
            throw new ConflictException("resource availability", "the resource is inactive");
        }
        if (resource.getStatus() == null || resource.getStatus() != ResourceStatus.ACTIVE) {
            throw new ConflictException("resource availability", "the resource status does not allow bookings");
        }
    }

    /**
     * Refuse la plage demandee, en expliquant pourquoi.
     *
     * <p>Reserver et chercher n'appellent pas le meme jugement sur la meme reponse. Reserver hors
     * politique est une erreur, et doit le rester. Chercher hors politique n'en est pas une : le
     * client demande ce qui est disponible, la reponse est qu'il n'y a rien. D'ou une regle unique
     * et deux facons de s'en servir · {@link #assertPolicyAllowsWindow} pour l'ecriture,
     * {@link #policyViolation} directement pour la lecture.</p>
     *
     * @return le motif du refus, ou {@code null} si la plage est acceptable
     */
    private String policyViolation(Resource resource, LocalDateTime startedAt, LocalDateTime endedAt) {
        ResourcePolicy policy = resource.getResourcePolicy();
        if (policy == null) {
            return null;
        }

        int requestedMinutes = Math.toIntExact(Duration.between(startedAt, endedAt).toMinutes());
        if (requestedMinutes < policy.getMinBookingDurationMinutes()) {
            return "the requested duration is below the policy minimum";
        }
        if (requestedMinutes > policy.getMaxBookingDurationMinutes()) {
            return "the requested duration exceeds the policy maximum";
        }
        if (startedAt.isBefore(LocalDateTime.now().plusMinutes(policy.getMinBookingNoticeMinutes()))) {
            return "the requested start time violates the minimum booking notice";
        }
        return null;
    }

    private void assertPolicyAllowsWindow(Resource resource, LocalDateTime startedAt, LocalDateTime endedAt) {
        String violation = policyViolation(resource, startedAt, endedAt);
        if (violation != null) {
            throw new ConflictException("resource availability", violation);
        }
    }

    private void assertAvailabilityCreationWindow(LocalDateTime startedAt, LocalDateTime endedAt) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime latestAllowedEnd = now.toLocalDate()
                .plusMonths(MAX_AVAILABILITY_CREATION_MONTHS)
                .atTime(WORKING_DAY_END);

        if (startedAt.isBefore(now)) {
            throw new ConflictException("resource availability", "availability cannot be created in the past");
        }
        if (endedAt.isAfter(latestAllowedEnd)) {
            throw new ConflictException("resource availability", "availability can only be created up to one month in advance");
        }
        if (!isValidAvailabilityStart(startedAt.toLocalTime()) || !isValidAvailabilityEnd(endedAt.toLocalTime())) {
            throw new ConflictException("resource availability", "availability must be created within working hours from 08:00 to 20:00");
        }
    }

    private boolean respectsBookingNotice(Resource resource, LocalDateTime startedAt) {
        ResourcePolicy policy = resource.getResourcePolicy();
        if (policy == null) {
            return true;
        }
        return !startedAt.isBefore(LocalDateTime.now().plusMinutes(policy.getMinBookingNoticeMinutes()));
    }

    private List<ResourceAvailability> buildSlots(Resource resource,
                                                  LocalDateTime startedAt,
                                                  LocalDateTime endedAt,
                                                  int capacity,
                                                  Boolean active) {
        // Copie figee de la duree de la ressource · lire l'historique d'un creneau ne doit pas
        // dependre d'une valeur que la ressource peut changer ensuite.
        int slotMinutes = slotPolicy.slotMinutes(resource);
        List<ResourceAvailability> slots = new ArrayList<>();
        LocalDateTime current = startedAt;

        while (current.isBefore(endedAt)) {
            LocalDateTime workingDayStart = current.toLocalDate().atTime(WORKING_DAY_START);
            LocalDateTime workingDayEnd = current.toLocalDate().atTime(WORKING_DAY_END);

            if (current.isBefore(workingDayStart)) {
                current = workingDayStart;
            }
            if (!current.isBefore(endedAt)) {
                break;
            }
            if (!current.isBefore(workingDayEnd)) {
                current = current.toLocalDate().plusDays(1).atTime(WORKING_DAY_START);
                continue;
            }

            LocalDateTime next = current.plusMinutes(slotMinutes);
            if (next.isAfter(workingDayEnd) || next.isAfter(endedAt)) {
                break;
            }
            slots.add(ResourceAvailability.builder()
                    .resource(resource)
                    .startedAt(current)
                    .endedAt(next)
                    .slotDurationMinutes(slotMinutes)
                    .totalCapacity(capacity)
                    .remainingCapacity(capacity)
                    .available(capacity > 0)
                    .active(active == null ? Boolean.TRUE : active)
                    .build());
            current = next;
        }

        return slots;
    }

    private boolean isValidAvailabilityStart(LocalTime value) {
        return !value.isBefore(WORKING_DAY_START) && value.isBefore(WORKING_DAY_END);
    }

    private boolean isValidAvailabilityEnd(LocalTime value) {
        return value.isAfter(WORKING_DAY_START) && !value.isAfter(WORKING_DAY_END);
    }

    private ResourceAvailabilityResponse toResponse(ResourceAvailability slot) {
        return ResourceAvailabilityResponse.builder()
                .id(slot.getId())
                .resourceCode(slot.getResource().getCode())
                .startedAt(slot.getStartedAt())
                .endedAt(slot.getEndedAt())
                .slotDurationMinutes(slot.getSlotDurationMinutes())
                .totalCapacity(slot.getTotalCapacity())
                .remainingCapacity(slot.getRemainingCapacity())
                .available(slot.getAvailable())
                .active(slot.getActive())
                .build();
    }

    private boolean isContiguous(List<ResourceAvailability> slots) {
        for (int i = 0; i < slots.size() - 1; i++) {
            if (!slots.get(i).getEndedAt().equals(slots.get(i + 1).getStartedAt())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Le creneau peut-il accueillir la demande.
     *
     * <p>Pour une ressource louee en entier, c'est tout ou rien : la piece doit etre entierement
     * libre. Accepter un creneau partiellement pris reviendrait a louer une salle dont quelqu'un
     * occupe deja une partie, ce qui n'a pas de sens quand on loue la piece.</p>
     */
    private boolean allReservable(List<ResourceAvailability> slots, int quantity, Resource resource) {
        boolean whole = resource != null && resource.isWholeResourceBooking();
        return slots.stream().allMatch(slot ->
                Boolean.TRUE.equals(slot.getActive())
                        && Boolean.TRUE.equals(slot.getAvailable())
                        && slot.getRemainingCapacity() != null
                        && (whole
                        ? slot.getRemainingCapacity().equals(slot.getTotalCapacity())
                        : slot.getRemainingCapacity() >= quantity)
        );
    }

    private void assertExactRequestedCoverage(List<ResourceAvailability> slots, LocalDateTime startedAt, LocalDateTime endedAt) {
        if (slots.isEmpty()) {
            throw new ConflictException("resource reservation", "no availability exists for the requested range");
        }

        long expectedSlotCount = slotPolicy.slotCount(
                slots.getFirst().getResource(), Duration.between(startedAt, endedAt).toMinutes());
        if (slots.size() != expectedSlotCount || !slots.getFirst().getStartedAt().equals(startedAt) || !slots.getLast().getEndedAt().equals(endedAt) || !isContiguous(slots)) {
            throw new ConflictException("resource reservation", "the requested range is not fully covered by contiguous availability slots");
        }
    }

    /**
     * Prend en compte une duree de creneau fournie a la creation d'une disponibilite.
     *
     * <p>La duree appartient a la ressource, pas a la disponibilite · le champ de la requete ne
     * fait donc que porter la valeur jusqu'a elle. Trois cas :
     *
     * <ul>
     *   <li>absente ou identique a celle de la ressource · rien ne change. C'est le cas courant,
     *       et c'est ce qui rend la reprise indolore : une interface qui envoie 30 sur un parc a
     *       30 continue de fonctionner a l'identique ;</li>
     *   <li>differente, et la ressource n'a aucun creneau futur · elle est adoptee ;</li>
     *   <li>differente, mais des creneaux futurs existent · refus. Les changer laisserait la
     *       ressource avec des creneaux de durees melangees, ce qui fausserait silencieusement la
     *       composition des fenetres.</li>
     * </ul>
     */
    private Resource applyRequestedSlotDuration(Resource resource, Integer requested) {
        if (requested == null) {
            return resource;
        }
        slotPolicy.assertAcceptable(requested);
        int current = slotPolicy.slotMinutes(resource);
        if (requested == current) {
            return resource;
        }
        long futureSlots = availabilityRepository.countByResourceAndEndedAtAfter(resource, LocalDateTime.now());
        if (futureSlots > 0) {
            throw new ConflictException("resource availability",
                    "La ressource " + resource.getCode() + " a " + futureSlots + " creneau(x) futur(s) de "
                            + current + " minutes · supprimez-les avant de passer a " + requested
                            + " minutes, sinon ses creneaux auraient des durees melangees et les"
                            + " fenetres calculees seraient fausses.");
        }
        resource.setSlotDurationMinutes(requested);
        return resourceService.saveResource(resource);
    }

    private int resolveCapacity(Resource resource, Integer requestedCapacity) {
        int bookableSlots = resource.resolveBookableSlots();
        // Never exceed the type-level concurrent booking limit, even if the caller passes a higher value.
        int baseCapacity = requestedCapacity != null
                ? Math.min(requestedCapacity, bookableSlots)
                : bookableSlots;
        if (baseCapacity < 1) {
            throw new BadRequestException("Availability capacity must be greater than zero");
        }
        return baseCapacity;
    }

    private int normalizeDurationMinutes(Resource resource, Integer durationMinutes) {
        int slotMinutes = slotPolicy.slotMinutes(resource);
        if (durationMinutes == null || durationMinutes < slotMinutes) {
            throw new BadRequestException("Duration must be at least " + slotMinutes + " minutes");
        }
        if (durationMinutes % slotMinutes != 0) {
            throw new BadRequestException("Duration must be a multiple of " + slotMinutes + " minutes");
        }
        return durationMinutes;
    }

    private int normalizeQuantity(Integer quantity) {
        if (quantity == null || quantity < 1) {
            throw new BadRequestException("Quantity must be greater than zero");
        }
        return quantity;
    }

    private static final int EXPIRY_BATCH_SIZE = 200;

    private void expirePastAvailabilitySlots() {
        LocalDateTime now = LocalDateTime.now();
        List<ResourceAvailability> expiredSlots = availabilityRepository.findExpiredSlots(now, Limit.of(EXPIRY_BATCH_SIZE));
        if (expiredSlots.isEmpty()) {
            return;
        }

        expiredSlots.forEach(slot -> {
            slot.setAvailable(Boolean.FALSE);
            slot.setActive(Boolean.FALSE);
        });
        availabilityRepository.saveAll(expiredSlots);
    }

    private List<ResourceAvailabilityWindowResponse> findFullRemainingWindows(Resource resource, int quantity) {
        List<ResourceAvailability> slots = availabilityRepository.findFutureActiveSlots(resource, LocalDateTime.now(), Pageable.unpaged());

        List<ResourceAvailabilityWindowResponse> windows = new ArrayList<>();
        List<ResourceAvailability> currentWindow = new ArrayList<>();

        for (ResourceAvailability slot : slots) {
            if (currentWindow.isEmpty()) {
                currentWindow.add(slot);
                continue;
            }
            ResourceAvailability previous = currentWindow.getLast();
            if (!previous.getEndedAt().equals(slot.getStartedAt())) {
                flushWindow(resource, currentWindow, windows);
            }
            currentWindow.add(slot);
        }

        flushWindow(resource, currentWindow, windows);
        return windows;
    }

    private void flushWindow(Resource resource,
                             List<ResourceAvailability> currentWindow,
                             List<ResourceAvailabilityWindowResponse> windows) {
        if (currentWindow.isEmpty()) {
            return;
        }

        int remainingCapacity = currentWindow.stream()
                .map(ResourceAvailability::getRemainingCapacity)
                .min(Integer::compareTo)
                .orElse(0);
        LocalDateTime windowStart = currentWindow.getFirst().getStartedAt();
        LocalDateTime windowEnd = currentWindow.getLast().getEndedAt();

        windows.add(ResourceAvailabilityWindowResponse.builder()
                .resourceCode(resource.getCode())
                .startedAt(windowStart)
                .endedAt(windowEnd)
                .durationMinutes(Math.toIntExact(Duration.between(windowStart, windowEnd).toMinutes()))
                .remainingCapacity(remainingCapacity)
                .slotCount(currentWindow.size())
                .build());
        currentWindow.clear();
    }

    private void validateSearchWindow(LocalDateTime startedAt, LocalDateTime endedAt, int durationMinutes) {
        if (startedAt == null || endedAt == null) {
            throw new BadRequestException("Start and end dates are required");
        }
        if (!endedAt.isAfter(startedAt)) {
            throw new BadRequestException("End date must be after start date");
        }
        if (startedAt.plusMinutes(durationMinutes).isAfter(endedAt.plusMinutes(1))) {
            throw new BadRequestException("Requested duration does not fit inside the requested search range");
        }
    }

    private List<String> validateCreateRequest(CreateResourceAvailabilityRequest request) {
        List<String> errors = new ArrayList<>();
        if (request == null) {
            errors.add("Invalid resource availability request, the request body is required");
            return errors;
        }
        if (!StringUtils.hasText(request.getResourceCode())) {
            errors.add("Invalid resource availability request, the resource code is required");
        }
        validateTimeRange(errors, request.getStartedAt(), request.getEndedAt());
        if (request.getCapacity() != null && request.getCapacity() < 1) {
            errors.add("Invalid resource availability request, the capacity must be greater than zero");
        }
        return errors;
    }

    private List<String> validateReservationRequest(ReserveResourceAvailabilityRequest request) {
        List<String> errors = new ArrayList<>();
        if (request == null) {
            errors.add("Invalid resource reservation request, the request body is required");
            return errors;
        }
        if (!StringUtils.hasText(request.getResourceCode())) {
            errors.add("Invalid resource reservation request, the resource code is required");
        }
        validateTimeRange(errors, request.getStartedAt(), request.getEndedAt());
        if (request.getQuantity() == null || request.getQuantity() < 1) {
            errors.add("Invalid resource reservation request, the quantity must be greater than zero");
        }
        return errors;
    }

    private List<String> validateReleaseRequest(ReleaseResourceAvailabilityRequest request) {
        List<String> errors = new ArrayList<>();
        if (request == null) {
            errors.add("Invalid resource availability release request, the request body is required");
            return errors;
        }
        if (!StringUtils.hasText(request.getResourceCode())) {
            errors.add("Invalid resource availability release request, the resource code is required");
        }
        validateTimeRange(errors, request.getStartedAt(), request.getEndedAt());
        if (request.getQuantity() == null || request.getQuantity() < 1) {
            errors.add("Invalid resource availability release request, the quantity must be greater than zero");
        }
        return errors;
    }

    private void validateTimeRange(List<String> errors, LocalDateTime startedAt, LocalDateTime endedAt) {
        if (startedAt == null) {
            errors.add("The start date is required");
        }
        if (endedAt == null) {
            errors.add("The end date is required");
        }
        if (startedAt != null && endedAt != null) {
            if (!endedAt.isAfter(startedAt)) {
                errors.add("The end date must be after the start date");
            }
            // L'alignement sur les creneaux ne se verifie pas ici : il depend de la duree de la
            // ressource, encore inconnue a ce stade. Il est controle apres resolution, par
            // ResourceSlotPolicy.assertAligned, qui peut alors nommer la duree effective.
            if (startedAt.getSecond() != 0 || endedAt.getSecond() != 0 || startedAt.getNano() != 0 || endedAt.getNano() != 0) {
                errors.add("The requested range must not contain seconds or fractional seconds");
            }
        }
    }
}
