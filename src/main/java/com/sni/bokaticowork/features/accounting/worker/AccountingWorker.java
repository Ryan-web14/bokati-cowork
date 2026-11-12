package com.sni.bokaticowork.features.accounting.worker;

import com.sni.bokaticowork.features.accounting.model.AccEntry;
import com.sni.bokaticowork.features.accounting.model.AccPendingEntryLog;
import com.sni.bokaticowork.features.accounting.repository.AccEntryRepository;
import com.sni.bokaticowork.features.accounting.repository.AccPendingEntryLogRepository;
import com.sni.bokaticowork.features.accounting.service.implementation.AccountingReportService;
import com.sni.bokaticowork.features.accounting.service.support.AccountingNotifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import com.sni.bokaticowork.core.settings.platform.SettingTriggers;
import java.time.ZoneId;

/**
 * Traitements de nuit de la comptabilite (lot F1) : controle de continuite des numeros et
 * relances des pieces en attente d'equilibrage (J+1, J+3, J+7 puis escalade).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccountingWorker implements SchedulingConfigurer {

    private static final String NIGHT_WORKER_CRON_KEY = "accounting.night-worker-cron";

    private SettingTriggers triggers;
    private ObjectProvider<AccountingWorker> self;

    @Autowired
    void setTriggers(SettingTriggers triggers) {
        this.triggers = triggers;
    }

    @Autowired
    void setSelf(ObjectProvider<AccountingWorker> self) {
        this.self = self;
    }

    /** Planification relue a chaque passage : un changement du parametre prend effet sans redemarrage. */
    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addTriggerTask(() -> self.getObject().nightly(), triggers.cron(NIGHT_WORKER_CRON_KEY, "0 30 2 * * *", ZoneId.of("Africa/Brazzaville")));
    }

    static final List<Integer> REMINDER_DAYS = List.of(1, 3, 7);

    private final AccountingReportService reports;
    private final AccPendingEntryLogRepository pendingRepository;
    private final AccEntryRepository entryRepository;
    private final AccountingNotifier notifier;
    private final com.sni.bokaticowork.features.accounting.service.implementation.BudgetReportService budgetReports;

    public void nightly() {
        try {
            reports.checkAll();
        } catch (RuntimeException exception) {
            log.error("Comptabilite · controle de continuite de nuit en echec : {}", exception.getMessage(), exception);
        }
        try {
            remindPending();
        } catch (RuntimeException exception) {
            log.error("Comptabilite · relances des pieces en attente en echec : {}", exception.getMessage(), exception);
        }
        try {
            budgetReports.alerts(String.valueOf(java.time.LocalDate.now(java.time.ZoneId.of("Africa/Brazzaville")).getYear()));
        } catch (RuntimeException exception) {
            log.error("Comptabilite · alertes budgetaires en echec : {}", exception.getMessage(), exception);
        }
    }

    @Transactional
    public void remindPending() {
        Instant now = Instant.now();
        List<AccPendingEntryLog> open = pendingRepository.findByResolvedAtIsNullOrderByCreatedAtAsc();
        StringBuilder recap = new StringBuilder();
        for (AccPendingEntryLog log : open) {
            AccEntry entry = entryRepository.findById(log.getEntryId()).orElse(null);
            if (entry == null) {
                continue;
            }
            long days = Duration.between(log.getCreatedAt(), now).toDays();
            int done = log.getReminders() == null ? 0 : log.getReminders();
            boolean due = done < REMINDER_DAYS.size() && days >= REMINDER_DAYS.get(done);
            boolean escalate = done >= REMINDER_DAYS.size() && days > REMINDER_DAYS.get(REMINDER_DAYS.size() - 1)
                    && (log.getLastReminderAt() == null || Duration.between(log.getLastReminderAt(), now).toDays() >= 7);
            if (due || escalate) {
                notifier.notifyAccountants((escalate ? "Escalade : " : "Relance : ") + "piece en attente depuis " + days + " jours · "
                                + entry.getProvisionalNumber(),
                        "La piece " + entry.getProvisionalNumber() + " du journal " + entry.getJournalCode() + " (" + entry.getLabel()
                                + ") attend toujours son equilibrage : " + entry.getPendingDetail()
                                + (escalate ? ". Le responsable financier est sollicite." : "."),
                        entry.getProvisionalNumber(), "/accounting/entries/pending", entry.getCreatedBy());
                log.setReminders(done + 1);
                log.setLastReminderAt(now);
                pendingRepository.save(log);
            }
            recap.append(entry.getProvisionalNumber()).append(" (").append(entry.getJournalCode()).append(", ").append(days).append(" j) ; ");
        }
        if (!open.isEmpty()) {
            notifier.notifyAccountants("Recapitulatif : " + open.size() + " piece(s) en attente d'equilibrage",
                    "Pieces encore en attente : " + recap, null, "/accounting/entries/pending", null);
        }
    }
}
