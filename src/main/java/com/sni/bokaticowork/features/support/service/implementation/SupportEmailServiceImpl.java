package com.sni.bokaticowork.features.support.service.implementation;

import com.sni.bokaticowork.core.communication.mailService.baseService.DefaultEmailSender;
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
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupportEmailServiceImpl implements SupportEmailService {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of("Africa/Brazzaville"));

    private final DefaultEmailSender emailSender;
    private final SpringTemplateEngine templateEngine;
    private final UserService userService;

    @Value("${bokati.support.manager-email:support@bokaticowork.com}")
    private String managerEmail;

    @Override
    @Async
    public void sendTicketCreated(SupportTicket ticket) {
        if (!StringUtils.hasText(ticket.getContactEmail())) return;
        Context ctx = base(ticket);
        ctx.setVariable("eventType", "TICKET_CREATED");
        ctx.setVariable("slaDeadline",
                ticket.getFirstResponseDueAt() != null ? FMT.format(ticket.getFirstResponseDueAt()) : null);
        send(ticket.getContactEmail(),
                "Ticket ouvert — " + ticket.getTicketNumber(),
                render(ctx));
    }

    @Override
    @Async
    public void sendAgentMessage(SupportTicket ticket, TicketMessage message) {
        if (!StringUtils.hasText(ticket.getContactEmail())) return;
        Context ctx = base(ticket);
        ctx.setVariable("eventType", "AGENT_MESSAGE");
        ctx.setVariable("messageContent", message.getMessage());
        ctx.setVariable("agentName",
                StringUtils.hasText(message.getSenderName()) ? message.getSenderName() : "Notre équipe");
        send(ticket.getContactEmail(),
                "Réponse sur votre ticket " + ticket.getTicketNumber(),
                render(ctx));
    }

    @Override
    @Async
    public void sendClientMessage(SupportTicket ticket, TicketMessage message) {
        String agentEmail = resolveAgentEmail(ticket.getAssignedTo());
        if (!StringUtils.hasText(agentEmail)) agentEmail = managerEmail;
        Context ctx = base(ticket);
        ctx.setVariable("eventType", "CLIENT_MESSAGE");
        ctx.setVariable("messageContent", message.getMessage());
        ctx.setVariable("clientName",
                StringUtils.hasText(message.getSenderName()) ? message.getSenderName() : ticket.getContactName());
        send(agentEmail,
                "[Support] Nouveau message client — " + ticket.getTicketNumber(),
                render(ctx));
    }

    @Override
    @Async
    public void sendTicketResolved(SupportTicket ticket) {
        if (!StringUtils.hasText(ticket.getContactEmail())) return;
        Context ctx = base(ticket);
        ctx.setVariable("eventType", "TICKET_RESOLVED");
        send(ticket.getContactEmail(),
                "Ticket résolu — " + ticket.getTicketNumber(),
                render(ctx));
    }

    @Override
    @Async
    public void sendSlaBreachAlert(SupportTicket ticket, String breachType) {
        Context ctx = base(ticket);
        ctx.setVariable("eventType", "SLA_BREACH");
        ctx.setVariable("breachType", breachType);
        send(managerEmail,
                "[ALERTE SLA] " + breachType + " — " + ticket.getTicketNumber(),
                render(ctx));
    }

    @Override
    @Async
    public void sendCsatRequest(SupportTicket ticket) {
        if (!StringUtils.hasText(ticket.getContactEmail())) return;
        Context ctx = base(ticket);
        ctx.setVariable("eventType", "CSAT_REQUEST");
        send(ticket.getContactEmail(),
                "Donnez votre avis — " + ticket.getTicketNumber(),
                render(ctx));
    }

    private Context base(SupportTicket ticket) {
        Context ctx = new Context(Locale.FRENCH);
        ctx.setVariable("ticketNumber", ticket.getTicketNumber());
        ctx.setVariable("ticketTitle", ticket.getTitle());
        ctx.setVariable("ticketPriority", ticket.getPriority() != null ? ticket.getPriority().name() : null);
        ctx.setVariable("ticketCategory", ticket.getCategory() != null ? ticket.getCategory().name() : null);
        ctx.setVariable("ticketStatus", ticket.getStatus() != null ? ticket.getStatus().name() : null);
        ctx.setVariable("contactName",
                StringUtils.hasText(ticket.getContactName()) ? ticket.getContactName() : "Client");
        ctx.setVariable("createdAt",
                ticket.getCreatedAt() != null ? FMT.format(ticket.getCreatedAt()) : null);
        return ctx;
    }

    private String render(Context ctx) {
        return templateEngine.process("email/support-event", ctx);
    }

    private void send(String to, String subject, String html) {
        try {
            emailSender.queueEmail(to, subject, html, true, "SUPPORT_TICKET",
                    (String) null);
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
