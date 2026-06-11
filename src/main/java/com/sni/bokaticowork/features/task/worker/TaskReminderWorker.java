package com.sni.bokaticowork.features.task.worker;

import com.sni.bokaticowork.features.task.enums.TaskStatus;
import com.sni.bokaticowork.features.task.model.TaskItem;
import com.sni.bokaticowork.features.task.repository.TaskItemRepository;
import com.sni.bokaticowork.features.task.service.interfaces.TaskEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskReminderWorker {

    private static final List<TaskStatus> EXCLUDED_STATUSES =
            List.of(TaskStatus.COMPLETED, TaskStatus.CANCELLED, TaskStatus.ARCHIVED);

    private final TaskItemRepository taskRepository;
    private final TaskEmailService emailService;

    @Value("${bokati.task.due-soon-hours:24}")
    private long dueSoonHours;

    @Scheduled(cron = "${bokati.task.reminder-cron:0 0 * * * *}")
    @Transactional
    public void sendTaskReminders() {
        Instant now = Instant.now();
        int dueSoonCount = processDueSoon(now);
        int overdueCount = processOverdue(now);
        if (dueSoonCount > 0 || overdueCount > 0) {
            log.info("Task reminders queued: {} due soon, {} overdue", dueSoonCount, overdueCount);
        }
    }

    private int processDueSoon(Instant now) {
        Instant dueSoonUntil = now.plus(dueSoonHours, ChronoUnit.HOURS);
        List<TaskItem> tasks = taskRepository.findDueSoonReminderCandidates(now, dueSoonUntil, EXCLUDED_STATUSES);
        for (TaskItem task : tasks) {
            try {
                emailService.sendTaskDueSoon(task);
                task.setDueSoonAlertSentAt(Instant.now());
                taskRepository.save(task);
            } catch (Exception ex) {
                log.error("Could not queue due-soon reminder for task {}", task.getId(), ex);
            }
        }
        return tasks.size();
    }

    private int processOverdue(Instant now) {
        List<TaskItem> tasks = taskRepository.findOverdueReminderCandidates(now, EXCLUDED_STATUSES);
        for (TaskItem task : tasks) {
            try {
                emailService.sendTaskOverdue(task);
                task.setOverdueAlertSentAt(Instant.now());
                taskRepository.save(task);
            } catch (Exception ex) {
                log.error("Could not queue overdue reminder for task {}", task.getId(), ex);
            }
        }
        return tasks.size();
    }
}
