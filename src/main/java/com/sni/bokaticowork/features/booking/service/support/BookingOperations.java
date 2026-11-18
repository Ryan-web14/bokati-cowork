package com.sni.bokaticowork.features.booking.service.support;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.booking.enums.BookingEventType;
import com.sni.bokaticowork.features.booking.enums.BookingStatus;
import com.sni.bokaticowork.features.booking.model.Booking;
import com.sni.bokaticowork.features.booking.model.BookingEndReport;
import com.sni.bokaticowork.features.booking.repository.BookingEndReportRepository;
import com.sni.bokaticowork.features.booking.repository.BookingRepository;
import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.ressource.model.ResourceTaskTemplate;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceTaskTemplateRepository;
import com.sni.bokaticowork.features.ressource.service.support.ResourceOperations;
import com.sni.bokaticowork.features.task.dto.TaskDtos;
import com.sni.bokaticowork.features.task.enums.TaskPriority;
import com.sni.bokaticowork.features.task.enums.TaskRecurrence;
import com.sni.bokaticowork.features.task.enums.TaskStatus;
import com.sni.bokaticowork.features.task.model.TaskItem;
import com.sni.bokaticowork.features.task.repository.TaskItemRepository;
import com.sni.bokaticowork.features.task.service.interfaces.TaskManagementService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

/**
 * Lot R5d : exploitation d'une reservation · taches de l'equipe creees a la confirmation (et a la
 * vente d'un service), annulees avec la reservation ; reglement d'usage accepte ; etat des lieux
 * de fin d'utilisation, avec incident si la ressource est rendue abimee.
 */
@Slf4j
@Component
public class BookingOperations {

    public static final String TASK_SOURCE = "BOOKING";
    static final ZoneId ZONE = ZoneId.of("Africa/Brazzaville");

    public record ChecklistAnswer(String label, Boolean done) {
    }

    /** Etat des lieux : GOOD ou DAMAGED, points verifies, photos de la GED, remarques. */
    public record EndReportRequest(String condition, List<ChecklistAnswer> checklist, List<String> photoDocumentCodes, String notes) {
    }

    public record EndReportResponse(Long id, String bookingNumber, String condition, List<ChecklistAnswer> checklist,
                                    List<String> photoDocumentCodes, String notes, String reportedByType, String reportedBy,
                                    String incidentCode, Instant createdAt) {
    }

    /** Reglement d'usage de la ressource et etat de son acceptation pour la reservation. */
    public record UsageRulesView(String rules, String version, Instant acceptedAt, boolean upToDate, List<String> endChecklist) {
    }

    private final BookingRepository bookings;
    private final BookingEndReportRepository reports;
    private final ResourceTaskTemplateRepository templates;
    private final BookingEventWriter eventWriter;
    private TaskManagementService tasks;
    private TaskItemRepository taskItems;
    private ResourceOperations operations;

    public BookingOperations(BookingRepository bookings, BookingEndReportRepository reports,
                             ResourceTaskTemplateRepository templates, BookingEventWriter eventWriter) {
        this.bookings = bookings;
        this.reports = reports;
        this.templates = templates;
        this.eventWriter = eventWriter;
    }

    @Autowired(required = false)
    void setOptional(@Lazy TaskManagementService tasks, TaskItemRepository taskItems, @Lazy ResourceOperations operations) {
        this.tasks = tasks;
        this.taskItems = taskItems;
        this.operations = operations;
    }

    // ------------------------------------------------------------------ taches

    /** Des modeles s'appliquent-ils a cette reservation ? Ils remplacent alors les taches par defaut. */
    public boolean hasTemplates(Booking booking) {
        return booking.getResource() != null && !templates.forBooking(booking.getResource()).isEmpty();
    }

    /** Taches des modeles de la ressource (et de toutes les ressources), une fois par reservation. */
    public int createBookingTasks(Booking booking) {
        if (tasks == null || booking.getResource() == null) {
            return 0;
        }
        return create(booking, templates.forBooking(booking.getResource()));
    }

    /** Taches d'un service vendu avec la reservation (servir le cafe). */
    public int createServiceTasks(Booking booking, String itemCode) {
        if (tasks == null || booking.getResource() == null || !StringUtils.hasText(itemCode)) {
            return 0;
        }
        return create(booking, templates.forService(booking.getResource(), itemCode.trim()));
    }

