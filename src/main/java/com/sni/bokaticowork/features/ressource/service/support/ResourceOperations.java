package com.sni.bokaticowork.features.ressource.service.support;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.core.utils.code.CodeComposer;
import com.sni.bokaticowork.features.ressource.enums.ResourceStatus;
import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.ressource.model.ResourceClosure;
import com.sni.bokaticowork.features.ressource.model.ResourceClosureKind;
import com.sni.bokaticowork.features.ressource.model.ResourceIncident;
import com.sni.bokaticowork.features.ressource.model.ResourceTaskTemplate;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceClosureRepository;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceIncidentRepository;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceRepository;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceTaskTemplateRepository;
import com.sni.bokaticowork.features.ressource.service.interfaces.ResourceService;
import com.sni.bokaticowork.features.support.dto.SupportDtos;
import com.sni.bokaticowork.features.support.enums.TicketCategory;
import com.sni.bokaticowork.features.support.enums.TicketPriority;
import com.sni.bokaticowork.features.support.service.interfaces.SupportTicketService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Lot R5d : exploitation des ressources · modeles de taches, incidents, historique de maintenance. */
@Slf4j
@Component
public class ResourceOperations {

    private static final Set<String> SEVERITIES = Set.of("LOW", "MEDIUM", "HIGH", "CRITICAL");
    private static final Set<String> PRIORITIES = Set.of("LOW", "MEDIUM", "HIGH", "URGENT");

    // ------------------------------------------------------------------ contrats

    public record TemplateRequest(String resourceCode, String serviceItemCode, String title, String description, String moment,
                                  Integer offsetMinutes, String priority, Long assignedTo, List<String> checklist, Boolean active) {
    }

    public record TemplateResponse(Long id, String resourceCode, String serviceItemCode, String title, String description,
                                   String moment, Integer offsetMinutes, String priority, Long assignedTo, List<String> checklist,
                                   Boolean active) {
    }

    /** Signalement ; {@code block} et {@code cancelBookings} sont reserves a l'equipe. */
    public record IncidentRequest(String resourceCode, String bookingNumber, String title, String description, String severity,
                                  Boolean block, Boolean cancelBookings, List<String> photoDocumentCodes) {
    }

    /** Auteur d'un signalement : MEMBER ou TEAM, avec de quoi le recontacter. */
    public record Reporter(String type, String code, String name, String email, String phone) {
    }

    public record IncidentResponse(Long id, String code, String resourceCode, String resourceName, String bookingNumber,
                                   String reportedByType, String reporterName, String title, String description, String severity,
                                   String status, Boolean blocked, List<String> photoDocumentCodes, String ticketNumber,
                                   String resolution, String resolvedBy, Instant resolvedAt, Instant createdAt) {
    }

    /** Une ligne de l'historique de maintenance : incident ou fermeture de maintenance. */
    public record HistoryEntry(String kind, String reference, String title, String status, LocalDateTime startedAt,
                               LocalDateTime endedAt, String detail) {
    }

    private final ResourceTaskTemplateRepository templates;
    private final ResourceIncidentRepository incidents;
    private final ResourceRepository resources;
    private final ResourceClosureRepository closures;
    private final SequenceGeneratorFacade sequenceGenerator;
    private ResourceService resourceService;
    private SupportTicketService tickets;

    public ResourceOperations(ResourceTaskTemplateRepository templates, ResourceIncidentRepository incidents,
                              ResourceRepository resources, ResourceClosureRepository closures,
                              SequenceGeneratorFacade sequenceGenerator) {
        this.templates = templates;
        this.incidents = incidents;
        this.resources = resources;
        this.closures = closures;
        this.sequenceGenerator = sequenceGenerator;
    }

    @Autowired(required = false)
    void setOptional(@Lazy ResourceService resourceService, @Lazy SupportTicketService tickets) {
        this.resourceService = resourceService;
        this.tickets = tickets;
    }

    // ------------------------------------------------------------------ modeles de taches

    @Transactional(readOnly = true)
    public List<TemplateResponse> templates() {
        return templates.findAllByOrderByIdAsc().stream().map(ResourceOperations::toResponse).toList();
    }

