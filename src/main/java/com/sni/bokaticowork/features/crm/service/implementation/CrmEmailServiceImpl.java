package com.sni.bokaticowork.features.crm.service.implementation;

import com.sni.bokaticowork.core.communication.mailService.baseService.DefaultEmailSender;
import com.sni.bokaticowork.features.crm.model.Lead;
import com.sni.bokaticowork.features.crm.service.interfaces.CrmEmailService;
import com.sni.bokaticowork.security.admin.user.service.interfaces.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class CrmEmailServiceImpl implements CrmEmailService {

    private final DefaultEmailSender emailSender;
    private final SpringTemplateEngine templateEngine;
    private final UserService userService;

    @Override
    @Async
    public void sendLeadAssigned(Lead lead) {
        String agentEmail = resolveAgentEmail(lead.getAssignedTo());
        if (!StringUtils.hasText(agentEmail)) return;
        Context ctx = base(lead);
        ctx.setVariable("eventType", "LEAD_ASSIGNED");
        send(agentEmail, "Lead assigné — " + lead.getLeadNumber(), render(ctx));
    }

    @Override
    @Async
    public void sendStageChanged(Lead lead, String previousStage) {
        String agentEmail = resolveAgentEmail(lead.getAssignedTo());
        if (!StringUtils.hasText(agentEmail)) return;
        Context ctx = base(lead);
        ctx.setVariable("eventType", "STAGE_CHANGED");
        ctx.setVariable("previousStage", previousStage);
        send(agentEmail,
                "[CRM] Avancement — " + lead.getLeadNumber() + " → " + lead.getStage(),
                render(ctx));
    }

    @Override
    @Async
    public void sendDormantAlert(Lead lead) {
        String agentEmail = resolveAgentEmail(lead.getAssignedTo());
        if (!StringUtils.hasText(agentEmail)) return;
        Context ctx = base(lead);
        ctx.setVariable("eventType", "LEAD_DORMANT");
        send(agentEmail, "[CRM] Lead dormant — " + lead.getLeadNumber(), render(ctx));
    }

    private Context base(Lead lead) {
        Context ctx = new Context(Locale.FRENCH);
        ctx.setVariable("leadNumber", lead.getLeadNumber());
        ctx.setVariable("fullName", lead.getFullName());
        ctx.setVariable("company", lead.getCompany());
        ctx.setVariable("email", lead.getEmail());
        ctx.setVariable("phone", lead.getPhone());
        ctx.setVariable("stage", lead.getStage() != null ? lead.getStage().name() : null);
        ctx.setVariable("source", lead.getSource() != null ? lead.getSource().name() : null);
        ctx.setVariable("interest", lead.getInterest() != null ? lead.getInterest().name() : null);
        ctx.setVariable("estimatedAmount", lead.getEstimatedAmount());
        return ctx;
    }

    private String render(Context ctx) {
        return templateEngine.process("email/crm-event", ctx);
    }

    private void send(String to, String subject, String html) {
        try {
            emailSender.queueEmail(to, subject, html, true, "CRM_LEAD", null);
        } catch (Exception ex) {
            log.warn("Échec envoi email CRM à {}: {}", to, ex.getMessage());
        }
    }

    private String resolveAgentEmail(Long agentId) {
        if (agentId == null) return null;
        try {
            return userService.getUserByIdForService(agentId).getEmail();
        } catch (Exception ex) {
            log.warn("Email agent introuvable pour id {}: {}", agentId, ex.getMessage());
            return null;
        }
    }
}