    private int create(Booking booking, List<ResourceTaskTemplate> found) {
        int created = 0;
        for (ResourceTaskTemplate template : found) {
            String title = template.getTitle() + " · " + booking.getResource().getName() + " · " + booking.getBookingNumber();
            if (taskItems != null && taskItems.existsBySourceTypeAndSourceCodeAndTitle(TASK_SOURCE, booking.getBookingNumber(), title)) {
                continue;
            }
            LocalDateTime due = template.getMoment() == ResourceTaskTemplate.Moment.AFTER_END
                    ? booking.getEndedAt().plusMinutes(template.getOffsetMinutes() == null ? 0 : template.getOffsetMinutes())
                    : booking.getStartedAt().minusMinutes(template.getOffsetMinutes() == null ? 0 : template.getOffsetMinutes());
            List<String> points = ResourceOperations.checklist(template);
            List<TaskDtos.ChecklistRequest> checklist = IntStream.range(0, points.size())
                    .mapToObj(i -> new TaskDtos.ChecklistRequest(points.get(i), false, i + 1)).toList();
            try {
                tasks.createFromAutomation(new TaskDtos.CreateTaskRequest(title,
                        description(template, booking), template.getAssignedTo(), priority(template.getPriority()),
                        due.atZone(ZONE).toInstant(), TASK_SOURCE, booking.getBookingNumber(), TaskRecurrence.NONE,
                        checklist.isEmpty() ? null : checklist));
                created++;
            } catch (RuntimeException ex) {
                log.warn("Reservation {} : tache « {} » non creee ({})", booking.getBookingNumber(), template.getTitle(), ex.getMessage());
            }
        }
        return created;
    }

    private static String description(ResourceTaskTemplate template, Booking booking) {
        String when = booking.getStartedAt().toLocalDate() + " " + booking.getStartedAt().toLocalTime() + " - " + booking.getEndedAt().toLocalTime();
        String who = booking.getContactName() == null ? "" : ", " + booking.getContactName();
        return (template.getDescription() == null ? "" : template.getDescription() + "\n")
                + "Réservation " + booking.getBookingNumber() + " : " + booking.getResource().getName() + ", " + when + who;
    }

    private static TaskPriority priority(String value) {
        try {
            return value == null ? TaskPriority.MEDIUM : TaskPriority.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return TaskPriority.MEDIUM;
        }
    }

    /** Taches ouvertes de la reservation annulees avec elle. */
    public int cancelTasks(Booking booking) {
        if (taskItems == null) {
            return 0;
        }
        int cancelled = 0;
        for (TaskItem item : taskItems.findAllBySourceTypeAndSourceCode(TASK_SOURCE, booking.getBookingNumber())) {
            if (item.getStatus() == TaskStatus.OPEN || item.getStatus() == TaskStatus.IN_PROGRESS) {
                item.setStatus(TaskStatus.CANCELLED);
                item.setUpdatedAt(Instant.now());
                taskItems.save(item);
                cancelled++;
            }
        }
        return cancelled;
    }

    // ------------------------------------------------------------------ reglement d'usage

    @Transactional(readOnly = true)
    public UsageRulesView usageRules(Booking booking) {
        Resource resource = booking.getResource();
        String rules = resource == null ? null : resource.getUsageRules();
        String version = version(rules);
        return new UsageRulesView(rules, version, booking.getUsageRulesAcceptedAt(),
                rules == null || (booking.getUsageRulesAcceptedAt() != null && version.equals(booking.getUsageRulesVersion())),
                resource == null || resource.getEndChecklist() == null ? List.of() : List.of(resource.getEndChecklist().split("\n")));
    }

    /** Acceptation du reglement en vigueur (sa version est gardee : un reglement modifie se re-accepte). */
    @Transactional
    public UsageRulesView acceptUsageRules(Booking booking, String actor) {
        String rules = booking.getResource() == null ? null : booking.getResource().getUsageRules();
        if (!StringUtils.hasText(rules)) {
            throw new ConflictException("La ressource n'a pas de règlement d'usage à accepter");
        }
        booking.setUsageRulesAcceptedAt(Instant.now());
        booking.setUsageRulesVersion(version(rules));
        bookings.save(booking);
        eventWriter.write(booking, BookingEventType.USAGE_RULES_ACCEPTED, "Règlement d'usage accepté",
                "Version " + booking.getUsageRulesVersion() + (actor == null ? "" : " par " + actor), null);
        return usageRules(booking);
    }