    @Transactional
    public TemplateResponse createTemplate(TemplateRequest request) {
        if (request == null || !StringUtils.hasText(request.title())) {
            throw new BadRequestException("Le titre de la tâche est requis");
        }
        ResourceTaskTemplate template = ResourceTaskTemplate.builder().title(request.title().trim()).build();
        apply(template, request);
        return toResponse(templates.save(template));
    }

    @Transactional
    public TemplateResponse updateTemplate(Long id, TemplateRequest request) {
        ResourceTaskTemplate template = templates.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Modèle de tâche introuvable : " + id));
        if (request != null && StringUtils.hasText(request.title())) {
            template.setTitle(request.title().trim());
        }
        apply(template, request);
        return toResponse(templates.save(template));
    }

    @Transactional
    public void deleteTemplate(Long id) {
        templates.delete(templates.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Modèle de tâche introuvable : " + id)));
    }

    private void apply(ResourceTaskTemplate template, TemplateRequest request) {
        if (request == null) {
            return;
        }
        if (request.resourceCode() != null) {
            template.setResource(StringUtils.hasText(request.resourceCode()) ? resource(request.resourceCode()) : null);
        }
        if (request.serviceItemCode() != null) {
            template.setServiceItemCode(StringUtils.hasText(request.serviceItemCode()) ? request.serviceItemCode().trim() : null);
        }
        if (request.description() != null) {
            template.setDescription(StringUtils.hasText(request.description()) ? request.description().trim() : null);
        }
        if (StringUtils.hasText(request.moment())) {
            try {
                template.setMoment(ResourceTaskTemplate.Moment.valueOf(request.moment().trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ex) {
                throw new BadRequestException("Moment invalide : BEFORE_START ou AFTER_END");
            }
        }
        if (request.offsetMinutes() != null) {
            if (request.offsetMinutes() < 0 || request.offsetMinutes() > 10080) {
                throw new BadRequestException("Le décalage est compris entre 0 et 10 080 minutes (une semaine)");
            }
            template.setOffsetMinutes(request.offsetMinutes());
        }
        if (request.priority() != null) {
            String priority = request.priority().trim().toUpperCase(Locale.ROOT);
            if (!priority.isEmpty() && !PRIORITIES.contains(priority)) {
                throw new BadRequestException("Priorité invalide : LOW, MEDIUM, HIGH ou URGENT");
            }
            template.setPriority(priority.isEmpty() ? null : priority);
        }
        if (request.assignedTo() != null) {
            template.setAssignedTo(request.assignedTo() <= 0 ? null : request.assignedTo());
        }
        if (request.checklist() != null) {
            List<String> items = request.checklist().stream().filter(StringUtils::hasText).map(String::trim).toList();
            template.setChecklist(items.isEmpty() ? null : String.join("\n", items));
        }
        if (request.active() != null) {
            template.setActive(request.active());
        }
    }

    private static TemplateResponse toResponse(ResourceTaskTemplate template) {
        return new TemplateResponse(template.getId(), template.getResource() == null ? null : template.getResource().getCode(),
                template.getServiceItemCode(), template.getTitle(), template.getDescription(),
                template.getMoment() == null ? null : template.getMoment().name(), template.getOffsetMinutes(), template.getPriority(),
                template.getAssignedTo(), checklist(template), template.getActive());
    }

    public static List<String> checklist(ResourceTaskTemplate template) {
        return template.getChecklist() == null ? List.of() : List.of(template.getChecklist().split("\n"));
    }

    // ------------------------------------------------------------------ incidents

    @Transactional
    public IncidentResponse report(IncidentRequest request, Reporter reporter) {
        if (request == null || !StringUtils.hasText(request.resourceCode()) || !StringUtils.hasText(request.title())) {
            throw new BadRequestException("La ressource et l'objet de l'incident sont requis");
        }
        String severity = StringUtils.hasText(request.severity()) ? request.severity().trim().toUpperCase(Locale.ROOT) : "MEDIUM";
        if (!SEVERITIES.contains(severity)) {
            throw new BadRequestException("Gravité invalide : LOW, MEDIUM, HIGH ou CRITICAL");
        }
        boolean team = reporter != null && "TEAM".equals(reporter.type());
        Resource resource = resource(request.resourceCode());
        long seq = CodeComposer.extractSeq(sequenceGenerator.next("resource_incident", com.sni.bokaticowork.core.utils.time.AppTime.today()));
        ResourceIncident incident = ResourceIncident.builder()
                .code(CodeComposer.refWithYear("INC", com.sni.bokaticowork.core.utils.time.AppTime.today(), seq))
                .resource(resource)
                .bookingNumber(StringUtils.hasText(request.bookingNumber()) ? request.bookingNumber().trim() : null)
                .reportedByType(team ? "TEAM" : "MEMBER")
                .reporterCode(reporter == null ? null : reporter.code())
                .reporterName(reporter == null ? null : reporter.name())
                .title(request.title().trim())
                .description(StringUtils.hasText(request.description()) ? request.description().trim() : null)
                .severity(severity)
                .photoDocumentCodes(request.photoDocumentCodes() == null || request.photoDocumentCodes().isEmpty() ? null
                        : String.join(",", request.photoDocumentCodes().stream().filter(StringUtils::hasText).map(String::trim).toList()))
                .build();
        // Seule l'equipe met la ressource en maintenance (reservations a venir : refus, ou annulation explicite).
        if (team && Boolean.TRUE.equals(request.block()) && resourceService != null) {
            if (resource.getStatus() != ResourceStatus.MAINTENANCE) {
                resourceService.updateStatus(resource.getCode(), ResourceStatus.MAINTENANCE, Boolean.TRUE.equals(request.cancelBookings()));
            }
            incident.setBlocked(Boolean.TRUE);
        }
        incident = incidents.save(incident);
        incident.setTicketNumber(ticket(incident, reporter));
        return toResponse(incidents.save(incident));
    }

    /** Ticket au support, pour suivre l'incident avec le membre ; un echec ne bloque pas le signalement. */
    private String ticket(ResourceIncident incident, Reporter reporter) {
        if (tickets == null) {
            return null;
        }
        try {
            TicketPriority priority = switch (incident.getSeverity()) {
                case "CRITICAL" -> TicketPriority.URGENT;
                case "HIGH" -> TicketPriority.HIGH;
                case "LOW" -> TicketPriority.LOW;
                default -> TicketPriority.MEDIUM;
            };
            boolean member = "MEMBER".equals(incident.getReportedByType());
            return tickets.createFromAutomation(new SupportDtos.CreateTicketRequest(
                    "Incident " + incident.getCode() + " · " + incident.getResource().getName() + " : " + incident.getTitle(),
                    incident.getDescription(), priority, TicketCategory.TECHNICAL,
                    member ? "MEMBER" : null, member && reporter != null ? reporter.code() : null,
                    reporter == null ? null : reporter.name(), reporter == null ? null : reporter.email(),
                    reporter == null ? null : reporter.phone(), "RESOURCE_INCIDENT", incident.getCode())).ticketNumber();
        } catch (RuntimeException ex) {
            log.warn("Incident {} : ticket non cree ({})", incident.getCode(), ex.getMessage());
            return null;
        }
    }

    @Transactional
    public IncidentResponse start(String code) {
        ResourceIncident incident = incident(code);
        if ("RESOLVED".equals(incident.getStatus())) {
            throw new ConflictException("Incident " + incident.getCode() + " déjà résolu");
        }
        incident.setStatus("IN_PROGRESS");
        return toResponse(incidents.save(incident));
    }

    /**
     * Resolution ; {@code reopen} remet la ressource en service si cet incident l'avait bloquee et
     * qu'aucun autre incident ouvert ne la bloque.
     */
    @Transactional
    public IncidentResponse resolve(String code, String resolution, boolean reopen, String actor) {
        ResourceIncident incident = incident(code);
        if ("RESOLVED".equals(incident.getStatus())) {
            throw new ConflictException("Incident " + incident.getCode() + " déjà résolu");
        }
        if (!StringUtils.hasText(resolution)) {
            throw new BadRequestException("La résolution est requise");
        }
        incident.setStatus("RESOLVED");
        incident.setResolution(resolution.trim());
        incident.setResolvedBy(actor);
        incident.setResolvedAt(Instant.now());
        incidents.save(incident);
        Resource resource = incident.getResource();
        if (reopen && Boolean.TRUE.equals(incident.getBlocked()) && resourceService != null
                && resource.getStatus() == ResourceStatus.MAINTENANCE
                && incidents.countByResourceAndBlockedTrueAndStatusNot(resource, "RESOLVED") == 0) {
            resourceService.updateStatus(resource.getCode(), ResourceStatus.ACTIVE);
        }
        return toResponse(incident);
    }

    @Transactional(readOnly = true)
    public List<IncidentResponse> list(String resourceCode, String status) {
        List<ResourceIncident> found = StringUtils.hasText(resourceCode)
                ? incidents.findAllByResourceOrderByCreatedAtDesc(resource(resourceCode))
                : incidents.findAllByStatusInOrderByCreatedAtDesc(StringUtils.hasText(status)
                ? List.of(status.trim().toUpperCase(Locale.ROOT)) : List.of("OPEN", "IN_PROGRESS"));
        return found.stream()
                .filter(incident -> !StringUtils.hasText(status) || incident.getStatus().equalsIgnoreCase(status.trim()))
                .map(ResourceOperations::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public IncidentResponse get(String code) {
        return toResponse(incident(code));
    }

    /** Historique de maintenance : incidents et fermetures de maintenance, du plus recent au plus ancien. */
    @Transactional(readOnly = true)
    public List<HistoryEntry> history(String resourceCode) {
        Resource resource = resource(resourceCode);
        List<HistoryEntry> entries = new ArrayList<>();
        ZoneId zone = ZoneId.of("Africa/Brazzaville");
        for (ResourceIncident incident : incidents.findAllByResourceOrderByCreatedAtDesc(resource)) {
            entries.add(new HistoryEntry("INCIDENT", incident.getCode(), incident.getTitle(), incident.getStatus(),
                    LocalDateTime.ofInstant(incident.getCreatedAt(), zone),
                    incident.getResolvedAt() == null ? null : LocalDateTime.ofInstant(incident.getResolvedAt(), zone),
                    incident.getResolution()));
        }
        for (ResourceClosure closure : closures.findAllByResource(resource, org.springframework.data.domain.Pageable.unpaged())) {
            if (closure.getKind() == ResourceClosureKind.MAINTENANCE) {
                entries.add(new HistoryEntry("MAINTENANCE", String.valueOf(closure.getId()), closure.getReason(),
                        Boolean.TRUE.equals(closure.getActive()) ? "ACTIVE" : "INACTIVE", closure.getStartedAt(), closure.getEndedAt(),
                        closure.getRecurrence() == null ? null : closure.getRecurrence().name()));
            }
        }
        entries.sort(Comparator.comparing(HistoryEntry::startedAt, Comparator.nullsLast(Comparator.reverseOrder())));
        return entries;
    }

    private ResourceIncident incident(String code) {
        if (!StringUtils.hasText(code)) {
            throw new BadRequestException("Le code de l'incident est requis");
        }
        return incidents.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Incident introuvable : " + code.trim()));
    }

    private Resource resource(String code) {
        return resources.findByCode(code.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Ressource introuvable : " + code.trim()));
    }

    private static IncidentResponse toResponse(ResourceIncident incident) {
        return new IncidentResponse(incident.getId(), incident.getCode(), incident.getResource().getCode(), incident.getResource().getName(),
                incident.getBookingNumber(), incident.getReportedByType(), incident.getReporterName(), incident.getTitle(),
                incident.getDescription(), incident.getSeverity(), incident.getStatus(), incident.getBlocked(),
                incident.getPhotoDocumentCodes() == null ? List.of() : List.of(incident.getPhotoDocumentCodes().split(",")),
                incident.getTicketNumber(), incident.getResolution(), incident.getResolvedBy(), incident.getResolvedAt(),
                incident.getCreatedAt());
    }
}
