package com.sni.bokaticowork.features.task.service.implementation;

import com.sni.bokaticowork.core.communication.mailService.baseService.DefaultEmailSender;
import com.sni.bokaticowork.features.task.model.TaskItem;
import com.sni.bokaticowork.features.task.service.interfaces.TaskEmailService;
import com.sni.bokaticowork.security.admin.user.service.interfaces.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
public class TaskEmailServiceImpl implements TaskEmailService {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of("Africa/Lagos"));

    private final DefaultEmailSender emailSender;
    private final SpringTemplateEngine templateEngine;
    private final UserService userService;

    @Override
    @Async
    public void sendTaskAssigned(TaskItem task) {
        String agentEmail = resolveAgentEmail(task.getAssignedTo());
        if (!StringUtils.hasText(agentEmail)) return;
        Context ctx = base(task, "TASK_ASSIGNED");
        send(agentEmail, "[Task] Tache assignee - " + task.getTitle(), render(ctx), task);
    }

    @Override
    @Async
    public void sendTaskDueSoon(TaskItem task) {
        String agentEmail = resolveAgentEmail(task.getAssignedTo());
        if (!StringUtils.hasText(agentEmail)) return;
        Context ctx = base(task, "TASK_DUE_SOON");
        send(agentEmail, "[Task] Echeance proche - " + task.getTitle(), render(ctx), task);
    }

    @Override
    @Async
    public void sendTaskOverdue(TaskItem task) {
        String agentEmail = resolveAgentEmail(task.getAssignedTo());
        if (!StringUtils.hasText(agentEmail)) return;
        Context ctx = base(task, "TASK_OVERDUE");
        send(agentEmail, "[Task] En retard - " + task.getTitle(), render(ctx), task);
    }

    private Context base(TaskItem task, String eventType) {
        Context ctx = new Context(Locale.FRENCH);
        ctx.setVariable("eventType", eventType);
        ctx.setVariable("taskId", task.getId());
        ctx.setVariable("title", task.getTitle());
        ctx.setVariable("description", task.getDescription());
        ctx.setVariable("status", task.getStatus() != null ? task.getStatus().name() : null);
        ctx.setVariable("priority", task.getPriority() != null ? task.getPriority().name() : null);
        ctx.setVariable("dueAt", task.getDueAt() != null ? FMT.format(task.getDueAt()) : null);
        ctx.setVariable("sourceType", task.getSourceType());
        ctx.setVariable("sourceCode", task.getSourceCode());
        ctx.setVariable("recurrence", task.getRecurrence() != null ? task.getRecurrence().name() : null);
        return ctx;
    }

    private String render(Context ctx) {
        return templateEngine.process("email/task-event", ctx);
    }

    private void send(String to, String subject, String html, TaskItem task) {
        try {
            emailSender.queueEmail(to, subject, html, true, "TASK_ITEM", task.getId() == null ? null : String.valueOf(task.getId()));
        } catch (Exception ex) {
            log.warn("Failed to queue task email to {} for task {}: {}", to, task.getId(), ex.getMessage());
        }
    }

    private String resolveAgentEmail(Long agentId) {
        if (agentId == null) return null;
        try {
            return userService.getUserByIdForService(agentId).getEmail();
        } catch (Exception ex) {
            log.warn("Could not resolve task assignee email for id {}: {}", agentId, ex.getMessage());
            return null;
        }
    }
}