    static String version(String rules) {
        if (rules == null) {
            return "";
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(rules.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest).substring(0, 12);
        } catch (Exception ex) {
            return Integer.toHexString(rules.hashCode());
        }
    }

    // ------------------------------------------------------------------ etat des lieux

    @Transactional
    public EndReportResponse endReport(Booking booking, EndReportRequest request, String reporterType, String reporterCode,
                                       String reporterName, String email, String phone) {
        if (request == null || !StringUtils.hasText(request.condition())) {
            throw new BadRequestException("L'état de la ressource est requis (GOOD ou DAMAGED)");
        }
        String condition = request.condition().trim().toUpperCase(Locale.ROOT);
        if (!condition.equals("GOOD") && !condition.equals("DAMAGED")) {
            throw new BadRequestException("État invalide : GOOD ou DAMAGED");
        }
        if (booking.getStatus() != BookingStatus.IN_PROGRESS && booking.getStatus() != BookingStatus.COMPLETED
                && booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new ConflictException("Réservation " + booking.getBookingNumber() + " : l'état des lieux suit une utilisation");
        }
        List<ChecklistAnswer> answers = request.checklist() == null ? List.of() : request.checklist().stream()
                .filter(answer -> answer != null && StringUtils.hasText(answer.label())).toList();
        List<String> photos = request.photoDocumentCodes() == null ? List.of()
                : request.photoDocumentCodes().stream().filter(StringUtils::hasText).map(String::trim).toList();
        BookingEndReport report = BookingEndReport.builder()
                .booking(booking)
                .condition(condition)
                .checklist(answers.isEmpty() ? null : String.join("\n", answers.stream()
                        .map(answer -> answer.label().trim().replace("|", "/") + "|" + (Boolean.TRUE.equals(answer.done()) ? "oui" : "non"))
                        .toList()))
                .photoDocumentCodes(photos.isEmpty() ? null : String.join(",", photos))
                .notes(StringUtils.hasText(request.notes()) ? request.notes().trim() : null)
                .reportedByType("TEAM".equals(reporterType) ? "TEAM" : "MEMBER")
                .reportedBy(reporterName == null ? reporterCode : reporterName)
                .build();
        if (condition.equals("DAMAGED") && operations != null) {
            ResourceOperations.IncidentResponse incident = operations.report(new ResourceOperations.IncidentRequest(
                            booking.getResource().getCode(), booking.getBookingNumber(), "Ressource rendue abîmée",
                            report.getNotes(), "HIGH", false, false, photos),
                    new ResourceOperations.Reporter(report.getReportedByType(), reporterCode, reporterName, email, phone));
            report.setIncidentCode(incident.code());
        }
        report = reports.save(report);
        eventWriter.write(booking, BookingEventType.END_REPORT_RECORDED, "État des lieux de fin",
                condition + (report.getIncidentCode() == null ? "" : ", incident " + report.getIncidentCode()), null);
        return toResponse(report);
    }

    @Transactional(readOnly = true)
    public List<EndReportResponse> endReports(Booking booking) {
        return reports.findAllByBookingOrderByCreatedAtDesc(booking).stream().map(BookingOperations::toResponse).toList();
    }

    private static EndReportResponse toResponse(BookingEndReport report) {
        List<ChecklistAnswer> answers = new ArrayList<>();
        if (report.getChecklist() != null) {
            for (String line : report.getChecklist().split("\n")) {
                int bar = line.lastIndexOf('|');
                answers.add(bar < 0 ? new ChecklistAnswer(line, null) : new ChecklistAnswer(line.substring(0, bar), "oui".equals(line.substring(bar + 1))));
            }
        }
        return new EndReportResponse(report.getId(), report.getBooking().getBookingNumber(), report.getCondition(), answers,
                report.getPhotoDocumentCodes() == null ? List.of() : List.of(report.getPhotoDocumentCodes().split(",")),
                report.getNotes(), report.getReportedByType(), report.getReportedBy(), report.getIncidentCode(), report.getCreatedAt());
    }

    public Booking booking(String number) {
        if (!StringUtils.hasText(number)) {
            throw new BadRequestException("Le numéro de réservation est requis");
        }
        return bookings.findByBookingNumber(number.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Réservation introuvable : " + number.trim()));
    }
}
