package com.sni.bokaticowork.features.booking.worker;

import com.sni.bokaticowork.features.booking.service.interfaces.BookingWaitlistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.sni.bokaticowork.core.settings.platform.SettingTriggers;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingWaitlistWorker implements SchedulingConfigurer {

    private static final String DELAY_KEY = "bokati.booking.waitlist.worker-delay-ms";
    private static final long DEFAULT_DELAY_MS = 300_000L;

    private SettingTriggers triggers;
    private ObjectProvider<BookingWaitlistWorker> self;

    private final BookingWaitlistService waitlistService;

    public void process() {
        try {
            int expired = waitlistService.expireOffers(100);
            int promoted = waitlistService.promoteAvailable(100);
            if (expired > 0 || promoted > 0) {
                log.info("Booking waitlist processed: expired={}, promoted={}", expired, promoted);
            }
        } catch (Exception ex) {
            log.error("BookingWaitlistWorker failed: {}", ex.getMessage(), ex);
        }
    }

    @Autowired
    void setSettingTriggers(SettingTriggers triggers) {
        this.triggers = triggers;
    }

    @Autowired
    void setSelf(ObjectProvider<BookingWaitlistWorker> self) {
        this.self = self;
    }

    /** Planification relue a chaque passage : un changement de parametre vaut au passage suivant. */
    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addTriggerTask(() -> self.getObject().process(), triggers.fixedDelay(DELAY_KEY, DEFAULT_DELAY_MS));
    }
}
