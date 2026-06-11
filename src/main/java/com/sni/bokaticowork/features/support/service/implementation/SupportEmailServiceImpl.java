package com.sni.bokaticowork.features.support.service.implementation;

import com.sni.bokaticowork.core.communication.mailService.baseService.DefaultEmailSender;
import com.sni.bokaticowork.core.richtext.RichTextSupport;
import com.sni.bokaticowork.features.support.model.SupportTicket;
import com.sni.bokaticowork.features.support.model.TicketMessage;
import com.sni.bokaticowork.features.support.service.interfaces.SupportEmailService;
import com.sni.bokaticowork.security.admin.user.service.interfaces.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class  SupportEmailServiceImpl implements SupportEmailService {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of("Africa/Brazzaville"));

    private final DefaultEmailSender emailSender;
    private final UserService userService;
    private final RichTextSupport richTextSupport;
    private final SpringTemplateEngine templateEngine;
    private final CsatTokenService csatTokenService;

    @Value("${bokati.support.sender-email:supportela@elleaose.com}")
    private String supportEmail;

    @Value("${bokati.support.manager-email:supportela@elleaose.com}")
    private String managerEmail;

    @Value("${bokati.support.admin-email:supportela@elleaose.com}")
    private String adminEmail;

    @Value("${app.api-base-url:https://api.elleaose.com}")
    private String apiBaseUrl;

    @Override
    @Async
    public void sendTicketCreated(SupportTicket ticket) {
        if (!StringUtils.hasText(ticket.getContactEmail())) return;
        Context ctx = base(ticket, "TICKET_CREATED");
        ctx.setVariable("messageContentHtml", richTextSupport.toSafeHtml(ticket.getDescription()));
        send(ticket.getContactEmail(),
                "Ticket ouvert — " + ticket.getTicketNumber(),
                render(ctx), ticket.getTicketNumber());
    }

    @Override
    @Async
    public void sendAgentMessage(SupportTicket ticket, TicketMessage message) {
        if (!StringUtils.hasText(ticket.getContactEmail())) return;
        String agentName = message != null && StringUtils.hasText(message.getSenderName())
                ? message.getSenderName()
                : "Notre équipe";
        Context ctx = base(ticket, "AGENT_MESSAGE");
        ctx.setVariable("agentName", agentName);
        ctx.setVariable("messageContentHtml", richTextSupport.toSafeHtml(message == null ? null : message.getMessage()));
        send(ticket.getContactEmail(),
                "Réponse sur votre ticket " + ticket.getTicketNumber(),
                render(ctx), ticket.getTicketNumber());
    }

    @Override
    @Async
    public void sendClientMessage(SupportTicket ticket, TicketMessage message) {
        String agentEmail = resolveAgentEmail(ticket.getAssignedTo());
        if (!StringUtils.hasText(agentEmail)) agentEmail = managerEmail;
        String clientName = message != null && StringUtils.hasText(message.getSenderName())
                ? message.getSenderName()
                : ticket.getContactName();
        Context ctx = base(ticket, "CLIENT_MESSAGE");
        ctx.setVariable("clientName", clientName);
        ctx.setVariable("messageContentHtml", richTextSupport.toSafeHtml(message == null ? null : message.getMessage()));
        send(agentEmail,
                "[Support] Nouveau message client — " + ticket.getTicketNumber(),
                render(ctx), ticket.getTicketNumber());
    }

    @Override
    @Async
    public void sendTicketResolved(SupportTicket ticket) {
        if (!StringUtils.hasText(ticket.getContactEmail())) return;
        Context ctx = base(ticket, "TICKET_RESOLVED");
        send(ticket.getContactEmail(),
                "Ticket résolu — " + ticket.getTicketNumber(),
                render(ctx), ticket.getTicketNumber());
    }

    @Override
    @Async
    public void sendTicketReopened(SupportTicket ticket) {
        if (!StringUtils.hasText(ticket.getContactEmail())) return;
        Context ctx = base(ticket, "TICKET_REOPENED");
        send(ticket.getContactEmail(),
                "Ticket réouvert — " + ticket.getTicketNumber(),
                render(ctx), ticket.getTicketNumber());
    }

    @Override
    @Async
    public void sendTicketReopenedToAgent(SupportTicket ticket, TicketMessage message) {
        String agentEmail = resolveAgentEmail(ticket.getAssignedTo());
        if (!StringUtils.hasText(agentEmail)) agentEmail = managerEmail;
        String clientName = message != null && StringUtils.hasText(message.getSenderName())
                ? message.getSenderName()
                : ticket.getContactName();
        Context ctx = base(ticket, "TICKET_REOPENED_AGENT");
        ctx.setVariable("clientName", clientName);
        ctx.setVariable("messageContentHtml", richTextSupport.toSafeHtml(message == null ? null : message.getMessage()));
        send(agentEmail,
                "[Support] Ticket réouvert par le client — " + ticket.getTicketNumber(),
                render(ctx), ticket.getTicketNumber());
    }

    @Override
    @Async
    public void sendSlaBreachAlert(SupportTicket ticket, String breachType) {
        Context ctx = base(ticket, "SLA_BREACH");
        ctx.setVariable("breachType", breachType);
        send(managerEmail,
                "[ALERTE SLA] " + breachType + " — " + ticket.getTicketNumber(),
                render(ctx), ticket.getTicketNumber());
    }

    @Override
    @Async
    public void sendEscalationAlert(SupportTicket ticket, int level, String reason) {
        String recipient = switch (level) {
            case 1 -> {
                String agentEmail = resolveAgentEmail(ticket.getAssignedTo());
                yield StringUtils.hasText(agentEmail) ? agentEmail : managerEmail;
            }
            case 2 -> managerEmail;
            default -> adminEmail;
        };
        Context ctx = base(ticket, "SLA_ESCALATION");
        ctx.setVariable("escalationLevel", level);
        ctx.setVariable("escalationReason", reason);
        send(recipient,
                "[Escalade niveau " + level + "] " + ticket.getTicketNumber(),
                render(ctx), ticket.getTicketNumber());
    }

    @Override
    @Async
    public void sendCsatRequest(SupportTicket ticket) {
        if (!StringUtils.hasText(ticket.getContactEmail())) return;
        Context ctx = base(ticket, "CSAT_REQUEST");
        send(ticket.getContactEmail(),
                "Donnez votre avis — " + ticket.getTicketNumber(),
                render(ctx), ticket.getTicketNumber());
    }

    private Context base(SupportTicket ticket, String eventType) {
        Context ctx = new Context(Locale.FRENCH);
        ctx.setVariable("eventType", eventType);
        ctx.setVariable("contactName", ticket.getContactName());
        ctx.setVariable("ticketNumber", ticket.getTicketNumber());
        ctx.setVariable("ticketTitle", ticket.getTitle());
        ctx.setVariable("ticketPriority", ticket.getPriority() != null ? ticket.getPriority().name() : null);
        ctx.setVariable("ticketCategory", ticket.getCategory() != null ? ticket.getCategory().name() : null);
        ctx.setVariable("ticketStatus", ticket.getStatus() != null ? ticket.getStatus().name() : null);
        ctx.setVariable("createdAt", ticket.getCreatedAt() != null ? FMT.format(ticket.getCreatedAt()) : null);
        ctx.setVariable("slaDeadline", ticket.getFirstResponseDueAt() != null ? FMT.format(ticket.getFirstResponseDueAt()) : null);
        if ("TICKET_RESOLVED".equals(eventType) || "CSAT_REQUEST".equals(eventType)) {
            ctx.setVariable("csatLinks", buildCsatLinks(ticket.getTicketNumber()));
        }
        return ctx;
    }

    private Map<Integer, String> buildCsatLinks(String ticketNumber) {
        Map<Integer, String> links = new LinkedHashMap<>();
        String base = apiBaseUrl + "/sni/api/v1/public/support/csat?ticket=" + ticketNumber;
        for (int score = 1; score <= 5; score++) {
            links.put(score, base + "&score=" + score + "&token=" + csatTokenService.generate(ticketNumber, score));
        }
        return links;
    }

    private String render(Context ctx) {
        return templateEngine.process("email/support-event", ctx);
    }

    private void send(String to, String subject, String html, String ticketNumber) {
        try {
            emailSender.queueEmail(supportEmail, to, subject, html, true, "SUPPORT_TICKET", ticketNumber);
        } catch (Exception ex) {
            log.warn("Failed to queue support email to {}: {}", to, ex.getMessage());
        }
    }

    private String resolveAgentEmail(Long agentId) {
        if (agentId == null) return null;
        try {
            return userService.getUserByIdForService(agentId).getEmail();
        } catch (Exception ex) {
            log.warn("Could not resolve email for agent id {}: {}", agentId, ex.getMessage());
            return null;
        }
    }
}
