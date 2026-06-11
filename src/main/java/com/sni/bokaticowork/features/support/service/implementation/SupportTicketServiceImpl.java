package com.sni.bokaticowork.features.support.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.richtext.RichTextSupport;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.client.customer.repository.CustomerRepository;
import com.sni.bokaticowork.features.client.member.repository.repo.MemberRepository;
import com.sni.bokaticowork.features.notification.dto.request.SendNotificationRequest;
import com.sni.bokaticowork.features.notification.enums.NotificationChannel;
import com.sni.bokaticowork.features.notification.enums.NotificationRecipientType;
import com.sni.bokaticowork.features.notification.service.interfaces.NotificationService;
import com.sni.bokaticowork.features.support.dto.SupportDtos.*;
import com.sni.bokaticowork.features.support.enums.*;
import com.sni.bokaticowork.features.support.mapper.SupportTicketMapper;
import com.sni.bokaticowork.features.support.model.SupportRoutingRule;
import com.sni.bokaticowork.features.support.model.SupportTag;
import com.sni.bokaticowork.features.support.model.SupportTicket;
import com.sni.bokaticowork.features.support.model.SupportTicketTag;
import com.sni.bokaticowork.features.support.model.TicketMessage;
import com.sni.bokaticowork.features.support.repository.SupportTagRepository;
import com.sni.bokaticowork.features.support.repository.SupportTicketEventRepository;
import com.sni.bokaticowork.features.support.repository.SupportTicketRepository;
import com.sni.bokaticowork.features.support.repository.SupportTicketTagRepository;
import com.sni.bokaticowork.features.support.repository.TicketAttachmentRepository;
import com.sni.bokaticowork.features.support.repository.TicketMessageRepository;
import com.sni.bokaticowork.features.support.service.interfaces.SupportEmailService;
import com.sni.bokaticowork.features.support.service.interfaces.SupportTicketService;
import com.sni.bokaticowork.features.task.dto.TaskDtos.CreateTaskRequest;
import com.sni.bokaticowork.features.task.dto.TaskDtos.TaskResponse;
import com.sni.bokaticowork.features.task.enums.TaskRecurrence;
import com.sni.bokaticowork.features.task.service.interfaces.TaskManagementService;
import com.sni.bokaticowork.security.admin.user.model.UserPrincipal;
import com.sni.bokaticowork.security.admin.user.model.Users;
import com.sni.bokaticowork.security.admin.user.service.interfaces.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class SupportTicketServiceImpl implements SupportTicketService {

    private final SupportTicketRepository ticketRepository;
    private final TicketMessageRepository messageRepository;
    private final TicketAttachmentRepository attachmentRepository;
    private final SupportTagRepository tagRepository;
    private final SupportTicketTagRepository ticketTagRepository;
    private final SupportRoutingRuleServiceImpl routingRuleService;
    private final SupportTicketEventWriter eventWriter;
    private final SupportTicketEventRepository eventRepository;
    private final SupportTicketMapper mapper;
    private final RichTextSupport richTextSupport;
    private final UserService userService;
    private final CustomerRepository customerRepository;
    private final MemberRepository memberRepository;
    @Lazy private final NotificationService notificationService;
    @Lazy private final SupportEmailService emailService;
    @Lazy private final TaskManagementService taskManagementService;

    // ── Création ─────────────────────────────────────────────────

    @Override
    public SupportTicketResponse create(CreateTicketRequest request) {
        if (request == null || !StringUtils.hasText(request.title())) {
            throw new BadRequestException("Ticket title is required");
        }
        assertOwnerNotSuppressed(request.ownerType(), request.ownerCode());
        TicketPriority priority = request.priority() == null ? TicketPriority.MEDIUM : request.priority();
        SupportTicket ticket = SupportTicket.builder()
                .ticketNumber("TCK-" + Instant.now().toEpochMilli())
                .title(request.title().trim())
                .description(richTextSupport.normalize(request.description()))
                .status(TicketStatus.OPEN)
                .priority(priority)
                .category(request.category() == null ? TicketCategory.OTHER : request.category())
                .ownerType(request.ownerType())
                .ownerCode(request.ownerCode())
                .contactName(request.contactName())
                .contactEmail(request.contactEmail())
                .contactPhone(request.contactPhone())
                .relatedType(request.relatedType())
                .relatedCode(request.relatedCode())
                .firstResponseDueAt(Instant.now().plus(firstResponseHours(priority), ChronoUnit.HOURS))
                .resolutionDueAt(Instant.now().plus(resolutionHours(priority), ChronoUnit.HOURS))
                .build();
        SupportTicket saved = ticketRepository.save(ticket);
        eventWriter.write(saved, TicketEventType.TICKET_CREATED,
                StringUtils.hasText(saved.getOwnerType()) ? saved.getOwnerType() : "CLIENT",
                saved.getOwnerCode(),
                "Ticket créé (" + saved.getCategory() + " / " + saved.getPriority() + ")");
        applyRoutingRule(saved);
        if (StringUtils.hasText(request.description())) {
            saveMessage(saved, TicketSenderType.CLIENT, request.ownerCode(),
                    request.contactName(), request.description(), false, null);
        }
        SupportTicketResponse response = get(saved.getTicketNumber());
        emailService.sendTicketCreated(saved);
        return response;
    }

    @Override
    public SupportTicketResponse createFromAutomation(CreateTicketRequest request) {
        if (request != null && StringUtils.hasText(request.relatedType()) && StringUtils.hasText(request.relatedCode())
                && ticketRepository.existsByRelatedTypeAndRelatedCodeAndStatusIn(
                        request.relatedType(), request.relatedCode(), List.of(TicketStatus.OPEN, TicketStatus.IN_PROGRESS, TicketStatus.WAITING_CLIENT))) {
            return null;
        }
        return create(request);
    }

    // ── Recherche ─────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<SupportTicketResponse> search(TicketStatus status, Long assignedTo,
                                                           String ownerType, String ownerCode,
                                                           TicketCategory category, TicketPriority priority,
                                                           String relatedType, String relatedCode,
                                                           Boolean overdueOnly,
                                                           String searchText, Pageable pageable) {
        String text = StringUtils.hasText(searchText) ? searchText.trim() : null;
        Pageable unsortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        return new PaginatedResponse<>(
                ticketRepository.search(
                                status != null ? status.name() : null,
                                assignedTo,
                                ownerType, ownerCode,
                                category != null ? category.name() : null,
                                priority != null ? priority.name() : null,
                                StringUtils.hasText(relatedType) ? relatedType.trim() : null,
                                StringUtils.hasText(relatedCode) ? relatedCode.trim() : null,
                                Boolean.TRUE.equals(overdueOnly),
                                Instant.now(),
                                text, unsortedPageable)
                        .map(this::toResponse));
    }

    // ── Lecture ───────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public SupportTicketResponse get(String ticketNumber) {
        SupportTicket ticket = getTicket(ticketNumber);
        return toResponse(ticket);
    }

    // ── Assignation ───────────────────────────────────────────────

    @Override
    public SupportTicketResponse assign(String ticketNumber, AssignTicketRequest request) {
        SupportTicket ticket = getTicket(ticketNumber);
        Long previousAgent = ticket.getAssignedTo();
        ticket.setAssignedTo(resolveAssignedTo(request));
        if (ticket.getStatus() == TicketStatus.OPEN && ticket.getAssignedTo() != null) {
            ticket.setStatus(TicketStatus.IN_PROGRESS);
        }
        SupportTicket saved = ticketRepository.save(ticket);
        if (!java.util.Objects.equals(saved.getAssignedTo(), previousAgent)) {
            String description = saved.getAssignedTo() != null
                    ? "Ticket assigné à l'agent #" + saved.getAssignedTo()
                    : "Ticket désassigné";
            eventWriter.write(saved, TicketEventType.ASSIGNED, "AGENT", currentUserId(), description);
        }
        if (saved.getAssignedTo() != null && !saved.getAssignedTo().equals(previousAgent)) {
            emailService.sendClientMessage(saved, null);
        }
        return toResponse(saved);
    }

    // ── Changement de statut ──────────────────────────────────────

    @Override
    public SupportTicketResponse updateStatus(String ticketNumber, UpdateTicketStatusRequest request) {
        if (request == null || request.status() == null) {
            throw new BadRequestException("Ticket status is required");
        }
        SupportTicket ticket = getTicket(ticketNumber);
        TicketStatus previous = ticket.getStatus();
        ticket.setStatus(request.status());
        if (request.status() == TicketStatus.RESOLVED && ticket.getResolvedAt() == null) {
            ticket.setResolvedAt(Instant.now());
        }
        if (request.status() == TicketStatus.CLOSED && ticket.getClosedAt() == null) {
            ticket.setClosedAt(Instant.now());
        }
        SupportTicket saved = ticketRepository.save(ticket);
        if (previous != saved.getStatus()) {
            eventWriter.write(saved, TicketEventType.STATUS_CHANGED, "AGENT", currentUserId(),
                    "Statut changé de " + previous + " à " + saved.getStatus());
        }
        if (previous != TicketStatus.RESOLVED && saved.getStatus() == TicketStatus.RESOLVED) {
            emailService.sendTicketResolved(saved);
        }
        return toResponse(saved);
    }

    // ── Ajout de message ──────────────────────────────────────────

    @Override
    public SupportTicketResponse addMessage(String ticketNumber, AddTicketMessageRequest request) {
        if (request == null || !StringUtils.hasText(request.content())) {
            throw new BadRequestException("Message is required");
        }
        SupportTicket ticket = getTicket(ticketNumber);
        TicketSenderType senderType = request.senderType() == null ? TicketSenderType.AGENT : request.senderType();
        boolean isInternal = Boolean.TRUE.equals(request.internal());

        boolean wasClosedBeforeMessage = senderType == TicketSenderType.CLIENT
                && ticket.getStatus() == TicketStatus.CLOSED;

        TicketMessage message = saveMessage(ticket, senderType, request.senderId(),
                request.senderName(), request.content(), isInternal, null);

        eventWriter.write(ticket, TicketEventType.MESSAGE_ADDED,
                senderType.name(), request.senderId(),
                (isInternal ? "Note interne ajoutée" : "Message ajouté") + " par " + senderType);

        // #2 WAITING_CLIENT: agent replied publicly → waiting for client response
        if (senderType == TicketSenderType.AGENT) {
            if (ticket.getFirstRespondedAt() == null) {
                ticket.setFirstRespondedAt(Instant.now());
            }
            TicketStatus current = ticket.getStatus();
            if (!isInternal && (current == TicketStatus.OPEN || current == TicketStatus.IN_PROGRESS)) {
                ticket.setStatus(TicketStatus.WAITING_CLIENT);
            }
            ticketRepository.save(ticket);
        }

        if (senderType == TicketSenderType.CLIENT) {
            if (wasClosedBeforeMessage) {
                ticket.setStatus(ticket.getAssignedTo() != null ? TicketStatus.IN_PROGRESS : TicketStatus.OPEN);
                ticket.setClosedAt(null);
                ticket.setResolvedAt(null);
                ticketRepository.save(ticket);
                eventWriter.write(ticket, TicketEventType.REOPENED, "CLIENT", request.senderId(),
                        "Ticket réouvert suite à une réponse client");
                emailService.sendTicketReopened(ticket);
            } else {
                // Client replied (including WAITING_CLIENT → IN_PROGRESS)
                ticket.setStatus(TicketStatus.IN_PROGRESS);
                ticketRepository.save(ticket);
            }
        }

        if (!isInternal) {
            if (senderType == TicketSenderType.AGENT) {
                emailService.sendAgentMessage(ticket, message);
            } else if (senderType == TicketSenderType.CLIENT) {
                if (wasClosedBeforeMessage) {
                    emailService.sendTicketReopenedToAgent(ticket, message);
                } else {
                    emailService.sendClientMessage(ticket, message);
                }
                notifyAgentClientReplied(ticket, message);
            }
        }

        return get(ticketNumber);
    }

    // ── Clôture ───────────────────────────────────────────────────

    @Override
    public SupportTicketResponse close(String ticketNumber) {
        return updateStatus(ticketNumber, new UpdateTicketStatusRequest(TicketStatus.CLOSED));
    }

    // ── CSAT ─────────────────────────────────────────────────────

    @Override
    public SupportTicketResponse submitCsat(String ticketNumber, SubmitCsatRequest request) {
        if (request == null || request.score() == null) {
            throw new BadRequestException("CSAT score is required");
        }
        if (request.score() < 1 || request.score() > 5) {
            throw new BadRequestException("CSAT score must be between 1 and 5");
        }
        SupportTicket ticket = getTicket(ticketNumber);
        if (ticket.getStatus() != TicketStatus.CLOSED && ticket.getStatus() != TicketStatus.RESOLVED) {
            throw new BadRequestException("CSAT can only be submitted for resolved or closed tickets");
        }
        if (ticket.getCsatSubmittedAt() != null) {
            throw new BadRequestException("CSAT already submitted for this ticket");
        }
        ticket.setCsatScore(request.score());
        ticket.setCsatComment(request.comment());
        ticket.setCsatSubmittedAt(Instant.now());
        SupportTicket saved = ticketRepository.save(ticket);
        eventWriter.write(saved, TicketEventType.CSAT_SUBMITTED, "CLIENT", saved.getOwnerCode(),
                "Score CSAT soumis : " + request.score() + "/5");
        return toResponse(saved);
    }

    // ── Tags ──────────────────────────────────────────────────────

    @Override
    public SupportTicketResponse addTag(String ticketNumber, AddTagRequest request) {
        if (request == null || !StringUtils.hasText(request.tag())) {
            throw new BadRequestException("Le tag est requis");
        }
        SupportTicket ticket = getTicket(ticketNumber);
        SupportTag tag = resolveOrCreateTag(request.tag());
        if (!ticketTagRepository.existsByTicketAndTag(ticket, tag)) {
            ticketTagRepository.save(SupportTicketTag.builder().ticket(ticket).tag(tag).build());
        }
        return toResponse(ticket);
    }

    @Override
    public SupportTicketResponse removeTag(String ticketNumber, String tag) {
        if (!StringUtils.hasText(tag)) {
            throw new BadRequestException("Le tag est requis");
        }
        SupportTicket ticket = getTicket(ticketNumber);
        tagRepository.findByNameIgnoreCase(normalizeTag(tag))
                .flatMap(existing -> ticketTagRepository.findByTicketAndTag(ticket, existing))
                .ifPresent(ticketTagRepository::delete);
        return toResponse(ticket);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TagResponse> listTags() {
        return tagRepository.findAllByActiveTrueOrderByNameAsc().stream()
                .map(mapper::toTagResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketEventResponse> getTimeline(String ticketNumber) {
        SupportTicket ticket = getTicket(ticketNumber);
        return eventRepository.findAllByTicketOrderByCreatedAtAsc(ticket).stream()
                .map(mapper::toEventResponse)
                .toList();
    }

    @Override
    public TaskResponse createTask(String ticketNumber, CreateTicketTaskRequest request) {
        SupportTicket ticket = getTicket(ticketNumber);
        TaskResponse task = taskManagementService.create(new CreateTaskRequest(
                request.title(),
                request.description(),
                request.assignedTo(),
                request.priority(),
                request.dueAt(),
                "SUPPORT_TICKET",
                ticket.getTicketNumber(),
                TaskRecurrence.NONE,
                request.checklist()
        ));
        eventWriter.write(ticket, TicketEventType.TASK_CREATED, "AGENT", currentUserId(),
                "Tâche créée à partir du ticket : « " + task.title() + " » (#" + task.id() + ")");
        return task;
    }

    private SupportTag resolveOrCreateTag(String raw) {
        String normalized = normalizeTag(raw);
        return tagRepository.findByNameIgnoreCase(normalized)
                .orElseGet(() -> tagRepository.save(SupportTag.builder().name(normalized).active(true).build()));
    }

    private String normalizeTag(String raw) {
        String normalized = raw.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", "-");
        if (!StringUtils.hasText(normalized)) {
            throw new BadRequestException("Le tag est requis");
        }
        return normalized;
    }

    // ── Métriques ─────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public SupportMetricsResponse metrics() {
        Instant now = Instant.now();
        return new SupportMetricsResponse(
                ticketRepository.countOpen(),
                ticketRepository.countOverdueFirstResponse(now),
                ticketRepository.countOverdueResolution(now),
                ticketRepository.countResolved()
        );
    }

    // ── Analytics ─────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public SupportAnalyticsResponse analytics(Instant from, Instant to) {
        if (from == null) from = Instant.now().minus(30, ChronoUnit.DAYS);
        if (to == null) to = Instant.now();

        Instant now = Instant.now();
        Map<String, Long> byCategory = toMap(ticketRepository.countByCategoryBetween(from, to));
        Map<String, Long> byPriority = toMap(ticketRepository.countByPriorityBetween(from, to));
        Map<String, Long> topTags = toMap(ticketTagRepository.topTagsBetween(from, to));
        Map<String, Long> volumeByRelatedType = toMap(ticketRepository.volumeByRelatedTypeBetween(from, to));
        Map<String, Long> backlogByAgent = toMap(ticketRepository.currentBacklogByAgent());
        Map<String, Double> csatByCategory = toDoubleMap(ticketRepository.csatByCategoryBetween(from, to));
        Map<String, Double> csatByAgent = toDoubleMap(ticketRepository.csatByAgentBetween(from, to));

        List<AgentWorkloadEntry> agentWorkload = ticketRepository.agentWorkloadBetween(from, to)
                .stream()
                .map(row -> new AgentWorkloadEntry(
                        ((Number) row[0]).longValue(),
                        ((Number) row[1]).longValue(),
                        ((Number) row[2]).longValue()))
                .toList();

        Double avgFirstResponse = ticketRepository.avgFirstResponseHoursBetween(from, to);
        Double avgResolution = ticketRepository.avgResolutionHoursBetween(from, to);
        Double avgCsat = ticketRepository.avgCsatScoreBetween(from, to);

        long totalCreated = ticketRepository.countCreatedBetween(from, to);
        long slaBreachCount = ticketRepository.countSlaBreachesBetween(now, from, to);
        double slaBreachRate = totalCreated > 0 ? Math.round((slaBreachCount * 100.0 / totalCreated) * 10.0) / 10.0 : 0.0;

        return new SupportAnalyticsResponse(
                from, to,
                totalCreated,
                ticketRepository.countResolvedBetween(from, to),
                ticketRepository.countOpenAt(to),
                slaBreachCount,
                slaBreachRate,
                avgFirstResponse != null ? Math.round(avgFirstResponse * 10.0) / 10.0 : 0.0,
                avgResolution != null ? Math.round(avgResolution * 10.0) / 10.0 : 0.0,
                eventRepository.countByEventTypeBetween(TicketEventType.REOPENED, from, to),
                ticketRepository.countByStatus(TicketStatus.WAITING_CLIENT),
                byCategory,
                byPriority,
                topTags,
                volumeByRelatedType,
                backlogByAgent,
                avgCsat != null ? Math.round(avgCsat * 10.0) / 10.0 : 0.0,
                ticketRepository.countCsatResponsesBetween(from, to),
                csatByCategory,
                csatByAgent,
                agentWorkload
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgentPerformanceEntry> agentPerformance(Instant from, Instant to) {
        if (from == null) from = Instant.now().minus(30, ChronoUnit.DAYS);
        if (to == null) to = Instant.now();
        return ticketRepository.agentPerformanceBetween(from, to).stream()
                .map(row -> new AgentPerformanceEntry(
                        ((Number) row[0]).longValue(),
                        ((Number) row[1]).longValue(),
                        ((Number) row[2]).longValue(),
                        roundOrZero((Number) row[3]),
                        roundOrZero((Number) row[4]),
                        row[5] == null ? null : Math.round(((Number) row[5]).doubleValue() * 10.0) / 10.0))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public String analyticsCsv(Instant from, Instant to) {
        SupportAnalyticsResponse analytics = analytics(from, to);
        StringBuilder csv = new StringBuilder();
        csv.append("Rapport analytics support\n");
        csv.append("Periode:,").append(analytics.from()).append(",a,").append(analytics.to()).append('\n');
        csv.append('\n');
        csv.append("Indicateur,Valeur\n");
        csv.append("Tickets crees,").append(analytics.totalCreated()).append('\n');
        csv.append("Tickets resolus,").append(analytics.totalResolved()).append('\n');
        csv.append("Ouverts en fin de periode,").append(analytics.openAtEndOfPeriod()).append('\n');
        csv.append("Violations SLA,").append(analytics.slaBreachCount()).append('\n');
        csv.append("Taux de violation SLA (%),").append(analytics.slaBreachRate()).append('\n');
        csv.append("Temps moyen 1ere reponse (h),").append(analytics.avgFirstResponseHours()).append('\n');
        csv.append("Temps moyen resolution (h),").append(analytics.avgResolutionHours()).append('\n');
        csv.append("Tickets reouverts,").append(analytics.reopenedCount()).append('\n');
        csv.append("En attente client (actuel),").append(analytics.waitingClientCount()).append('\n');
        csv.append("CSAT moyen,").append(analytics.avgCsatScore()).append('\n');
        csv.append("Reponses CSAT,").append(analytics.csatResponseCount()).append('\n');
        csv.append('\n');
        appendMapSection(csv, "Par categorie", analytics.byCategory());
        appendMapSection(csv, "Par priorite", analytics.byPriority());
        appendMapSection(csv, "Top tags", analytics.topTags());
        appendMapSection(csv, "Volume par type lie", analytics.volumeByRelatedType());
        appendMapSection(csv, "Backlog par agent (id)", analytics.backlogByAgent());
        appendDoubleMapSection(csv, "CSAT moyen par categorie", analytics.csatByCategory());
        appendDoubleMapSection(csv, "CSAT moyen par agent (id)", analytics.csatByAgent());
        return csv.toString();
    }

    private void appendMapSection(StringBuilder csv, String title, Map<String, Long> values) {
        if (values == null || values.isEmpty()) return;
        csv.append(escapeCsv(title)).append('\n');
        values.forEach((key, value) -> csv.append(escapeCsv(key)).append(',').append(value).append('\n'));
        csv.append('\n');
    }

    private void appendDoubleMapSection(StringBuilder csv, String title, Map<String, Double> values) {
        if (values == null || values.isEmpty()) return;
        csv.append(escapeCsv(title)).append('\n');
        values.forEach((key, value) -> csv.append(escapeCsv(key)).append(',').append(value).append('\n'));
        csv.append('\n');
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private double roundOrZero(Number value) {
        return value != null ? Math.round(value.doubleValue() * 10.0) / 10.0 : 0.0;
    }

    // ── Client 360 / owner summary ────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public OwnerTicketSummaryResponse ownerSummary(String ownerType, String ownerCode) {
        if (!StringUtils.hasText(ownerType) || !StringUtils.hasText(ownerCode)) {
            throw new BadRequestException("ownerType et ownerCode sont requis");
        }
        long openTickets = ticketRepository.countOpenByOwner(ownerType, ownerCode);
        long slaBreaches = ticketRepository.countSlaBreachesByOwner(ownerType, ownerCode, Instant.now());
        Double avgCsat = ticketRepository.avgCsatScoreByOwner(ownerType, ownerCode);
        Double roundedCsat = avgCsat != null ? Math.round(avgCsat * 10.0) / 10.0 : null;
        boolean atRisk = openTickets >= 3 || slaBreaches > 0 || (roundedCsat != null && roundedCsat < 3.0);
        List<SupportTicketResponse> recentTickets = ticketRepository
                .findTop5ByOwnerTypeAndOwnerCodeOrderByCreatedAtDesc(ownerType, ownerCode).stream()
                .map(this::toResponse)
                .toList();
        return new OwnerTicketSummaryResponse(ownerType, ownerCode, openTickets, slaBreaches, roundedCsat, atRisk, recentTickets);
    }

    // ── Helpers ───────────────────────────────────────────────────

    private SupportTicketResponse toResponse(SupportTicket ticket) {
        return mapper.toResponse(ticket,
                messageRepository.findAllByTicketOrderByCreatedAtAsc(ticket),
                attachmentRepository.findAllByTicketAndActiveTrueOrderByCreatedAtAsc(ticket),
                ticketTagRepository.findAllByTicketOrderByCreatedAtAsc(ticket));
    }

    private SupportTicket getTicket(String ticketNumber) {
        if (!StringUtils.hasText(ticketNumber)) throw new BadRequestException("Ticket number is required");
        return ticketRepository.findByTicketNumber(ticketNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Support ticket not found: " + ticketNumber));
    }

    private TicketMessage saveMessage(SupportTicket ticket, TicketSenderType senderType,
                                      String senderId, String senderName,
                                      String message, boolean internal) {
        return saveMessage(ticket, senderType, senderId, senderName, message, internal, null);
    }

    private TicketMessage saveMessage(SupportTicket ticket, TicketSenderType senderType,
                                      String senderId, String senderName,
                                      String message, boolean internal, String externalMessageId) {
        return messageRepository.save(TicketMessage.builder()
                .ticket(ticket)
                .senderType(senderType)
                .senderId(senderId)
                .senderName(senderName)
                .message(richTextSupport.normalize(message))
                .internal(internal)
                .externalMessageId(externalMessageId)
                .build());
    }

    // ── Email reply (inbound, with deduplication) ─────────────────

    @Override
    public SupportTicketResponse addEmailReply(String ticketNumber, String graphMessageId,
                                               String senderEmail, String senderName, String content) {
        if (!StringUtils.hasText(content)) throw new BadRequestException("Message content is required");
        if (StringUtils.hasText(graphMessageId) && messageRepository.existsByExternalMessageId(graphMessageId)) {
            return get(ticketNumber);
        }
        SupportTicket ticket = getTicket(ticketNumber);
        boolean wasClosedBeforeMessage = ticket.getStatus() == TicketStatus.CLOSED;

        TicketMessage message = saveMessage(ticket, TicketSenderType.CLIENT,
                senderEmail, senderName, content, false, graphMessageId);

        eventWriter.write(ticket, TicketEventType.MESSAGE_ADDED, "CLIENT", senderEmail,
                "Message reçu par email entrant");

        if (wasClosedBeforeMessage) {
            ticket.setStatus(ticket.getAssignedTo() != null ? TicketStatus.IN_PROGRESS : TicketStatus.OPEN);
            ticket.setClosedAt(null);
            ticket.setResolvedAt(null);
            ticketRepository.save(ticket);
            eventWriter.write(ticket, TicketEventType.REOPENED, "CLIENT", senderEmail,
                    "Ticket réouvert suite à une réponse client par email");
            emailService.sendTicketReopened(ticket);
            emailService.sendTicketReopenedToAgent(ticket, message);
        } else {
            ticket.setStatus(TicketStatus.IN_PROGRESS);
            ticketRepository.save(ticket);
            emailService.sendClientMessage(ticket, message);
        }
        notifyAgentClientReplied(ticket, message);

        return get(ticketNumber);
    }

    private Long resolveAssignedTo(AssignTicketRequest request) {
        if (request == null || !StringUtils.hasText(request.assignedTo())) {
            return null;
        }
        String value = request.assignedTo().trim();
        if (value.matches("\\d+")) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException ex) {
                throw new BadRequestException("Assigned agent id is invalid");
            }
        }
        if (!value.contains("@")) {
            throw new BadRequestException("Assigned agent must be a user id or email");
        }
        Users user = userService.getUserByEmailForService(value);
        return user.getId();
    }

    private String currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return String.valueOf(principal.getUser().getId());
        }
        return null;
    }

    private Map<String, Long> toMap(List<Object[]> rows) {
        Map<String, Long> result = new LinkedHashMap<>();
        for (Object[] row : rows) {
            result.put(row[0].toString(), ((Number) row[1]).longValue());
        }
        return result;
    }

    private Map<String, Double> toDoubleMap(List<Object[]> rows) {
        Map<String, Double> result = new LinkedHashMap<>();
        for (Object[] row : rows) {
            if (row[0] == null || row[1] == null) continue;
            result.put(row[0].toString(), Math.round(((Number) row[1]).doubleValue() * 10.0) / 10.0);
        }
        return result;
    }

    private void assertOwnerNotSuppressed(String ownerType, String ownerCode) {
        if (!StringUtils.hasText(ownerType) || !StringUtils.hasText(ownerCode)) return;
        switch (ownerType.trim().toUpperCase()) {
            case "MEMBER" -> {
                if (!memberRepository.existsByMemberIdAndDeletedFalse(ownerCode.trim())) {
                    throw new BadRequestException("Le membre " + ownerCode + " est supprimé ou introuvable et ne peut pas ouvrir de ticket");
                }
            }
            case "CUSTOMER" -> {
                if (!customerRepository.existsByCustomerIdAndDeletedFalse(ownerCode.trim())) {
                    throw new BadRequestException("Le client " + ownerCode + " est supprimé ou introuvable et ne peut pas ouvrir de ticket");
                }
            }
        }
    }

    private long firstResponseHours(TicketPriority priority) {
        return switch (priority) { case URGENT -> 2; case HIGH -> 8; case MEDIUM -> 24; case LOW -> 48; };
    }

    private long resolutionHours(TicketPriority priority) {
        return switch (priority) { case URGENT -> 8; case HIGH -> 24; case MEDIUM -> 72; case LOW -> 120; };
    }

    // ── Routage automatique ────────────────────────────────────────

    private void applyRoutingRule(SupportTicket ticket) {
        if (ticket.getAssignedTo() != null) return;
        routingRuleService.matchRule(ticket).ifPresent(rule -> {
            if (rule.getAssignedTo() == null) return;
            ticket.setAssignedTo(rule.getAssignedTo());
            ticket.setStatus(TicketStatus.IN_PROGRESS);
            ticketRepository.save(ticket);
            eventWriter.writeSystem(ticket, TicketEventType.ASSIGNED,
                    "Assigné automatiquement à l'agent #" + rule.getAssignedTo() + " via la règle « " + rule.getName() + " »");
            notifyAgentTicketRouted(ticket, rule);
        });
    }

    private void notifyAgentTicketRouted(SupportTicket ticket, SupportRoutingRule rule) {
        Long assignedTo = ticket.getAssignedTo();
        if (assignedTo == null) return;
        String ticketNumber = ticket.getTicketNumber();
        String title = ticket.getTitle();
        String ruleName = rule.getName();

        runAfterCommit(() -> {
            try {
                String agentEmail;
                try {
                    agentEmail = userService.getUserByIdForService(assignedTo).getEmail();
                } catch (Exception ex) {
                    log.warn("Cannot resolve agent email for routing notification (assignedTo={}): {}", assignedTo, ex.getMessage());
                    return;
                }
                notificationService.send(new SendNotificationRequest(
                        "SUPPORT_TICKET_ROUTED",
                        "SUPPORT_TICKET",
                        ticketNumber,
                        NotificationChannel.IN_APP,
                        NotificationRecipientType.ADMIN,
                        null,
                        agentEmail,
                        null,
                        "Nouveau ticket assigné — " + ticketNumber,
                        null,
                        null,
                        Map.of(
                                "ticketNumber", ticketNumber,
                                "title", title != null ? title : "",
                                "ruleName", ruleName != null ? ruleName : ""
                        ),
                        null
                ));
            } catch (Exception ex) {
                log.warn("Could not send IN_APP notification for ticket routing on {}: {}", ticketNumber, ex.getMessage());
            }
        });
    }

    private void notifyAgentClientReplied(SupportTicket ticket, TicketMessage message) {
        if (ticket.getAssignedTo() == null) {
            return;
        }
        // Snapshot immutable values now (before commit), resolve agent email AFTER commit
        // to avoid polluting the current transaction with any exception from userService
        Long assignedTo      = ticket.getAssignedTo();
        String ticketNumber  = ticket.getTicketNumber();
        String ownerCode     = ticket.getOwnerCode() != null ? ticket.getOwnerCode() : "";
        String senderName    = StringUtils.hasText(message.getSenderName())
                ? message.getSenderName()
                : StringUtils.hasText(ticket.getContactName()) ? ticket.getContactName() : ownerCode;
        String rawPreview    = message.getMessage();
        String preview       = rawPreview == null ? ""
                : rawPreview.length() > 120 ? rawPreview.substring(0, 120) + "…" : rawPreview;

        runAfterCommit(() -> {
            try {
                String agentEmail;
                try {
                    agentEmail = userService.getUserByIdForService(assignedTo).getEmail();
                } catch (Exception ex) {
                    log.warn("Cannot resolve agent email for IN_APP notification (assignedTo={}): {}", assignedTo, ex.getMessage());
                    return;
                }
                notificationService.send(new SendNotificationRequest(
                        "SUPPORT_CLIENT_REPLY",
                        "SUPPORT_TICKET",
                        ticketNumber,
                        NotificationChannel.IN_APP,
                        NotificationRecipientType.ADMIN,
                        null,
                        agentEmail,
                        null,
                        "Réponse client — " + ticketNumber,
                        null,
                        null,
                        Map.of(
                                "ticketNumber", ticketNumber,
                                "senderName",   senderName != null ? senderName : "",
                                "ownerCode",    ownerCode,
                                "preview",      preview
                        ),
                        null
                ));
            } catch (Exception ex) {
                log.warn("Could not send IN_APP notification for client reply on {}: {}", ticketNumber, ex.getMessage());
            }
        });
    }

    private void runAfterCommit(Runnable task) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
        } else {
            task.run();
        }
    }
}
