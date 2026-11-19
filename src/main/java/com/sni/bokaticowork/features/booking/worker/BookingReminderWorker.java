package com.sni.bokaticowork.features.booking.worker;

import com.sni.bokaticowork.core.settings.platform.SettingTriggers;
import com.sni.bokaticowork.features.booking.service.interfaces.BookingNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Component;

/**
 * Lot B6 (ecarts 5 et Bn-16) : rappels de reservation automatiques. Toutes les 5 minutes
 * ({@code bokati.booking.reminders.worker-delay-ms}) : prevoit les rappels des reservations
 * confirmees a venir, puis envoie ceux dont l'heure est venue. Avant, la file ne partait que sur
 * un appel manuel de {@code POST /bookings/notifications/dispatch-due}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingReminderWorker implements SchedulingConfigurer {

    private static final String DELAY_KEY = "bokati.booking.reminders.worker-delay-ms";
    private static final long DEFAULT_DELAY_MS = 300_000L;

    private final BookingNotificationService notifications;

    private SettingTriggers triggers;
    private ObjectProvider<BookingReminderWorker> self;

    public void process() {
        try {
            int queued = notifications.scheduleUpcoming(200);
            int sent = notifications.dispatchDue(100);
            if (queued > 0 || sent > 0) {
                log.info("Rappels de reservation : {} prevu(s), {} traite(s)", queued, sent);
            }
        } catch (Exception ex) {
            log.error("BookingReminderWorker : {}", ex.getMessage(), ex);
        }
    }

    @Autowired
    void setSettingTriggers(SettingTriggers triggers) {
        this.triggers = triggers;
    }

    @Autowired
    void setSelf(ObjectProvider<BookingReminderWorker> self) {
        this.self = self;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addTriggerTask(() -> self.getObject().process(), triggers.fixedDelay(DELAY_KEY, DEFAULT_DELAY_MS));
    }
}
