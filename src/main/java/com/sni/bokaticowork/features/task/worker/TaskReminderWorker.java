package com.sni.bokaticowork.features.task.worker;

import com.sni.bokaticowork.features.task.enums.TaskStatus;
import com.sni.bokaticowork.features.task.model.TaskItem;
import com.sni.bokaticowork.features.task.repository.TaskItemRepository;
import com.sni.bokaticowork.features.task.service.interfaces.TaskEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import com.sni.bokaticowork.core.settings.platform.SettingTriggers;
import com.sni.bokaticowork.core.settings.platform.PlatformSettings;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskReminderWorker implements SchedulingConfigurer {

    private static final String REMINDER_CRON_KEY = "bokati.task.reminder-cron";
    private static final String DUE_SOON_HOURS_KEY = "bokati.task.due-soon-hours";

    private SettingTriggers triggers;
    private ObjectProvider<TaskReminderWorker> self;
    private PlatformSettings platformSettings;

    @Autowired
    void setTriggers(SettingTriggers triggers) {
        this.triggers = triggers;
    }

    @Autowired
    void setSelf(ObjectProvider<TaskReminderWorker> self) {
        this.self = self;
    }

    @Autowired(required = false)
    void setPlatformSettings(PlatformSettings platformSettings) {
        this.platformSettings = platformSettings;
    }

    /** Planification relue a chaque passage : un changement du parametre prend effet sans redemarrage. */
    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addTriggerTask(() -> self.getObject().sendTaskReminders(), triggers.cron(REMINDER_CRON_KEY, "0 0 * * * *"));
    }

    private static final List<TaskStatus> EXCLUDED_STATUSES =
            List.of(TaskStatus.COMPLETED, TaskStatus.CANCELLED, TaskStatus.ARCHIVED);

    private final TaskItemRepository taskRepository;
    private final TaskEmailService emailService;

    @Value("${bokati.task.due-soon-hours:24}")
    private long dueSoonHours;

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
        long dueSoonHours = platformSettings == null ? this.dueSoonHours : platformSettings.number(DUE_SOON_HOURS_KEY, this.dueSoonHours);
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
